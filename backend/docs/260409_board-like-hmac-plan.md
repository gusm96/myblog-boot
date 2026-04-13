# BoardLike HMAC 쿠키 전환 계획서

> 작성일: 2026-04-09

---

## 개요

현재 `BoardLike` 엔티티는 `Member` FK에 종속되어 있다.  
`Member` → `Admin` 전환으로 인해 일반 회원 개념이 사라지므로,  
**HMAC 서명 쿠키** 기반으로 비회원 좋아요 중복 방지 시스템으로 전환한다.

---

## 기존 시스템 vs 변경 후

| 항목 | 기존 (Member 기반) | 변경 후 (HMAC 쿠키) |
|---|---|---|
| 좋아요 저장 위치 | `board_like` 테이블 (DB) | 브라우저 쿠키 |
| 중복 방지 방식 | `board_id + member_id` FK 유니크 | HMAC 서명된 좋아요 ID 목록 |
| 인증 필요 여부 | JWT 필요 (ROLE_NORMAL) | 불필요 |
| 좋아요 취소 | DB DELETE | 쿠키에서 boardId 제거 후 재서명 |
| 좋아요 수 저장 | Redis (`board:{id}:likes`) | 동일 유지 |
| 인기 게시글 집계 | board.likes 컬럼 기준 | 동일 유지 |

---

## 쿠키 설계

### 쿠키 이름

```java
// CookieName.java에 추가
public static final String LIKED_BOARDS = "liked_boards";
```

### 쿠키 값 형식

```
{boardId1}_{boardId2}_{boardId3}|{HMAC_SIGNATURE}
```

**구분자 선택 이유:**
- `_` : boardId 구분자 — RFC 6265 허용 문자
- `|` : payload / signature 구분자 — RFC 6265 허용 문자
- `,` : RFC 6265 금지 문자 (Tomcat `IllegalArgumentException`) — **사용 불가**
- 기존 `BoardViewCookieServiceImpl`의 구분자 패턴 재활용

**예시:**
```
1_5_12_33|dGhpcyBpcyBhIHNpZ25hdHVyZQ==
```

### BoardView 쿠키와의 차이점

| | `viewed_boards` | `liked_boards` |
|---|---|---|
| 형식 | `{date}:{ids}|{sig}` | `{ids}|{sig}` |
| 날짜 prefix | ✅ 있음 (자정 만료용) | ❌ 없음 (장기 유지) |
| 만료 | 자정 (secondsUntilMidnight) | 365일 (MaxAge) |
| 삭제 지원 | ❌ (조회는 취소 없음) | ✅ 좋아요 취소 가능 |

### 쿠키 속성

`ResponseCookie` (Spring 5.3+) 사용 — `SameSite` 지원을 위해 기존 `Cookie` 대신 사용.

```java
ResponseCookie.from(CookieName.LIKED_BOARDS, newValue)
    .path("/")
    .httpOnly(true)
    .secure(true)
    .sameSite("Lax")
    .maxAge(Duration.ofDays(365))
    .build();
```

> **`HttpOnly(true)` 이유:** JS에서 쿠키 직접 접근 차단 (XSS 방어)  
> **`SameSite("Lax")` 이유:** 외부 사이트에서의 CSRF 방어 + 직접 링크 접근 허용  
> **`MaxAge(365일)` 이유:** 좋아요는 조회수와 달리 장기 유지가 자연스러움

---

## 신규 구현: BoardLikeHmacService

### 인터페이스

```java
public interface BoardLikeHmacService {

    /** 쿠키 서명 검증 */
    boolean isValid(String cookieValue);

    /** boardId가 좋아요 목록에 있는지 확인 */
    boolean isLiked(String cookieValue, Long boardId);

    /** 좋아요 추가 → 새 쿠키 값 반환 */
    String addLike(String cookieValue, Long boardId);

    /** 좋아요 취소 → 새 쿠키 값 반환 */
    String removeLike(String cookieValue, Long boardId);
}
```

### 구현체

```java
@Service
public class BoardLikeHmacServiceImpl implements BoardLikeHmacService {

    private static final String ALGORITHM = "HmacSHA256";
    private static final char SIG_DELIMITER = '|';
    private static final String ID_DELIMITER = "_";

    private final String secretKey;

    public BoardLikeHmacServiceImpl(
            @Value("${board.like.hmac.secret}") String secretKey) {
        this.secretKey = secretKey;
    }

    @Override
    public boolean isValid(String cookieValue) {
        if (cookieValue == null || cookieValue.isBlank()) return false;
        int pipeIdx = cookieValue.lastIndexOf(SIG_DELIMITER);
        if (pipeIdx <= 0) return false;

        String payload = cookieValue.substring(0, pipeIdx);
        String sig = cookieValue.substring(pipeIdx + 1);
        return sign(payload).equals(sig);
    }

    @Override
    public boolean isLiked(String cookieValue, Long boardId) {
        if (cookieValue == null) return false;
        int pipeIdx = cookieValue.lastIndexOf(SIG_DELIMITER);
        if (pipeIdx <= 0) return false;

        String payload = cookieValue.substring(0, pipeIdx);
        String target = boardId.toString();
        for (String id : payload.split(ID_DELIMITER, -1)) {
            if (id.equals(target)) return true;
        }
        return false;
    }

    @Override
    public String addLike(String cookieValue, Long boardId) {
        String newPayload;
        if (cookieValue == null || cookieValue.isBlank()) {
            newPayload = boardId.toString();
        } else {
            int pipeIdx = cookieValue.lastIndexOf(SIG_DELIMITER);
            String existingPayload = (pipeIdx > 0)
                    ? cookieValue.substring(0, pipeIdx)
                    : cookieValue;
            newPayload = existingPayload + ID_DELIMITER + boardId;
        }
        return newPayload + SIG_DELIMITER + sign(newPayload);
    }

    @Override
    public String removeLike(String cookieValue, Long boardId) {
        if (cookieValue == null) return null;
        int pipeIdx = cookieValue.lastIndexOf(SIG_DELIMITER);
        if (pipeIdx <= 0) return null;

        String payload = cookieValue.substring(0, pipeIdx);
        String target = boardId.toString();

        String newPayload = Arrays.stream(payload.split(ID_DELIMITER, -1))
                .filter(id -> !id.equals(target))
                .collect(Collectors.joining(ID_DELIMITER));

        if (newPayload.isBlank()) return null; // 남은 좋아요 없음
        return newPayload + SIG_DELIMITER + sign(newPayload);
    }

    private String sign(String data) {
        try {
            Mac mac = Mac.getInstance(ALGORITHM);
            mac.init(new SecretKeySpec(
                    secretKey.getBytes(StandardCharsets.UTF_8), ALGORITHM));
            return Base64.getEncoder().encodeToString(
                    mac.doFinal(data.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            throw new IllegalStateException("HMAC 서명 실패", e);
        }
    }
}
```

---

## API 변경

### 변경 전 (JWT 기반)

| 메서드 | 엔드포인트 | 인증 | 설명 |
|---|---|---|---|
| GET | `/api/v2/likes/{boardId}` | JWT 필요 | 좋아요 여부 확인 |
| POST | `/api/v2/likes/{boardId}` | JWT 필요 | 좋아요 추가 |
| DELETE | `/api/v2/likes/{boardId}` | JWT 필요 | 좋아요 취소 |

### 변경 후 (쿠키 기반)

| 메서드 | 엔드포인트 | 인증 | 설명 |
|---|---|---|---|
| GET | `/api/v2/likes/{boardId}` | 불필요 | 쿠키로 좋아요 여부 확인 |
| POST | `/api/v2/likes/{boardId}` | 불필요 | 좋아요 추가 + 쿠키 갱신 |
| DELETE | `/api/v2/likes/{boardId}` | 불필요 | 좋아요 취소 + 쿠키 갱신 |

### 변경 후 컨트롤러

```java
@RestController
@RequiredArgsConstructor
public class BoardLikeController {

    private final BoardLikeHmacService boardLikeHmacService;
    private final BoardLikeService boardLikeService;

    @GetMapping("/api/v2/likes/{boardId}")
    public ResponseEntity<Boolean> checkBoardLike(
            @PathVariable Long boardId,
            @CookieValue(name = CookieName.LIKED_BOARDS, required = false) String likedCookie) {

        boolean liked = boardLikeHmacService.isValid(likedCookie)
                && boardLikeHmacService.isLiked(likedCookie, boardId);
        return ResponseEntity.ok(liked);
    }

    @PostMapping("/api/v2/likes/{boardId}")
    public ResponseEntity<Long> addBoardLike(
            @PathVariable Long boardId,
            @CookieValue(name = CookieName.LIKED_BOARDS, required = false) String likedCookie,
            HttpServletResponse response) {

        // 이미 좋아요한 경우 중복 방지
        if (boardLikeHmacService.isValid(likedCookie)
                && boardLikeHmacService.isLiked(likedCookie, boardId)) {
            throw new DuplicateException(ErrorCode.DUPLICATE_BOARD_LIKE);
        }

        Long totalLikes = boardLikeService.addLikes(boardId);
        String newCookieValue = boardLikeHmacService.addLike(likedCookie, boardId);
        addLikedBoardsCookie(response, newCookieValue);
        return ResponseEntity.ok(totalLikes);
    }

    @DeleteMapping("/api/v2/likes/{boardId}")
    public ResponseEntity<Long> cancelBoardLike(
            @PathVariable Long boardId,
            @CookieValue(name = CookieName.LIKED_BOARDS, required = false) String likedCookie,
            HttpServletResponse response) {

        if (!boardLikeHmacService.isValid(likedCookie)
                || !boardLikeHmacService.isLiked(likedCookie, boardId)) {
            throw new EntityNotFoundException(ErrorCode.BOARD_LIKE_NOT_FOUND);
        }

        Long totalLikes = boardLikeService.cancelLikes(boardId);
        String newCookieValue = boardLikeHmacService.removeLike(likedCookie, boardId);

        if (newCookieValue == null) {
            // 남은 좋아요 없음 → 쿠키 삭제
            deleteLikedBoardsCookie(response);
        } else {
            addLikedBoardsCookie(response, newCookieValue);
        }
        return ResponseEntity.ok(totalLikes);
    }

    private void addLikedBoardsCookie(HttpServletResponse response, String value) {
        ResponseCookie cookie = ResponseCookie.from(CookieName.LIKED_BOARDS, value)
                .path("/")
                .httpOnly(true)
                .secure(true)
                .sameSite("Lax")
                .maxAge(Duration.ofDays(365))
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }

    private void deleteLikedBoardsCookie(HttpServletResponse response) {
        ResponseCookie cookie = ResponseCookie.from(CookieName.LIKED_BOARDS, "")
                .path("/")
                .httpOnly(true)
                .secure(true)
                .sameSite("Lax")
                .maxAge(Duration.ZERO)
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }
}
```

---

## BoardLikeService 변경

memberId 의존성 제거, 순수 Redis 카운트 연산만 수행.

```java
public interface BoardLikeService {
    Long addLikes(Long boardId);
    Long cancelLikes(Long boardId);
}
```

```java
@Service
@RequiredArgsConstructor
public class BoardLikeServiceImpl implements BoardLikeService {

    private final BoardCacheService boardCacheService;
    private final BoardRedisRepository boardRedisRepository;

    @Override
    public Long addLikes(Long boardId) {
        BoardForRedis board = boardCacheService.getBoardFromCache(boardId);
        return boardRedisRepository.incrementLikes(board).totalLikes();
    }

    @Override
    public Long cancelLikes(Long boardId) {
        BoardForRedis board = boardCacheService.getBoardFromCache(boardId);
        return boardRedisRepository.decrementLikes(board).totalLikes();
    }
}
```

---

## 환경변수 추가

**`application.yaml`:**
```yaml
board:
  view:
    hmac:
      secret: ${BOARD_VIEW_HMAC_SECRET}
  like:
    hmac:
      secret: ${BOARD_LIKE_HMAC_SECRET}   # 신규 추가
```

**`.env.prod`:**
```env
BOARD_LIKE_HMAC_SECRET=your-random-256bit-secret
```

**`application-test.yaml`:**
```yaml
board:
  like:
    hmac:
      secret: ${BOARD_LIKE_HMAC_SECRET:test-board-like-hmac-secret-key-for-testing}
```

---

## 삭제 대상

| 파일 | 이유 |
|---|---|
| `domain/board/BoardLike.java` | DB 기반 좋아요 엔티티 불필요 |
| `domain/member/MemberBoardLike.java` | Member FK 종속 |
| `repository/BoardLikeRepository.java` | DB 조회 불필요 |

### Board 엔티티에서 제거

```java
// Board.java — 제거
@OneToMany(mappedBy = "board", cascade = CascadeType.ALL, orphanRemoval = true)
private List<BoardLike> boardLikes = new ArrayList<>();
```

### Member 엔티티에서 제거 (Admin 전환 후 이미 삭제)

```java
// Member.java → 삭제됨으로 자연 해결
@OneToMany(mappedBy = "member", cascade = CascadeType.ALL, orphanRemoval = true)
private Set<BoardLike> boardLikes = new HashSet<>();
```

---

## DB 마이그레이션

```sql
DROP TABLE IF EXISTS board_like;
```

---

## 제약사항 및 고려사항

### 쿠키 크기 제한

- 브라우저 쿠키 최대 크기: 4KB
- boardId(Long) 평균 2~5자 + `_` 구분자 = 게시글당 약 4~6 bytes
- HMAC signature (Base64 SHA256) = 44 bytes
- 약 **700~900개 게시글 좋아요** 시 4KB 한도 도달
- 개인 블로그 규모에서는 현실적으로 문제 없음

### 브라우저/기기 간 좋아요 비공유

- 쿠키는 브라우저 단위 저장 → 다른 기기에서 좋아요 상태 미동기화
- 개인 블로그 특성상 허용 가능한 수준

### 인기 게시글 집계

- `board.likes` 컬럼은 Redis → DB 스케줄러 동기화로 유지됨
- 인기 게시글 기능 구현 시 `board.likes` 기준 정렬로 바로 활용 가능

---

## 구현 순서

```
Step 1. BoardLikeHmacService 인터페이스 + 구현체 작성
Step 2. application.yaml에 board.like.hmac.secret 추가
Step 3. BoardLikeService에서 memberId 의존성 제거
Step 4. BoardLikeController 전면 교체 (ResponseCookie 적용)
Step 5. CookieName.java에 LIKED_BOARDS 상수 추가
Step 6. WebSecurityConfig에서 /api/v2/likes/** 인증 규칙 제거
Step 7. Board 엔티티에서 boardLikes 컬렉션 제거
Step 8. BoardLike, MemberBoardLike, BoardLikeRepository 삭제
Step 9. DB 마이그레이션 (board_like 테이블 DROP)
Step 10. 테스트 코드 수정
```
