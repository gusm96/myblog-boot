# 백엔드 보완 계획서

> 작성일: 2026-03-26
> 대상: `backend/src/main/java` 전체
> 점검 기준: Spring Boot 3, Spring Security, Spring Data JPA, Spring Data Redis 공식 문서 (context7 MCP 참조)

---

## 점검 요약

| 우선순위 | 항목 수 | 설명 |
|---------|---------|------|
| P0 — 즉시 수정 | 2 | 런타임 안정성·성능에 직접 영향 |
| P1 — 이번 작업 | 5 | 보안·정합성 결함 |
| P2 — 다음 작업 | 5 | 코드 품질·잠재적 버그 |
| P3 — 나중에 | 4 | 설정·최적화 |

---

## P0 — 즉시 수정

### P0-1. Redis 게시글 캐시 키 TTL 없음 — 메모리 누수

**위치**: `repository/implementation/BoardRedisRepositoryImpl.java:122`

**문제**: `setBoardForRedis()`가 만료 시간(TTL) 없이 영구 저장.
`board:{id}`, `board:{id}:views`, `board:{id}:likes` 세 키 모두 TTL 미설정.

```java
// 현재 — TTL 없이 저장
private void setBoardForRedis(String key, BoardForRedis boardForRedis) {
    redisTemplate.opsForValue().set(key, boardForRedis); // TTL 없음
}
```

**영향**:
- 게시글이 수천 개로 늘어날수록 Redis 메모리가 계속 증가
- 영구 삭제(`deletePermanently`)에서는 `boardRedisRepository.delete()` 호출로 정리되지만, 정상 동작 중인 게시글 캐시는 절대 만료되지 않음
- 스케줄러(`updateFromRedisStoreToDB`)가 10분마다 캐시를 DB에 동기화하고 삭제하지만, 에러 발생 시 스케줄러를 통한 삭제가 누락될 수 있음

**수정 방향**:
```java
// 변경 후 — TTL 적용 (예: 1시간)
private static final long CACHE_TTL_SECONDS = 60 * 60L; // 1시간

private void setBoardForRedis(String key, BoardForRedis boardForRedis) {
    redisTemplate.opsForValue().set(key, boardForRedis, CACHE_TTL_SECONDS, TimeUnit.SECONDS);
}
```

> **주의**: TTL 설정 후 `incrementViews()`, `incrementLikes()`에서 `setBoardForRedis()`를 호출할 때마다 TTL이 갱신됨 → 자주 조회되는 게시글은 캐시가 유지됨. 스케줄러 동기화 주기(10분)보다 TTL이 짧으면 데이터 손실 위험이 있으므로 최소 스케줄러 주기의 2~3배(20~30분) 이상으로 설정.

---

### P0-2. `UselessAdvisor` — 프로덕션 AOP 오버헤드

**위치**: `advisor/UselessAdvisor.java`

**문제**: 파일 이름 자체가 `Useless`인 AOP가 모든 서비스 구현체 메서드에
StopWatch + 로깅을 적용함. `@Profile` 없이 프로덕션에서도 동작.

```java
// 현재 — 프로파일 없이 모든 환경에서 실행
@Around("execution(public * com.moya.myblogboot.service.implementation.*.*(..))")
public Object stopWatch(ProceedingJoinPoint joinPoint) throws Throwable {
    StopWatch stopWatch = new StopWatch();
    stopWatch.start();
    log.info("Current Thread {}", Thread.currentThread().getName());
    Object result = joinPoint.proceed();
    stopWatch.stop();
    log.info("request spent {} ms", stopWatch.getLastTaskTimeMillis());
    return result;
}
```

**영향**:
- 모든 서비스 메서드에 AOP 프록시 오버헤드 추가
- 프로덕션 로그에 불필요한 INFO 레벨 메시지 남용 (방문자 카운트처럼 자주 호출되는 서비스의 경우 로그 폭증)
- `UselessAdvisor`라는 이름이 명시하듯 의도된 개발용 코드

**수정 방향** (둘 중 선택):
- A. 파일 삭제 (성능 측정 필요 없을 경우)
- B. `@Profile("dev")` 추가로 개발 환경에서만 활성화

```java
// 변경 후 — 개발 환경에서만 활성화
@Profile("dev")
@Aspect
@Component
public class UselessAdvisor { ... }
```

---

## P1 — 이번 작업 (보안·정합성)

### P1-1. `ShouldNotFilterPath` — Deprecated 버전 경로 잔존 + V8 누락

**위치**: `constants/ShouldNotFilterPath.java`

**문제**: JWT 필터 제외 경로 목록에 두 가지 문제가 동시에 존재.

```java
// 현재
public static final List<String> EXCLUDE_PATHS = Arrays.asList(
    "/api/v1/join",
    "/api/v1/login",
    "/api/v1/logout",
    "/api/v2/boards",   // Deprecated 버전
    "/api/v3/boards",   // Deprecated 버전
    "/api/v4/boards",   // Deprecated 버전
    "/api/v5/boards",   // Deprecated 버전
    "/api/v6/boards",   // Deprecated 버전
    "/api/v7/boards",   // Deprecated 버전
    "/api/v1/boards/search",
    "/api/v1/reissuing-token",
    "/api/v1/password-strength-check"
    // /api/v8/boards/** 없음 ← 현재 활성 버전인데 누락!
);
```

**영향 1**: V2~V7 Deprecated API는 컨트롤러에서도 제거됐을 것이나, 필터 제외 목록에 남아있으면 해당 경로로 들어오는 임의 요청이 JWT 검증 없이 통과됨 (컨트롤러가 없으면 404로 끝나지만 불필요한 우회 경로 존재).

**영향 2**: 현재 활성 버전인 `/api/v8/boards/**`가 목록에 없음 → 게시글 상세 조회에 JWT 토큰이 없어도 토큰 검증을 통과하는지 여부 불명확. `shouldNotFilter()`의 `startsWith` 매칭을 고려해야 함.

**수정 방향**:
```java
public static final List<String> EXCLUDE_PATHS = Arrays.asList(
    "/api/v1/join",
    "/api/v1/login",
    "/api/v1/logout",
    "/api/v1/reissuing-token",
    "/api/v1/password-strength-check",
    "/api/v1/boards",          // 게시글 목록 (공개)
    "/api/v1/boards/search",   // 검색 (공개)
    "/api/v2/categories",      // 카테고리 목록 (공개)
    "/api/v8/boards",          // 게시글 상세 V8 (공개)
    "/api/v2/visitor-count"    // 방문자 수 (공개)
);
```

> **실제 컨트롤러에서 사용 중인 공개 경로를 목록으로 재정의**하고 Deprecated 경로는 전부 제거.

---

### P1-2. `Member.username` DB 수준 unique 제약 없음

**위치**: `domain/member/Member.java:19`

**문제**: `username` 필드에 `@Column(unique=true)` 없이 서비스 레이어에서만 중복 체크.

```java
// 현재 — DB unique 제약 없음
@Entity
public class Member extends BaseTimeEntity {
    private String username;  // @Column(unique=true) 없음
}
```

동시에 두 요청이 `existsByUsername()` 체크를 통과하면 같은 username으로 두 Member가 저장될 수 있음 (Race Condition).

**수정 방향**:
```java
@Column(unique = true, nullable = false)
private String username;
```

> `application-prod.yaml`의 `ddl-auto: validate`이므로 반드시 **DB 마이그레이션 스크립트**를 함께 작성해야 함:
> ```sql
> ALTER TABLE member ADD CONSTRAINT uk_member_username UNIQUE (username);
> ```

---

### P1-3. `CORS setExposedHeaders(List.of("*"))` — 모든 헤더 노출

**위치**: `configuration/WebSecurityConfig.java:76`

**문제**: 응답 헤더 전체를 브라우저 JavaScript에 노출.

```java
// 현재
configuration.setExposedHeaders(List.of("*")); // 모든 헤더 노출
```

`Authorization`, `Set-Cookie`, 내부 서버 정보 헤더 등 민감한 헤더까지 노출될 수 있음.
Spring Security 공식 문서도 wildcard `"*"` 사용을 피하고 필요한 헤더만 명시할 것을 권장.

**수정 방향**:
```java
// 변경 후 — 필요한 헤더만 명시
configuration.setExposedHeaders(List.of("Authorization", "Content-Disposition"));
```

---

### P1-4. `findAllByCategoryName`, `findByDeletionStatus` — N+1 위험

**위치**: `repository/BoardRepository.java:30-34`

**문제**: 카테고리별 목록·휴지통 목록 쿼리에 `member`, `category` fetch join 없음.

```java
// 현재 — fetch join 없음
@Query("select b from Board b where b.category.name = :categoryName and b.boardStatus = 'VIEW'")
Page<Board> findAllByCategoryName(...);

@Query("select b from Board b where b.deleteDate is not null")
Page<Board> findByDeletionStatus(...);
```

`convertToBoardListResDto()`에서 `BoardResDto.of(board)` 호출 시 `board.getMember()`, `board.getCategory()`가 Lazy 로딩되어 게시글 수(8개)만큼 추가 쿼리 발생.

**수정 방향**:
```java
@Query(value = "select b from Board b join fetch b.member join fetch b.category " +
               "where b.category.name = :categoryName and b.boardStatus = 'VIEW'",
       countQuery = "select count(b) from Board b where b.category.name = :categoryName and b.boardStatus = 'VIEW'")
Page<Board> findAllByCategoryName(@Param("categoryName") String categoryName, Pageable pageable);

@Query(value = "select b from Board b join fetch b.member join fetch b.category " +
               "where b.deleteDate is not null",
       countQuery = "select count(b) from Board b where b.deleteDate is not null")
Page<Board> findByDeletionStatus(Pageable pageable);
```

> `Page` 쿼리의 `countQuery`는 fetch join 없이 단순 count만 수행하도록 별도 분리해야 성능에 유리.

---

### P1-5. `WebSecurityConfig` — `AuthService` Setter 주입

**위치**: `configuration/WebSecurityConfig.java:31-34`

**문제**: Spring Security 설정 클래스에서 `AuthService`를 setter로 주입.
Spring 표준은 생성자 주입이며, Security 설정 클래스에서 순환 참조 위험을 낮추기 위해 특히 중요.

```java
// 현재 — setter 주입
private AuthService authService;
public void setAuthService(AuthService authService) {
    this.authService = authService;
}
```

**수정 방향**: `@RequiredArgsConstructor` + `final` 필드로 생성자 주입 전환. 순환 참조가 발생한다면 `@Lazy` 적용.

```java
// 변경 후
@RequiredArgsConstructor
public class WebSecurityConfig {
    @Value("${jwt.secret}")
    private String secret;
    @Value("${cors.allowed-origins}")
    private String allowedOrigins;
    private final AuthService authService;
    ...
}
```

---

## P2 — 다음 작업 (코드 품질)

### P2-1. `MemberJoinReqDto` — `@Pattern` 검증 없음

**위치**: `domain/member/MemberJoinReqDto.java`

**문제**: 유효성 검사 메시지에 "영문 소문자, 숫자를 조합"이라고 명시되어 있지만, 실제로 패턴을 검증하는 `@Pattern` 애노테이션이 없음. 길이(6~20자)만 체크하므로 "@@@@@@", "한글닉네임!" 같은 입력이 통과됨.

```java
// 현재 — 길이만 검증, 형식 미검증
@NotBlank
@Size(min = 6, max = 20, message = "아이디는 6-20자의 영문 소문자, 숫자를 조합하여 입력하세요.")
private String username;
```

**수정 방향**:
```java
// 변경 후 — 패턴 검증 추가
@NotBlank
@Size(min = 6, max = 20)
@Pattern(regexp = "^[a-z0-9]+$", message = "아이디는 6-20자의 영문 소문자, 숫자를 조합하여 입력하세요.")
private String username;

@NotBlank
@Size(min = 8, max = 16)
@Pattern(regexp = "^(?=.*[A-Za-z])(?=.*\\d)(?=.*[!@#$%^&*]).{8,16}$",
         message = "비밀번호는 8~16자의 영문 대/소문자, 숫자, 특수기호를 조합하여 입력하세요.")
private String password;
```

---

### P2-2. `BoardServiceImpl.findById()` — `e.printStackTrace()` 사용

**위치**: `service/implementation/BoardServiceImpl.java:193`

**문제**: 예외 발생 시 `e.printStackTrace()`로 스택 트레이스를 stdout에 직접 출력.
프로덕션 환경에서 로그 수집 시스템을 우회하며, `log.error()`보다 성능이 낮음.

```java
// 현재
} catch (InvalidDataAccessApiUsageException e) {
    e.printStackTrace(); // ❌ stdout 직접 출력
    throw new RuntimeException("게시글 조회중 오류 발생.");
}
```

**수정 방향**:
```java
} catch (InvalidDataAccessApiUsageException e) {
    log.error("게시글 조회 중 오류 발생: {}", e.getMessage(), e);
    throw new RuntimeException("게시글 조회중 오류 발생.");
}
```

---

### P2-3. `JwtFilter` — INFO 레벨 민감 정보 로깅

**위치**: `configuration/JwtFilter.java:38-71`

**문제**: 모든 요청에서 HTTP 메서드, 경로, `memberPrimaryKey`, `role`이 INFO 레벨로 로깅됨.

```java
// 현재 — 프로덕션 로그에 민감 정보 노출
log.info("HTTP Method : {}", request.getMethod()); // 모든 요청마다
log.info("Path : {}", path);                       // 모든 요청마다
log.info("Member_Primary_Key : {}", memberPrimaryKey); // 사용자 PK 노출
log.info("Role : {}", tokenInfo.getRole());            // 권한 정보 노출
```

**영향**: 트래픽이 많을수록 로그 파일이 빠르게 증가. 사용자 PK + Role이 평문 로그에 남음.

**수정 방향**:
- `log.info()` → `log.debug()`로 레벨 낮추기 (프로덕션에서는 DEBUG 미출력)
- 또는 `memberPrimaryKey` 직접 로깅 제거

```java
// 변경 후
log.debug("HTTP Method: {}, Path: {}", request.getMethod(), request.getRequestURI());
log.debug("Authenticated memberId: {}", memberPrimaryKey);
```

---

### P2-4. `RedisConfig` — `shutdownTimeout: 0` Graceful Shutdown 충돌

**위치**: `configuration/RedisConfig.java` (Lettuce 설정)

**문제**: Lettuce 커넥션의 `shutdownTimeout`이 0으로 설정되어 있어, 애플리케이션 종료 시 진행 중인 Redis 명령이 완료되기 전에 연결이 즉시 끊김.

`VisitorCountScheduledTask`의 `onApplicationEvent(ContextClosedEvent)`에서 종료 시 Redis → DB 동기화를 시도하지만, `shutdownTimeout: 0`으로 인해 Redis 명령이 실행되기 전에 연결이 닫힐 수 있음. Graceful Shutdown의 목적을 무력화.

**수정 방향**:
```java
// 변경 후 — 종료 전 진행 중인 Redis 명령 완료 대기 (예: 2초)
config.setShutdownTimeout(Duration.ofMillis(2000));
```

---

### P2-5. `saveImageFile()` — 루프 내 개별 `save()` 호출

**위치**: `service/implementation/BoardServiceImpl.java:255-258`

**문제**: 이미지 파일 목록을 `Stream.map().save()` 개별 호출로 저장.
이미지 5개면 INSERT 쿼리 5회 발생.

```java
// 현재 — 루프마다 INSERT
List<ImageFile> imageFiles = images.stream()
    .map(image -> imageFileRepository.save(image.toEntity(board)))
    .collect(Collectors.toList());
```

**수정 방향**: `saveAll()` 일괄 처리. Hibernate의 JDBC batch insert와 조합하면 단일 쿼리로 처리 가능.

```java
// 변경 후
List<ImageFile> imageFiles = images.stream()
    .map(image -> image.toEntity(board))
    .collect(Collectors.toList());
imageFileRepository.saveAll(imageFiles);
imageFiles.forEach(board::addImageFile);
```

---

## P3 — 나중에 (설정·최적화)

### P3-1. `InitDb` — `@Component` 주석 처리로 완전히 비활성화

**위치**: `configuration/InitDb.java:17`

**문제**: `@Component`가 주석 처리되어 있어 Spring 빈으로 등록되지 않음.
`@Profile("dev")`는 붙어 있지만 빈 자체가 없어 어떤 환경에서도 동작 안 함.
테스트 데이터 초기화가 필요할 때 활성화하려면 `@Component`를 직접 해제해야 하는 불명확한 상태.

**수정 방향**: 파일을 삭제하거나, 명확하게 사용할 의도가 있다면 주석을 정리하고 `@Component` 복원.

---

### P3-2. `application-dev.yaml` — `ddl-auto: update` 위험

**위치**: `src/main/resources/application-dev.yaml`

**문제**: `ddl-auto: update`는 엔티티 변경 사항을 자동으로 스키마에 반영하지만, **컬럼 삭제·타입 변경은 처리하지 않음**. 스키마 변경 이력이 남지 않아 팀 협업 시 누락 위험.

**수정 방향**: Flyway 또는 Liquibase 도입을 고려하거나, 최소한 `create-drop`으로 변경해 매번 깨끗한 상태에서 시작.

```yaml
# 개발 환경에서 안전한 대안
jpa:
  hibernate:
    ddl-auto: create-drop  # 매번 스키마 재생성 (테스트 데이터도 재생성 필요)
```

---

### P3-3. Hibernate Batch Fetch Size 미설정

**위치**: `src/main/resources/application.yaml`

**문제**: `@OneToMany` 컬렉션을 Lazy 로딩할 때 개별 SELECT가 발생.
`default_batch_fetch_size` 설정으로 IN 절로 일괄 조회하면 N+1을 부분적으로 완화.

**수정 방향**:
```yaml
spring:
  jpa:
    properties:
      hibernate:
        default_batch_fetch_size: 100
```

---

### P3-4. `findAllByCategoryName` — boardStatus 문자열 하드코딩

**위치**: `repository/BoardRepository.java:30`

**문제**: `'VIEW'` 문자열이 JPQL에 하드코딩됨. `BoardStatus` enum 리팩토링 시 컴파일 오류 없이 런타임에서만 발견.

```java
// 현재 — 하드코딩
@Query("select b from Board b where b.category.name = :categoryName and b.boardStatus = 'VIEW'")
```

**수정 방향**: 파라미터로 전달하거나 enum 상수를 직접 참조.

```java
// 변경 후
@Query("select b from Board b ... where b.category.name = :categoryName and b.boardStatus = :boardStatus")
Page<Board> findAllByCategoryName(@Param("categoryName") String categoryName,
                                  @Param("boardStatus") BoardStatus boardStatus,
                                  Pageable pageable);
```

---

## 작업 순서 요약

```
Phase 1 (P0)
  └── P0-2: UselessAdvisor @Profile("dev") 추가 또는 삭제
  └── P0-1: BoardRedisRepositoryImpl TTL 설정

Phase 2 (P1)
  └── P1-1: ShouldNotFilterPath 정리 (Deprecated 제거 + V8 추가)
  └── P1-5: WebSecurityConfig setter → 생성자 주입
  └── P1-3: CORS setExposedHeaders 최소화
  └── P1-2: Member.username @Column(unique=true) + DB 마이그레이션
  └── P1-4: findAllByCategoryName / findByDeletionStatus fetch join 추가

Phase 3 (P2)
  └── P2-2: findById() e.printStackTrace() → log.error()
  └── P2-3: JwtFilter INFO 로그 → DEBUG 레벨 변경
  └── P2-4: RedisConfig shutdownTimeout 0 → 2000ms
  └── P2-1: MemberJoinReqDto @Pattern 검증 추가
  └── P2-5: saveImageFile() save() → saveAll()

Phase 4 (P3)
  └── P3-1: InitDb 정리
  └── P3-3: Hibernate default_batch_fetch_size 설정
  └── P3-4: boardStatus 하드코딩 제거
  └── P3-2: ddl-auto 전략 재검토
```

---

## 참조

- Spring Security CORS: https://docs.spring.io/spring-security/reference/servlet/integrations/cors.html
- Spring Data JPA countQuery: https://docs.spring.io/spring-data/jpa/docs/current/reference/html/#jpa.query-methods.at-query
- Hibernate batch_fetch_size: https://docs.spring.io/spring-boot/docs/3.0.x/reference/html/data.html#data.sql.jpa-and-spring-data.jpa-properties
- Spring Data Redis TTL: https://docs.spring.io/spring-boot/docs/3.0.x/reference/html/io.html#io.caching.provider.redis
