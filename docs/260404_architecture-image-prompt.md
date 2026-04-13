# 시스템 아키텍처 이미지 생성 프롬프트

아래 프롬프트를 이미지 생성 AI (NanoBanana, Midjourney, DALL-E 등)에 입력하세요.

---

## 전체 시스템 구성도 프롬프트

```
Create a clean, professional system architecture diagram for a full-stack blog application called "MyBlog-Boot".

Use a modern dark theme with blue and purple gradient accents. Use rounded rectangles for components, arrows with labels for connections, and colored group boxes for layers.

Layout (left to right, top to bottom):

[CLIENT LAYER - Blue group box]
- Browser icon with label "React 18 + Vite"
- Redux Toolkit box (Auth State)
- TanStack Query box (Server State Cache)
- Axios box (HTTP Client)

[CI/CD LAYER - Orange group box, top right]
- Jenkins icon
- Docker Hub icon

[SERVER LAYER - Dark gray group box, center-right]
  [SPRING BOOT 3 - Green sub-box, Port 8080]
  - JwtFilter
  - Controllers (Auth / Board / Comment / Category)
  - Services (Business Logic)
  - Scheduler (@EnableScheduling)
  - Repositories (JPA + QueryDSL + Redis)

  [MariaDB - Cyan sub-box, Port 3306]
  - Tables: Board, Member, Category, Comment, ImageFile, BoardLike, Visitor

  [Redis 7 - Red sub-box, Port 6379]
  - Board Views/Likes Cache
  - Visitor Count

  [Nginx - Gray sub-box, Port 80]
  - React Build (dist/)

Connections:
- Browser → Axios: "HTTP + Bearer Token" (blue arrow)
- Axios → JwtFilter: "REST API /api/v1/" (blue arrow)
- JwtFilter → Controllers → Services → Repositories (white arrows)
- Repositories → MariaDB: "JPA / QueryDSL" (cyan arrow)
- Repositories → Redis: "Lettuce" (red arrow)
- Scheduler → Redis: "every 10min sync" (dashed orange arrow)
- Jenkins → Docker Hub → Server: "CI/CD" (orange arrow)

Style: tech blueprint aesthetic, dark background (#0d1117), neon accent colors, clean sans-serif font, icons for each technology, professional and minimalist
```

---

## 백엔드 레이어 아키텍처 프롬프트

```
Create a clean layered architecture diagram for a Spring Boot backend application.

Style: light background, pastel color groups, clean flat design, sans-serif font.

4 horizontal layers from top to bottom:

[PRESENTATION LAYER - Light blue]
7 boxes in a row:
AuthController | BoardController | BoardLikeController | DeletedBoardController | CategoryController | CommentController | FileUploadController

[SERVICE LAYER - Light green]
10 boxes in a row:
AuthService | BoardService | BoardCacheService | BoardLikeService | BoardViewCookieService | CategoryService | CommentService | FileUploadService | VisitorCountService | VisitorHmacService

[REPOSITORY LAYER - Light purple]
3 sub-groups side by side:
  [Spring Data JPA]: BoardRepository | MemberRepository | CategoryRepository | CommentRepository | BoardLikeRepository | ImageFileRepository
  [QueryDSL]: BoardQuerydslRepository | CategoryQuerydslRepository | CommentQuerydslRepository
  [Redis]: BoardRedisRepository | VisitorCountRedisRepository

[DOMAIN LAYER - Light orange]
7 boxes: Board | Member | Category | Comment | ImageFile | BoardLike | Visitor

Thick downward arrows connecting each layer.
Professional, clean, suitable for technical documentation.
```

---

## JWT 인증 흐름 프롬프트

```
Create a professional sequence diagram showing JWT authentication flow.

Style: white background, blue and gray color scheme, clean lines, monospace font for code snippets.

Participants (left to right):
Client (browser icon) | JwtFilter | SecurityContext | Controller | AuthService

Sequence:
1. Client → Controller: POST /api/v1/login {username, password}
2. Controller → AuthService: login(dto)
3. AuthService → Controller: {access_token, refresh_token}
4. Controller → Client: access_token in body + Set-Cookie: refresh_token (HttpOnly)
   [Note on Client: Store accessToken in Redux]
5. Client → JwtFilter: GET request with Authorization: Bearer <token>
6. JwtFilter → JwtFilter: validateToken() + extractClaims()
7. JwtFilter → SecurityContext: set memberId + ROLE
8. JwtFilter → Controller: pass through
9. Client → Controller: GET /api/v1/reissuing-token with Cookie
10. Controller → AuthService: reissueAccessToken()
11. AuthService → Controller: new accessToken
12. Controller → Client: new access_token

Use colored arrows: blue for requests, green for responses, orange for internal calls.
```

---

## Redis Write-Behind 캐싱 전략 프롬프트

```
Create a professional sequence diagram showing Redis Write-Behind caching pattern for a blog application.

Style: dark background, neon green and blue accents, clean tech aesthetic.

Participants (left to right):
Client | BoardController | BoardCacheService | Redis (red) | MariaDB (blue) | Scheduler (orange)

Sequence:
1. Client → BoardController: GET /api/v8/boards/{id}
2. BoardController → BoardCacheService: getBoardFromCache(id)
3. BoardCacheService → Redis: GET board:{id}

[Cache HIT path - green]:
4a. Redis → BoardCacheService: return BoardForRedis

[Cache MISS path - yellow]:
4b. BoardCacheService → MariaDB: findById(id)
4c. MariaDB → BoardCacheService: Board entity
4d. BoardCacheService → Redis: SET board:{id}

5. BoardController → BoardCacheService: incrementViews(id)
6. BoardCacheService → Redis: INCR views (single thread)
7. BoardController → Client: BoardDetailResDto

[Scheduler - dashed orange, repeating every 10min]:
8. Scheduler → Redis: scan all board:* keys
9. Scheduler → MariaDB: sync views/likes (Write-Behind)
10. Scheduler → Redis: delete keys (eviction)

Include a performance note: "Before Redis: 789ms / 1171 TPS → After Redis: 174ms / 4882 TPS (4.5x improvement)"
```

---

## ERD 프롬프트

```
Create a clean Entity Relationship Diagram (ERD) for a blog application database.

Style: white background, light blue table headers, clean borders, professional database diagram style.

Tables and fields:

MEMBER: id(PK), username(UK), password, nickname, role(ENUM)
BOARD: id(PK), title, content(TEXT), views, likes, status(ENUM), createDate, modifyDate, deleteDate, memberId(FK), categoryId(FK)
CATEGORY: id(PK), name(UK)
COMMENT: id(PK), comment, writeDate, modStatus(ENUM), memberId(FK), boardId(FK), parentId(FK, self-ref)
BOARD_LIKE: id(PK), boardId(FK), memberId(FK)
IMAGE_FILE: id(PK), imageUrl, keyName, boardId(FK)
VISITOR: id(PK), date, count

Relationships:
- MEMBER 1--N BOARD (writes)
- MEMBER 1--N COMMENT (writes)
- MEMBER 1--N BOARD_LIKE (likes)
- CATEGORY 1--N BOARD (contains)
- BOARD 1--N COMMENT (has)
- BOARD 1--N IMAGE_FILE (has)
- BOARD 1--N BOARD_LIKE (liked by)
- COMMENT 1--N COMMENT (self-referential, replies)

Use crow's foot notation for cardinality. Color-code PK in yellow, FK in light blue, UK in light green.
```

