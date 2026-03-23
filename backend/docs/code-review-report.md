# 코드 검사 리포트 — myblog-boot (Java 17 / Spring Boot 3.0.4)

> 작성일: 2026-03-10
> 대상: `backend/src/main/java/com/moya/myblogboot/**`
> 기준: Java 17 기능 활용, Spring Boot 3.x 적합성, 코드 품질 및 보안

## 목차
1. [심각도 높은 문제 (Critical)](#1-critical--심각도-높은-문제)
2. [보안 취약점 (Security)](#2-security--보안-취약점)
3. [Spring Boot 3.x 적합성 (Compatibility)](#3-compatibility--spring-boot-3x-적합성)
4. [Java 17 기능 활용 (Modernization)](#4-modernization--java-17-기능-활용)
5. [코드 품질 및 안티패턴 (Quality)](#5-quality--코드-품질-및-안티패턴)
6. [성능 문제 (Performance)](#6-performance--성능-문제)
7. [요약 우선순위 매트릭스](#요약-우선순위-매트릭스)

---

## 1. Critical — 심각도 높은 문제

### 1-1. JWT 라이브러리 심각한 구버전 사용 (jjwt 0.9.1)

**파일:** `build.gradle:31`

`io.jsonwebtoken:jjwt:0.9.1`은 2018년 릴리즈로 **보안 패치가 중단**된 버전이다. `Jwts.parser().setSigningKey(String)` 등 deprecated API를 사용 중이며, 문자열 키를 그대로 사용하면 **HMAC 키 길이 검증이 우회**된다.

```java
// 현재 코드 (JwtUtil.java:19) — deprecated API
Claims claims = Jwts.parser().setSigningKey(secret).parseClaimsJws(token).getBody();
```

**권장 수정 — jjwt 0.12.x 마이그레이션:**

```gradle
// build.gradle — jaxb-api 의존성도 함께 제거
implementation 'io.jsonwebtoken:jjwt-api:0.12.6'
runtimeOnly 'io.jsonwebtoken:jjwt-impl:0.12.6'
runtimeOnly 'io.jsonwebtoken:jjwt-jackson:0.12.6'
// 삭제: implementation 'javax.xml.bind:jaxb-api:2.3.0'
// 삭제: implementation 'io.jsonwebtoken:jjwt:0.9.1'
```

```java
// JwtUtil.java — 최신 API 사용
import io.jsonwebtoken.security.Keys;
import javax.crypto.SecretKey;

private static SecretKey getSigningKey(String secret) {
    return Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
}

public static TokenInfo getTokenInfo(String token, String secret) {
    Claims claims = Jwts.parser()
            .verifyWith(getSigningKey(secret))
            .build()
            .parseSignedClaims(token)
            .getPayload();
    // ...
}

private static String jwtBuild(Claims claims, Long expiration, String secret) {
    return Jwts.builder()
            .claims(claims)
            .issuedAt(new Date(System.currentTimeMillis()))
            .expiration(new Date(System.currentTimeMillis() + expiration))
            .signWith(getSigningKey(secret))
            .compact();
}
```

### 1-2. `javax.xml.bind:jaxb-api` 불필요한 javax 의존성

**파일:** `build.gradle:32`

이 의존성은 jjwt 0.9.x의 Base64 처리용이었다. jjwt 0.12.x로 마이그레이션하면 완전히 제거 가능하다. **`javax.*` 패키지가 Spring Boot 3 (Jakarta EE) 프로젝트에 혼재하는 것은 부적절**하다.

### 1-3. `@Async`가 동일 클래스 내부 호출로 작동하지 않음

**파일:** `BoardServiceImpl.java:259-278`, `BoardLikeServiceImpl.java:63-79`

Spring AOP의 프록시 기반 제한으로, **같은 클래스 내부에서 `@Async` 메서드를 호출하면 비동기로 실행되지 않고 동기 실행**된다.

```java
// BoardServiceImpl.java — 내부 호출이므로 @Async 무효
@Async
protected void updateBoardForRedis(Board board) { ... }

// 같은 클래스에서 호출 → 동기 실행됨
updateBoardForRedis(board);  // line 137
```

**수정안:** 별도의 `@Service` 클래스로 분리하거나, `@Async`를 제거하고 의도를 명확히 한다.

```java
@Service
@RequiredArgsConstructor
public class BoardCacheService {
    private final BoardRedisRepository boardRedisRepository;

    @Async
    public void updateBoardForRedis(BoardForRedis boardForRedis, Board board) {
        boardForRedis.update(board);
        boardRedisRepository.update(boardForRedis);
    }

    @Async
    public void deleteBoardForRedis(BoardForRedis boardForRedis) {
        boardRedisRepository.delete(boardForRedis);
    }
}
```

---

## 2. Security — 보안 취약점

### 2-1. Authorization 헤더 파싱 NPE → 500 노출

**파일:** `AuthController.java:85-87`

```java
private static String getToken(HttpServletRequest request) {
    return request.getHeader(HttpHeaders.AUTHORIZATION).split(" ")[1];
}
```

Authorization 헤더가 없거나 형식이 잘못되면 `NullPointerException` 또는 `ArrayIndexOutOfBoundsException`이 발생하여 **스택트레이스가 클라이언트에 노출**될 수 있다.

```java
// 수정안
private static String getToken(HttpServletRequest request) {
    String authorization = request.getHeader(HttpHeaders.AUTHORIZATION);
    if (authorization == null || !authorization.startsWith("Bearer ")) {
        throw new InvalidateTokenException("유효하지 않은 인증 정보입니다.");
    }
    return authorization.substring(7);
}
```

### 2-2. JwtFilter에서 `"bearer "` 소문자 비교

**파일:** `JwtFilter.java:43`

```java
if (authorization == null || !authorization.startsWith("bearer ")) {
```

RFC 6750 표준은 `"Bearer"`(대문자 B)이다. 대소문자 불일치로 인증이 실패할 수 있다. `AuthController.getToken()`은 `split(" ")`만 사용하여 일관성도 없다.

```java
// 수정안 — 대소문자 무관하게 처리
if (authorization == null || !authorization.toLowerCase().startsWith("bearer ")) {
```

### 2-3. JwtFilter에서 빈 토큰 후 필터 체인 계속 진행

**파일:** `JwtFilter.java:49-52`

```java
if (token.isEmpty()) {
    log.error("Token is null");
    response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "토큰이 존재하지 않습니다.");
    // ⚠️ return 누락! 아래 코드가 계속 실행됨
}
```

`return;`이 누락되어 에러 응답을 보낸 후에도 토큰 검증 로직이 이어서 실행된다.

```java
if (token.isEmpty()) {
    response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "토큰이 존재하지 않습니다.");
    return;  // 반드시 추가
}
```

### 2-4. Cookie `Secure` 플래그 미설정

**파일:** `CookieUtil.java:8-13`

```java
public static Cookie addCookie(String cookieName, String cookieValue) {
    Cookie cookie = new Cookie(cookieName, cookieValue);
    cookie.setPath("/");
    cookie.setHttpOnly(true);
    // ⚠️ cookie.setSecure(true) 누락 → HTTPS 환경에서도 HTTP로 쿠키 전송 가능
    // ⚠️ SameSite 속성도 미설정
    return cookie;
}
```

Refresh Token이 담긴 쿠키에 `Secure`, `SameSite` 속성이 없으면 **CSRF 및 중간자 공격에 취약**하다.

### 2-5. `GlobalExceptionHandler`에서 `RuntimeException`을 catch하여 메시지 노출

**파일:** `GlobalExceptionHandler.java:70-73`

```java
@ExceptionHandler({RuntimeException.class, PersistenceException.class})
public ResponseEntity<?> handleRuntimeException(RuntimeException e) {
    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(e.getMessage());
}
```

예상치 못한 RuntimeException의 **내부 에러 메시지(DB 쿼리, 스택 정보 등)가 클라이언트에 그대로 노출**된다. 프로덕션에서는 일반적인 메시지로 대체해야 한다.

### 2-6. `ExpiredRefreshTokenException` 핸들러에서 NPE 가능성

**파일:** `GlobalExceptionHandler.java:63-67`

```java
Cookie refreshTokenCookie = CookieUtil.findCookie(request, "refresh_token_key");
CookieUtil.deleteCookie(response, refreshTokenCookie); // ⚠️ null일 수 있음
```

`findCookie`가 `null`을 반환할 수 있으나 null 체크 없이 `deleteCookie`를 호출한다.

---

## 3. Compatibility — Spring Boot 3.x 적합성

### 3-1. Spring Security DSL: Deprecated 체이닝 스타일 사용

**파일:** `WebSecurityConfig.java:35-61`

Spring Security 6.1+(Spring Boot 3.1+)부터 `.httpBasic()`, `.csrf()`, `.cors()`, `.authorizeHttpRequests()` 등 **메서드 체이닝 방식이 deprecated** 되고 Lambda DSL이 권장된다. Spring Boot 3.0.4에서도 동작하지만, 향후 제거 예정이다.

```java
// 현재 코드 — deprecated 스타일
return http.httpBasic().disable()
        .csrf().disable()
        .cors().and()
        .authorizeHttpRequests()
        .requestMatchers(...)
        // ...
        .and().sessionManagement()
        .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
        .and().addFilterBefore(...).build();
```

```java
// 권장 수정 — Lambda DSL
@Bean
public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
    return http
        .httpBasic(AbstractHttpConfigurer::disable)
        .csrf(AbstractHttpConfigurer::disable)
        .cors(Customizer.withDefaults())
        .authorizeHttpRequests(auth -> auth
            .requestMatchers(HttpMethod.POST, "/api/v1/boards").hasRole("ADMIN")
            .requestMatchers(HttpMethod.PUT, "/api/v1/boards/**").hasRole("ADMIN")
            .requestMatchers(HttpMethod.DELETE, "/api/v1/boards/**").hasRole("ADMIN")
            // ... 나머지 규칙 동일
            .anyRequest().permitAll()
        )
        .sessionManagement(session ->
            session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
        )
        .addFilterBefore(new JwtFilter(authService, secret), UsernamePasswordAuthenticationFilter.class)
        .build();
}
```

### 3-2. `spring-cloud-starter-aws:2.2.6.RELEASE` — Spring Boot 3 비호환

**파일:** `build.gradle:30`

이 라이브러리는 **Spring Boot 2.x 전용**이며, Spring Boot 3에서 공식 지원이 중단되었다. Spring Cloud AWS 3.x 또는 `io.awspring.cloud:spring-cloud-aws-starter-s3:3.0.x`로 마이그레이션해야 한다.

```gradle
// 수정안
implementation 'io.awspring.cloud:spring-cloud-aws-starter-s3:3.0.4'
```

### 3-3. `io.lettuce:lettuce-core` 수동 의존성 불필요

**파일:** `build.gradle:34`

`spring-boot-starter-data-redis`에 이미 lettuce-core가 포함되어 있다. 수동 지정 시 **Spring Boot의 의존성 관리와 버전 충돌** 가능성이 있다.

```gradle
// 삭제 권장
// implementation 'io.lettuce:lettuce-core:6.2.6.RELEASE'
```

### 3-4. `spring-boot-devtools` 중복 선언

**파일:** `build.gradle:26, 47`

```gradle
implementation 'org.springframework.boot:spring-boot-devtools'    // line 26
developmentOnly 'org.springframework.boot:spring-boot-devtools'   // line 47
```

`implementation`으로 선언하면 **프로덕션 빌드에도 devtools가 포함**된다. `developmentOnly`만 유지해야 한다.

### 3-5. `@Param` import 오류

**파일:** `BoardRepository.java:6`

```java
import io.lettuce.core.dynamic.annotation.Param;  // ⚠️ Lettuce의 @Param!
```

JPA 쿼리의 `@Param`은 `org.springframework.data.repository.query.Param`을 사용해야 한다. 현재 Lettuce의 `@Param`을 import하고 있어 **JPQL 파라미터 바인딩이 정상 작동하지 않을 수 있다**.

```java
// 수정
import org.springframework.data.repository.query.Param;
```

---

## 4. Modernization — Java 17 기능 활용

### 4-1. Record 클래스 미사용 — DTO에 적극 활용 가능

불변 데이터 전송 객체에 Lombok `@Getter/@Builder/@NoArgsConstructor/@AllArgsConstructor`를 사용하고 있으나, Java 16+의 **Record**로 대체하면 보일러플레이트를 대폭 줄일 수 있다.

```java
// 현재: Token.java
@Getter @Builder @NoArgsConstructor @AllArgsConstructor
public class Token {
    private String access_token;
    private String refresh_token;
}

// Record로 변환
public record Token(String accessToken, String refreshToken) {}
```

적용 가능 대상: `Token`, `TokenInfo`, `TokenReqDto`, `CategoryReqDto`, `CommentReqDto`, `PwStrengthCheckReqDto`, `ImageFileDto`, `VisitorCountDto`, `RandomUserNumberDto`, `BoardResDto`

### 4-2. Pattern Matching for `instanceof` 미사용

**파일:** `BoardController.java:239-241`, `CommentController.java:67-70`

```java
// 현재 코드 — 전통적 캐스팅
if (principal instanceof UsernamePasswordAuthenticationToken) {
    memberId = (Long) ((UsernamePasswordAuthenticationToken) principal).getPrincipal();
}

// Java 16+ Pattern Matching 적용
if (principal instanceof UsernamePasswordAuthenticationToken authToken) {
    memberId = (Long) authToken.getPrincipal();
}
```

### 4-3. `getMemberId()` 중복 정의

**파일:** `BoardController.java:237-243`, `CommentController.java:66-72`

동일한 `getMemberId(Principal)` 메서드가 두 컨트롤러에 복사-붙여넣기되어 있다.

---

## 5. Quality — 코드 품질 및 안티패턴

### 5-1. `getBoardFromCache()`의 `synchronized` 키워드 — 성능 병목

**파일:** `BoardServiceImpl.java:243`

```java
@Override
public synchronized BoardForRedis getBoardFromCache(Long boardId) {
```

**모든 게시글 캐시 조회가 하나의 모니터 락을 공유**하여 동시성이 완전히 차단된다. Board A를 읽을 때 Board B 조회도 블로킹된다. `ConcurrentHashMap`이나 Redis 자체 원자성에 위임하는 것이 적절하다.

### 5-2. 과도한 포괄적 `try-catch(Exception e)` 패턴

**파일:** 다수 (BoardServiceImpl, CategoryServiceImpl, CommentServiceImpl, AuthServiceImpl)

```java
// 예: CategoryServiceImpl.java:37-43
try {
    categoryRepository.save(category);
    return "카테고리가 정상적으로 등록되었습니다.";
} catch (Exception e) {
    throw new PersistenceException("카테고리 등록을 실패했습니다.");
}
```

모든 곳에서 `Exception`을 잡고 `RuntimeException`으로 재포장하면 **원인 예외가 소실**되고, NPE 등 버그성 예외까지 숨겨진다. 특정 예외만 catch하거나, 원인을 체이닝해야 한다.

```java
} catch (DataAccessException e) {
    throw new PersistenceException("카테고리 등록을 실패했습니다.", e);  // cause 포함
}
```

### 5-3. `e.printStackTrace()` 사용

**파일:** `BoardServiceImpl.java:194`, `BoardServiceImpl.java:276`, `CommentServiceImpl.java:61`, `BoardLikeServiceImpl.java:38,53`

프로덕션에서 `e.printStackTrace()`는 **stdout으로 직접 출력**되어 로그 시스템을 우회한다. `log.error("message", e)` 로 대체해야 한다.

### 5-4. Raw Type `List` 사용

**파일:** `CommentController.java:29,35`, `CategoryController.java:22,28,34`

```java
public ResponseEntity<List> getComments(...)    // raw type
public ResponseEntity<List> getCategoryList()   // raw type
```

제네릭 타입이 지정되지 않아 **컴파일 타임 타입 안전성이 없다**.

```java
// 수정
public ResponseEntity<List<CommentResDto>> getComments(...)
public ResponseEntity<List<CategoryResDto>> getCategoryList()
```

### 5-5. 미사용 import

**파일:** `CommentController.java:4,6,10,12,19`

```java
import com.moya.myblogboot.domain.board.Board;          // 미사용
import com.moya.myblogboot.domain.token.TokenInfo;       // 미사용
import jakarta.servlet.http.HttpServletRequest;           // 미사용
import org.apache.coyote.Response;                        // 미사용
import java.util.List;                                    // raw type에 사용
```

### 5-6. 필드 네이밍 컨벤션 위반 (snake_case)

**파일:** `Token.java`, `Comment.java:28`

```java
private String access_token;   // Token.java
private String refresh_token;  // Token.java
private LocalDateTime write_date;  // Comment.java
```

Java 표준은 **camelCase**이다. JSON 직렬화가 필요하면 `@JsonProperty`를 사용해야 한다.

### 5-7. `ShouldNotFilterPath`에 `/api/v1/boards` 누락

**파일:** `ShouldNotFilterPath.java`

`/api/v1/boards` GET (모든 게시글 리스트)은 공개 API인데 exclude 경로에 없다. JwtFilter를 통과하므로 문제없이 동작하지만(토큰 없으면 필터가 패스), `/api/v1/boards/category` 경로도 누락되어 있어 **일관성이 부족**하다.

### 5-8. `deletePermanently(LocalDateTime)`에서 삭제가 아닌 상태 변경만 수행

**파일:** `BoardServiceImpl.java:172-175`

```java
public void deletePermanently(LocalDateTime thresholdDate) {
    List<Board> boards = boardRepository.findByDeleteDate(thresholdDate);
    boards.stream().forEach(Board::deleteBoard);  // ⚠️ 상태만 변경, 실제 삭제 X
}
```

메서드 이름은 `deletePermanently`이지만 실제로는 `BoardStatus.HIDE`로 변경할 뿐, **DB에서 삭제하지 않는다**. `deleteBoards(board)`를 호출해야 한다.

---

## 6. Performance — 성능 문제

### 6-1. N+1 잠재적 문제 — 게시글 목록 조회

**파일:** `BoardRepository.java:26-27`

```java
@Query("select b from Board b where b.boardStatus = :boardStatus")
Page<Board> findAll(@Param("boardStatus") BoardStatus boardStatus, Pageable pageable);
```

목록 조회 시 `member`, `category`를 fetch join하지 않아, `BoardResDto.of(board)`에서 지연 로딩이 발생하면 **N+1 쿼리**가 된다.

```java
// 수정안 — fetch join 추가 (카운트 쿼리는 분리)
@Query(value = "select b from Board b join fetch b.member join fetch b.category where b.boardStatus = :boardStatus",
       countQuery = "select count(b) from Board b where b.boardStatus = :boardStatus")
Page<Board> findAll(@Param("boardStatus") BoardStatus boardStatus, Pageable pageable);
```

### 6-2. `findById` 쿼리에서 `boardLikes` 전체 fetch join

**파일:** `BoardRepository.java:19-24`

```java
@Query("select distinct b from Board b " +
        "join fetch b.member " +
        "left join fetch b.boardLikes boardLike " +
        "left join fetch boardLike.member " +
        "join fetch b.category where b.id = :boardId")
```

좋아요가 많은 게시글의 경우 **모든 BoardLike + 연관 Member를 한 번에 로딩**한다. 좋아요 수만 필요하다면 `COUNT` 쿼리로 대체하고, 좋아요 목록은 필요할 때만 조회하는 것이 적절하다.

### 6-3. Redis SCAN 명령 — `getKeys()` Connection 미반환

**파일:** `BoardRedisRepositoryImpl.java:27-38`

```java
RedisKeyCommands keyCommands = redisTemplate.getRequiredConnectionFactory().getConnection().keyCommands();
ScanOptions options = ScanOptions.scanOptions().match(pattern).build();
Cursor<byte[]> cursor = keyCommands.scan(options);
```

`getConnection()`으로 직접 얻은 커넥션이 **닫히지 않아 커넥션 풀 누수**가 발생한다. `RedisCallback`이나 try-with-resources를 사용해야 한다.

```java
@Override
public Set<Long> getKeys(String pattern) {
    return redisTemplate.execute((RedisCallback<Set<Long>>) connection -> {
        Set<Long> keys = new HashSet<>();
        ScanOptions options = ScanOptions.scanOptions().match(pattern).build();
        try (Cursor<byte[]> cursor = connection.keyCommands().scan(options)) {
            while (cursor.hasNext()) {
                String key = new String(cursor.next());
                keys.add(Long.parseLong(key.split(":")[1]));
            }
        }
        return keys;
    });
}
```

### 6-4. `BoardForRedis`에 `Serializable` 미구현

**파일:** `BoardForRedis.java`

Redis에 Java 직렬화로 저장할 경우 `Serializable`이 필요하다. Jackson JSON 직렬화를 사용한다면 기본 생성자와 getter만으로 충분하지만, `RedisTemplate<String, Object>`의 기본 설정에 따라 문제가 될 수 있다.

---

## 요약 우선순위 매트릭스

| 우선순위 | 항목 | 영향도 |
|---------|------|--------|
| **P0** | JWT 라이브러리 0.9.1 → 0.12.x 마이그레이션 | 보안 |
| **P0** | JwtFilter return 누락 (빈 토큰 계속 진행) | 보안 |
| **P0** | `@Param` import 오류 (Lettuce → Spring Data) | 런타임 오류 |
| **P1** | `@Async` 내부 호출 무효 | 기능 결함 |
| **P1** | Redis Connection 누수 (SCAN) | 성능/안정성 |
| **P1** | `deletePermanently` 실제 삭제 미수행 | 기능 결함 |
| **P1** | spring-cloud-aws 2.x → 3.x 마이그레이션 | 호환성 |
| **P2** | Security Lambda DSL 마이그레이션 | 유지보수성 |
| **P2** | Authorization 헤더 NPE, Cookie Secure 플래그 | 보안 |
| **P2** | `synchronized` 캐시 조회 병목 | 성능 |
| **P2** | N+1 쿼리 (게시글 목록) | 성능 |
| **P3** | Java 17 Record/Pattern Matching 활용 | 코드 품질 |
| **P3** | Raw Type, 미사용 import, snake_case 필드명 | 코드 품질 |
| **P3** | devtools 중복 선언, lettuce 수동 의존성 | 빌드 정리 |
