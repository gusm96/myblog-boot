# MyBlog-Boot 전체 아키텍처 개요

> 작성일: 2026-04-04

---

## 1. 전체 시스템 구성도

```mermaid
graph TB
    subgraph Client["클라이언트 (Browser)"]
        UI[React 18 + Vite]
        Redux["Redux Toolkit\n(Auth State)"]
        TQ["TanStack Query v5\n(Server State Cache)"]
        Axios["Axios\n(HTTP Client)"]
    end

    subgraph CI_CD["CI/CD (Jenkins)"]
        Jenkins[Jenkinsfile]
        DockerHub[Docker Hub\nseongmogu/myblog-boot]
    end

    subgraph Server["서버 (EC2 / Docker Compose)"]
        subgraph Backend["Spring Boot 3.0.4 (Port 8080)"]
            Filter["JwtFilter\n(OncePerRequestFilter)"]
            Controllers["Controllers\n(Auth/Board/Comment/Category...)"]
            Services["Services\n(Business Logic)"]
            Scheduler["Scheduler\n(@EnableScheduling)"]
            Repos["Repositories\n(JPA + QueryDSL + Redis)"]
        end

        subgraph DB["MariaDB (Port 3306)"]
            Tables["Board / Member / Category\nComment / ImageFile / BoardLike\nVisitor"]
        end

        subgraph Cache["Redis 7 (Port 6379)"]
            ViewCache["Board Views/Likes Cache"]
            VisitorCache["Visitor Count"]
        end

        subgraph Frontend_Static["Frontend (Nginx Port 80)"]
            Static["React Build (dist/)"]
        end
    end

    UI -->|HTTP Requests\nBearer Token| Axios
    Axios -->|API calls\n/api/v1/...| Filter
    Filter --> Controllers
    Controllers --> Services
    Services --> Repos
    Repos -->|JPA / QueryDSL| DB
    Repos -->|Lettuce| Cache
    Scheduler -->|Every 10min\nRedis → DB sync| Repos
    Scheduler -->|Midnight\nDelete expired boards| Repos

    Static -->|SPA routing| UI
    Jenkins -->|Build + Push| DockerHub
    DockerHub -->|Pull + Deploy| Server
```

---

## 2. 백엔드 레이어 아키텍처

```mermaid
graph LR
    subgraph Presentation["Presentation Layer"]
        AC[AuthController]
        BC[BoardController]
        BLC[BoardLikeController]
        DBC[DeletedBoardController]
        CC[CategoryController]
        CMC[CommentController]
        FUC[FileUploadController]
    end

    subgraph Service["Service Layer"]
        AS[AuthService]
        BS[BoardService]
        BCS[BoardCacheService]
        BLS[BoardLikeService]
        BVCS[BoardViewCookieService]
        CS[CategoryService]
        CMS[CommentService]
        FUS[FileUploadService]
        VCS[VisitorCountService]
        VHS[VisitorHmacService]
    end

    subgraph Repository["Repository Layer"]
        subgraph JPA["Spring Data JPA"]
            BR[BoardRepository]
            MR[MemberRepository]
            CR[CategoryRepository]
            CMR[CommentRepository]
            BLR[BoardLikeRepository]
            IFR[ImageFileRepository]
        end
        subgraph QueryDSL["QueryDSL"]
            BQR[BoardQuerydslRepository]
            CQR[CategoryQuerydslRepository]
            CMQR[CommentQuerydslRepository]
        end
        subgraph RedisRepo["Redis"]
            BRR[BoardRedisRepository]
            VCRR[VisitorCountRedisRepository]
        end
    end

    subgraph Domain["Domain / Entity"]
        Board & Member & Category & Comment & ImageFile & BoardLike & Visitor
    end

    Presentation --> Service
    Service --> Repository
    Repository --> Domain
```

---

## 3. 보안 / 인증 흐름

```mermaid
sequenceDiagram
    participant Client
    participant JwtFilter
    participant SecurityContext
    participant Controller
    participant AuthService
    participant Redis_Cookie as httpOnly Cookie

    Client->>Controller: POST /api/v1/login {username, password}
    Controller->>AuthService: login(dto)
    AuthService-->>Controller: {access_token, refresh_token}
    Controller-->>Client: access_token (body) + Set-Cookie: refresh_token

    Note over Client: Redux에 accessToken 저장

    Client->>JwtFilter: GET /api/v1/boards\nAuthorization: Bearer <accessToken>
    JwtFilter->>JwtFilter: JwtUtil.validateToken() + extractClaims()
    JwtFilter->>SecurityContext: memberId + ROLE 설정
    JwtFilter->>Controller: 요청 통과

    Client->>Controller: GET /api/v1/reissuing-token\n(Cookie: refresh_token)
    Controller->>AuthService: reissueAccessToken()
    AuthService-->>Controller: new accessToken
    Controller-->>Client: new access_token (body)
```

---

## 4. Redis 캐싱 전략 (Write-Behind 패턴)

```mermaid
sequenceDiagram
    participant Client
    participant BoardController
    participant BoardCacheService
    participant Redis
    participant DB as MariaDB
    participant Scheduler

    Client->>BoardController: GET /api/v8/boards/{id}
    BoardController->>BoardCacheService: getBoardFromCache(id)
    BoardCacheService->>Redis: GET board:{id}

    alt 캐시 HIT
        Redis-->>BoardCacheService: BoardForRedis
    else 캐시 MISS
        BoardCacheService->>DB: findById(id)
        DB-->>BoardCacheService: Board entity
        BoardCacheService->>Redis: SET board:{id}
    end

    BoardController->>BoardCacheService: incrementViews(id)
    BoardCacheService->>Redis: INCR views (단일 스레드)
    BoardController-->>Client: BoardDetailResDto

    Note over Scheduler: 매 10분마다
    Scheduler->>Redis: 모든 board:* 키 조회
    Scheduler->>DB: views/likes 동기화 (Write-Behind)
    Scheduler->>Redis: 키 삭제 (eviction)
```

---

## 5. 도메인 모델 (ERD)

```mermaid
erDiagram
    MEMBER {
        Long id PK
        String username UK
        String password
        String nickname
        Role role
    }

    BOARD {
        Long id PK
        String title
        Text content
        Long views
        Long likes
        BoardStatus status
        LocalDateTime createDate
        LocalDateTime modifyDate
        LocalDateTime deleteDate
        Long memberId FK
        Long categoryId FK
    }

    CATEGORY {
        Long id PK
        String name UK
    }

    COMMENT {
        Long id PK
        String comment
        LocalDateTime writeDate
        ModificationStatus modStatus
        Long memberId FK
        Long boardId FK
        Long parentId FK
    }

    BOARD_LIKE {
        Long id PK
        Long boardId FK
        Long memberId FK
    }

    IMAGE_FILE {
        Long id PK
        String imageUrl
        String keyName
        Long boardId FK
    }

    VISITOR {
        Long id PK
        LocalDate date
        Long count
    }

    MEMBER ||--o{ BOARD : "writes"
    MEMBER ||--o{ COMMENT : "writes"
    MEMBER ||--o{ BOARD_LIKE : "likes"
    CATEGORY ||--o{ BOARD : "contains"
    BOARD ||--o{ COMMENT : "has"
    BOARD ||--o{ IMAGE_FILE : "has"
    BOARD ||--o{ BOARD_LIKE : "liked by"
    COMMENT ||--o{ COMMENT : "replies"
```

---

## 6. 프론트엔드 아키텍처

```mermaid
graph TB
    subgraph App["App.js (Root)"]
        QP["QueryClientProvider\n(TanStack Query)"]
        PP["PersistGate\n(redux-persist)"]
        Router["React Router v7"]
    end

    subgraph Pages["screens/ (페이지)"]
        Home & SearchPage & PageByCategory
        Management & TemporaryStorage
        LoginForm & JoinForm
        NotFound
    end

    subgraph Components["components/ (컴포넌트)"]
        subgraph Board_C["Board"]
            BoardList & BoardDetail & BoardEditor & BoardEditForm & BoardLike
        end
        subgraph Comment_C["Comment"]
            CommentList & CommentForm & Comment
        end
        subgraph Layout_C["Layout"]
            ProtectedRoute & UserLayout & Header
        end
    end

    subgraph StateManagement["상태 관리"]
        subgraph Redux_S["Redux (Auth State)"]
            UserSlice["userSlice\n{isLoggedIn, accessToken}"]
            localStorage1["localStorage\n(redux-persist)"]
        end
        subgraph TQ_S["TanStack Query (Server State)"]
            QueryCache["QueryCache\n{boards, categories, comments, likes}"]
            localStorage2["localStorage\n(SyncStoragePersister, 24h)"]
        end
    end

    subgraph API["services/ (API Layer)"]
        apiClient["apiClient.js\n(Axios + Interceptors)"]
        boardApi & authApi & categoryApi
        queryKeys["queryKeys.js\n(Key Factory)"]
    end

    App --> Pages
    Pages --> Components
    Components --> StateManagement
    Components --> API
    StateManagement --> API
```

---

## 7. 스케줄러 태스크

```mermaid
graph LR
    subgraph BoardScheduledTask
        S1["deleteExpiredBoards()\n@Cron: 0 0 0 (자정)"]
        S2["updateFromRedisStoreToDB()\n@FixedRate: 600,000ms (10분)"]
    end

    subgraph VisitorCountScheduledTask
        S3["resetDailyVisitorCount()\n@Cron: 자정 리셋"]
        S4["onContextClose()\nApplicationListener<ContextClosedEvent>\n(Graceful Shutdown 시 Redis → DB 저장)"]
    end

    S1 -->|ReentrantLock| DB1[(MariaDB\n만료 게시글 영구 삭제)]
    S2 -->|ReentrantLock| Redis1[(Redis\nviews/likes 읽기)]
    Redis1 -->|Write-Behind| DB2[(MariaDB\n동기화)]
    S3 --> Redis2[(Redis\n방문자 카운트 초기화)]
    S4 --> Redis3[(Redis\n방문자 데이터 플러시)]
```

---

## 8. 예외 처리 체계

```mermaid
graph TD
    EX["BusinessException\n(RuntimeException)"]

    EX --> E1[ExpiredTokenException]
    EX --> E2[ExpiredRefreshTokenException]
    EX --> E3[InvalidateTokenException]
    EX --> E4[UnauthorizedException]
    EX --> E5[UnauthorizedAccessException]
    EX --> E6[EntityNotFoundException]
    EX --> E7[DuplicateException]
    EX --> E8[ImageUploadFailException]
    EX --> E9[ImageDeleteFailException]

    GEH["GlobalExceptionHandler\n(@RestControllerAdvice)"]
    GEH --> EX
    GEH --> StdEx["표준 예외\n(MethodArgumentNotValid\nTypeMismatch 등)"]

    EC["ErrorCode Enum\n(22개)"]
    EC --> C["Common: C001~C004"]
    EC --> A["Auth: A001~A006"]
    EC --> M["Member: M001~M002"]
    EC --> B["Board: B001~B004"]
    EC --> CM["Comment: CM001~CM002"]
    EC --> CT["Category: CT001~CT003"]
    EC --> F["File: F001~F002"]

    ER["ErrorResponse DTO\n{code, message, status}"]
    GEH -->|생성| ER
    EC -->|참조| ER
```

---

## 9. API 엔드포인트 요약

| Domain | Method | Endpoint | Auth | 설명 |
|--------|--------|----------|------|------|
| Auth | POST | `/api/v1/login` | 없음 | 로그인, 토큰 발급 |
| Auth | GET | `/api/v1/logout` | 없음 | 로그아웃, 쿠키 제거 |
| Auth | GET | `/api/v1/reissuing-token` | Cookie | Access Token 재발급 |
| Auth | GET | `/api/v1/token-validation` | 없음 | 토큰 유효성 확인 |
| Board | GET | `/api/v1/boards` | 없음 | 게시글 목록 (페이징) |
| Board | GET | `/api/v8/boards/{id}` | 없음 | 게시글 상세 (Cookie+HMAC 조회수) |
| Board | POST | `/api/v1/boards` | ADMIN | 게시글 작성 |
| Board | PUT | `/api/v1/boards/{id}` | ADMIN | 게시글 수정 |
| Board | DELETE | `/api/v1/boards/{id}` | ADMIN | 게시글 삭제 (soft) |
| Board | GET | `/api/v1/boards/search` | 없음 | 게시글 검색 |
| Board | GET | `/api/v1/boards/category` | 없음 | 카테고리별 게시글 |
| Like | GET | `/api/v2/likes/{boardId}` | USER | 좋아요 상태 조회 |
| Like | POST | `/api/v2/likes/{boardId}` | USER | 좋아요 추가 |
| Like | DELETE | `/api/v2/likes/{boardId}` | USER | 좋아요 취소 |
| Comment | GET | `/api/v1/comments/{boardId}` | 없음 | 댓글 목록 |
| Comment | POST | `/api/v1/comments` | USER | 댓글 작성 |
| Comment | PUT | `/api/v1/comments/{id}` | USER | 댓글 수정 |
| Comment | DELETE | `/api/v1/comments/{id}` | USER | 댓글 삭제 |
| Category | GET | `/api/v1/categories` | 없음 | 카테고리 목록 |
| Category | POST | `/api/v1/categories` | ADMIN | 카테고리 생성 |
| Category | PUT | `/api/v1/categories/{id}` | ADMIN | 카테고리 수정 |
| Category | DELETE | `/api/v1/categories/{id}` | ADMIN | 카테고리 삭제 |
| File | POST | `/api/v1/images` | ADMIN | 이미지 업로드 |
| File | DELETE | `/api/v1/images` | ADMIN | 이미지 삭제 |

---

## 10. 기술 스택 요약

| 영역 | 기술 | 버전 |
|------|------|------|
| Backend Framework | Spring Boot | 3.0.4 |
| Language | Java | 17 |
| ORM | Spring Data JPA + Hibernate | - |
| 동적 쿼리 | QueryDSL | 5.0 |
| DB | MariaDB | latest |
| Cache | Redis (Lettuce) | 7-alpine |
| Security | Spring Security + JWT (JJWT) | 0.12.6 |
| Build | Gradle | - |
| Test | JUnit 5 + Testcontainers | 2.0.3 |
| Frontend Framework | React | 18.2.0 |
| Bundler | Vite | 6.4.1 |
| Routing | React Router | 7.13 |
| 서버 상태 관리 | TanStack Query | 5.90 |
| 클라이언트 상태 | Redux Toolkit | 2.11.2 |
| HTTP Client | Axios | 1.13 |
| Rich Editor | TipTap | 3.20 |
| CSS-in-JS | Styled Components | 6.3 |
| CI/CD | Jenkins | - |
| Container | Docker + Docker Compose | - |
| Web Server | Nginx | - |
