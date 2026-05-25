# AuthController & 인증 도메인 시니어 리뷰

> 작성일: 2026-05-13
> 리뷰 대상: `AuthController`, `AuthServiceImpl`, `JwtUtil`, `JwtFilter`, `CookieUtil`, `LoginReqDto`, `Token`/`TokenInfo`, `ShouldNotFilterPath`, `GlobalExceptionHandler`(인증 관련), `WebSecurityConfig`, `application.yaml`
> 분류: P0 (즉시 수정) / P1 (보안 강화) / P2 (품질 개선)
>
> **진행 상황 갱신: 2026-05-25** — 잔여 P2(Q-3·Q-4·Q-6 접두사 노출·Q-7 재확인·Q-8) **완료·검증**. 실제 코드 대조 + `AuthControllerTest` BUILD SUCCESSFUL로 검증(커밋 메시지 아님). 구현·검증 상세: 계획서 `260524_auth-p2-remaining-plan.md` §7
> 이전 갱신: 2026-05-22

---

## 📊 진행 상황 요약 (2026-05-25 기준)

범례: ✅ 완료 · 🟡 부분 완료 · ❌ 미착수

| 항목 | 상태 | 근거 커밋 / 비고 |
|---|---|---|
| **B-1** 토큰 검증 메서드 시멘틱 정정 | ✅ 완료 | `a7c150f` — `isTokenValid` / `parseAccessToken` 분리, dead code 제거 |
| **B-2** RefreshToken 쿠키명 오타 | ✅ 완료 | `a2d6ae8` — `CookieName.REFRESH_TOKEN_COOKIE` 상수 사용 |
| **B-3** RefreshToken 서버측 회전/재사용 탐지 | ✅ 완료 | `693af43` — Redis 화이트리스트 + jti/family + Lua 원자 회전 |
| **B-4** Access/Refresh 토큰 구분 claim | ✅ 완료 | `26c3850` — `tokenType` claim + `validateTokenType` 강제 |
| **S-1** User Enumeration | ✅ 완료 | `97844fc` — `INVALID_CREDENTIALS` 단일화, 상세 사유는 서버 로그만 |
| **S-2** 로그인 Brute Force 방지 | ✅ 완료 | `6eaeb25` — Redis+Lua 계정/IP 이중 키, 점진 지연·단계 잠금, 429+`Retry-After`(A009) |
| **S-3** 표준 에러 응답 (EntryPoint) | ✅ 완료 | `97844fc` — `SecurityErrorResponseWriter` + EntryPoint/AccessDeniedHandler, JwtFilter `sendError` 제거 |
| **S-4** Access Token raw string 반환 | ✅ 완료 | Access Token HttpOnly 쿠키 전환 + 메타 JSON 응답 (계획서 `260519_cookie-response-policy-plan.md`) |
| **S-5** JWT Secret 길이 검증 | ✅ 완료 | 32byte fail-fast (Q-1에서 `JwtSecretValidator` → `JwtProperties` compact 생성자로 이전, 메시지 포맷 유지) |
| **S-6** Refresh 쿠키 maxAge 미지정 | ✅ 완료 | `addRefreshTokenCookie`에 `refreshTokenExpiration/1000` 적용 |
| **S-7** 쿠키 SameSite/Secure/도메인 정책 | ✅ 완료 | `CookieProperties`/`CookieFactory` 외부화, `application-{dev,prod}.yaml` `security.cookie` 환경 분기 |
| **S-8** `/logout` GET | ✅ 완료 | `logout`·`reissuing-token` POST 전용화 (계획서에서 S-4와 함께 마감) |
| **Q-1** JwtUtil 정적 + @Component | ✅ 완료 | `JwtTokenProvider` 빈 신설(`SecretKey` 1회 캐싱) + `JwtProperties`(@ConfigurationProperties) SSOT. `JwtUtil`·`JwtSecretValidator` 삭제, 운영 코드 `@Value("${jwt.secret}")` 0건 |
| **Q-2** Token DTO snake_case | ✅ 완료 | `Token` 삭제 → `IssuedToken` record 통합. snake_case 게터 0건 |
| **Q-3** LoginReqDto 메시지 불일치 | ✅ 완료 | 거짓 특수문자 안내 제거, 민감 필드 `rejectedValue` 응답 마스킹 |
| **Q-4** `/token-validation` 효용 | ✅ 완료 | 미사용 공개 엔드포인트와 `AuthService.isTokenValid` 제거, RestDocs 섹션 삭제 |
| **Q-5** JWT 표준 claim | ✅ 완료 | access에 `jti`/`iss`/`aud`/`nbf` 부착 + parse 강제검증(`requireIssuer`/`requireAudience`, `clockSkewSeconds(30)`). jjwt 예외 전부 도메인 예외로 매핑 (raw 누출 차단) |
| **Q-6** role `"ROLE_ADMIN"` 하드코딩 | ✅ 완료 | 내부 authority는 `ROLE_ADMIN` 유지, `/token-role` 외부 응답은 `ADMIN`으로 변환. 프론트 `RoleGate` 전이 호환 반영 |
| **Q-7** Bearer 파싱 중복 | ✅ 완료 | `TokenResolver` 단일 경로로 쿠키 토큰 해석 통합 확인. 코드 변경 없음 |
| **Q-8** 인증 시도 audit 로그 | ✅ 완료 | `AUDIT` logger로 로그인/로그아웃/재발급/잠금/reuse 이벤트에 IP·UA·결과 기록 |

**요약**: P0 4건 전부 완료 ✅. **P1 8건 전부 완료 ✅** (S-1~S-8). **P2 전부 완료 ✅**.

**다음 권장 작업**: static docs 재생성 결과와 배포 순서 확인. Q-6은 프론트 전이 호환이 먼저 반영되어 있으므로 백엔드 `/token-role` → `ADMIN` 전환과 함께 배포 가능.

---

## 🔴 P0 — 명백한 버그 / 보안 결함

### ✅ B-1. `tokenIsExpired()` 메서드명과 반환 의미가 정반대 — **완료 (`a7c150f`)**

**위치**: `AuthServiceImpl.java:63-71`

```java
public boolean tokenIsExpired(String token) {
    try { JwtUtil.validateToken(token, secret); return true; }   // 유효하면 true ?!
    catch (Exception e) { return false; }
}
```

- 이름은 "expired"인데 **유효할 때 true**. `/api/v1/token-validation` 응답 의미가 클라이언트와 어긋남.
- `getTokenInfo()`에서 호출 결과를 **사용하지도 않음** (`tokenIsExpired(token);`만 호출) → dead code.
- `catch (Exception e)`로 광범위하게 잡아 `ExpiredTokenException`을 삼킴 → "만료"와 "서명 위조" 구분 불가.

**권장**: `isTokenValid` / `assertNotExpired`로 시멘틱 분리. `getTokenInfo`에서는 사전 호출 제거 (parser가 어차피 만료를 던짐).

---

### ✅ B-2. RefreshToken 만료 핸들러의 쿠키명 하드코딩 오타 — **완료 (`a2d6ae8`)**

**위치**: `GlobalExceptionHandler.java:39`

```java
Cookie refreshTokenCookie = CookieUtil.findCookie(request, "refresh_token_key");
```

- 실제 쿠키명은 `CookieName.REFRESH_TOKEN_COOKIE = "refresh_token"` (`CookieName.java:5`).
- 이 분기는 **절대 매치되지 않음** → 만료된 refresh_token이 클라이언트에 그대로 남아 다음 요청에서 또 401.

**수정**: `CookieUtil.findCookie(request, CookieName.REFRESH_TOKEN_COOKIE)`. 1줄 패치.

---

### ✅ B-3. RefreshToken이 서버 측에 저장되지 않음 — **완료 (`693af43`)**

- `JwtUtil.createToken`이 refresh를 단순히 만료 길게 잡은 JWT로만 발급. DB/Redis 화이트리스트 없음.
- `/api/v1/logout`은 클라이언트 쿠키만 비움 → 서버는 토큰 살아있는지 모름.
- 탈취된 refresh로 `/api/v1/reissuing-token` 무한 호출 → 14일치 access token 발급 가능.

**표준 패턴**: Refresh Rotation + Reuse Detection (Redis에 `jti` 또는 해시 저장, 사용 시 회전·기존 폐기, 동일 jti 재사용 감지 시 전체 family 폐기).

---

### ✅ B-4. AccessToken ↔ RefreshToken 구분 claim이 없음 (Token Confusion) — **완료 (`26c3850`)**

**위치**: `JwtUtil.java:54-62`

```java
String accessToken  = jwtBuild(admin.getId(), "ROLE_ADMIN", accessTokenExpiration, secret);
String refreshToken = jwtBuild(admin.getId(), "ROLE_ADMIN", refreshTokenExpiration, secret);
```

- 두 토큰의 차이는 **만료시간뿐**. `typ`/`tokenType` claim 없음.
- access token을 refresh 쿠키로 보내거나 그 역도 JwtFilter가 그대로 통과시킴. `/api/v1/reissuing-token`은 access token으로도 호출 가능.

**수정**: `typ:"access"|"refresh"` claim 강제 + validate 시 검사. 가능하면 secret 자체를 분리(`jwt.access-secret`, `jwt.refresh-secret`).

---

## 🟠 P1 — 보안/설계상 보완 필요

### ✅ S-1. User Enumeration — 로그인 실패가 두 코드로 갈림 — **완료 (`97844fc`)**

**위치**: `AuthServiceImpl.java:40-44` → `MEMBER_NOT_FOUND` vs `INVALID_PASSWORD`

- 공격자가 어떤 username이 존재하는지 응답코드로 식별 가능.
- **수정**: 로그인 실패는 한 종류(`UNAUTHORIZED` 또는 `INVALID_CREDENTIALS`)로 통일. 서버 로그에만 상세 사유 남길 것.

### ✅ S-2. 로그인 Brute Force 방지 장치 없음 — **완료 (`6eaeb25`)**

> 구현: 계획서 `260517_brute-force-protection-plan.md` 기반. Redis+Lua 원자 연산, 계정/IP
> 이중 키(SHA-256 세그먼트), sliding 윈도우 카운트, 점진 지연 + 단계별 임시 잠금,
> `TooManyLoginAttemptsException` → 전용 핸들러로 429 + `Retry-After`(`A009`).
> 트랜잭션 경계 밖에서 지연/Redis I/O 수행.

- 어드민 단일 계정이라 더 치명적. IP+계정별 슬라이딩 윈도우 시도 카운트(Redis) + 점진적 지연(또는 임시 잠금) 권장.
- Bucket4j 도입이 가장 간단.

### ✅ S-3. JwtFilter가 직접 `response.sendError(...)` — 표준 에러 응답 우회 — **완료 (`97844fc`)**

**위치**: `JwtFilter.java:46, 53-60`

- `GlobalExceptionHandler`를 거치지 않고 톰캣 기본 에러 페이지(또는 빈 401)가 나감 → 응답 포맷이 `ErrorResponse`와 달라 프론트가 분기 어려움.
- 헤더가 없으면 통과(`chain.doFilter`), 헤더는 있는데 비면 401 → 비일관.
- **수정**: `AuthenticationEntryPoint` 등록 후 `ErrorResponse` JSON으로 통일.

### ✅ S-4. Access Token을 응답 body의 raw string으로 반환 — **완료** (HttpOnly 쿠키 전환 + 메타 JSON)

**위치**: `AuthController.java:31`

```java
return ResponseEntity.ok().body(newToken.getAccess_token());
```

- 응답 포맷 통일성, 메타데이터(만료시각 등) 확장 측면에서 모두 손해.
- **수정**: `{ "accessToken": "...", "tokenType":"Bearer", "expiresIn": 600 }` JSON으로 응답.
- 보수적 안: access token도 HttpOnly 쿠키로 내려 XSS 노출 표면 축소 검토.

### ✅ S-5. JWT Secret 길이/시작시 검증 없음 — **완료** (32byte fail-fast)

- HS256은 32바이트 이상 필요. 짧은 secret이면 첫 사용 시점에 런타임 예외.
- `@PostConstruct`에서 길이 검증 + 시작 실패하는 게 안전.
- **갱신(2026-05-22, Q-1)**: 초기 구현은 `JwtSecretValidator` `@PostConstruct`였으나, Q-1에서 `JwtProperties`(record) compact 생성자로 이전(바인딩 시점 fail-fast). `JwtSecretValidator` 삭제. 메시지 포맷(`"jwt.secret must be >= 32 bytes ..."`) 유지, `JwtPropertiesTest`로 회귀 가드.

### ✅ S-6. Refresh token 쿠키가 세션 쿠키 — **완료**

**위치**: `AuthController.java:30` → `CookieUtil.addCookie(name, value)` (maxAge 미지정 오버로드)

- `refreshTokenExpiration = 14일`인데 쿠키는 브라우저 종료 시 사라짐 → 정책 불일치.
- **수정**: `addCookie(name, value, (int)(refreshTokenExpiration / 1000))` 사용.

### ✅ S-7. 쿠키 SameSite/도메인 정책 — **완료** (`CookieProperties`/`CookieFactory` 환경 분기)

**위치**: `CookieUtil.java:13`

- `SameSite=Lax` 고정. 프론트/백엔드 도메인이 다르면 cross-site POST에서 refresh 쿠키가 전송되지 않을 수 있음 → 분리 도메인이면 `SameSite=None; Secure` 필수.
- `Secure=true` 고정 → 로컬 HTTP 개발 시 쿠키 미전송. 환경별 분기 권장.
- 도메인 명시(`setDomain`) 없음.

### ✅ S-8. `/api/v1/logout`이 GET — **완료** (`logout`·`reissuing-token` POST 전용화)

- 상태 변경 액션인데 GET → prefetch/크롤러 우려, 시멘틱 불일치.
- `POST /api/v1/logout`이 RESTful. B-3의 서버측 refresh 토큰 무효화와 함께 처리.

---

## 🟡 P2 — 코드 품질 / 유지보수성

### ✅ Q-1. `JwtUtil`이 정적 메서드 모음 + `@Component` — **완료** (`260522_jwt-token-quality-plan.md`)

- 빈으로 등록만 되고 정적 호출만 됨. 인스턴스 메서드로 전환하고 secret/SecretKey/expiration을 필드 캐싱 → 매 요청 `getBytes()`/`hmacShaKeyFor()` 재생성 제거.
- 권장 구조: `JwtTokenProvider` 빈에서 `SecretKey` 1회 빌드, `createAccessToken/createRefreshToken/parse` 등 명시 메서드.

**→ 완료**: `JwtTokenProvider` 빈 신설(생성자에서 `SecretKey`·issuer·audience·expiration 1회 캐싱, `createAccessToken`/`createRefreshToken`/`parse*`/`validate*` 명시 메서드). 흩어진 `@Value("${jwt.secret}")` 5개 지점을 `JwtProperties`(@ConfigurationProperties record) 한 곳으로 SSOT화. `JwtUtil`·`JwtSecretValidator` 삭제(32byte fail-fast는 `JwtProperties` compact 생성자가 흡수). 운영 코드 `@Value("${jwt.secret}")` 0건 확인.

### ✅ Q-2. Token DTO 필드명 snake_case — **완료** (`260522_jwt-token-quality-plan.md`)

**위치**: `Token.java`

```java
private String access_token;
private String refresh_token;
```

- Java 컨벤션 위반. Lombok이 `getAccess_token()` 메서드 생성.
- 외부 JSON 키만 snake로 유지하려면 필드는 camelCase + `@JsonProperty("access_token")`.

**→ 완료**: S-4(쿠키 전환) 이후 `Token`은 외부 직렬화 경로가 없는 내부 운반체이고 `IssuedToken`(record)과 형태가 100% 중복이었음. rename 대신 **`Token` 삭제 후 `IssuedToken`으로 통합**(`AuthService.adminLogin` 반환 타입 `IssuedToken`, `AuthServiceImpl`이 `issueOnLogin` 결과를 재포장 없이 반환). snake_case 게터(`getAccess_token`/`getRefresh_token`) 잔존 0건.

### ✅ Q-3. `LoginReqDto` 메시지가 실제 규칙과 불일치 — **완료** (`260524_auth-p2-remaining-plan.md`)

**위치**: `LoginReqDto.java:13-16`

- "공백/특수문자 입력 불가능", "`!@#$%` 외 특수문자 불가" 라고 안내하지만 `@Pattern`이 없어 **실제로는 모두 통과**.
- **수정**: 로그인 DTO에는 포맷 게이트를 추가하지 않고, 실제 제약(`@NotBlank`, `@Size`)과 일치하도록 거짓 특수문자 안내를 제거. `GlobalExceptionHandler.handleValidation`에서 `password`/`token`/`secret` 계열 민감 필드의 `rejectedValue`를 `[PROTECTED]`로 마스킹.

### ✅ Q-4. `/api/v1/token-validation` 엔드포인트 효용 — **완료** (`260524_auth-p2-remaining-plan.md`)

- 보호 API 호출 시 401로 알 수 있고, 사전 검증해도 race condition 회피 안 됨 → 라운드트립 낭비.
- 필요하면 access token 만료까지 남은 초(`expiresIn`) 반환이 더 유용.
- **수정**: 현재 프론트 소비처가 없어 공개 계약을 제거. `AuthController.tokenValidate`, `AuthService.isTokenValid`, 관련 테스트와 RestDocs `auth.adoc`의 "토큰 인증" 섹션 삭제.

### ✅ Q-5. JWT 표준 claim 미사용 — **완료** (`260522_jwt-token-quality-plan.md`)

- `jti`, `iss`, `aud`, `nbf` 없음 → 향후 blacklist/다중 audience 운영 시 회수 어려움.
- 최소 `jti`(UUID)는 부여해두면 future-proofing 효과 큼.

**→ 완료**: access token에 `jti`(UUID)·`iss`·`aud`·`nbf` 부착, parser에 `requireIssuer`/`requireAudience` 강제검증 + `clockSkewSeconds(30)`. provider의 `parse*`/`validate*`가 jjwt 예외를 전부 도메인 예외로 매핑(`ExpiredJwtException`→`ExpiredTokenException`, 그 외 `JwtException`/`IllegalArgumentException`→`InvalidateTokenException`) → `require*` 강제로 새 발생하는 `MissingClaimException`/`IncorrectClaimException`이 `/reissuing-token` 500으로 누출되지 않음.
- **이월**: access `jti` 소비(블랙리스트/강제 회수)는 미도입 — claim 심기까지만. `AccessTokenClaims` record에는 jti 미노출(소비 코드 부재). 블랙리스트 저장소 도입은 별도 작업.
- **배포 주의**: `iss`/`aud` 강제검증으로 기존 발급 토큰 전량 무효화 → 어드민 재로그인 1회 필요. `application.yaml`/`application-test.yaml`에 non-blank `jwt.issuer`/`jwt.audience` 기본값 추가됨.

### ✅ Q-6. role을 `"ROLE_ADMIN"` 단일 문자열로 박아둠 — **완료** (`260522_jwt-token-quality-plan.md`, `260524_auth-p2-remaining-plan.md`)

- `/api/v1/token-role`이 `ROLE_` 접두사 그대로 프론트에 노출.
- 응답 시 `"ADMIN"`으로 변환하거나 claim을 배열로 가져가는 게 확장에 유리.

**→ 완료**: `Role` enum(`ADMIN("ROLE_ADMIN")`)으로 내부 authority를 SSOT화했고, 외부 `/token-role` 응답은 `Role.fromAuthority(authority).displayName()`으로 `"ADMIN"`을 반환한다. 프론트 `RoleGate.tsx`는 배포 순서 리스크를 줄이기 위해 `"ADMIN"`/`"ROLE_ADMIN"`을 모두 허용하는 전이 호환 상태로 변경.

### ✅ Q-7. `AuthController.getToken()` 중복 — **완료 확인** (`260524_auth-p2-remaining-plan.md`)

- `TokenResolver` 도입으로 컨트롤러와 `JwtFilter`가 쿠키 토큰 해석을 단일 경로로 사용한다. `AuthController.getToken()` 및 중복 `Bearer ` 파싱 코드는 남아 있지 않음. `authorizationHeaderOnlyCannotAccessProtectedApi` 테스트가 헤더 단독 인증 불가를 회귀 가드한다.

### ✅ Q-8. 인증 시도 audit 로그 없음 — **완료** (`260524_auth-p2-remaining-plan.md`)

- `AuthServiceImpl`에 로그인 성공/실패 로그 전무.
- 어드민 인증 시도/성공/실패는 보안 감사상 최소 INFO 레벨로 남는 게 표준 (사용자명, IP, UA, 결과).
- **수정**: 전용 `AUDIT` logger를 도입해 `LOGIN_SUCCESS`, `LOGIN_FAILURE`, `LOGIN_LOCKED`, `LOGOUT`, `TOKEN_REISSUE`, `TOKEN_REISSUE_REUSE_DETECTED`를 정형 `key=value` 한 줄로 기록. `UnauthorizedException`만 `LOGIN_FAILURE`로 기록해 잠금 이벤트와 중복되지 않게 했고, refresh reuse는 `InvalidateTokenException`의 `ErrorCode.REFRESH_TOKEN_REUSE_DETECTED`를 핸들러에서 판별해 기록한다.

---

## ✅ 잘 되어 있는 부분 (강점)

- BCrypt(`PasswordEncoder.matches`) 사용 — 평문/단순 해시 아님.
- HS256 + `Keys.hmacShaKeyFor` (jjwt 0.12.6 최신 API) — 약한 알고리즘 회피.
- Stateless 세션 정책 (`SessionCreationPolicy.STATELESS`) 명시.
- CORS allow-list가 환경변수 기반.
- `ShouldNotFilterPath`의 GET/그 외 메서드 분리 매칭 — 의도 명확, prefix 충돌(`/api/v1/categories` ↔ `/api/v1/categories-management`)도 방지.

---

## 권장 작업 순서

| 순서 | 작업 | 상태 | 비고 |
|---|---|---|---|
| 1 | **B-2 즉시 패치** | ✅ 완료 | `a2d6ae8` |
| 2 | **B-1, B-4 동시 리팩토링** | ✅ 완료 | `a7c150f`, `26c3850` — `typ` claim + 시멘틱 정정 (단 `JwtTokenProvider` 신설은 Q-1로 미착수) |
| 3 | **B-3 Refresh Token 회전 / Reuse Detection** | ✅ 완료 | `693af43` — Redis jti/family + Lua 원자 회전 |
| 4 | S-1, S-2, S-3 묶음 보안 강화 PR | ✅ 완료 | S-1·S-3 `97844fc`, S-2 `6eaeb25` |
| 5 | S-4 ~ S-7 쿠키·응답 정책 정비 | ✅ 완료 | S-4 HttpOnly 쿠키 전환·S-5 secret fail-fast·S-7 쿠키 정책 외부화·S-8 POST 전용화 (계획서 `260519_cookie-response-policy-plan.md`) |
| 6 | P2 항목 점진 개선 | ✅ 완료 | Q-1·Q-2·Q-5 완료(`260522_jwt-token-quality-plan.md`), Q-3·Q-4·Q-6·Q-7·Q-8 완료(`260524_auth-p2-remaining-plan.md`) |

---

## 변경 영향 범위 메모

- **B-1, B-4**: 토큰 형식 변경 → 운영중 발급된 모든 토큰은 무효화됨. 배포 시 사용자 재로그인 안내 필요.
- **B-3**: Redis 의존성 추가 (이미 운영중인 Redis 재사용 가능). 기존 발급 refresh와의 호환을 위해 grace period 설계 필요.
- **S-4**: 응답 포맷 변경 → 프론트엔드 `lib/apiClient.ts` / 로그인 처리 코드 동시 수정 필요.
- **S-8**: HTTP 메서드 변경 → 프론트엔드 호출부 수정 필요.
- **Q-5**: `iss`/`aud` 강제검증 도입 → 기존 발급 access/refresh 토큰 전량 무효화. 배포 시 어드민 재로그인 1회 필요(access 10분이라 영향 짧음). `jwt.issuer`/`jwt.audience`는 **non-blank** 값 필수(빈 값이면 부팅 실패).
- **Q-6**: `/token-role` 응답이 `"ADMIN"`으로 변경됨. 프론트 `RoleGate.tsx`는 `"ADMIN"`/`"ROLE_ADMIN"` 전이 호환을 반영해 배포 순서 리스크를 완화.
- **Q-4**: `/api/v1/token-validation` 공개 계약 제거. 외부 소비자가 있었다면 404가 발생하므로 배포 전 소비처 부재 재확인 필요.

P0 항목(특히 B-2, B-3)은 운영에 실제 영향이 가므로 다음 작업 세션 1순위로 권장.
