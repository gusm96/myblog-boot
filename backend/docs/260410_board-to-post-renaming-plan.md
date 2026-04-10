# Phase 0: Board → Post 도메인 리네이밍 — 작업 계획서

> 작성일: 2026-04-10  
> 상위 계획: `260410_seo-optimization-plan.md` Phase 0  
> 범위: 백엔드 (`-b`)

---

## 1. 작업 목표 및 배경

- `Board`는 게시판/커뮤니티 도메인 용어이며, 블로그에서는 `Post`가 업계 표준
- 이후 SEO Phase(slug URL `/posts/{slug}`, JSON-LD `BlogPosting`)에서 `Post` 기반으로 일관성 유지 필요
- 이 Phase에서는 **백엔드 코드만** 리네이밍 (프론트엔드는 별도 작업)

---

## 2. 리네이밍 매핑 테이블

### 2.1 패키지 (디렉토리)

| 현재 | 변경 |
|------|------|
| `domain.board` | `domain.post` |
| `dto.board` | `dto.post` |

### 2.2 엔티티 / 도메인

| 현재 | 변경 |
|------|------|
| `Board` | `Post` |
| `BoardStatus` | `PostStatus` |
| `ModificationStatus` | (변경 없음 — Comment 전용) |
| `SearchType` | (변경 없음 — 범용) |

### 2.3 DTO

| 현재 | 변경 |
|------|------|
| `BoardReqDto` | `PostReqDto` |
| `BoardResDto` | `PostResDto` |
| `BoardListResDto` | `PostListResDto` |
| `BoardDetailResDto` | `PostDetailResDto` |
| `BoardForRedis` | `PostForRedis` |

### 2.4 Repository

| 현재 | 변경 |
|------|------|
| `BoardRepository` | `PostRepository` |
| `BoardQuerydslRepository` | `PostQuerydslRepository` |
| `BoardQuerydslRepositoryImpl` | `PostQuerydslRepositoryImpl` |
| `BoardRedisRepository` | `PostRedisRepository` |
| `BoardRedisRepositoryImpl` | `PostRedisRepositoryImpl` |

### 2.5 Service

| 현재 | 변경 |
|------|------|
| `BoardService` | `PostService` |
| `BoardServiceImpl` | `PostServiceImpl` |
| `BoardCacheService` | `PostCacheService` |
| `BoardViewCookieService` | `PostViewCookieService` |
| `BoardViewCookieServiceImpl` | `PostViewCookieServiceImpl` |
| `BoardLikeService` | `PostLikeService` |
| `BoardLikeServiceImpl` | `PostLikeServiceImpl` |
| `BoardLikeHmacService` | `PostLikeHmacService` |
| `BoardLikeHmacServiceImpl` | `PostLikeHmacServiceImpl` |

### 2.6 Controller

| 현재 | 변경 |
|------|------|
| `BoardController` | `PostController` |
| `BoardLikeController` | `PostLikeController` |
| `DeletedBoardController` | `DeletedPostController` |

### 2.7 Scheduler

| 현재 | 변경 |
|------|------|
| `BoardScheduledTask` | `PostScheduledTask` |

### 2.8 테스트

| 현재 | 변경 |
|------|------|
| `BoardControllerTest` | `PostControllerTest` |
| `BoardRedisRepositoryTest` | `PostRedisRepositoryTest` |
| `BoardViewCountRedisRepositoryTest` | `PostViewCountRedisRepositoryTest` |
| `BoardScheduledTaskTest` | `PostScheduledTaskTest` |
| `BoardViewsServiceTest` | `PostViewsServiceTest` |
| `BoardServiceImplTest` | `PostServiceImplTest` |
| `BoardViewCookieServiceImplTest` | `PostViewCookieServiceImplTest` |

### 2.9 비-Board 파일 내부 참조 변경

| 파일 | 변경 내용 |
|------|-----------|
| `Comment.java` | `Board board` → `Post post`, `@JoinColumn(name = "board_id")` → `@JoinColumn(name = "post_id")` |
| `Category.java` | `List<Board> boards` → `List<Post> posts`, 메서드명 `removeBoards` → `removePosts`, `addBoard` → `addPost` |
| `ImageFile.java` | `Board board` → `Post post`, `@JoinColumn(name = "board_id")` → `@JoinColumn(name = "post_id")` |
| `ImageFileDto.java` | `toEntity(Board board)` → `toEntity(Post post)` |
| `CommentServiceImpl.java` | Board 참조 → Post 참조 |
| `CommentQuerydslRepositoryImpl.java` | Board 참조 → Post 참조 |
| `CommentResDto.java` | Board 관련 필드 변경 |
| `CategoryQuerydslRepositoryImpl.java` | board 참조 → post 참조 |
| `ErrorCode.java` | `BOARD_NOT_FOUND` → `POST_NOT_FOUND`, `BOARD_ACCESS_DENIED` → `POST_ACCESS_DENIED`, `DUPLICATE_BOARD_LIKE` → `DUPLICATE_POST_LIKE`, `BOARD_LIKE_NOT_FOUND` → `POST_LIKE_NOT_FOUND`, `CATEGORY_HAS_BOARDS` → `CATEGORY_HAS_POSTS` |
| `RedisKey.java` | `BOARD_KEY` → `POST_KEY`, `BOARD_VIEWS_KEY` → `POST_VIEWS_KEY` 등 |
| `CookieName.java` | `VIEWED_BOARDS` → `VIEWED_POSTS`, `LIKED_BOARDS` → `LIKED_POSTS` |
| `ShouldNotFilterPath.java` | URL 경로 `boards` → `posts` |
| `WebSecurityConfig.java` | URL 경로 `boards` → `posts`, `deleted-boards` → `deleted-posts` |

### 2.10 API 엔드포인트 URL

| 현재 | 변경 |
|------|------|
| `GET /api/v1/boards` | `GET /api/v1/posts` |
| `GET /api/v1/boards/category` | `GET /api/v1/posts/category` |
| `GET /api/v1/boards/search` | `GET /api/v1/posts/search` |
| `GET /api/v8/boards/{boardId}` | `GET /api/v8/posts/{postId}` |
| `GET /api/v1/management/boards/{boardId}` | `GET /api/v1/management/posts/{postId}` |
| `POST /api/v1/boards` | `POST /api/v1/posts` |
| `PUT /api/v1/boards/{boardId}` | `PUT /api/v1/posts/{postId}` |
| `DELETE /api/v1/boards/{boardId}` | `DELETE /api/v1/posts/{postId}` |
| `GET /api/v2/likes/{boardId}` | `GET /api/v2/likes/{postId}` |
| `POST /api/v2/likes/{boardId}` | `POST /api/v2/likes/{postId}` |
| `DELETE /api/v2/likes/{boardId}` | `DELETE /api/v2/likes/{postId}` |
| `GET /api/v1/boards/{boardId}/views` | `GET /api/v1/posts/{postId}/views` |
| `GET /api/v1/boards/{boardId}/likes` | `GET /api/v1/posts/{postId}/likes` |
| `GET /api/v1/deleted-boards` | `GET /api/v1/deleted-posts` |
| `PUT /api/v1/deleted-boards/{boardId}` | `PUT /api/v1/deleted-posts/{postId}` |
| `DELETE /api/v1/deleted-boards/{boardId}` | `DELETE /api/v1/deleted-posts/{postId}` |

### 2.11 설정 파일

| 파일 | 변경 내용 |
|------|-----------|
| `application.yaml` | `board.view.hmac.secret` → `post.view.hmac.secret`, `board.like.hmac.secret` → `post.like.hmac.secret` |
| `application-test.yaml` | 동일 |
| REST Docs (`board.adoc`) | `board.adoc` → `post.adoc` (파일명 + 내용) |

### 2.12 DB 테이블 — 변경하지 않음

> **중요 결정:** DB 테이블명(`board`)과 FK 컬럼명(`board_id`)은 **이번 Phase에서 변경하지 않는다.**
> - JPA `@Table(name = "board")`, `@JoinColumn(name = "board_id")`로 명시하여 Java 코드와 DB 스키마를 분리
> - DB 마이그레이션은 Phase 1(slug 필드 추가)과 합쳐서 한 번에 실행하여 다운타임 최소화
> - 현재 운영 중인 프론트엔드 API 호출도 고려해야 하므로, 엔드포인트 변경은 프론트엔드 작업과 동시에 반영

---

## 3. 구현 접근 방식

### Step 1: 엔티티/도메인 리네이밍
- `Board.java` → `Post.java` (패키지 `domain.post`로 이동)
- `@Table(name = "board")`, `@Column(name = "board_id")` JPA 매핑 명시 (DB 스키마 유지)
- `BoardStatus` → `PostStatus`
- `ModificationStatus`, `SearchType`은 패키지만 이동 (`domain.post`)

### Step 2: DTO 리네이밍
- 5개 DTO 클래스 파일 생성 + 패키지 `dto.post`로 이동
- 내부 Board 참조 → Post로 변경

### Step 3: Repository 리네이밍
- 5개 Repository 파일 리네이밍 + 내부 참조 변경

### Step 4: Service 리네이밍
- 9개 Service 파일 리네이밍 + 내부 참조 변경

### Step 5: Controller 리네이밍
- 3개 Controller 파일 리네이밍
- **API URL 경로는 현재 유지** (프론트엔드와 동시 변경 필요하므로)
- `@PathVariable("boardId")` → `@PathVariable("postId")` 파라미터명만 변경

### Step 6: Scheduler 리네이밍

### Step 7: 비-Board 파일 내부 참조 변경
- Comment, Category, ImageFile, ErrorCode, RedisKey, CookieName, ShouldNotFilterPath, WebSecurityConfig

### Step 8: 설정 파일 변경
- `application.yaml`, `application-test.yaml` HMAC 프로퍼티 키 변경
- HMAC 서비스의 `@Value` 어노테이션도 함께 변경

### Step 9: 테스트 코드 리네이밍
- 7개 테스트 파일 리네이밍 + 내부 참조 변경
- Board를 참조하는 비-Board 테스트 파일(CommentControllerTest 등)도 수정

### Step 10: REST Docs + QueryDSL
- `board.adoc` → `post.adoc` 리네이밍
- `./gradlew clean compileJava`로 Q클래스 재생성

---

## 4. 변경 대상 파일 목록 (총 42개)

### main 소스 (31개)
- `domain/board/` → `domain/post/`: Board.java, BoardStatus.java, ModificationStatus.java, SearchType.java (4)
- `dto/board/` → `dto/post/`: BoardReqDto, BoardResDto, BoardListResDto, BoardDetailResDto, BoardForRedis (5)
- `repository/`: BoardRepository, BoardQuerydslRepository, BoardRedisRepository (3)
- `repository/implementation/`: BoardQuerydslRepositoryImpl, BoardRedisRepositoryImpl (2)
- `service/`: BoardService, BoardCacheService, BoardViewCookieService, BoardLikeService, BoardLikeHmacService (5)
- `service/implementation/`: BoardServiceImpl, BoardViewCookieServiceImpl, BoardLikeServiceImpl, BoardLikeHmacServiceImpl (4)
- `controller/`: BoardController, BoardLikeController, DeletedBoardController (3)
- `scheduler/`: BoardScheduledTask (1)
- 비-Board 파일: Comment, Category, ImageFile, ImageFileDto, CommentServiceImpl, CommentQuerydslRepositoryImpl, CommentResDto, CategoryQuerydslRepositoryImpl, ErrorCode, RedisKey, CookieName, ShouldNotFilterPath, WebSecurityConfig (13)
- **소계: 40개**

### 설정 파일 (2개)
- `application.yaml`, `application-test.yaml`

### 테스트 소스 (9개)
- Board 테스트 7개 + CommentControllerTest, CategoryControllerTest, CommentServiceImplTest, CategoryServiceImplTest
- **소계: 11개**

### 문서 (1개)
- `board.adoc` → `post.adoc`

---

## 5. 예상 이슈 및 대응

| 이슈 | 대응 |
|------|------|
| QueryDSL Q클래스(`QBoard`) 참조 깨짐 | `clean compileJava`로 `QPost` 재생성, 모든 QueryDSL 코드에서 `QBoard` → `QPost` 변경 |
| Redis에 저장된 기존 `board:*` 키 호환 | `RedisKey` 상수값은 `"board:"`로 유지 (런타임 데이터 호환). Java 상수명만 `POST_KEY`로 변경 |
| 프론트엔드 API 호출 깨짐 | API URL 경로(`/api/v1/boards`)는 이번 Phase에서 유지 — 프론트엔드 작업 시 동시 변경 |
| `application.yaml` 프로퍼티 키 변경 시 환경변수 불일치 | `@Value` 매핑만 변경, 환경변수명(`BOARD_VIEW_HMAC_SECRET`)은 유지하되 yaml에서 매핑 |
| REST Docs 스니펫 경로 변경 | `board.adoc` → `post.adoc`, 스니펫 디렉토리명도 변경 필요 |

---

## 6. 프론트엔드 API 호환성 주의사항

> **API URL 경로는 이번 Phase에서 변경하지 않는다.**
> - 현재 프론트엔드가 `/api/v1/boards`, `/api/v8/boards/{boardId}` 등을 호출 중
> - 백엔드 Java 코드만 리네이밍하고, `@RequestMapping` URL은 기존 유지
> - 프론트엔드 리네이밍 Phase에서 URL을 `posts`로 동시 전환
