# S-4 ~ S-7 쿠키·응답 정책 정비 작업 계획서

> 작성일: 2026-05-19
> 출처: `backend/docs/260513_auth-controller-senior-review.md` — P1 / S-4·S-5·S-7
> 분류: P1 (보안 강화)
> 선행 완료: B-1·B-2·B-3·B-4, S-1·S-2·S-3·**S-6**
> 작업 범위: **backend (-b)**. S-4 의 프론트엔드 계약 변경은 **별도 `-f` 후속**(§6 부록)으로 분리.
>
> **확정된 설계 결정 (2026-05-19, 사용자 승인)**
> - 범위: 백엔드 계획서 + 프론트 변경은 `-f` 후속 분리.
> - **백엔드 PR 은 API 계약을 깨는 변경(breaking change)을 포함한다.** 백엔드 단독 배포 시
>   기존 프론트의 로그인/재발급 흐름은 **호환되지 않는다**(아래 §2.2 참조).
> - S-4: Access Token 을 **HttpOnly 쿠키**로 전환 (응답 body raw string 폐기, XSS 노출 표면 축소).
> - S-7: 운영은 **동일 사이트(같은 등록 도메인)** → `SameSite=Lax` 유지, `Secure` 만 환경 분기.

---

## 0. 재검토 이력 (2026-05-19, Codex)

| # | 쟁점 | 확정 |
|---|---|---|
| 1 | "백엔드 하위호환 유지" 오해 소지 | **breaking change** 명시. 백 단독 배포 시 구 로그인/재발급 비호환. 운영은 백→프 **동일 릴리즈 윈도우 순차** 배포 (§2.2, §5) |
| 2 | 헤더 폴백 의미 과장 | ~~"보호 API 호환용 폴백"으로 축소~~ → **최종검토 #2 로 폴백 자체 제거(아래)**. 본 행은 후속 결정으로 무효 |
| 3 | `reissuing-token` GET 잔존 | refresh 회전 = 상태 변경 → **reissuing-token 도 POST 전용화** (§3.2, §3.5, §6) |
| 4 | optional-auth 엔드포인트 영향 | access 쿠키 자동전송 → 만료/손상 쿠키가 비회원 경로(`CommentController:33`)를 401 차단. **optional-auth 경로는 invalid token 을 미인증 취급** 정책 추가 (§2.6, §3.2, §4) |
| 5 | CORS/쿠키 전송 조건 | 성공 기준에 `Allow-Credentials:true`·명시 origin·프론트 credentials include 추가 (§4.4) |
| 6 | REST Docs 갱신 누락 | login/reissue 응답·Set-Cookie 변경 → REST Docs 스니펫 갱신 범위 포함 (§3.5, §4) |
| 7 | S-4 refresh 토큰 표현 부정확 | refresh 는 이미 HttpOnly — "직접 읽기는 어렵지만 XSS 환경서 인증요청 악용 가능"으로 정정 (§1) |

**최종 검토 (2026-05-19, Codex 2차) — 추가 확정 4건**

| # | 쟁점 | 확정 |
|---|---|---|
| 1 | `JwtFilter` 주입 경로 누락 | `WebSecurityConfig.java:62` `new JwtFilter(authService, secret)` 직접 생성 → **`WebSecurityConfig` 를 변경 대상에 추가**, 의존성 주입(또는 빈 등록)으로 변경 (§3.5) |
| 2 | 쿠키 우선 ↔ 헤더 폴백 충돌 | **사용자 결정: Authorization 헤더 폴백 전면 제거 (hard cutover)**. 인증은 `access_token` 쿠키 단일 경로. 이전 "폴백 유지" 결정은 **본 결정으로 무효**. 우선순위 충돌 자체가 소멸 (§2.2, §3.2, §3.6, §4, §5) |
| 3 | `domain` 빈 문자열 | `CookieFactory` 는 `StringUtils.hasText(domain)` 일 때만 `domain` 설정. **삭제 쿠키도 생성 쿠키와 동일 domain/path** 사용(삭제 안정성) (§3.4) |
| 4 | "전 권한 탈취" 과한 표현 | §1 "해결하지 않으면" 의 표현을 "access token 탈취 및 인증 요청 악용"으로 톤 정정 (§1) |

> **주의(범위 변화)**: #2 로 전환 모델이 "쿠키 우선 + 헤더 폴백"에서 **hard cutover**
> 로 바뀐다. 백엔드 배포 즉시 모든 구 클라이언트가 인증 실패하므로, 프론트(`-f`)
> 배포·재로그인 완료 전까지의 **점검 창(maintenance window)** 운영이 사실상 필수다(§5).

**최종 검토 (2026-05-19, Codex 3차) — 추가 확정 2건**

| # | 쟁점 | 확정 |
|---|---|---|
| 1 | `token-validation`/`token-role` invalid 쿠키 처리 모호 | 필터=익명 강등이지만 두 조회 엔드포인트는 **고유 계약 확정**: `token-validation` invalid→`200 false`, `token-role` invalid→표준 `401`. 의도된 분리임을 주석화 (§3.2, §4) |
| 2 | refresh 실패 시 access 쿠키 잔존 | refresh 무효/만료 **모든 지점**(reissuing 실패·`ExpiredRefreshTokenException`·`InvalidateTokenException`)에서 logout 과 동일하게 **auth 쿠키 전체 삭제**(`CookieFactory.expireAuthCookies`). stale access 쿠키 잔존·왕복 차단 (§3.2, §3.3, §3.4, §3.5, §4) |

---

## 1. Problem (문제 정의)

### 무엇을 해결하는가
시니어 리뷰 P1 중 쿠키·응답 정책 4건. S-6(refresh 쿠키 maxAge)은 커밋 완료라
**실제 작업 대상은 S-4·S-5·S-7**.

| 항목 | 현재 상태 | 결함 |
|---|---|---|
| **S-4** | `AuthController.login` / `reissuingAccessToken` 이 access token 을 `ResponseEntity<String>` **raw 문자열**로 반환. 프론트가 Redux + `redux-persist`(localStorage)에 저장 | 응답 포맷 비표준, 메타데이터 확장 불가, **localStorage 저장 → XSS 시 토큰 탈취 표면 노출** |
| **S-5** | `JwtUtil.getSigningKey` 가 매 호출 `Keys.hmacShaKeyFor(secret.getBytes())`. 길이 검증 없음 | HS256 은 키 ≥ 32 byte 필요. 짧은 `JWT_SECRET_KEY` 주입 시 **첫 토큰 발급/검증 런타임에야 폭발** — 기동은 성공해 탐지가 늦음 |
| **S-7** | `CookieUtil` 이 `Secure=true`·`SameSite=Lax` **하드코딩**, `setDomain` 없음 | 로컬 HTTP 개발 시 `Secure=true` 라 **쿠키 미전송**(개발 불편 + 환경 분기 부재). 정책이 코드에 박혀 운영 변경 시 재빌드 필요 |

### 왜 중요한가
- S-4: 단일 어드민 access token 이 localStorage 에 평문 저장 → 에디터(TipTap)·Markdown·외부
  스크립트 등 XSS 경로 하나만 뚫려도 **access token 은 JS 로 직접 탈취 가능**.
  refresh token 은 이미 HttpOnly 쿠키라 JS 가 값을 직접 읽지는 못하지만, **XSS 환경에서는
  같은 출처로 인증 요청을 위조·악용**할 수 있다. access 도 HttpOnly 쿠키로 옮기면 토큰
  값의 JS 노출 자체를 제거한다(악용 표면 축소, 완전 제거는 아님 — §5 잔여 위험).
- S-5: 운영 비밀키 교체/오타 시 "기동은 됐는데 로그인만 안 되는" 침묵 장애. fail-fast 가 표준.
- S-7: 로컬 HTTP 에서 쿠키가 안 붙어 인증 흐름 디버깅이 어렵고, 정책 하드코딩은 운영 도메인
  변경·SameSite 정책 조정을 코드 수정으로 강제한다.

### 해결하지 않으면
XSS 1회로 어드민 access token 탈취 및 인증 요청 악용(S-4), 비밀키 설정 오류의 지연
발견(S-5), 환경별 쿠키 디버깅 비용·운영 경직(S-7)이 계속된다.

---

## 2. Analyze (분석 및 선택지 검토)

### 2.1 S-4 — Access Token 전달 방식

| 선택지 | 장점 | 단점 | 판정 |
|---|---|---|---|
| JSON body `{accessToken, tokenType, expiresIn}` 유지 | 변경 폭 최소, 프론트 토큰 모델 유지 | localStorage 노출 표면 그대로 — XSS 방어 무개선 | 기각 |
| **Access Token 도 HttpOnly 쿠키** | JS 접근 차단으로 XSS 탈취 표면 제거, refresh 쿠키와 일관 | 프론트 토큰 모델 전면 변경, CSRF 표면 신설(쿠키 ambient) | **채택 (사용자 결정)** |

- **CSRF 트레이드오프**: access 까지 쿠키가 되면 인증이 ambient(자동 전송)가 되어 CSRF 대상이 된다.
  완화책: ① 운영이 **동일 사이트**(2.3) → cross-site 요청에 `SameSite=Lax` 가 쿠키를
  withhold. ② 상태 변경 API 는 전부 비-GET(POST/PUT/DELETE)이고 Lax 는 cross-site 비-GET 에
  쿠키를 보내지 않음. → 본 PR 범위에서 SameSite=Lax + 동일 사이트로 **실용적 CSRF 방어 성립**.
  잔여 위험(동일 사이트 내 XSS 기반 CSRF, GET 부작용 엔드포인트)은 §5 리스크에 기록하고
  CSRF 토큰 도입은 후속 과제로 분리(스코프 규율 — 리뷰가 S-4 에서 CSRF 토큰을 요구하지 않음).
- **S-8 연동 메모**: `/api/v1/logout` 이 아직 GET 도 허용(S-8 부분완료). 인증이 쿠키가 되면
  GET 부작용 우려가 커지므로, 본 PR 에서 **logout 을 POST 전용으로 좁히는 것**을 함께 처리(저비용).

### 2.2 S-4 — 전환 호환성 (백→프 동일 릴리즈 윈도우)

> **이것은 breaking change 다.** 백엔드가 `/login`·`/reissuing-token` 응답 body 에서
> access token 문자열을 제거하면, 현재 프론트(`LoginContent.tsx:26` 가 body 를 토큰으로
> 간주해 Redux 저장)는 **로그인·재발급이 깨진다**. "백엔드 단독 배포 시 구 프론트가
> 그대로 동작한다"는 성립하지 않는다.

**전환 모델: Hard Cutover (헤더 폴백 없음 — 최종검토 #2, 사용자 결정)**

Authorization 헤더 인증 경로를 **전면 제거**한다. 인증은 `access_token` 쿠키 단일
경로뿐이다. (이전 라운드의 "쿠키 우선 + 헤더 폴백" 결정은 본 결정으로 **무효**.)

| 항목 | 내용 |
|---|---|
| 폴백 | **없음.** `JwtFilter`·`AuthController` 어디서도 `Authorization` 헤더를 보지 않음 |
| 백엔드 배포 즉시 영향 | 구 프론트가 보유한 헤더 토큰은 **무시됨** → 모든 구 클라이언트가 보호 API·로그인·재발급 전부 인증 실패 |
| 회복 조건 | 프론트(`-f`) 배포 + 어드민 **재로그인**(쿠키 신규 발급)까지 어드민 기능 사용 불가 |

**배포 원칙 (필수)**:
- 백엔드 PR 은 API 계약 변경 + 헤더 인증 제거를 **포함**한다(완전한 breaking change).
- 백엔드와 프론트(`-f`)를 **같은 릴리즈 윈도우에서 연속 배포**하고, 그 사이 어드민
  기능이 전면 중단되므로 **점검 창(maintenance window) 공지·운영을 사실상 필수**로 한다.
- "헤더 폴백으로 구 클라이언트 완충"은 더 이상 존재하지 않음 — 구현·릴리즈 노트에서
  폴백을 언급하지 말 것.

> 트레이드오프: 폴백 제거로 `JwtFilter`/전환 로직이 최소화되고 우선순위 충돌(최종검토 #2)이
> 원천 소멸한다. 대가는 전환 창의 어드민 다운타임 — 단일 어드민·저트래픽이라 점검 창으로
> 흡수 가능하다는 판단(사용자 결정).

### 2.3 S-7 — 쿠키 정책 외부화

- 운영 도메인 = **동일 등록 도메인**(사용자 확정) → `SameSite=Lax` 유지로 충분, `SameSite=None`
  불필요, `setDomain` 필수 아님(설정 가능하게만 열어둠).
- 분기 필요한 축은 사실상 **`Secure`** 하나(운영 HTTPS=true, 로컬 HTTP=false).
- 정책 외부화 방식: 기존 확립 패턴인 `@ConfigurationProperties` + 전용
  `@Configuration @EnableConfigurationProperties`(예: `LoginAttemptConfig`,
  `RevalidateWebhookConfig`) 재사용. 전역 `@ConfigurationPropertiesScan` 없음을 고려해
  **전용 config 에서 명시 enable**.

### 2.4 S-5 — 검증 위치

- `JwtUtil` 은 정적 메서드 + `@Component`(Q-1 미해결). `@PostConstruct` 를 정적 유틸에 직접
  달기 부적합 → **별도 `JwtSecretValidator @Component`** 에 `@Value("${jwt.secret}")` +
  `@PostConstruct` 로 길이 검증. Q-1(JwtTokenProvider 신설) 리팩토링과 **분리**(스코프 규율).
- 실패 시 `IllegalStateException` → 컨텍스트 로딩 실패 = 기동 실패(fail-fast).

### 2.5 S-7 — `CookieUtil` 정적 한계

`CookieUtil` 은 정적이라 빈 주입 불가. 또한 auth 외 쿠키(`user_n`, `viewed_posts`,
`liked_posts`)도 동일 유틸을 쓸 수 있어 무분별 변경은 사이드이펙트 위험.
→ **본 PR 은 auth 쿠키(`refresh_token`, 신규 `access_token`)만** 정책 외부화 대상.
방문자/조회/좋아요 쿠키는 현행 유지(별도 후속). `CookieUtil` 사용처는 구현 착수 시 grep 으로
전수 확인하여 auth 경로만 신규 `CookieFactory` 로 이관.

### 2.6 S-4 — Optional-auth 엔드포인트 영향 (재검토 #4)

**문제**: 현재 댓글 작성/수정/삭제(`CommentController:33` 외)는
`Principal principal; boolean isAdmin = principal != null` 패턴 — **JWT 있으면 어드민,
없으면 비회원**. 헤더 방식에서는 비회원이 애초에 `Authorization` 헤더를 안 보내므로
`JwtFilter` 가 그냥 통과(익명)시킨다.

access 가 **쿠키로 자동 전송**되면, 한 번 로그인했다 만료된 사용자의 브라우저는
**만료/손상된 `access_token` 쿠키를 모든 요청에 자동 첨부**한다. 현재 `JwtFilter` 는
유효하지 않은 토큰을 만나면 `SecurityErrorResponseWriter` 로 **401 을 쓰고 `return`**
→ 비회원도 쓸 수 있어야 할 댓글 작성이 **401 로 막힌다**(회귀).

| 선택지 | 장점 | 단점 | 판정 |
|---|---|---|---|
| 현행 유지(invalid → 항상 401) | 단순 | optional-auth 비회원 경로 회귀 | 기각 |
| **invalid/expired access 쿠키를 "미인증"으로 취급** (인증 세팅 없이 chain 계속, 보호 경로는 EntryPoint 가 401) | 비회원 흐름 보존, 보호 경로 보안 유지 | JwtFilter 분기 추가 | **채택** |

- 인증 경로가 **쿠키 단일**(헤더 폴백 제거, §2.2)이므로 분기가 단순하다:
  `JwtFilter` 에서 **access 쿠키가 invalid/expired 이면 401 쓰지 않고 인증 미설정 상태로
  `filterChain.doFilter` 계속**(익명 강등). 보호가 필요한 경로는 Spring Security 인가 +
  `AuthenticationEntryPoint`(S-3)가 깔끔한 401 `ErrorResponse` 처리 → 정책 일관.
- "헤더 출처는 엄격 401" 같은 출처별 분기는 **불필요**(헤더 인증 자체가 없음).
- 손상 쿠키 누적 방지: invalid access 쿠키 감지 시 응답에 해당 쿠키 만료(`Set-Cookie maxAge=0`,
  생성 쿠키와 동일 domain/path)를 함께 내려 다음 요청부터 깨끗하게.

---

## 3. Action (구현 계획 및 설계)

### 3.1 목표 및 범위
- 범위 내: S-4(access HttpOnly 쿠키 전환 + **헤더 인증 제거** + 응답 메타 JSON), S-5(secret fail-fast),
  S-7(auth 쿠키 정책 외부화·환경 분기), S-8 잔여(logout·reissuing-token POST 전용화).
- 범위 밖: 프론트 계약 변경(§6 `-f` 후속), Q-1(JwtTokenProvider), CSRF 토큰, 비-auth 쿠키 이관.

### 3.2 S-4 설계

**쿠키 신설**: `CookieName.ACCESS_TOKEN_COOKIE = "access_token"`.

> **완전한 breaking change 명시**: ① `/login`·`/reissuing-token` 응답 body 에서 access
> token 문자열이 **사라지고**, ② `Authorization` 헤더 인증 경로가 **제거**된다(§2.2 hard
> cutover). 구 클라이언트는 로그인·재발급뿐 아니라 보호 API 접근도 전부 실패하므로 점검
> 창에서 백→프 연속 배포 + 재로그인이 필요하다.

**`AuthController` 변경**
| 엔드포인트 | 변경 |
|---|---|
| `POST /login` | refresh 쿠키(현행) + **access_token HttpOnly 쿠키** (maxAge = `access-token-expiration/1000`) 세팅. 응답 body: **토큰 없는** 메타 JSON `{ "tokenType":"Bearer", "expiresIn":600 }` (프론트 선제 refresh 스케줄용). ← **raw string 폐기 = breaking** |
| `reissuing-token` | 성공: 회전된 refresh 쿠키 + 새 access_token 쿠키 세팅, body 메타 JSON. **실패(만료/무효): refresh + access 쿠키 모두 삭제**(최종검토 #2). **POST 전용으로 축소**(재검토 #3) |
| `logout` | refresh + **access_token 쿠키 모두 삭제**. **POST 전용으로 축소**(S-8 잔여 마감) |
| `token-role` / `token-validation` | 토큰 해석을 **`access_token` 쿠키에서만** 추출(헤더 파싱 제거) + **invalid 쿠키 처리 정책 확정**(아래) |

> 상태 변경 3종(`login`·`reissuing-token`·`logout`) 전부 **POST 전용**으로 통일 → access/
> refresh 쿠키 기반에서 SameSite=Lax 의 CSRF 방어 논리(비-GET 에 cross-site 쿠키 미전송)가
> 일관되게 성립.

**invalid 쿠키 처리 정책 — `token-validation` / `token-role` (최종검토 #1)**

`JwtFilter` 는 invalid/expired 쿠키를 익명 강등(401 아님)으로 통과시키므로, 이 두 GET
조회 엔드포인트가 같은 요청의 invalid 쿠키 값을 다시 해석할 때 동작을 **명시적으로
확정**한다(필터=관용 / 컨트롤러=중복 예외 의 어색한 흐름 방지):

| 엔드포인트 | 쿠키 없음 / invalid / expired | 유효 |
|---|---|---|
| `GET /token-validation` | **`200` + body `false`** (예외 던지지 않음 — "로그인 상태 확인" 성격). `authService.isTokenValid` 가 이미 예외를 잡아 `false` 반환하므로 그 계약을 그대로 유지 | `200` + `true` |
| `GET /token-role` | **표준 `401` `ErrorResponse`** (S-3 일관). 쿠키 미존재·파싱 실패·만료 모두 동일하게 401 — role 은 인증 전제 정보 | `200` + role |

- 두 엔드포인트는 `JwtFilter` 의 익명 강등과 **모순이 아니라 의도된 분리**다:
  필터는 optional-auth 보존용(관용), 이 두 조회는 각자 고유 계약(검증=불리언 / 역할=401).
  구현 시 이 표를 주석으로 남겨 "필터에서 통과시켰는데 컨트롤러에서 또 던진다"는
  혼란을 차단한다.
- `token-role` 의 401 도 `SecurityErrorResponseWriter`/`GlobalExceptionHandler` 표준
  `ErrorResponse` 포맷을 따른다(S-3). invalid 쿠키 감지 시 `Set-Cookie maxAge=0`
  (생성과 동일 domain/path) 동봉해 stale 쿠키를 정리.

**토큰 해석 (`TokenResolver`, 쿠키 단일)**
- 신규 `utils/TokenResolver.java`: `resolve(HttpServletRequest)` =
  `access_token` 쿠키 값 → 없으면 null. **`Authorization` 헤더 파싱 없음**(헤더 인증 제거).
- `JwtFilter`·`AuthController` 의 토큰 조회가 이 헬퍼 1곳을 사용(중복 제거).
- 기존 `AuthController.getToken` 의 `Bearer ` 파싱·`JwtFilter` 의 헤더 파싱 로직은 **삭제**.
  (Q-7 의 "헤더 파싱 중복"은 헤더 경로 제거로 자연 소멸.)

**`JwtFilter` 변경**
- `request.getHeader(AUTHORIZATION)` 직접 파싱 **삭제** → `TokenResolver.resolve(request)`
  (쿠키 단일)로 교체.
- 쿠키 없으면 `filterChain.doFilter`(통과 — 보호 경로는 EntryPoint 가 401).
- **invalid/expired 쿠키 분기 (재검토 #4, optional-auth)**: 401 쓰지 않고 인증 미설정으로
  `filterChain.doFilter` 계속(익명 강등) + 손상 `access_token` 쿠키 만료(`Set-Cookie
  maxAge=0`, 생성 쿠키와 동일 domain/path) 첨부. 보호 경로는 Spring Security 인가 +
  `AuthenticationEntryPoint`(S-3)가 401 처리. (출처별 분기 없음 — 쿠키 단일.)
- 정상 토큰 경로·인증 세팅 로직은 현행 유지.

**Auth 쿠키 전체 삭제 정책 (최종검토 #2)**

refresh 실패 상황에서 `refresh_token` 만 지우면 브라우저에 **stale `access_token` 쿠키가
잔존** → 이후 매 요청 `JwtFilter` 가 만료 쿠키를 보고 익명 강등/삭제를 반복하거나
프론트 refresh 재시도 흐름과 엮여 불필요한 왕복이 생긴다.

→ **auth 쿠키는 항상 한 묶음으로 삭제**한다. `CookieFactory.expireAuthCookies(response)`
(가칭) 하나로 `refresh_token` + `access_token` 을 동시에 만료(`maxAge=0`, 생성과 동일
domain/path)시키고, 아래 경로 전부가 이 메서드를 호출:

| 경로 | 동작 |
|---|---|
| `POST /logout` | auth 쿠키 전체 삭제 (현행 의도 유지) |
| `reissuing-token` 실패 (catch `InvalidateTokenException` / `ExpiredRefreshTokenException`) | refresh 만 지우던 현행 → **auth 쿠키 전체 삭제** 후 예외 재던짐 |
| `GlobalExceptionHandler` `ExpiredRefreshTokenException` 핸들러 | refresh 쿠키만 삭제하던 현행 → **auth 쿠키 전체 삭제** |
| `GlobalExceptionHandler` `InvalidateTokenException`(refresh 무효) 경로 | 동일하게 **auth 쿠키 전체 삭제** |
| `JwtFilter` invalid access 쿠키 | access 쿠키 만료(앞 항목). refresh 는 건드리지 않음(refresh 유효성은 reissuing 경로 책임) |

- 일관 규칙: **"refresh 가 무효/만료로 판명되는 모든 지점 = auth 쿠키 전체 삭제"**.
  로그아웃과 동일 정책으로 묶어 stale access 쿠키 잔존·왕복을 원천 차단.

### 3.3 S-5 설계
- 신규 `configuration/JwtSecretValidator.java` (`@Component`):
  ```
  @Value("${jwt.secret}") String secret;
  @PostConstruct void validate() {
      int bytes = secret.getBytes(StandardCharsets.UTF_8).length;
      if (bytes < 32) throw new IllegalStateException(
          "jwt.secret must be >= 32 bytes for HS256 (current: " + bytes + ")");
  }
  ```
- 비밀값 자체는 로그/예외 메시지에 **미출력**(바이트 수만).

### 3.4 S-7 설계
- 신규 `configuration/CookieProperties.java`
  `@ConfigurationProperties("security.cookie")` (record): `secure(boolean)`,
  `sameSite(String, 기본 "Lax")`, `domain(String, nullable)`, `path(String, 기본 "/")`.
- 신규 `configuration/CookieConfig.java`: `@Configuration` +
  `@EnableConfigurationProperties(CookieProperties.class)` (패턴 일관).
- 신규 `utils/CookieFactory.java` (`@Component`): `CookieProperties` 주입,
  `accessTokenCookie(value)` / `refreshTokenCookie(value)` / `expire(name)` /
  **`expireAuthCookies(response)`**(refresh+access 동시 만료, 최종검토 #2) 생성.
  속성(secure/sameSite/path/httpOnly=true) 일괄 적용.
- **`domain` 빈 문자열 처리 (최종검토 #3)**: `${COOKIE_DOMAIN:}` 는 `null` 이 아니라
  **빈 문자열**로 바인딩된다. `CookieFactory` 는 `StringUtils.hasText(domain)` 일 때만
  `setDomain`/`domain(...)` 을 호출하고, 그 외에는 domain 속성을 **아예 설정하지 않는다**
  (`setDomain("")` 호출 시 브라우저/서버 동작 모호). **삭제(만료) 쿠키도 생성 쿠키와
  동일한 domain/path** 로 만들어야 브라우저가 안정적으로 삭제한다 — `expire(name)` 가
  생성 시와 같은 domain/path 를 적용하도록 구현.
- `AuthController`·`GlobalExceptionHandler`(refresh 만료 쿠키 삭제)의 auth 쿠키 생성·삭제를
  `CookieFactory` 로 이관. `CookieUtil.findCookie`(순수 조회)는 정적 유지.
- 설정값 (`application*.yaml`, env placeholder):
  | 프로파일 | secure | sameSite | domain |
  |---|---|---|---|
  | 공통(`application.yaml`) | `${COOKIE_SECURE:true}` | `${COOKIE_SAMESITE:Lax}` | `${COOKIE_DOMAIN:}` |
  | dev | `false` | `Lax` | (미설정) |
  | prod | `true` | `Lax` | (동일 사이트라 미설정 기본; 필요 시 env) |

### 3.5 신규/변경 파일

**신규**
| 파일 | 역할 |
|---|---|
| `utils/TokenResolver.java` | access 토큰 해석: `access_token` 쿠키 단일(헤더 파싱 없음) |
| `configuration/JwtSecretValidator.java` | S-5 기동 시 secret 길이 fail-fast |
| `configuration/CookieProperties.java` | `security.cookie` 바인딩 |
| `configuration/CookieConfig.java` | `@EnableConfigurationProperties` 등록 |
| `utils/CookieFactory.java` | 정책 적용 auth 쿠키 생성/삭제 |

**변경**
| 파일 | 변경 |
|---|---|
| `constants/CookieName.java` | `ACCESS_TOKEN_COOKIE = "access_token"` 추가 |
| `controller/AuthController.java` | login access 쿠키 세팅·메타 JSON, **`reissuing-token`·`logout` POST 전용**(GET 매핑 제거), 양 쿠키 삭제, getToken→`TokenResolver`(쿠키 단일, Bearer 파싱 삭제) |
| `configuration/JwtFilter.java` | 헤더 파싱 삭제 → `TokenResolver`(쿠키 단일)로 교체, invalid 쿠키=익명강등+쿠키만료 분기(재검토 #4). 생성자에 `TokenResolver`/`CookieFactory` 의존성 추가 |
| **`configuration/WebSecurityConfig.java`** (최종검토 #1) | `WebSecurityConfig.java:62` `new JwtFilter(authService, secret)` → 신규 의존성(`TokenResolver`,`CookieFactory`) 주입 형태로 변경. `JwtFilter` 를 `@Bean` 등록 후 `addFilterBefore(jwtFilter, ...)` 로 전환하거나, config 에서 빈 주입해 생성자 인자 확장 |
| `exception/GlobalExceptionHandler.java` | `ExpiredRefreshTokenException`·`InvalidateTokenException`(refresh 무효) 처리 시 **auth 쿠키 전체 삭제**(`CookieFactory.expireAuthCookies`, 생성과 동일 domain/path) — 최종검토 #2 |
| `domain/token/...` 응답 DTO | 메타 응답용 `TokenMetaResponse(tokenType, expiresIn)` 신설(또는 record) |
| `resources/application.yaml`/`-dev`/`-prod` | `security.cookie` 블록 |
| **REST Docs 스니펫** (재검토 #6) | `AuthControllerTest` 의 login/reissuing 응답이 raw string→JSON meta, `Set-Cookie` 가 refresh+access 2개로 변경 → 해당 `*.adoc` include/스니펫 재생성. logout/reissuing GET→POST 문서도 갱신 |
| 테스트 | `AuthControllerTest`, 신규 `JwtSecretValidatorTest`·`CookieFactoryTest`, `JwtFilterTest`·`CommentControllerTest`(optional-auth) 보강 |

### 3.6 트레이드오프 및 대응
| 이슈 | 대응 |
|---|---|
| access 쿠키화로 CSRF 표면 신설 | 동일 사이트 + `SameSite=Lax` + 상태변경 API(`login`/`reissuing-token`/`logout`) **전부 POST 전용** → cross-site 비-GET 에 쿠키 미전송으로 실용 방어. CSRF 토큰은 후속 분리(리스크 기록) |
| **구 프론트 호환 없음 (hard cutover)** | 헤더 인증 제거로 폴백 자체가 없음. 백엔드 배포 즉시 구 클라이언트 전부 인증 실패 → **점검 창에서 백→프 연속 배포 + 어드민 재로그인** 필수(§2.2, §5). 단일 어드민·저트래픽이라 다운타임 흡수 가능(사용자 결정) |
| optional-auth 경로가 만료 쿠키로 401 회귀 | invalid access 쿠키 → **익명 강등**(401 미발생) + 손상 쿠키 만료 첨부. 인증 경로가 쿠키 단일이라 출처별 분기 불필요 (재검토 #4) |
| 메타 JSON 에 토큰 미포함 → 프론트가 만료 시점 모름 | `expiresIn` 제공으로 선제 refresh 가능. 그래도 401→refresh 인터셉터가 최종 안전망 |
| `CookieUtil` 광범위 사용 → 사이드이펙트 | auth 쿠키만 `CookieFactory` 이관, 사용처 grep 전수 확인, 비-auth 쿠키 현행 유지 |
| `Secure=false`(dev) 운영 유출 위험 | 기본값 `true`, dev 프로파일에서만 false. prod yaml 은 `true` 고정 |
| S-5 가 기존 정상 secret 까지 막을 위험 | 32 byte 미만만 차단(HS256 규격). 운영 `JWT_SECRET_KEY` 길이 사전 점검 메모(§5) |

---

## 4. Result (검증 계획)

### 4.1 단위/슬라이스 테스트
- **`JwtSecretValidatorTest`**: 31 byte → 컨텍스트/빈 초기화 `IllegalStateException`,
  32 byte 이상 → 통과. (비밀값 로그 미출력 확인)
- **`CookieFactoryTest`**: dev 프로파일 → `Secure=false`,`SameSite=Lax`,`HttpOnly=true`;
  prod → `Secure=true`. `domain` 설정 시 반영, 미설정 시 미포함. maxAge 정확.
- **`JwtFilterTest` 보강**: ① 유효 `access_token` 쿠키 → 인증 성공
  ② 쿠키 없음 → 통과(미인증, 보호 경로는 EntryPoint 401)
  ③ **invalid/expired 쿠키 → 401 아님, 익명 진행 + `Set-Cookie access_token maxAge=0`**
  ④ **`Authorization` 헤더만 있고 쿠키 없음 → 헤더 무시(미인증)** — 헤더 인증 제거 회귀 검증.
- **`CommentControllerTest` 보강 (재검토 #4)**: 만료 `access_token` 쿠키를 단
  비회원 댓글 작성 `POST /api/v1/comments/{postId}` → **401 아님, 200 비회원 처리**.
- **`CookieFactoryTest` 보강 (최종검토 #2)**: `expireAuthCookies` 가 `refresh_token`·
  `access_token` **둘 다** `maxAge=0` + 생성과 동일 domain/path 로 만료.

### 4.2 통합 테스트 — `AuthControllerTest`
- `POST /login` 성공 → `Set-Cookie: access_token`(HttpOnly) + `refresh_token`,
  body = `{tokenType:"Bearer", expiresIn:600}`, **body 에 토큰 문자열 없음**.
- `reissuing-token` **POST** → 새 access_token 쿠키 + 회전 refresh 쿠키, 동일 메타 body.
  **GET /reissuing-token 은 405**.
- `logout` **POST** → access_token·refresh_token 삭제(maxAge=0). **GET /logout 은 405**.
- **reissuing-token 실패(만료/무효 refresh) → `refresh_token`·`access_token` 둘 다
  삭제(maxAge=0)** + 표준 에러 응답(최종검토 #2). `ExpiredRefreshTokenException`/
  `InvalidateTokenException` 경로 모두 동일 검증.
- **`GET /token-validation`**: 쿠키 없음/invalid/expired → `200` body `false`; 유효 → `true`.
- **`GET /token-role`**: 쿠키 없음/invalid/expired → 표준 `401` `ErrorResponse`; 유효 → role (최종검토 #1).
- 보호 API 를 access_token 쿠키로 호출 → 200 (헤더 없이).
- 보호 API 를 구 방식 `Authorization` 헤더로만 호출 → **401**(헤더 인증 제거, hard cutover 검증).
- **REST Docs(재검토 #6)**: login/reissuing 스니펫이 JSON meta 응답 + `Set-Cookie`
  2개(refresh/access)로 재생성됨을 확인(`./gradlew bootJar` 시 `*.adoc` 빌드 깨지지 않음).
- secret/토큰 형식 회귀: S-1(enumeration), S-3(표준 에러) 영향 없음.

### 4.3 실행 범위 (CLAUDE.md §4)
```bash
./gradlew test --tests "com.moya.myblogboot.configuration.JwtSecretValidatorTest"
./gradlew test --tests "com.moya.myblogboot.utils.CookieFactoryTest"
./gradlew test --tests "com.moya.myblogboot.configuration.JwtFilterTest"
./gradlew test --tests "com.moya.myblogboot.controller.AuthControllerTest"
./gradlew test --tests "com.moya.myblogboot.controller.CommentControllerTest"
```
전체 테스트 실행 금지. 위 5개 클래스만 선택 실행.

### 4.4 성공 기준
- [ ] login/reissuing 이 access token 을 **HttpOnly 쿠키**로 내리고 body 에 토큰 미포함(S-4, breaking).
- [ ] **`Authorization` 헤더 인증 경로 완전 제거** — 헤더만으로 보호 API 호출 시 401(hard cutover).
- [ ] `WebSecurityConfig` 가 `JwtFilter` 에 `TokenResolver`/`CookieFactory` 를 주입(최종검토 #1).
- [ ] `logout`·`reissuing-token` POST 전용, GET 405, 양 auth 쿠키 삭제(S-8 잔여 마감, 재검토 #3).
- [ ] optional-auth(`CommentController`)에 만료 access 쿠키 첨부 시 **401 아님, 비회원 처리**(재검토 #4).
- [ ] `token-validation` invalid 쿠키 → `200 false`, `token-role` invalid 쿠키 → 표준 `401`(최종검토 #1).
- [ ] refresh 만료/무효 모든 지점(reissuing 실패·`ExpiredRefreshTokenException`·
      `InvalidateTokenException`)에서 **refresh+access 쿠키 동시 삭제**(최종검토 #2).
- [ ] `jwt.secret` 32 byte 미만이면 **기동 실패**(S-5), 비밀값 로그 미노출.
- [ ] auth 쿠키 속성이 프로파일별로 분기(dev `Secure=false` / prod `true`), `SameSite=Lax`(S-7).
- [ ] `CookieProperties` 가 `CookieConfig` `@EnableConfigurationProperties` 로 주입.
- [ ] **CORS/쿠키 전송 조건(재검토 #5)**: 응답 `Access-Control-Allow-Credentials: true`,
      `cors.allowed-origins` 가 `*` 아닌 명시 origin, 프론트(`-f`)가 `withCredentials`/
      `credentials:"include"` 사용 — 셋 다 충족해야 access 쿠키 인증 동작.
- [ ] REST Docs 스니펫(login/reissuing/logout)이 갱신되어 `bootJar` 문서 빌드 통과(재검토 #6).
- [ ] 비-auth 쿠키(`user_n` 등) 동작 회귀 없음.

---

## 5. 변경 영향 범위 / 리스크 메모
- **배포 순서(필수, hard cutover breaking change)**: 본 백엔드 PR 은 `/login`·
  `/reissuing-token` 응답 계약을 깨고 **`Authorization` 헤더 인증을 제거**한다. 폴백이
  없으므로 백엔드 배포 **즉시 모든 구 클라이언트(로그인·재발급·보호 API 전부)가 인증
  실패**한다. → **점검 창(maintenance window) 공지** 후 ① 백엔드(본 PR) 배포 → ② `-f`
  프론트 배포를 **연속 완료** → ③ 어드민 **재로그인**(쿠키 신규 발급)으로 회복. 점검 창
  동안 어드민 기능 전면 중단됨을 릴리즈 노트에 명시(단일 어드민·저트래픽이라 수용 가능).
- **운영 사전 점검**: `JWT_SECRET_KEY` 가 32 byte 이상인지 배포 전 확인 — 미만이면 S-5 로
  기동 실패. 런북에 "secret ≥ 32 byte" 명시.
- **REST Docs**: 응답/`Set-Cookie` 변경으로 스니펫 재생성 필요. 미갱신 시 `bootJar`
  (Asciidoctor) 문서 빌드가 깨질 수 있으므로 본 PR 에 포함(재검토 #6).
- **optional-auth 정책 전파**: invalid 쿠키=익명 강등 정책은 `CommentController` 외 향후
  optional-auth 엔드포인트에도 동일 적용되어야 함을 코드 주석/이 문서로 고지(재검토 #4).
- **CORS 민감도**: access 쿠키 인증은 `Access-Control-Allow-Credentials:true` + 명시 origin +
  프론트 credentials include 가 **모두** 충족돼야 동작. 하나라도 빠지면 쿠키 미전송으로
  전 인증 실패 → 배포 체크리스트에 포함(재검토 #5).
- **CSRF 잔여 위험(기록)**: 동일 사이트 + Lax + 비-GET 상태변경으로 실용 방어. 동일 사이트
  내 XSS 기반 CSRF·GET 부작용은 미해결 → CSRF 토큰/`SameSite=Strict` 검토를 후속 과제로 등록.
- **신규 의존성 없음** — 기존 Spring/Redis 재사용.

---

## 6. 부록 — `-f` 후속 작업 (별도 세션, 본 PR 비포함)

S-4 백엔드 전환에 맞춘 프론트 계약 변경. **본 계획서 구현·배포 후** `-f` 로 진행.

| 대상 | 변경 |
|---|---|
| `lib/authApi.ts` `login` | 반환을 raw 토큰 가정 → 메타 JSON(`expiresIn`) 처리. 토큰은 쿠키라 저장 불필요 |
| `app/login/LoginContent.tsx:26-27` | `dispatch(userLogin(data))` — `data` 가 더는 토큰 문자열 아님. accessToken localStorage 저장 제거 |
| `store/userSlice.ts` / `authActions.ts` | `accessToken` 상태/`redux-persist` 제거 또는 `isLoggedIn`만 유지 |
| `lib/apiClient.ts` | 요청 인터셉터의 `Authorization` 헤더 주입 제거(쿠키 자동 전송). **기존 버그 동시 수정**: 응답 인터셉터가 존재하지 않는 `POST /api/v1/auth/refresh` 호출(`apiClient.ts:43`) → 실제 엔드포인트 **`POST /api/v1/reissuing-token`** 로 정정(POST 전용화 반영). `withCredentials:true` 유지 확인 |
| `lib/authApi.ts` `reissuingAccessToken` | `GET` → **`POST`** (백엔드 reissuing-token POST 전용화, 재검토 #3). 반환 raw 토큰 가정 제거 — 토큰은 쿠키 |
| `lib/authApi.ts` `logout` | `GET` → `POST`(백엔드 logout POST 전용화 대응) |

> **발견된 기존 결함**: `apiClient.ts:42-43` 의 자동 refresh 가 백엔드에 없는
> `/api/v1/auth/refresh` 로 POST 한다. 현재 토큰 만료 시 자동 재발급이 사실상 실패하는
> 상태일 가능성 → `-f` 에서 반드시 정정.
