# 보안 리팩토링 작업

> 작성일: 2026-03-10
> 기반 문서: `docs/code-review-report.md`
> 검증: 컴파일 성공 + 컨트롤러 통합 테스트 45개 전체 통과
> 2차 검토: context7 MCP 기반 공식 문서 대조 검증 완료 (2026-03-10)

---

## 1. JWT 라이브러리 마이그레이션 (jjwt 0.9.1 → 0.12.6) — P0

### 코드 수정 내용

**`build.gradle`** — 의존성 교체

```diff
- implementation 'io.jsonwebtoken:jjwt:0.9.1'
- implementation 'javax.xml.bind:jaxb-api:2.3.0'
+ implementation 'io.jsonwebtoken:jjwt-api:0.12.6'
+ runtimeOnly 'io.jsonwebtoken:jjwt-impl:0.12.6'
+ runtimeOnly 'io.jsonwebtoken:jjwt-jackson:0.12.6'
```

**`JwtUtil.java`** — 전면 리라이트

```diff
- Claims claims = Jwts.parser().setSigningKey(secret).parseClaimsJws(token).getBody();
+ Claims claims = Jwts.parser()
+         .verifyWith(getSigningKey(secret))
+         .build()
+         .parseSignedClaims(token)
+         .getPayload();
```

```diff
- return Jwts.builder()
-         .setClaims(claims)
-         .setIssuedAt(new Date(System.currentTimeMillis()))
-         .setExpiration(new Date(System.currentTimeMillis() + expiration))
-         .signWith(SignatureAlgorithm.HS256, secret)
-         .compact();
+ return Jwts.builder()
+         .claim("memberPrimaryKey", memberPrimaryKey)
+         .claim("role", role)
+         .issuedAt(new Date(System.currentTimeMillis()))
+         .expiration(new Date(System.currentTimeMillis() + expiration))
+         .signWith(getSigningKey(secret))
+         .compact();
```

### 기존 코드의 문제점

- **jjwt 0.9.1은 2018년 릴리즈로 보안 패치가 중단**되어 알려진 취약점에 대한 업데이트를 받을 수 없는 상태임을 알게 되었다.
- 구버전의 `Jwts.parser().setSigningKey(String)`은 **문자열 키를 직접 사용하여 HMAC 키 길이 검증을 우회**한다. 0.12.x의 `Keys.hmacShaKeyFor()`는 최소 256비트(32바이트)를 강제하여 약한 키를 원천 차단한다.
- `javax.xml.bind:jaxb-api`는 jjwt 0.9.x의 Base64 처리 의존성으로, Jakarta EE 기반인 Spring Boot 3 프로젝트에 **`javax.*` 패키지가 혼재하는 부적절한 상태**를 해소했다.
- `SignatureAlgorithm.HS256`을 직접 지정하는 대신 `signWith(SecretKey)`가 키 크기에 따라 **자동으로 최적 알고리즘을 선택**하므로 알고리즘 불일치 오류가 방지된다.
- **`JwtFilter.java`**: `io.jsonwebtoken.SignatureException`이 0.12.x에서 `io.jsonwebtoken.security.SecurityException`으로 이동됨에 따라 import와 catch 블록을 업데이트했다.
- **`GlobalExceptionHandler.java`**: 동일한 이유로 `SignatureException` → `SecurityException` 교체. 기존에 잘못 import된 `java.security.SignatureException`(JWT와 무관)을 올바른 클래스로 교정했다.

---

## 2. JwtFilter — 빈 토큰 처리 후 `return` 누락 수정 — P0

### 코드 수정 내용

**`JwtFilter.java:47-50`**

```diff
  if (token.isEmpty()) {
-     log.error("Token is null");
      response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "토큰이 존재하지 않습니다.");
+     return;
  }
```

### 기존 코드의 문제점

- `return`이 없으면 **401 에러를 전송한 후에도 아래 토큰 검증 로직이 계속 실행**된다. 빈 문자열을 JWT 파서에 전달하면 예측 불가능한 예외가 발생하고, 이미 커밋된 응답에 추가 `sendError`가 시도되어 `IllegalStateException`이 발생할 수 있다.

---

## 3. JwtFilter — Bearer 토큰 대소문자 처리 — P2

### 코드 수정 내용

**`JwtFilter.java:41,46`**

```diff
- if (authorization == null || !authorization.startsWith("bearer ")) {
+ if (authorization == null || !authorization.toLowerCase().startsWith("bearer ")) {
```

```diff
- String token = authorization.split(" ")[1];
+ String token = authorization.substring(7);
```

### 기존 코드의 문제점

- RFC 6750은 `"Bearer"` (대문자 B)를 표준으로 정의한다. 기존 코드는 `"bearer "`(소문자)만 허용하여 **표준 클라이언트("Bearer token")**의 요청을 거부할 수 있었다. `toLowerCase()`로 대소문자 무관하게 처리한다.
- `split(" ")[1]` 대신 `substring(7)`을 사용하여 토큰 값에 공백이 포함된 경우의 파싱 오류를 방지한다.

---

## 4. AuthController — Authorization 헤더 NPE 방어 — P2

### 코드 수정 내용

**`AuthController.java:85-91`**

```diff
  private static String getToken(HttpServletRequest request) {
-     return request.getHeader(HttpHeaders.AUTHORIZATION).split(" ")[1];
+     String authorization = request.getHeader(HttpHeaders.AUTHORIZATION);
+     if (authorization == null || !authorization.toLowerCase().startsWith("bearer ")) {
+         throw new InvalidateTokenException("유효하지 않은 인증 정보입니다.");
+     }
+     return authorization.substring(7);
  }
```

### 기존 코드의 문제점

- Authorization 헤더가 없거나 형식이 잘못된 경우 `NullPointerException` 또는 `ArrayIndexOutOfBoundsException`이 발생하여 **500 Internal Server Error와 함께 스택트레이스가 클라이언트에 노출**될 수 있었다.
- 명시적 검증과 비즈니스 예외(`InvalidateTokenException`)로 전환하여 `GlobalExceptionHandler`가 적절한 **401 응답**을 반환하도록 했다.

---

## 5. CookieUtil — Secure, SameSite 플래그 추가 — P2

### 코드 수정 내용

**`CookieUtil.java:12-13`** — 쿠키 생성 시

```diff
  cookie.setHttpOnly(true);
+ cookie.setSecure(true);
+ cookie.setAttribute("SameSite", "Lax");
```

**`CookieUtil.java:29-37`** — 쿠키 삭제 시 (2차 검토에서 추가)

```diff
  public static void deleteCookie(HttpServletResponse response, Cookie cookie) {
      cookie.setValue(null);
      cookie.setPath("/");
+     cookie.setHttpOnly(true);
+     cookie.setSecure(true);
+     cookie.setAttribute("SameSite", "Lax");
      cookie.setMaxAge(0);
      response.addCookie(cookie);
  }
```

### 기존 코드의 문제점

- Refresh Token이 담긴 쿠키에 `Secure` 플래그가 없으면 **HTTP(비암호화) 환경에서도 쿠키가 전송**되어 중간자 공격(MITM)으로 토큰이 탈취될 수 있었다.
- `SameSite=Lax`를 설정하여 **크로스 사이트 요청 위조(CSRF)** 공격 시 쿠키가 자동 전송되는 것을 방지한다.
- `Cookie.setAttribute()`는 Servlet 6.0(Jakarta EE 10, Spring Boot 3.x의 Tomcat 10.1)에서 지원된다. context7 Spring Boot 문서(`/spring-projects/spring-boot`)에서도 `SameSite` 속성 설정을 공식 지원함을 확인했다.
- `deleteCookie()`에도 동일한 보안 플래그를 적용했다. `addCookie()`에서만 `Secure`/`SameSite`를 설정하고 삭제 시 누락하면, 일부 브라우저에서 쿠키 속성 불일치로 삭제가 실패할 수 있다.

---

## 6. GlobalExceptionHandler — 내부 에러 메시지 노출 차단 — P2

### 코드 수정 내용

**`GlobalExceptionHandler.java:82-86`**

```diff
  @ExceptionHandler({RuntimeException.class, PersistenceException.class})
  public ResponseEntity<?> handleRuntimeException(RuntimeException e) {
-     return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(e.getMessage());
+     log.error("Internal Server Error: ", e);
+     return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("서버 내부 오류가 발생했습니다.");
  }
```

### 기존 코드의 문제점

- 예상치 못한 `RuntimeException`의 `getMessage()`에는 **DB 쿼리, 클래스 경로, 스택 정보 등 내부 구현 상세**가 포함될 수 있다. 이 정보가 API 응답으로 노출되면 공격자가 시스템 구조를 파악하는 데 악용할 수 있다.
- 에러 상세는 `log.error()`로 서버 로그에만 기록하고, 클라이언트에는 일반적인 에러 메시지만 반환한다.
- `@Slf4j` 어노테이션을 추가하여 로깅 인프라를 활성화했다.

---

## 7. GlobalExceptionHandler — ExpiredRefreshTokenException NPE 방어 — P2

### 코드 수정 내용

**`GlobalExceptionHandler.java:74-77`**

```diff
  Cookie refreshTokenCookie = CookieUtil.findCookie(request, "refresh_token_key");
- CookieUtil.deleteCookie(response, refreshTokenCookie);
+ if (refreshTokenCookie != null) {
+     CookieUtil.deleteCookie(response, refreshTokenCookie);
+ }
```

### 기존 코드의 문제점

- `findCookie()`는 쿠키를 찾지 못하면 `null`을 반환한다. null 체크 없이 `deleteCookie()`를 호출하면 **NPE가 발생하여 예외 핸들러 자체가 실패**하고, 500 에러가 클라이언트에 전달된다.

---

## 8. GlobalExceptionHandler — 잘못된 예외 클래스 import 교정 및 AccessDeniedException 상태 코드 분리 — P2

### 코드 수정 내용

**`GlobalExceptionHandler.java`** — import 교체

```diff
- import java.nio.file.AccessDeniedException;
- import java.security.SignatureException;
+ import io.jsonwebtoken.security.SecurityException;
+ import org.springframework.security.access.AccessDeniedException;
```

**`GlobalExceptionHandler.java:59-70`** — AccessDeniedException 핸들러 분리 (2차 검토에서 추가)

```diff
- // Token 예외 처리
- @ExceptionHandler({AccessDeniedException.class, SecurityException.class, ExpiredTokenException.class, ExpiredJwtException.class,
-         InvalidateTokenException.class, UnauthorizedAccessException.class})
- public ResponseEntity<?> handleUnauthorizedAccessException(Exception e) {
-     return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(e.getMessage());
- }
+ // 인가 실패 (403)
+ @ExceptionHandler(AccessDeniedException.class)
+ public ResponseEntity<?> handleAccessDeniedException(AccessDeniedException e) {
+     return ResponseEntity.status(HttpStatus.FORBIDDEN).body(e.getMessage());
+ }
+
+ // Token 예외 처리 (401)
+ @ExceptionHandler({SecurityException.class, ExpiredTokenException.class, ExpiredJwtException.class,
+         InvalidateTokenException.class, UnauthorizedAccessException.class})
+ public ResponseEntity<?> handleUnauthorizedAccessException(Exception e) {
+     return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(e.getMessage());
+ }
```

### 기존 코드의 문제점

- `java.nio.file.AccessDeniedException`은 **파일 시스템 접근 거부 예외**로, Spring Security의 인가 실패와 무관하다. 실제 Spring Security의 `AccessDeniedException`(403)을 catch하지 못하고 있었다.
- `java.security.SignatureException`은 **JCA(Java Cryptography Architecture)** 예외로, JWT 서명 검증 실패와 무관하다. JWT 서명 예외는 `io.jsonwebtoken.security.SecurityException`으로 교체했다.
- Spring Security 문서(`/websites/spring_io_spring-security_reference_6_5`)에서 `AccessDeniedException`은 **"인증은 되었지만 권한이 부족한 경우"** 에 발생하는 예외임을 확인했다. 이는 HTTP **403 Forbidden**에 해당하지만, 기존 코드에서는 토큰 예외(401)와 동일한 핸들러에 묶여 있어 **401 Unauthorized를 반환하는 의미적 오류**가 있었다. `AccessDeniedException`을 별도 핸들러로 분리하여 올바른 **403 Forbidden** 응답을 반환하도록 수정했다.

---

## 9. BoardRepository — @Param import 오류 수정 — P0

### 코드 수정 내용

**`BoardRepository.java:6`**

```diff
- import io.lettuce.core.dynamic.annotation.Param;
+ import org.springframework.data.repository.query.Param;
```

### 기존 코드의 문제점

- Lettuce(Redis 클라이언트)의 `@Param`은 Spring Data JPA의 JPQL 파라미터 바인딩과 **완전히 다른 어노테이션**이다. 잘못된 `@Param`으로 인해 JPQL named parameter(`:boardId`, `:boardStatus` 등)가 **바인딩되지 않아 런타임 쿼리 오류**가 발생할 수 있었다.
- Spring Data JPA는 파라미터 이름 기반 바인딩에 `org.springframework.data.repository.query.Param`을 사용해야 한다.

---

## 검증 결과

| 항목 | 결과 |
|------|------|
| `./gradlew compileJava` | BUILD SUCCESSFUL |
| `./gradlew compileTestJava` | BUILD SUCCESSFUL |
| 컨트롤러 통합 테스트 (45개) | 전체 통과 |
| 변경 전후 비교 (git stash) | 기존 통과 테스트에 regression 없음 |
| **2차 검토 — context7 공식 문서 대조** | **전 항목 공식 API와 일치 확인** |

## 2차 검토에서 추가 수정한 항목

| # | 항목 | 원인 | 수정 내용 |
|---|------|------|----------|
| 8 | `AccessDeniedException` 상태 코드 | import 교정 후 실제로 catch하게 되었으나 401을 반환 | 403 FORBIDDEN을 반환하는 별도 핸들러로 분리 |
| 5 | `CookieUtil.deleteCookie()` 보안 플래그 누락 | 생성 시에만 `Secure`/`SameSite` 적용, 삭제 시 미적용 | 삭제 쿠키에도 `HttpOnly`/`Secure`/`SameSite` 동일 적용 |

## 변경 파일 목록

| 파일 | 변경 유형 |
|------|----------|
| `build.gradle` | 의존성 교체 (jjwt 0.9.1 → 0.12.6, jaxb-api 제거) |
| `JwtUtil.java` | 전면 리라이트 (새 JJWT API, SecretKey 기반 서명) |
| `JwtFilter.java` | return 누락 수정, Bearer 대소문자 처리, SecurityException 교체 |
| `AuthController.java` | getToken() NPE 방어 |
| `CookieUtil.java` | Secure, SameSite 플래그 추가 (생성 + 삭제) |
| `GlobalExceptionHandler.java` | 에러 메시지 은닉, NPE 방어, 예외 import 교정, AccessDeniedException 403 분리, @Slf4j 추가 |
| `BoardRepository.java` | @Param import 교정 (Lettuce → Spring Data) |
