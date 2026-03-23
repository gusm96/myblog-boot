# 게시글 조회수 중복 방지 — Cookie + HMAC 방식 전환 계획서

작성일: 2026-03-23

---

## 1. 현황 및 문제 진단

### 1-1. 현재 아키텍처 (V7)

```
GET /api/v7/boards/{boardId}
        ↓
UserNumCookieInterceptor   (user_n 쿠키 HMAC 검증 → userToken 주입)
        ↓
userViewedBoardService.isViewedBoard(userToken, boardId)
    └─ Redis SET: SISMEMBER "userViewedBoards:{token}" {boardId}
        ↓
false → 조회수 증가 + SADD Redis SET
true  → 조회수 유지
```

### 1-2. 버그 원인 (두 가지)

#### 버그 A: Long vs Integer 직렬화 불일치 (주범)

`redisTemplate<String, Object>`는 Jackson으로 직렬화합니다.
`Long boardId`를 SET에 저장하면, Jackson이 int 범위 내 숫자를 **Integer로 역직렬화**합니다.

```java
redisTemplate.opsForSet().add(key, 42L);       // 저장: Integer(42) 로 직렬화됨
redisTemplate.opsForSet().isMember(key, 42L);  // 비교: Long(42) ≠ Integer(42) → false!
```

이것이 테스트 코드에서도 `boardId.intValue()` 를 쓴 이유입니다.
결과: `isViewedBoard()` 가 **항상 false** → 매 요청마다 조회수 증가.

#### 버그 B: 예외 삼킴 (Fail-Open)

```java
// UserUserViewedBoardServiceImpl.java
public boolean isViewedBoard(String userToken, Long boardId) {
    try {
        return userViewedBoardRedisRepository.isExists(userToken, boardId);
    } catch (Exception e) {
        log.error(...);
        return false;  // Redis 오류 시 "미조회"로 판단 → 항상 증가
    }
}
```

Redis 연결 문제가 발생해도 조용히 `false`를 반환합니다.

### 1-3. 구조적 한계

| 문제 | 상세 |
|------|------|
| Stateful 의존 | Redis 장애 = 중복 방지 기능 전체 불능 |
| Long/Integer 타입 불일치 | Jackson 직렬화 이슈, 수정해도 잠재 버그 |
| Fail-Open 설계 | Redis 오류 → 조회수 무한 증가 |
| TTL 레이스 컨디션 | `add()` 직후 `expire()` 전 재시작 시 TTL 미설정 |
| 불필요한 Redis 키 증가 | 사용자마다 `userViewedBoards:{token}` 키 생성 |

---

## 2. 제안 방식: Cookie + HMAC Signing (Stateless)

방문자수 HMAC(`user_n` 쿠키) 방식을 조회수에 맞게 확장합니다.
**Redis 없이 클라이언트 쿠키에 "오늘 조회한 게시글 목록"을 서명과 함께 보관합니다.**

### 2-1. 쿠키 설계

```
Cookie name  : viewed_boards
Cookie value : {date}:{boardId1}_{boardId2}_...|{HMAC_SIGNATURE}
  ※ '_' 사용 이유: ','(0x2C)는 RFC 6265 금지 문자 → Tomcat IllegalArgumentException

예시 (2개 조회 후):
  2026-03-23:42_108|AbCdEfGhIjKl...==
```

| 구성 요소 | 역할 |
|-----------|------|
| `date` | KST 오늘 날짜. 자정이 지나면 서명 검증 실패 → 자동 초기화 |
| `boardId1,boardId2,...` | 오늘 조회한 게시글 ID 목록 |
| `HMAC_SIGNATURE` | 서버 비밀키로 서명. 클라이언트 위변조 방지 |

### 2-2. 요청 흐름

```
GET /api/v8/boards/{boardId}   (viewed_boards 쿠키 포함 또는 미포함)
        ↓
1. viewed_boards 쿠키를 꺼낸다
2. 서명 검증 + 날짜 확인
   ├─ 유효하지 않음 (없음 / 위변조 / 날짜 만료)
   │       → 빈 목록으로 초기화
   └─ 유효함 → 목록 파싱
        ↓
3. boardId가 목록에 포함되어 있는가?
   ├─ 포함 (중복 조회)
   │       → 조회수 증가 없이 게시글 데이터 반환
   └─ 미포함 (신규 조회)
           → 조회수 증가
           → boardId를 목록에 추가 + 재서명
           → 응답 헤더에 Set-Cookie (갱신된 쿠키)
           → 게시글 데이터 반환
```

### 2-3. 쿠키 크기 추정

- boardId 최대 7자리 + 쉼표 구분자 = 약 8 byte / 게시글
- 하루 100개 조회 시 ≈ **860 bytes** (브라우저 쿠키 제한 4KB 내 안전)
- 자정 후 자동 초기화되므로 무한 증가 없음

### 2-4. 보안 고려

| 위협 | 대응 |
|------|------|
| 클라이언트가 boardId를 임의로 추가 | HMAC 서명 검증 — 위변조 시 즉시 초기화 |
| boardId를 삭제해 조회수 재증가 유도 | 위변조이므로 HMAC 실패 → 빈 목록 = 조회수 1회만 증가 |
| 쿠키를 통째로 삭제 후 재접속 | 정상적인 "새 방문"으로 처리 — 오남용 의미 없음 |
| 날짜 조작 | HMAC 서명에 날짜 포함 → 실패 → 초기화 |

> 조회수는 보안 핵심 지표가 아니므로 쿠키 삭제 후 1회 재증가는 허용 범위입니다.
> 악의적 남용(조회수 폭탄)은 Rate Limiting으로 별도 대응해야 하며, 이 계획의 범위 밖입니다.

---

## 3. 변경 대상 파일 목록

### 3-1. 신규 생성

| 파일 | 설명 |
|------|------|
| `service/BoardViewCookieService.java` | 인터페이스: 쿠키 검증 / 포함 여부 / 갱신 |
| `service/implementation/BoardViewCookieServiceImpl.java` | HMAC 서명 구현 |

### 3-2. 수정

| 파일 | 변경 내용 |
|------|-----------|
| `controller/BoardController.java` | V8 엔드포인트 추가, `UserViewedBoardService` 의존성 제거 |
| `constants/CookieName.java` | `VIEWED_BOARDS = "viewed_boards"` 상수 추가 |
| `configuration/WebConfig.java` | 인터셉터 경로에서 `/api/v7/boards/**` 제거 (V8은 인터셉터 불필요) |
| `frontend/src/apiConfig.js` | `v7` → `v8` URL 변경 |

### 3-3. 삭제 (Redis 의존 코드 제거)

| 파일 | 사유 |
|------|------|
| `service/UserViewedBoardService.java` | 역할 완전 대체 |
| `service/implementation/UserUserViewedBoardServiceImpl.java` | 역할 완전 대체 |
| `repository/UserViewedBoardRedisRepository.java` | Redis 의존 제거 |
| `repository/implementation/UserViewedBoardRedisRepositoryImpl.java` | Redis 의존 제거 |

> **주의**: `BoardController`의 `UserViewedBoardService` 의존성도 함께 제거합니다.

### 3-4. 테스트

| 파일 | 변경 내용 |
|------|-----------|
| `test/.../BoardControllerTest.java` | V7 테스트 → V8 테스트로 교체 |
| `test/.../BoardViewCookieServiceTest.java` | 신규: 쿠키 서명/검증/파싱 단위 테스트 |

---

## 4. 신규 서비스 인터페이스 설계

```java
public interface BoardViewCookieService {

    /**
     * 쿠키 값이 유효한지 검증 (HMAC 서명 + 오늘 날짜).
     * 유효하지 않으면 false → 빈 목록으로 초기화해야 함.
     */
    boolean isValid(String cookieValue);

    /**
     * 오늘 이미 조회한 게시글인지 확인.
     * cookieValue 는 isValid() == true 인 경우에만 호출.
     */
    boolean isViewed(String cookieValue, Long boardId);

    /**
     * boardId를 목록에 추가하고 재서명한 새 쿠키 값을 반환.
     * cookieValue == null 이면 최초 생성.
     */
    String addViewed(String cookieValue, Long boardId);

    /**
     * 쿠키 Max-Age: KST 자정까지 남은 초.
     */
    int secondsUntilMidnight();
}
```

---

## 5. V8 엔드포인트 설계

```java
@GetMapping("/api/v8/boards/{boardId}")
public ResponseEntity<BoardDetailResDto> getBoardDetailV8(
        @PathVariable Long boardId,
        HttpServletRequest request,
        HttpServletResponse response) {

    Cookie cookie = CookieUtil.findCookie(request, VIEWED_BOARDS);
    String cookieValue = (cookie != null) ? cookie.getValue() : null;

    boolean valid = boardViewCookieService.isValid(cookieValue);

    if (!valid || !boardViewCookieService.isViewed(cookieValue, boardId)) {
        // 신규 조회: 조회수 증가
        BoardDetailResDto dto = boardService.getBoardDetailAndIncrementViews(boardId);
        // 쿠키 갱신 (valid하면 기존 목록 유지, invalid하면 초기화)
        String newValue = boardViewCookieService.addViewed(valid ? cookieValue : null, boardId);
        response.addCookie(CookieUtil.addCookie(
                VIEWED_BOARDS, newValue, boardViewCookieService.secondsUntilMidnight()));
        return ResponseEntity.ok(dto);
    }

    // 중복 조회: 조회수 유지
    return ResponseEntity.ok(boardService.getBoardDetail(boardId));
}
```

### 인터셉터 불필요

V8은 `UserNumCookieInterceptor`를 거치지 않습니다.
`user_n` 쿠키(방문자 토큰) 와 `viewed_boards` 쿠키(조회 목록)는 완전히 독립적입니다.

---

## 6. 환경 변수

기존 `visitor.hmac.secret`을 **재사용**하거나, 별도 키로 분리할 수 있습니다.

| 선택지 | 장점 | 단점 |
|--------|------|------|
| **`visitor.hmac.secret` 재사용** | 환경 변수 추가 불필요, 간단 | 두 기능이 같은 키 공유 |
| **`board.view.hmac.secret` 신규** | 키 목적 명확, 독립적 교체 가능 | 환경 변수 추가 필요 |

> **권장**: 별도 키(`board.view.hmac.secret`) 사용.
> 이유: 방문자 키가 노출되더라도 조회수 쿠키에 영향 없고, 키 교체 시 독립적으로 대응 가능.

---

## 7. 작업 순서

```
Step 1. BoardViewCookieService 인터페이스 + 구현체 작성 (HMAC 서명/검증)
Step 2. CookieName.java — VIEWED_BOARDS 상수 추가
Step 3. BoardController — V8 엔드포인트 추가
Step 4. WebConfig — 인터셉터 경로에서 /api/v7/boards/** 제거
Step 5. 삭제 대상 4개 파일 제거 + BoardController의 UserViewedBoardService 의존성 제거
Step 6. application.yaml(또는 .env) — board.view.hmac.secret 환경 변수 추가
Step 7. 테스트 작성 및 기존 V7 테스트 교체
Step 8. frontend/src/apiConfig.js — v7 → v8 변경
```

---

## 8. 확인 필요 사항

구현 전에 답변이 필요한 항목입니다.

1. **HMAC 키 분리 여부**
   `visitor.hmac.secret` 재사용 vs `board.view.hmac.secret` 신규 생성 중 어느 쪽을 원하시나요?

2. **V7 엔드포인트 보존 여부**
   V7을 `@Deprecated` 상태로 유지하고 V8을 추가할까요,
   아니면 V7 코드를 V8로 교체(기존 V7 삭제)할까요?

3. **application.yaml secret 적용 위치**
   `application.yaml`에 직접 추가할까요, `application-local.yaml` 등 분리된 파일에 추가할까요?
