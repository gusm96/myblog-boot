# SRP 리팩토링 계획서

> 작성일: 2026-03-29
> 대상: Controller / Service 계층
> 원칙: 각 클래스가 변경되어야 하는 이유는 하나뿐이어야 한다

---

## 현황 분석

### Controller 계층

| 컨트롤러 | 엔드포인트 | 담당 도메인 | SRP 판정 |
|----------|-----------|-----------|---------|
| `CategoryController` | 6개 | 카테고리 CRUD | 준수 |
| `CommentController` | 5개 | 댓글 CRUD | 준수 |
| `FileUploadController` | 2개 | 이미지 업로드 | 준수 |
| `CommonController` | 1개 | 방문자 수 | 준수 |
| `AuthController` | 7개 | 인증 + 토큰 + 비밀번호 검증 | 경미한 위반 |
| **`BoardController`** | **20개** | **게시글 CRUD + 좋아요 + 조회수 + 삭제 관리 + Deprecated V4~V7** | **심각한 위반** |

### Service 계층

| 서비스 | public 메서드 | 담당 책임 | SRP 판정 |
|--------|-------------|----------|---------|
| `BoardViewCookieServiceImpl` | 4개 | 조회 쿠키 HMAC | 준수 |
| `VisitorHmacServiceImpl` | 3개 | 방문자 HMAC 토큰 | 준수 |
| `BoardCacheService` | 2개 | Redis 캐시 업데이트/삭제 | 준수 |
| `CategoryServiceImpl` | 7개 | 카테고리 CRUD | 준수 |
| `CommentServiceImpl` | 6개 | 댓글 CRUD | 준수 |
| `AuthServiceImpl` | 6개 | 인증 + 토큰 관리 | 경미한 위반 |
| `BoardLikeServiceImpl` | 3개 | 좋아요 + Redis 캐시 | 경미한 위반 |
| **`BoardServiceImpl`** | **15개** | **조회 + CRUD + 캐시 조회 + 이미지 + 권한 + Deprecated** | **심각한 위반** |

### 공통 문제

- `getMemberId(Principal)` — `BoardController`와 `CommentController`에 동일 메서드 중복
- `isDuplicateBoardViewCount()` — V5 전용 deprecated 메서드가 `BoardService` 인터페이스에 남아있음
- `getBoardFromCache()` — 캐시 조회 책임이 `BoardServiceImpl`에 위치 (BoardCacheService가 존재함에도)

---

## 리팩토링 계획

### Phase 1 — Deprecated 코드 삭제

가장 먼저 불필요한 코드를 제거하여 이후 작업의 복잡도를 낮춘다.

#### 1-1. Deprecated 엔드포인트 제거

**대상 (4개 메서드):**

| 메서드 | 위치 | 이유 |
|--------|------|------|
| `getBoardDetail()` V4 | `BoardController:61-64` | V8로 대체됨 |
| `getBoardDetailV5()` | `BoardController:68-81` | V8로 대체됨, `isDuplicateBoardViewCount()` 유일한 사용처 |
| `getBoardDetailV6()` | `BoardController:85-118` | V8로 대체됨, 순수 쿠키 기반 (HMAC 없음) |
| `getBoardDetailV7()` | `BoardController:122-125` | V8로 대체됨 |

#### 1-2. Deprecated 서비스 메서드 제거

| 메서드 | 위치 | 이유 |
|--------|------|------|
| `isDuplicateBoardViewCount()` | `BoardService` 인터페이스 + `BoardServiceImpl:82-91` | V5 전용, 사용처 없어짐 |

**연쇄 삭제 대상:**
- `BoardRedisRepository.isDuplicateBoardViewCount()` — 위 메서드에서만 호출
- `BoardRedisRepository.saveClientIp()` — 위 메서드에서만 호출

#### 1-3. 불필요한 import 정리

V4~V7 제거 후 `BoardController`에서 사용하지 않는 import:
- `jakarta.servlet.http.Cookie` (V8에서는 `CookieUtil`로 처리)
- `com.moya.myblogboot.utils.CookieUtil.findCookie` 유지 (V8에서 사용)

`CommentController`에서 사용하지 않는 import:
- `com.moya.myblogboot.domain.board.Board`
- `com.moya.myblogboot.domain.member.Member`
- `com.moya.myblogboot.domain.token.TokenInfo`
- `com.moya.myblogboot.service.AuthService`
- `com.moya.myblogboot.service.BoardService`
- `jakarta.servlet.http.HttpServletRequest`
- `org.springframework.http.HttpHeaders`
- `org.apache.coyote.Response`

#### Phase 1 결과

| 항목 | Before | After |
|------|--------|-------|
| `BoardController` 엔드포인트 | 20개 | **16개** (-4) |
| `BoardService` 인터페이스 메서드 | 15개 | **14개** (-1) |
| Deprecated 메서드 | 5개 | **0개** |

---

### Phase 2 — Controller 분리

`BoardController`(16개)를 도메인 책임별로 3개 컨트롤러로 분리한다.

#### 2-1. `BoardLikeController` (신규)

좋아요/조회수/좋아요수 통계를 담당하는 컨트롤러.

```
분리 대상 (5개):
├── GET    /api/v2/likes/{boardId}         — 좋아요 여부 확인
├── POST   /api/v2/likes/{boardId}         — 좋아요 추가
├── DELETE /api/v2/likes/{boardId}         — 좋아요 취소
├── GET    /api/v1/boards/{boardId}/views  — 조회수 조회
└── GET    /api/v1/boards/{boardId}/likes  — 좋아요수 조회
```

**의존성:** `BoardLikeService`, `BoardService` (조회수/좋아요수 조회용)

#### 2-2. `DeletedBoardController` (신규)

삭제된 게시글 관리(휴지통)를 담당하는 컨트롤러. 모두 관리자 전용.

```
분리 대상 (3개):
├── GET    /api/v1/deleted-boards              — 삭제 예정 목록
├── PUT    /api/v1/deleted-boards/{boardId}    — 삭제 취소 (복원)
└── DELETE /api/v1/deleted-boards/{boardId}    — 영구 삭제
```

**의존성:** `BoardService`

#### 2-3. `BoardController` (정리 후 유지)

게시글 핵심 CRUD + 조회만 담당.

```
유지 (8개):
├── GET    /api/v1/boards                         — 전체 목록
├── GET    /api/v1/boards/category                — 카테고리별 목록
├── GET    /api/v1/boards/search                  — 검색
├── GET    /api/v8/boards/{boardId}               — 게시글 상세 (현재 활성 버전)
├── GET    /api/v1/management/boards/{boardId}    — 관리자 상세
├── POST   /api/v1/boards                         — 작성
├── PUT    /api/v1/boards/{boardId}               — 수정
└── DELETE /api/v1/boards/{boardId}               — 삭제 (soft)
```

**의존성:** `BoardService`, `BoardViewCookieService` (V8 조회수 중복 방지)

> `BoardLikeService` 의존성이 제거되어 게시글 핵심 로직에만 집중할 수 있다.

#### 2-4. `PrincipalUtil` 추출

`BoardController`와 `CommentController`에 중복된 `getMemberId(Principal)` 메서드를 유틸로 추출.

```java
// 신규: com.moya.myblogboot.utils.PrincipalUtil
public final class PrincipalUtil {
    private PrincipalUtil() {}

    public static Long getMemberId(Principal principal) {
        if (principal instanceof UsernamePasswordAuthenticationToken token) {
            return (Long) token.getPrincipal();
        }
        return -1L;
    }
}
```

**적용 대상:** `BoardController`, `DeletedBoardController`, `BoardLikeController`, `CommentController` — 모두 `PrincipalUtil.getMemberId(principal)` 호출로 변경

#### Phase 2 결과

| 항목 | Before | After |
|------|--------|-------|
| `BoardController` 엔드포인트 | 16개 | **8개** |
| 컨트롤러 수 | 6개 | **8개** (+`BoardLikeController`, +`DeletedBoardController`) |
| `getMemberId()` 중복 | 2개소 | **0개** (`PrincipalUtil`로 통합) |
| `BoardController` 의존성 | 3개 | **2개** (`BoardLikeService` 제거) |

#### 테스트 영향

| 기존 테스트 | 변경 |
|------------|------|
| `BoardControllerTest` (17개) | 분리 — `BoardControllerTest`, `BoardLikeControllerTest`, `DeletedBoardControllerTest` |
| REST Docs 스니펫 | 엔드포인트 경로 변경 없으므로 스니펫 유지, 테스트 클래스만 이동 |

---

### Phase 3 — Service 분리

#### 3-1. `getBoardFromCache()` → `BoardCacheService`로 이동

현재 `BoardServiceImpl`에 있는 캐시 조회 로직을 기존 `BoardCacheService`로 이동하여 캐시 책임을 통합한다.

**이동 대상:**

| 메서드 | From | To |
|--------|------|----|
| `getBoardFromCache(Long boardId)` | `BoardServiceImpl:231-238` | `BoardCacheService` |
| `retrieveBoardAndSetRedisStore(Long boardId)` | `BoardServiceImpl:219-222` | `BoardCacheService` (private) |

**변경 후 `BoardCacheService`:**

```java
@Slf4j
@Service
@RequiredArgsConstructor
public class BoardCacheService {

    private final BoardRedisRepository boardRedisRepository;
    private final BoardRepository boardRepository;    // 신규 의존성

    // 캐시 조회 (miss 시 DB 조회 후 캐시 저장)
    public BoardForRedis getBoardFromCache(Long boardId) {
        return boardRedisRepository.findOne(boardId)
                .orElseGet(() -> retrieveAndCache(boardId));
    }

    private BoardForRedis retrieveAndCache(Long boardId) {
        Board board = boardRepository.findById(boardId)
                .orElseThrow(() -> new EntityNotFoundException(ErrorCode.BOARD_NOT_FOUND));
        return boardRedisRepository.save(board);
    }

    @Async
    public void updateBoard(BoardForRedis boardForRedis, Board board) { /* 기존 유지 */ }

    public void deleteBoard(BoardForRedis boardForRedis) { /* 기존 유지 */ }
}
```

**`BoardServiceImpl` 변경:**
- `getBoardFromCache()` 삭제 → `boardCacheService.getBoardFromCache()` 호출
- `retrieveBoardAndSetRedisStore()` 삭제
- `BoardService` 인터페이스에서 `getBoardFromCache()` 제거

**호출자 변경:**

| 호출자 | 변경 |
|--------|------|
| `BoardServiceImpl.getBoardDetail()` | `boardCacheService.getBoardFromCache(boardId)` |
| `BoardServiceImpl.getBoardDetailAndIncrementViews()` | 동일 |
| `BoardServiceImpl.edit()` | 동일 |
| `BoardServiceImpl.delete()` | 동일 |
| `BoardServiceImpl.undelete()` | 동일 |
| `BoardServiceImpl.deleteBoards()` | 동일 |
| `BoardLikeServiceImpl.addLikes()` | `boardCacheService.getBoardFromCache(boardId)` |
| `BoardLikeServiceImpl.cancelLikes()` | 동일 |

#### 3-2. `BoardLikeServiceImpl` 의존성 정리

`BoardService` 의존성을 `BoardCacheService`로 교체.

```
Before: BoardLikeServiceImpl → BoardService.getBoardFromCache()
After:  BoardLikeServiceImpl → BoardCacheService.getBoardFromCache()
```

이를 통해 `BoardLikeServiceImpl` → `BoardServiceImpl` 순환 의존 가능성을 제거한다.

#### Phase 3 결과

| 항목 | Before | After |
|------|--------|-------|
| `BoardServiceImpl` public 메서드 | 14개 | **12개** (캐시 2개 이동) |
| `BoardCacheService` public 메서드 | 2개 | **3개** (+`getBoardFromCache`) |
| `BoardServiceImpl` 의존성 | 7개 | **6개** (`BoardRedisRepository` 직접 사용 감소) |
| `BoardLikeServiceImpl` → `BoardService` 의존 | 있음 | **없음** (`BoardCacheService`로 교체) |

#### 테스트 영향

| 기존 테스트 | 변경 |
|------------|------|
| `BoardServiceImplTest` | `boardCacheService` mock 주입 추가, `getBoardFromCache` 테스트 이동 |
| `BoardLikeServiceImplTest` (존재 시) | `boardService` mock → `boardCacheService` mock 교체 |

---

## 작업 순서 요약

```
Phase 1 — Deprecated 삭제 (영향 최소, 즉시 실행 가능)
  1-1. V4~V7 엔드포인트 4개 삭제
  1-2. isDuplicateBoardViewCount() + 연쇄 삭제
  1-3. 불필요한 import 정리
  → 테스트 실행 → 커밋

Phase 2 — Controller 분리 (영향: Controller + 테스트)
  2-4. PrincipalUtil 추출
  2-1. BoardLikeController 분리
  2-2. DeletedBoardController 분리
  2-3. BoardController 정리
  → WebSecurityConfig 변경 불필요 (URL 경로 유지)
  → 테스트 분리 + 실행 → 커밋

Phase 3 — Service 분리 (영향: Service + Controller + 테스트)
  3-1. getBoardFromCache() → BoardCacheService 이동
  3-2. BoardLikeServiceImpl 의존성 정리
  → 테스트 실행 → 커밋
```

---

## 리팩토링 전후 비교

### Controller

| 항목 | Before | After |
|------|--------|-------|
| `BoardController` 엔드포인트 | 20개 | **8개** |
| 컨트롤러 수 | 6개 | **8개** |
| Deprecated 코드 | 5개 메서드 | **0개** |
| `getMemberId()` 중복 | 2개소 | **0개** |

### Service

| 항목 | Before | After |
|------|--------|-------|
| `BoardServiceImpl` public 메서드 | 15개 | **12개** |
| `BoardService` 인터페이스 메서드 | 15개 | **12개** |
| `BoardCacheService` public 메서드 | 2개 | **3개** |
| `BoardLikeServiceImpl` → `BoardService` 의존 | 있음 | **없음** |

### 신규 파일

| 파일 | 설명 |
|------|------|
| `BoardLikeController.java` | 좋아요/통계 엔드포인트 |
| `DeletedBoardController.java` | 삭제 관리 엔드포인트 |
| `PrincipalUtil.java` | Principal → memberId 추출 유틸 |

### 삭제 파일/메서드

| 대상 | 설명 |
|------|------|
| V4~V7 메서드 4개 | Deprecated 엔드포인트 |
| `isDuplicateBoardViewCount()` | V5 전용 deprecated |
| `BoardRedisRepository.isDuplicateBoardViewCount()` | 연쇄 삭제 |
| `BoardRedisRepository.saveClientIp()` | 연쇄 삭제 |
