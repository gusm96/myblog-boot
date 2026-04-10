# 백엔드 배포 전 점검 보고서

> 점검일: 2026-04-08  
> 대상: Spring Boot 3.0.4 / Java 17 / MariaDB / Redis

---

## 점검 결과 요약

| 등급 | 항목 수 | 내용 |
|---|---|---|
| 🔴 즉시 수정 | 3 | 배포 전 반드시 해결 |
| 🟡 개선 권장 | 5 | 배포 후 단기 개선 |
| 🟢 양호 | 다수 | 잘 설계된 항목 |

---

## 1. 보안

### 1-1. 입력 검증 — @Pattern 누락 🔴

**파일:** `dto/request/MemberJoinReqDto.java`

현재 `username`, `password`에 `@Size`만 적용되어 있고 형식 검증이 없다.  
예를 들어 `username`에 특수문자가 포함되거나, `password`가 숫자만으로 구성되어도 통과된다.

```java
// 현재
@NotBlank
@Size(min = 6, max = 20)
private String username;

@NotBlank
@Size(min = 8, max = 16)
private String password;
```

```java
// 수정 후
@NotBlank
@Pattern(regexp = "^[a-z0-9]{6,20}$",
         message = "아이디는 6-20자의 영문 소문자, 숫자만 입력하세요.")
private String username;

@NotBlank
@Pattern(regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&]).{8,16}$",
         message = "비밀번호는 8-16자 영문 대/소문자, 숫자, 특수문자를 조합하여 입력하세요.")
private String password;
```

---

### 1-2. 보안 응답 헤더 미설정 🟡

**파일:** `configuration/WebSecurityConfig.java`

현재 Spring Security 기본 헤더만 적용된다. 브라우저 보안 정책을 명시적으로 강화할 수 있다.

```java
// WebSecurityConfig.java filterChain() 내 추가
.headers(headers -> headers
    .frameOptions(HeadersConfigurer.FrameOptionsConfig::deny)
    .contentTypeOptions(withDefaults())
    .httpStrictTransportSecurity(hsts -> hsts
        .maxAgeInSeconds(31536000)
        .includeSubDomains(true)
    )
)
```

---

### 1-3. Refresh Token 쿠키 — SameSite 속성 누락 🟡

**파일:** `controller/AuthController.java`

현재 refresh_token 쿠키에 `HttpOnly`는 설정되어 있으나, `SameSite=Strict` 속성이 없다.  
CSRF 공격에 대한 추가 방어층으로 설정을 권장한다.

```java
// 현재 (ResponseCookie 사용 시 확인 필요)
ResponseCookie.from("refresh_token_key", refreshToken)
    .httpOnly(true)
    .path("/")
    // .sameSite("Strict")  ← 누락
    .build();
```

```java
// 수정 후
ResponseCookie.from("refresh_token_key", refreshToken)
    .httpOnly(true)
    .secure(true)         // HTTPS 환경
    .sameSite("Strict")
    .path("/")
    .build();
```

---

### 1-4. AWS 자격증명 더미값 확인 필요 🔴

**파일:** `src/main/resources/application.yaml` (34-46줄)

```yaml
cloud:
  aws:
    credentials:
      access-key: ${AWS_CREDENTIALS_ACCESS_KEY:dummy-access-key}
      secret-key: ${AWS_CREDENTIALS_SECRET_KEY:dummy-secret-key}
```

주석에 "S3 버킷 삭제됨"으로 명시되어 있다.  
`FileUploadController`의 S3 업로드/삭제 엔드포인트(`/api/v1/images`)가 현재도 노출되어 있어,  
배포 환경에서 해당 API 호출 시 오류가 발생한다.

**배포 전 결정 필요:**
- S3를 사용하지 않는다면 `FileUploadController`, `FileUploadService`, S3Config 및 관련 코드 제거
- S3를 재사용한다면 버킷 재생성 후 실제 자격증명으로 교체

---

### 1-5. CORS 설정 — 양호 🟢

**파일:** `configuration/WebSecurityConfig.java` (67-76줄)

- `allowedOrigins`를 환경변수(`${CORS_ALLOWED_ORIGINS}`)로 관리
- `allowCredentials(true)` 설정 시 와일드카드 미사용 — 올바름
- 허용 메서드: GET, POST, PUT, DELETE 명시

---

### 1-6. JWT 설정 — 양호 🟢

- JJWT 0.12.6 (최신), HMAC-SHA256 서명
- Access Token: 10분, Refresh Token: 14일
- 만료/위조 예외 세분화 처리 (`JwtFilter.java`)
- 필터 제외 경로 중앙 관리 (`ShouldNotFilterPath.java`)

---

### 1-7. 권한 설정 — 양호 🟢

**파일:** `configuration/WebSecurityConfig.java` (39-56줄)

- ADMIN 전용: 게시글 CUD, 카테고리 CUD, 이미지 업로드, 관리자 API
- NORMAL/ADMIN: 댓글 CUD, 좋아요
- 비인증: 게시글 조회, 카테고리 조회, 방문자 수
- `SessionCreationPolicy.STATELESS` — JWT Stateless 설계 일관성 유지

---

## 2. 성능 / 자원 관리

### 2-1. 컬렉션 fetch join — 카테시안 곱 주의 🟡

**파일:** `repository/querydsl/CategoryQuerydslRepositoryImpl.java` (24-44줄)

```java
List<Category> categories = queryFactory
    .selectFrom(category)
    .distinct()
    .leftJoin(category.boards, board).fetchJoin()
    .where(category.boards.size().gt(0).and(board.boardStatus.eq(BoardStatus.VIEW)))
    .fetch();
```

컬렉션(`boards`)에 fetch join을 하면 카테고리 수 × 게시글 수만큼 행이 생성된다.  
`distinct()`로 Hibernate 레벨에서 중복 제거하지만, DB에서 대량 데이터를 전송하는 낭비가 있다.  
카테고리/게시글 수가 적은 지금은 실질적 문제가 없으나, 개선 방향은 아래와 같다.

```java
// 개선 방향: 카테고리만 먼저 조회 후 게시글 수를 서브쿼리로
List<CategoryDto> result = queryFactory
    .select(Projections.constructor(CategoryDto.class,
        category.id,
        category.name,
        board.count()
    ))
    .from(category)
    .leftJoin(category.boards, board)
    .on(board.boardStatus.eq(BoardStatus.VIEW))
    .groupBy(category.id)
    .having(board.count().gt(0))
    .fetch();
```

---

### 2-2. 댓글 계층 쿼리 — 컬렉션 fetch join 🟡

**파일:** `repository/querydsl/CommentQuerydslRepositoryImpl.java` (22-32줄)

```java
List<Comment> comments = queryFactory.selectDistinct(comment1)
    .from(comment1)
    .leftJoin(comment1.child).fetchJoin()   // 컬렉션 fetch join
    .leftJoin(comment1.member).fetchJoin()
    .where(comment1.board.id.eq(boardId))
    .where(comment1.parent.isNull())
    .fetch();
```

부모 댓글과 자식 댓글을 한 번에 가져오는 의도는 좋다.  
댓글 수가 적으면 문제없으나, 게시글에 댓글이 많아질 경우 결과 행이 많아진다.  
현재 블로그 규모에서는 허용 가능한 수준.

---

### 2-3. @Transactional(readOnly = true) — 양호 🟢

**파일:** 6개 서비스 전체

클래스 레벨에 `@Transactional(readOnly = true)` 설정 후,  
쓰기 메서드만 `@Transactional`로 오버라이드하는 패턴이 일관되게 적용되어 있다.

- `BoardServiceImpl`, `AuthServiceImpl`, `CommentServiceImpl`
- `CategoryServiceImpl`, `VisitorCountServiceImpl`

읽기 전용 트랜잭션은 Hibernate의 dirty checking, flush 생략으로 성능에 유리하다.

---

### 2-4. 스케줄러 Lock 관리 — 양호 🟢

**파일:** `scheduled/BoardScheduledTask.java`, `scheduled/VisitorCountScheduledTask.java`

- `ReentrantLock.tryLock()`으로 중복 실행 방지
- `VisitorCountScheduledTask`: `fixedDelay` 사용 (이전 실행 완료 후 다음 실행)
- `onApplicationEvent(ContextClosedEvent)`: 컨테이너 종료 시 Redis → DB 동기화 보장

---

### 2-5. Redis 캐시 TTL 및 일관성 — 양호 🟢

**파일:** `repository/redis/BoardRedisRepositoryImpl.java`

- TTL: 1시간 (조회/좋아요 갱신 시마다 TTL 리셋)
- `@Async` 비동기 캐시 업데이트로 응답 지연 없음
- 캐시 실패 시 예외 캡처 + 로깅으로 서비스 중단 방지

---

### 2-6. HikariCP 연결 풀 설정 누락 🟡

**파일:** `src/main/resources/application-prod.yaml`

현재 HikariCP 기본값(maximumPoolSize=10)을 그대로 사용 중이다.  
t3.small 단일 인스턴스에서 Spring Boot + MariaDB가 함께 실행되므로,  
연결 풀을 명시적으로 제한해 DB 과부하를 방지하는 것이 좋다.

```yaml
# application-prod.yaml에 추가
spring:
  datasource:
    hikari:
      maximum-pool-size: 5
      minimum-idle: 2
      connection-timeout: 30000
      idle-timeout: 600000
```

---

## 3. 예외 처리

### 3-1. GlobalExceptionHandler — 양호 🟢

**파일:** `exception/GlobalExceptionHandler.java`

| 예외 | 처리 |
|---|---|
| `BusinessException` | `ErrorCode`의 HTTP 상태 코드 반환 |
| `ExpiredRefreshTokenException` | refresh_token 쿠키 삭제 후 401 |
| `AccessDeniedException` | 403 |
| `MethodArgumentNotValidException` | 필드별 검증 오류 상세 반환 |
| `HttpMessageNotReadableException` | 400 |
| `Exception` (fallback) | 스택 트레이스 로깅 후 500 |

스택 트레이스가 응답에 노출되지 않고 서버 로그에만 기록된다.

---

### 3-2. ErrorCode Enum — 양호 🟢

**파일:** `exception/ErrorCode.java`

도메인별 에러 코드 체계적으로 관리:
- 공통: `INVALID_INPUT`, `INTERNAL_SERVER_ERROR`
- 인증: `UNAUTHORIZED`, `EXPIRED_TOKEN`, `INVALID_TOKEN`, `EXPIRED_REFRESH_TOKEN`
- 회원: `MEMBER_NOT_FOUND`, `DUPLICATE_USERNAME`
- 게시글/댓글/카테고리: 각 도메인별 CRUD 에러 코드 분리

---

## 4. 설정 파일

### 4-1. prod 프로필 — 양호 🟢

**파일:** `src/main/resources/application-prod.yaml`

| 항목 | 값 | 평가 |
|---|---|---|
| `ddl-auto` | `validate` | ✅ 스키마 자동 변경 방지 |
| `show-sql` | `false` | ✅ SQL 로그 미출력 |
| `format_sql` | `false` | ✅ 불필요한 포맷팅 제거 |
| `sql.init.mode` | `never` | ✅ 초기화 스크립트 미실행 |
| `logging.level.root` | `warn` | ✅ 로그 최소화 |

---

### 4-2. open-in-view: false — 양호 🟢

**파일:** `src/main/resources/application.yaml`

OSIV(Open Session in View)가 비활성화되어 있다.  
컨트롤러/뷰 레이어까지 트랜잭션이 유지되지 않으므로,  
서비스 외부에서 지연 로딩 시 `LazyInitializationException`이 발생할 수 있다.  
현재 서비스 계층에서 모든 연관 데이터를 fetch join으로 로딩하고 있으므로 문제없다.

---

### 4-3. `board.view.hmac.secret` — dev 전용 설정 확인 필요 🔴

**파일:** `src/main/resources/application-dev.yaml`

```yaml
board:
  view:
    hmac:
      secret: ${BOARD_VIEW_HMAC_SECRET}
```

`board.view.hmac.secret`이 `application-dev.yaml`에만 정의되어 있고  
`application.yaml`(공통) 또는 `application-prod.yaml`에 없다.

prod 프로필로 실행 시 이 값이 주입되지 않아 게시글 조회수 중복 방지 기능이 동작하지 않을 수 있다.

**확인 및 수정 필요:**

```yaml
# application.yaml 또는 application-prod.yaml에 추가
board:
  view:
    hmac:
      secret: ${BOARD_VIEW_HMAC_SECRET}
```

---

## 5. 엔티티 / 연관관계

### 5-1. FetchType — 양호 🟢

모든 연관관계(`@ManyToOne`, `@OneToMany`)에 `FetchType.LAZY` 적용.  
필요한 시점에만 SELECT 쿼리가 발생하며, N+1 방지를 위해 fetch join을 명시적으로 사용.

---

### 5-2. orphanRemoval + CascadeType.ALL — 양호 🟢

- `Board.comments`, `Board.boardLikes`, `Board.imageFiles`
- `Comment.child`

부모 엔티티 삭제 시 자식 자동 삭제 처리 — 별도 삭제 로직 불필요.

---

### 5-3. BoardLike 복합 인덱스 — 양호 🟢

**파일:** `domain/BoardLike.java`

```java
@Table(indexes = @Index(name = "idx_board_like_member", columnList = "board_id, member_id"))
```

좋아요 중복 여부 확인 쿼리(`board_id + member_id`)가 인덱스를 탄다.

---

## 6. Docker / 배포 환경

### 6-1. Dockerfile 멀티스테이지 — 양호 🟢

- 빌드: `eclipse-temurin:17-jdk-alpine`
- 실행: `eclipse-temurin:17-jre-alpine` (JDK 제외로 경량화)
- 의존성 레이어 분리: `build.gradle` 변경 없으면 `./gradlew dependencies` 캐시 재사용
- JVM 플래그: `-XX:+UseContainerSupport`, `-XX:MaxRAMPercentage=75.0`

---

### 6-2. docker-compose.yaml — 양호 🟢

- MariaDB, Redis 헬스체크 설정
- `depends_on: condition: service_healthy`로 백엔드 시작 순서 보장
- `stop_grace_period: 30s` — 컨테이너 종료 시 진행 중인 요청 처리 시간 확보
- Redis AOF 활성화 (`--appendonly yes --appendfsync everysec`)

---

## 7. 배포 전 체크리스트

### 🔴 즉시 수정 (배포 전 필수)

- [ ] `board.view.hmac.secret`을 `application.yaml` 또는 `application-prod.yaml`에 추가
- [ ] S3 사용 여부 결정: 미사용 시 `FileUploadController` / S3 관련 코드 제거, 사용 시 자격증명 교체
- [ ] `MemberJoinReqDto.username`, `password`에 `@Pattern` 정규식 검증 추가

### 🟡 배포 후 단기 개선

- [ ] Refresh Token 쿠키에 `SameSite=Strict`, `Secure=true` 속성 추가
- [ ] `WebSecurityConfig`에 보안 응답 헤더 설정 추가
- [ ] `application-prod.yaml`에 HikariCP 연결 풀 설정 추가
- [ ] `CategoryQuerydslRepositoryImpl.findCategoriesWithViewBoards()` 쿼리 개선
- [ ] `.env.prod` 파일 생성 및 모든 환경변수 값 설정 확인

### 환경변수 설정 확인 목록 (`.env.prod`)

```env
PROFILE=prod
DB_URL=jdbc:mariadb://db:3306/myblog
DB_USERNAME=...
DB_PASSWORD=...
DB_ROOT_PASSWORD=...
JWT_SECRET_KEY=...          # 256bit 이상 랜덤 문자열
CORS_ALLOWED_ORIGINS=https://yourdomain.com
VISITOR_HMAC_SECRET=...
BOARD_VIEW_HMAC_SECRET=...  # 위 3-3 항목 수정 후 추가
REDIS_HOST=redis
REDIS_PORT=6379
```
