# Spring REST Docs 통합 테스트 검토 보고서

> 작업일: 2026-02-28
> 검토 대상: `src/test/java/com/moya/myblogboot/` (컨트롤러 통합 테스트 전체)
> 관련 파일: `build.gradle`, `src/docs/asciidoc/`, `src/test/resources/application-test.yaml`

---

## 최종 결과

**전체 45개 테스트 통과** (AuthControllerTest 10, BoardControllerTest 17, CategoryControllerTest 9, CommentControllerTest 7, FileUploadControllerTest 2)
**REST Docs 스니펫 333개 생성 완료**

---

## 변경 이력

### 이번 세션에서 수정한 항목

| ID | 내용 | 파일 |
|----|------|------|
| FIX-1 | `pathParameters()` 스니펫 생성 실패 — `RestDocumentationRequestBuilders` URI 템플릿으로 전환 | `BoardControllerTest`, `CategoryControllerTest`, `CommentControllerTest` |
| FIX-2 | `InvalidateTokenException`, `UnauthorizedAccessException` → 500 오반환 수정 (→ 401) | `GlobalExceptionHandler` |
| FIX-3 | 예외 시나리오 테스트 추가 (총 10개) | 모든 컨트롤러 테스트 |
| FIX-4 | Testcontainers → 로컬 Redis 전환으로 Docker Desktop 연결 문제 해결 | `AbstractContainerBaseTest`, `build.gradle` |
| FIX-5 | `CategoryControllerTest.getCategoryListV2` 테스트 데이터 추가 — VIEW 게시글 없어 빈 응답 반환 | `CategoryControllerTest` |
| R-1 | Security 설정 오타 수정 (`/api/v1/comment/**` → `/api/v1/comments/**`) | `WebSecurityConfig` |

### 이전에 수정 완료된 항목 (이미 반영)

| 이전 ID | 내용 |
|---------|------|
| C-1 | `board.adoc` 스니펫 참조 오류 (`get-board-detail` → `get-board-detail-v7`) |
| C-2 | `createDocument` 복사 경로 오류 (`static/` → `static/docs/`) |
| C-3 | `AbstractContainerBaseTest` Redis 프로퍼티 경로 수정 (`spring.redis.*` → `spring.data.redis.*`) |
| C-4 | `cancelDeletedBoard` URL 슬래시 누락 수정 |
| H-1 | 각 테스트에 `requestFields`, `responseFields`, `pathParameters`, `queryParameters` 설명 추가 |
| H-2 | `RestDocsConfiguration`에 Authorization 헤더 마스킹 추가 |
| H-3 | `FileUploadControllerTest` — `@MockBean AmazonS3` 적용으로 실제 S3 호출 차단 |
| M-1 | `ObjectMapper` import를 `com.fasterxml.jackson.databind.ObjectMapper`로 교체 |
| M-5 | `GenericContainer<?>` wildcard 제네릭 추가 |

---

## 수정 상세

---

### [FIX-1] `pathParameters()` 스니펫 생성 실패 (Critical)

**원인**: Spring REST Docs의 `pathParameters()` 스니펫은 URI 경로에서 변수명을 추출해야 한다.
`MockMvcRequestBuilders.get("/api/v1/boards/123")` 처럼 변수가 이미 치환된 경로를 사용하면
REST Docs가 `boardId` 같은 파라미터 이름을 인식하지 못해 스니펫 생성이 실패한다.

**수정**: `pathParameters()` 를 사용하는 모든 테스트를 `RestDocumentationRequestBuilders` 의
URI 템플릿 방식으로 전환.

```java
// Before (스니펫 생성 실패)
String path = "/api/v7/boards/" + boardId;
mockMvc.perform(MockMvcRequestBuilders.get(path));

// After (올바른 방식)
mockMvc.perform(get("/api/v7/boards/{boardId}", boardId));
```

**적용 범위**:

| 테스트 클래스 | 수정된 메서드 |
|--------------|-------------|
| `BoardControllerTest` | `getBoardDetailV7`, `getBoardDetailForAdmin`, `editBoard`, `deleteBoard`, `cancelDeletedBoard`, `deleteBoardPermanently`, `addBoardLike`, `checkBoardLike`, `cancelBoardLike` |
| `CategoryControllerTest` | `editCategory`, `deleteCategory` |
| `CommentControllerTest` | `getComments`, `getChildComments`, `writeComment`, `editComment`, `deleteComment` |

---

### [FIX-2] `InvalidateTokenException` / `UnauthorizedAccessException` 오반환 (High)

**원인**: 두 커스텀 예외가 `GlobalExceptionHandler`의 401 핸들러에 등록되지 않아
`RuntimeException` catch-all → **500** 으로 반환되고 있었다.

```java
// AuthController.reissuingAccessToken(): 쿠키 없으면 throw InvalidateTokenException
// CommentServiceImpl.update/delete(): 권한 없으면 throw UnauthorizedAccessException
// → 모두 RuntimeException catch-all에 걸려 500 반환
```

**수정**:

```java
// GlobalExceptionHandler.java
@ExceptionHandler({AccessDeniedException.class, SignatureException.class,
        ExpiredTokenException.class, ExpiredJwtException.class,
        InvalidateTokenException.class, UnauthorizedAccessException.class})
public ResponseEntity<?> handleUnauthorizedAccessException(Exception e) {
    return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(e.getMessage());
}
```

**영향**: 쿠키 없이 `/api/v1/reissuing-token` 요청 → 500 → **401**, 권한 없는 댓글/게시글 수정·삭제 → 500 → **401**

---

### [FIX-3] 예외 시나리오 테스트 추가 (High)

각 컨트롤러 테스트에 예외 케이스를 추가했다.
`alwaysDo(restDocs)` 설정으로 예외 테스트 스니펫도 자동 생성되나 `adoc`에 포함하지 않아도 된다.

#### AuthControllerTest

| 메서드 | 시나리오 | 예상 응답 |
|--------|---------|---------|
| `loginWithWrongPassword` | 틀린 비밀번호로 로그인 → `BadCredentialsException` | 400 |
| `joinWithInvalidInput` | 짧은 아이디(2자)로 회원가입 → `MethodArgumentNotValidException` | 400 |
| `joinWithDuplicateUsername` | 이미 존재하는 아이디로 회원가입 → `DuplicateKeyException` | 409 |
| `reissuingTokenWithNoCookie` | 쿠키 없이 토큰 재발급 요청 → `InvalidateTokenException` | 401 |

#### BoardControllerTest

| 메서드 | 시나리오 | 예상 응답 |
|--------|---------|---------|
| `getBoardDetailV7NotFound` | 존재하지 않는 게시글 ID → `EntityNotFoundException` | 404 |
| `writeBoardWithoutAuth` | Authorization 헤더 없이 게시글 등록 → Spring Security | 403 |
| `writeBoardWithInvalidInput` | 빈 제목으로 게시글 등록 → `MethodArgumentNotValidException` | 400 |

#### CategoryControllerTest

| 메서드 | 시나리오 | 예상 응답 |
|--------|---------|---------|
| `newCategoryWithDuplicateName` | 중복 카테고리명 등록 → `DuplicateKeyException` | 409 |
| `editCategoryNotFound` | 존재하지 않는 카테고리 수정 → `EntityNotFoundException` | 404 |
| `deleteCategoryWithoutAuth` | Authorization 헤더 없이 삭제 → Spring Security | 403 |

#### CommentControllerTest

| 메서드 | 시나리오 | 예상 응답 |
|--------|---------|---------|
| `deleteCommentNotFound` | 존재하지 않는 댓글 삭제 → `EntityNotFoundException` | 404 |
| `writeCommentWithNonExistentBoard` | 존재하지 않는 게시글에 댓글 작성 → `EntityNotFoundException` | 404 |

---

### [FIX-4] Testcontainers → 로컬 Redis 전환 (Critical)

**원인**: Docker Desktop (WSL2 모드)의 Windows 네임드 파이프가 docker-java에 stub 응답(400, 빈 필드)을 반환해
Testcontainers가 Docker 환경을 인식하지 못했다. `com.docker.desktop.address` 라벨 리다이렉션이
Testcontainers 1.18.3에서 지원되지 않았고, 1.20.6으로 업그레이드해도 동일 문제가 발생했다.

**수정**: `AbstractContainerBaseTest`에서 Testcontainers Redis 컨테이너를 제거하고
`application-test.yaml`의 기본값(`localhost:6379`, DB 1)을 그대로 사용.
로컬 Redis(`127.0.0.1:6379`)가 이미 실행 중이므로 별도 조치 없이 동작한다.

```java
// Before: Testcontainers로 Redis 컨테이너 시작
// After: application-test.yaml 기본값 사용 (localhost:6379)
public abstract class AbstractContainerBaseTest {
}
```

`build.gradle`에서 `DOCKER_HOST` 환경변수 설정도 제거했다.

---

### [FIX-5] `getCategoryListV2` 테스트 데이터 부족 (Medium)

**원인**: `findCategoriesWithViewBoards()`는 `BoardStatus.VIEW` 게시글이 있는 카테고리만 반환하는데,
`CategoryControllerTest.before()`가 카테고리만 생성하고 게시글을 생성하지 않아 빈 배열(`[]`)이 반환되었다.
REST Docs는 빈 배열에서 필드를 문서화할 수 없어 `SnippetException`이 발생했다.

**수정**: `before()`에 `BoardRepository`를 추가하고 각 카테고리에 VIEW 게시글을 생성.

---

### [R-1 → FIX] Security 설정 오타 수정 (High)

**파일**: `WebSecurityConfig.java`

```java
// Before (오타: comment → comments)
.requestMatchers(HttpMethod.POST, "/api/v1/comment/**").hasAnyRole("NORMAL", "ADMIN")
.requestMatchers(HttpMethod.PUT, "/api/v1/comment/**").hasAnyRole("NORMAL", "ADMIN")
.requestMatchers(HttpMethod.DELETE, "/api/v1/comment/**").hasAnyRole("NORMAL", "ADMIN")

// After (수정)
.requestMatchers(HttpMethod.POST, "/api/v1/comments/**").hasAnyRole("NORMAL", "ADMIN")
.requestMatchers(HttpMethod.PUT, "/api/v1/comments/**").hasAnyRole("NORMAL", "ADMIN")
.requestMatchers(HttpMethod.DELETE, "/api/v1/comments/**").hasAnyRole("NORMAL", "ADMIN")
```

---

## 잔존 이슈

---

### [R-2] 누락된 엔드포인트 테스트 및 문서 (Low)

| 컨트롤러 | 엔드포인트 | 누락 위치 |
|----------|-----------|----------|
| `CommonController` | `GET /api/v2/visitor-count` | 테스트 파일 없음, adoc 없음 |
| `AuthController` | `POST /api/v1/password-strength-check` | `AuthControllerTest` 미테스트, `auth.adoc` 미포함 |
| `BoardController` | `GET /api/v1/boards/{boardId}/views` | `BoardControllerTest` 미테스트, `board.adoc` 미포함 |
| `BoardController` | `GET /api/v1/boards/{boardId}/likes` | `BoardControllerTest` 미테스트, `board.adoc` 미포함 |

---

### [R-3] `@BeforeEach` 메서드 실행 순서 미보장 (Low)

**파일**: 모든 컨트롤러 테스트

각 테스트 클래스에 `setUp(WebApplicationContext, RestDocumentationContextProvider)` 과
`before()` / `login()` 등 두 개 이상의 `@BeforeEach` 메서드가 존재한다.
JUnit 5는 같은 클래스 내 `@BeforeEach` 메서드의 실행 순서를 보장하지 않는다.

실제로는 `setUp` 이 먼저 실행되어 문제가 없지만, `@TestMethodOrder(MethodOrderer.OrderAnnotation.class)` 와
`@Order` 를 사용해 명시적으로 순서를 고정하거나, 단일 `@BeforeEach` 로 통합하는 것이 더 안전하다.

**수정 방향**:
```java
@BeforeEach
void setUp(WebApplicationContext webApplicationContext,
           RestDocumentationContextProvider restDocumentationContextProvider) {
    this.mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext)
            .apply(documentationConfiguration(restDocumentationContextProvider))
            .apply(springSecurity())
            .alwaysDo(restDocs)
            .build();
    // 데이터 준비 코드를 여기에 통합
    setUpTestData();
}
```

---

### [R-4] `FileUploadControllerTest` — `@DisplayName` 누락 (Low)

**파일**: `FileUploadControllerTest.java`

```java
@Test
// @DisplayName 누락
void deleteImageFile() throws Exception {
```

`deleteImageFile` 테스트에 `@DisplayName` 이 없어 테스트 리포트에서 메서드명 그대로 표시된다.

---

### [R-5] 로컬 Redis 의존 (Note)

**파일**: `AbstractContainerBaseTest`, `application-test.yaml`

Testcontainers를 제거하고 로컬 Redis를 사용하도록 변경했다.
CI/CD 환경(Jenkins 등)에서 `127.0.0.1:6379`에 Redis가 없으면 통합 테스트가 실패한다.

**대응 방향**:
- CI 서버에 Redis 설치 또는 Docker를 통한 Redis 실행 (Gradle task로 컨테이너 기동)
- 또는 Testcontainers를 복원하되 Docker 소켓을 올바르게 노출 (`--socket-path` 설정 등)

---

## 수정 우선순위 요약

| 우선순위 | ID | 내용 | 상태 |
|---------|-----|------|------|
| 완료 | FIX-1 ~ FIX-5, R-1 | 이번 세션 수정 완료 | ✅ |
| 단기 | R-2 | 누락 엔드포인트 테스트·adoc 추가 | 미완 |
| 여유 | R-3 | `@BeforeEach` 단일화 리팩터링 | 미완 |
| 여유 | R-4 | `@DisplayName` 추가 | 미완 |
| 검토 | R-5 | CI 환경 Redis 설정 확인 | 미완 |
