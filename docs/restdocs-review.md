# Spring REST Docs 통합 테스트 검토 보고서

> 작업일: 2026-02-28
> 검토 대상: `src/test/java/com/moya/myblogboot/` (컨트롤러 통합 테스트 전체)
> 관련 파일: `build.gradle`, `src/docs/asciidoc/`, `src/test/resources/application-test.yaml`

---

## 요약

| 심각도 | 건수 |
|--------|------|
| **Critical** — 빌드 파손 또는 데이터 손실 | 4 |
| **High** — 문서 신뢰성/테스트 신뢰성 훼손 | 3 |
| **Medium** — 코드 품질·일관성 | 5 |
| **Low** — 문서 완성도 | 6 |

---

## Critical

---

### [C-1] `board.adoc`: 존재하지 않는 스니펫 참조 → Asciidoctor 빌드 실패

**파일**: `src/docs/asciidoc/board.adoc:15`

```adoc
=== 게시글 상세 조회
operation::board-controller-test/get-board-detail[snippets='http-request,http-response']
```

`RestDocsConfiguration`에서 설정한 식별자 패턴은 `{class-name}/{method-name}` (camelCase → kebab-case 변환).
`BoardControllerTest`에는 `getBoardDetail()`이라는 메서드가 없다.

| 테스트 메서드 | 생성되는 스니펫 식별자 |
|--------------|----------------------|
| `getBoardDetailV4()` | `board-controller-test/get-board-detail-v4` |
| `getBoardDetailV7()` | `board-controller-test/get-board-detail-v7` |
| `getBoardDetailForAdmin()` | `board-controller-test/get-board-detail-for-admin` |

`board-controller-test/get-board-detail`에 해당하는 스니펫은 **어떤 메서드도 생성하지 않는다.**
`./gradlew asciidoctor` 실행 시 해당 스니펫 디렉토리를 찾지 못해 빌드가 실패하거나
문서에 빈 섹션이 생성된다.

**수정 방향**:
- v7이 현재 사용 중인 버전이므로 `get-board-detail-v7`으로 참조 변경
- 또는 테스트 메서드명을 `getBoardDetail()`로 변경하고 v7 로직을 테스트

---

### [C-2] `build.gradle`: `createDocument` 복사 대상 경로 오류 → React `index.html` 덮어씀

**파일**: `build.gradle`

```groovy
// 현재 (잘못됨)
asciidoctor.doFirst {
    delete file('src/main/resources/static/docs')   // ← docs/ 를 삭제하지만
}
task createDocument(type: Copy) {
    dependsOn asciidoctor
    from file("build/docs/asciidoc")
    into file("src/main/resources/static")           // ← docs/ 가 아닌 루트에 복사
}
```

**문제 1**: `asciidoctor.doFirst`는 `static/docs`를 삭제하지만 실제 복사는 `static/` 루트로 이루어짐.
삭제 대상과 복사 대상이 불일치하여 `doFirst`의 정리 동작이 무의미하다.

**문제 2**: Asciidoctor가 생성하는 `index.html`(API 문서 진입점)이 React 앱의
`src/main/resources/static/index.html`을 **덮어쓴다.**
현재 `static/` 디렉토리에 API 문서 HTML(`auth.html`, `board.html` 등)이
React 정적 파일(`favicon.ico`, `manifest.json` 등)과 뒤섞여 배치되어 있다.

**수정 방향**:
```groovy
asciidoctor.doFirst {
    delete file('src/main/resources/static/docs')
}
task createDocument(type: Copy) {
    dependsOn asciidoctor
    from file("build/docs/asciidoc")
    into file("src/main/resources/static/docs")     // ← docs/ 로 통일
}
```
수정 후 API 문서는 `/docs/index.html`에서 접근 가능.

---

### [C-3] `AbstractContainerBaseTest`: Spring Boot 3.x Redis 프로퍼티 경로 불일치

**파일**: `src/test/java/com/moya/myblogboot/AbstractContainerBaseTest.java`

```java
// 현재 (Spring Boot 2.x 경로)
registry.add("spring.redis.host", REDIS_CONTAINER::getHost);
registry.add("spring.redis.port", () -> "" + REDIS_CONTAINER.getMappedPort(6379));
```

Spring Boot 3.0부터 Redis 프로퍼티가 `spring.redis.*` → `spring.data.redis.*`로 이동했다.
`application-test.yaml`은 이미 Spring Boot 3.x 경로를 사용 중이다.

```yaml
# application-test.yaml (Spring Boot 3.x 경로 사용 중)
spring:
  data:
    redis:
      host: ${REDIS_HOST:localhost}
      port: ${REDIS_PORT:6379}
```

`@DynamicPropertySource`가 `spring.redis.host`를 설정해도 애플리케이션은
`spring.data.redis.host`를 읽으므로 **Testcontainers의 Redis가 실제로 연결에 사용되지 않는다.**
`BoardControllerTest`(Redis 의존)가 로컬 Redis 실행 여부에 따라 통과 여부가 달라진다.

**수정 방향**:
```java
registry.add("spring.data.redis.host", REDIS_CONTAINER::getHost);
registry.add("spring.data.redis.port", () -> "" + REDIS_CONTAINER.getMappedPort(6379));
```

---

### [C-4] `BoardControllerTest`: `cancelDeletedBoard` 테스트 URL 오류

**파일**: `src/test/java/com/moya/myblogboot/controller/BoardControllerTest.java:272`

```java
// 현재 (슬래시 누락)
mockMvc.perform(MockMvcRequestBuilders.delete("/api/v1/boards" + boardId)
        .header(HttpHeaders.AUTHORIZATION, accessToken));
```

`/api/v1/boards` 뒤에 `/`가 없어 `/api/v1/boards123` (보드ID 직접 연결) 형태의 URL로
요청이 전송된다. 이 엔드포인트는 존재하지 않으므로 선행 삭제 요청이 항상 실패한다.
이후 복원 요청 자체는 성공하지만 실제 삭제→복원 흐름을 검증하지 못한다.
또한 생성된 스니펫은 잘못된 요청을 기록한다.

**수정 방향**:
```java
mockMvc.perform(MockMvcRequestBuilders.delete("/api/v1/boards/" + boardId)
        .header(HttpHeaders.AUTHORIZATION, accessToken));
```

---

## High

---

### [H-1] 모든 컨트롤러 테스트: 필드/파라미터 설명 없는 스니펫 생성

**파일**: 모든 컨트롤러 테스트, `RestDocsConfiguration.java`

현재 모든 테스트가 `alwaysDo(restDocs)` 설정만 사용하고
`andDo(document(..., ...descriptors))` 호출이 전혀 없다.
이 상태에서 생성되는 스니펫은 `http-request`와 `http-response` 원문만 포함하며,
쿼리 파라미터, 경로 변수, 요청/응답 필드에 대한 **설명이 전무**하다.

REST Docs의 핵심 가치는 테스트 코드를 통해 필드 설명까지 검증하는 것인데,
현재 구조는 Swagger의 단순 HTTP 캡처와 차별점이 없다.

**생성이 필요한 스니펫 종류 (예시: 게시글 목록 조회)**:

```java
// BoardControllerTest.getAllBoards() 개선 방향
resultActions
    .andExpect(status().isOk())
    .andDo(document("board-controller-test/get-all-boards",
        requestParameters(
            parameterWithName("p").description("페이지 번호 (기본값: 1)")
        ),
        responseFields(
            fieldWithPath("boardList").description("게시글 목록"),
            fieldWithPath("boardList[].id").description("게시글 ID"),
            fieldWithPath("boardList[].title").description("게시글 제목"),
            fieldWithPath("boardList[].categoryName").description("카테고리명"),
            fieldWithPath("boardList[].nickname").description("작성자 닉네임"),
            fieldWithPath("boardList[].views").description("조회수"),
            fieldWithPath("boardList[].likes").description("좋아요 수"),
            fieldWithPath("boardList[].createDate").description("작성일"),
            fieldWithPath("totalCount").description("전체 게시글 수")
        )
    ));
```

**각 테스트별 필요한 추가 스니펫**:

| 테스트 | 추가 필요 스니펫 |
|--------|----------------|
| `getAllBoards` | `requestParameters` (p) |
| `getCategoryBoards` | `requestParameters` (c, p) |
| `getSearchedBoards` | `requestParameters` (type, contents, p) |
| `getBoardDetail*` | `pathParameters` (boardId), `responseFields` |
| `writeBoard` / `editBoard` | `requestFields` (title, content, category), `responseFields` |
| `deleteBoard` / `deleteBoard*` | `pathParameters` (boardId) |
| `addBoardLike` / `cancelBoardLike` | `pathParameters` (boardId) |
| `join` / `login` | `requestFields`, `responseFields` |
| `newCategory` / `editCategory` | `requestFields`, `pathParameters` |
| `writeComment` / `editComment` | `requestFields`, `pathParameters` |

---

### [H-2] `RestDocsConfiguration`: Authorization 헤더 마스킹 없음

**파일**: `src/test/java/com/moya/myblogboot/config/RestDocsConfiguration.java`

```java
// 현재
MockMvcRestDocumentation.document(
    "{class-name}/{method-name}",
    preprocessRequest(prettyPrint()),
    preprocessResponse(prettyPrint())
);
```

인증이 필요한 API 테스트에서 실제 JWT 토큰 값이 스니펫의 `http-request`에 그대로 노출된다.
또한 Host가 `localhost`로 기록되어 실제 서비스 URL과 다른 문서가 생성된다.

**수정 방향**:
```java
MockMvcRestDocumentation.document(
    "{class-name}/{method-name}",
    preprocessRequest(
        prettyPrint(),
        modifyHeaders().remove("Authorization").add("Authorization", "Bearer {access_token}")
    ),
    preprocessResponse(prettyPrint()),
    preprocessRequest(
        modifyUris()
            .scheme("https")
            .host("api.myblog.com")
            .removePort()
    )
);
```

---

### [H-3] `FileUploadControllerTest`: 실제 AWS S3 호출 가능성

**파일**: `src/test/java/com/moya/myblogboot/controller/FileUploadControllerTest.java`

`deleteImageFile` 테스트는 내부에서 `uploadImageFile`을 먼저 실행한 뒤
응답값을 파싱하여 삭제 요청에 사용한다.

```java
// 현재 (업로드 응답을 파싱하여 삭제에 사용)
String contentAsString = mockMvc.perform(MockMvcRequestBuilders.multipart(...)
        .file(multipartFile)
        .header(HttpHeaders.AUTHORIZATION, accessToken))
    .andReturn().getResponse().getContentAsString();
ImageFileDto imageFileDto = objectMapper.readValue(contentAsString, ImageFileDto.class);
```

`application-test.yaml`에 더미 AWS 자격증명이 설정되어 있지만,
`spring-cloud-starter-aws`는 자격증명 설정과 무관하게 실제 S3 엔드포인트에 연결을 시도할 수 있다.
CI 환경에서 인터넷 연결이 제한되면 테스트 자체가 타임아웃으로 실패한다.

**두 번째 문제**: 두 테스트 간에 암묵적인 실행 순서 의존성이 존재한다.
`deleteImageFile`이 `uploadImageFile`의 성공을 전제로 설계되어 있어,
업로드가 실패하면 삭제 테스트도 의미 없는 결과를 낸다.

**수정 방향**:
- `AmazonS3` 빈을 `@MockBean`으로 교체하여 실제 S3 호출 차단
- 업로드와 삭제를 독립적인 `@BeforeEach` 데이터 준비로 분리

```java
@MockBean
private AmazonS3 amazonS3;

@BeforeEach
void mockS3() {
    PutObjectResult result = new PutObjectResult();
    given(amazonS3.putObject(any())).willReturn(result);
    // ...
}
```

---

## Medium

---

### [M-1] 모든 컨트롤러 테스트: Testcontainers 내부 `ObjectMapper` 사용

**파일**: `BoardControllerTest`, `AuthControllerTest`, `CategoryControllerTest`,
`CommentControllerTest`, `FileUploadControllerTest`

```java
// 현재 (내부 API, 사용 금지)
import org.testcontainers.shaded.com.fasterxml.jackson.databind.ObjectMapper;
```

`org.testcontainers.shaded.*`는 Testcontainers 라이브러리가 의존성 충돌을 피하기 위해
내부에 포함한 Shadow 패키지다. 외부에서 직접 사용하면 안 되며,
Jackson 버전이 Testcontainers에 의해 결정되어 프로젝트의 Jackson 설정과 불일치가 발생할 수 있다.

**수정 방향**:
```java
// 올바른 import
import com.fasterxml.jackson.databind.ObjectMapper;

// 또는 Spring의 ObjectMapper 빈을 주입
@Autowired
private ObjectMapper objectMapper;
```

---

### [M-2] 모든 컨트롤러 테스트: `static` 공유 필드

**파일**: `BoardControllerTest`, `CategoryControllerTest`, `CommentControllerTest`, `FileUploadControllerTest`

```java
private static Long boardId;
private static String accessToken;
```

JUnit 5에서 `@BeforeEach`는 테스트마다 새 인스턴스를 생성하므로
`static` 필드를 굳이 사용할 이유가 없다. `static` 필드는 테스트 클래스 간 상태 공유를
의도치 않게 유발할 수 있다. (특히 병렬 실행 환경)

**수정 방향**: `static` 제거 후 인스턴스 필드로 변경.

---

### [M-3] `AuthControllerTest`, `CategoryControllerTest`, `CommentControllerTest`: `AbstractContainerBaseTest` 미상속

**파일**: 위 세 컨트롤러 테스트

`BoardControllerTest`는 `AbstractContainerBaseTest`를 상속하여 Redis Testcontainer를 사용한다.
나머지 컨트롤러 테스트는 상속하지 않아 로컬/CI 환경의 Redis 실행 여부에 의존한다.

인증(로그인, 토큰 재발급)과 댓글 테스트는 내부적으로 Redis를 사용하는
서비스(토큰 저장, 조회수 캐시 등)를 간접 호출할 수 있어
Redis 연결이 없으면 예상치 못한 위치에서 실패할 수 있다.

**수정 방향**: C-3 수정 후 모든 컨트롤러 테스트가 `AbstractContainerBaseTest`를 상속하도록 통일.

---

### [M-4] `CategoryControllerTest`: 미사용 import

**파일**: `src/test/java/com/moya/myblogboot/controller/CategoryControllerTest.java`

```java
import org.junit.Before;                          // JUnit 4 어노테이션, 미사용
import static org.junit.jupiter.api.Assertions.*; // 미사용
```

JUnit 4의 `@Before`와 JUnit 5의 `@BeforeEach`를 혼동한 흔적.
현재 코드에는 `@Before`가 사용되지 않으며 `Assertions.*` 정적 임포트도 미사용.

---

### [M-5] `AbstractContainerBaseTest`: `GenericContainer` Raw Type 사용

**파일**: `src/test/java/com/moya/myblogboot/AbstractContainerBaseTest.java`

```java
// 현재 (컴파일 경고)
static final GenericContainer REDIS_CONTAINER;
REDIS_CONTAINER = new GenericContainer<>(REDIS_IMAGE)
```

**수정 방향**:
```java
static final GenericContainer<?> REDIS_CONTAINER;
```

---

## Low

---

### [L-1] 문서화 누락 엔드포인트

다음 엔드포인트가 컨트롤러에 존재하지만 adoc 파일과 테스트에서 누락되어 있다.

| 컨트롤러 | 엔드포인트 | 누락 위치 |
|----------|-----------|----------|
| `CommonController` | `GET /api/v2/visitor-count` | 테스트 파일 없음, adoc 없음 |
| `AuthController` | `POST /api/v1/password-strength-check` | `AuthControllerTest` 미테스트, `auth.adoc` 미포함 |
| `BoardController` | `GET /api/v1/boards/{boardId}/views` | `BoardControllerTest` 미테스트, `board.adoc` 미포함 |
| `BoardController` | `GET /api/v1/boards/{boardId}/likes` | `BoardControllerTest` 미테스트, `board.adoc` 미포함 |

---

### [L-2] `board.adoc`: Deprecated 엔드포인트 문서화

**파일**: `src/docs/asciidoc/board.adoc:6`, `BoardControllerTest.java:162`

RFC-BE-002에서 v4~v6 엔드포인트에 `@Deprecated` 처리를 완료했음에도
`BoardControllerTest`에 `getBoardDetailV4()` 테스트가 남아 있다.
해당 테스트가 생성하는 스니펫(`get-board-detail-v4`)은 `board.adoc`에도 없어
실질적으로 의미 없는 스니펫만 생성한다.

**수정 방향**: `getBoardDetailV4()`, `getBoardDetailV7()` 중 현재 사용 중인
v7 테스트 하나만 유지하고 `board.adoc`에 반영.

---

### [L-3] `FileUploadControllerTest`: `deleteImageFile` 테스트에 `@DisplayName` 누락

**파일**: `src/test/java/com/moya/myblogboot/controller/FileUploadControllerTest.java:109`

```java
@Test
// @DisplayName 없음
void deleteImageFile() throws Exception {
```

생성된 스니펫 식별자는 메서드명에서 파생되지만 테스트 리포트에서 의미 있는 이름이 표시되려면
`@DisplayName`이 필요하다. 다른 테스트들과 일관성을 위해 추가 권장.

---

### [L-4] `index.adoc`: 문서 메타 정보 부족

**파일**: `src/docs/asciidoc/index.adoc`

```adoc
= Myblog Application API Document
:toc: left
```

현재 API 버전, 서버 기본 URL, 공통 요청 헤더(`Authorization`, `Content-Type`) 설명,
공통 에러 응답 포맷 등이 없다. REST Docs를 통해 생성된 문서이지만
문서로서의 실용성이 낮다.

**추가 권장 내용**:
```adoc
= Myblog Application API Document
:doctype: book
:source-highlighter: highlightjs
:toc: left
:toclevels: 2
:sectlinks:

== 개요

=== 서버 정보
- 프로덕션: `https://api.myblog.com`
- 개발: `http://localhost:8080`

=== 인증
모든 인증이 필요한 API는 `Authorization` 헤더에 `Bearer {access_token}` 형식으로
Access Token을 포함해야 합니다.

=== 공통 에러 응답
...
```

---

### [L-5] `RestDocsConfiguration`: URI 정규화 없음

**파일**: `src/test/java/com/moya/myblogboot/config/RestDocsConfiguration.java`

스니펫의 `http-request`에 `Host: localhost`와 포트 정보가 포함된다.
실제 서비스 도메인과 다른 URL이 문서에 기록되어 오해를 유발할 수 있다.

**수정 방향**: `preprocessRequest`에 `modifyUris()` 추가 (H-2 참조).

---

### [L-6] `application-test.yaml`: `spring.jpa.hibernate.ddl-auto` 명시 없음

**파일**: `src/test/resources/application-test.yaml`

```yaml
spring:
  jpa:
    database: h2
    generate-ddl: off   # DDL 자동 생성 OFF
```

`generate-ddl: off`는 있지만 `ddl-auto`가 명시적으로 설정되지 않았다.
H2 임베디드 DB 사용 시 Spring Boot 기본값인 `create-drop`이 적용되어
테스트 실행마다 스키마가 생성/삭제된다.
`sql.init.mode: always`로 SQL 초기화를 활성화했으나 `schema.sql` / `data.sql`이
존재하지 않아 실질적인 효과가 없다.

**수정 방향**:
```yaml
spring:
  jpa:
    hibernate:
      ddl-auto: create-drop   # 명시적으로 선언
    generate-ddl: true        # ddl-auto와 일관성 맞추기
  sql:
    init:
      mode: never             # schema.sql이 없으면 never로 변경
```

---

## 수정 우선순위 요약

| 우선순위 | ID | 내용 |
|---------|-----|------|
| 즉시 | C-1 | `board.adoc` 스니펫 참조 오류 수정 |
| 즉시 | C-2 | `createDocument` 복사 경로 수정 (`static/docs/`) |
| 즉시 | C-3 | `AbstractContainerBaseTest` Redis 프로퍼티 경로 수정 |
| 즉시 | C-4 | `cancelDeletedBoard` URL 슬래시 추가 |
| 단기 | H-1 | 각 테스트에 Field/Parameter 설명 추가 |
| 단기 | H-2 | `RestDocsConfiguration`에 헤더 마스킹 + URI 정규화 추가 |
| 단기 | H-3 | `FileUploadControllerTest` S3 Mock 처리 |
| 단기 | M-1 | `ObjectMapper` import 교체 |
| 단기 | M-2 | `static` 필드 → 인스턴스 필드 변경 |
| 단기 | M-3 | 컨트롤러 테스트 `AbstractContainerBaseTest` 상속 통일 |
| 여유 | L-1 | 누락 엔드포인트 테스트 및 adoc 추가 |
| 여유 | L-4 | `index.adoc` 메타 정보 보강 |
