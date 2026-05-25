# 인증 도메인 잔여 P2 해결 계획서

> 작성일: 2026-05-24
> **구현 완료·검증: 2026-05-25** (실제 코드 검증 + `AuthControllerTest` 통과 — §7 참조)
> 상위 문서: `260513_auth-controller-senior-review.md` (P2 항목 정의)
> 선행 완료: `260522_jwt-token-quality-plan.md` (Q-1·Q-2·Q-5 완료, Q-6 부분)
> 대상 잔여 항목: **Q-3, Q-4, Q-6(접두사 노출), Q-7(재확인), Q-8(audit 보강)**
> 작업 범위: `-b` (단, Q-6은 프론트 동시 변경 필요 → `-b -f`)

---

## 0. 현재 상태 — 코드 기준 재검증 (커밋 메시지 아님)

> 작업/리뷰 완료 여부는 실제 코드로 검증 후 반영한다는 원칙에 따라, 상위 리뷰 문서(2026-05-22 기준)와 **현재 코드의 차이**를 먼저 정정한다.

| 항목 | 리뷰 문서 상태 | **실제 코드 상태 (2026-05-24 검증)** | 정정/비고 |
|---|---|---|---|
| Q-3 | ❌ 미착수 | ❌ 미착수 — `LoginReqDto`에 `@Pattern` 없음, 거짓 안내 메시지 유지 | 일치 |
| Q-4 | ❌ 미착수 | ❌ 미착수 — `tokenValidate()`가 `ResponseEntity<Boolean>` 반환 | 일치. **추가 사실**: 프론트 `validateAccessToken()`는 정의만 있고 사용처 0건 (dead code) |
| Q-6 | 🟡 부분 완료 | 🟡 — `/token-role`이 `"ROLE_ADMIN"` 원문 반환, 프론트 `RoleGate.tsx`가 `role !== "ROLE_ADMIN"` 결합 | 일치 |
| Q-7 | ❌ 미착수 | ✅ **사실상 해소** — `TokenResolver`(쿠키 단일 해석)로 통합. `AuthController.getToken()`·중복 `Bearer ` 파싱 코드 부재 | **정정 필요**: 신규 작업 없음, "검증 후 종료" |
| Q-8 | 🟡 부분 완료 (실패 warn만) | 🟡 — 실패 warn(`AuthCredentialVerifier`) + **성공 info도 존재**(`RefreshTokenServiceImpl:59`) | **정정**: 성공 로그는 이미 있음. 진짜 공백은 **IP·UA 미포함 + 분산된 비정형 포맷** |

### 검증 근거 (파일·라인)

- **Q-3**: `dto/auth/LoginReqDto.java:12-17` — `@NotBlank` + `@Size`만 존재. 메시지는 "공백 및 특수문자 입력 불가능", "`!@#$%`를 제외한 특수문자 불가"라고 안내하나 `@Pattern`이 없어 실제로는 전부 통과.
- **Q-4**: `controller/AuthController.java:90-94` — `ResponseEntity<Boolean>`. 프론트 `lib/authApi.ts:22-23`에 `validateAccessToken` 정의는 있으나 grep 결과 호출처 0건.
- **Q-6**: `AuthController.java:60-71`(`getRoleFromToken` → `getTokenInfo(token).getRole()` → `"ROLE_ADMIN"`), `frontend/components/management/RoleGate.tsx:43`(`role !== "ROLE_ADMIN"`).
- **Q-7**: `utils/TokenResolver.java`(쿠키 단일 해석), `JwtFilter.java:43`·`AuthController.java:62,92`가 모두 `tokenResolver.resolve(request)` 사용. 코드 전역 `Bearer`/`getToken`/`substring(7)` grep → 컨트롤러 라벨(`TokenMetaResponse("Bearer", ...)`)과 CORS `setExposedHeaders` 외 토큰 파싱 중복 없음.
- **Q-8**: `AuthCredentialVerifier.java:26,30`(실패 warn, username/IP/UA 없음), `RefreshTokenServiceImpl.java:59`(로그인 성공 info, id/family만)·`:95`(로그아웃 info, family만)·`:126`(reuse 탐지 warn, family만), `GlobalExceptionHandler.java:43`(잠금 429 warn, retryAfter만)로 **여러 클래스에 분산·비정형**. `ClientIpResolver`가 `adminLogin(dto, clientIp)`로 IP를 넘기지만 **로깅되지 않음**, UA는 어디서도 수집 안 함. (참고: `GlobalExceptionHandler.handleValidation:85-99`은 어떤 로그도 남기지 않음 → Q-3 비번 누출은 *응답 본문* 한정)

### 범위 외 — 조사 중 발견한 별도 이슈 (이번 계획에 미포함, 별도 처리 권장)

1. **프론트 인증 호출 메서드 불일치 (실동작 버그, `-f`)**: `lib/authApi.ts`의 `logout`(14-15행, `axios.get` 15행)·`reissuingAccessToken`(17-20행, `axios.get` 19행)이 `axios.get`인데, 백엔드는 S-8로 `@PostMapping` 전용화됨 → 현재 **405 Method Not Allowed**. 백엔드 테스트 `getLogoutAndGetReissuingTokenAreMethodNotAllowed`가 405를 보장하고 있으므로 프론트가 깨진 상태. → 별도 `-f` 핫픽스 필요.
2. **Q-4 dead code**: 위 1과 함께 프론트 정리 시 `validateAccessToken` export 제거 검토.

> #2(dead export)는 Q-6 프론트 세션에서 함께 처리하면 효율적이다. **단 #1(GET→POST)은 단순 정리가 아니라 로그아웃·재발급이 현재 405로 깨진 실동작 인증 버그이므로, Q-6보다 먼저 처리하는 선행 핫픽스(`-f`)로 격상한다.**

---

## 1. Problem (문제 정의)

잔여 P2는 "버그"보다는 **품질/유지보수성/감사성**의 결함이다. 방치 시:

- **Q-3** (`dto/auth/LoginReqDto.java:13,16`): 검증 메시지가 거짓("공백 및 특수문자 입력 불가능" 등을 안내하나 `@Pattern`이 없어 전부 통과) → 사용자/리뷰어가 실제 규칙을 오해. 또한 `MethodArgumentNotValidException` 핸들러가 `rejectedValue`를 응답 본문에 그대로 echo(`exception/GlobalExceptionHandler.java:91-92`)하므로, **비밀번호 `@Size` 위반 시 입력 비밀번호가 400 응답 본문에 노출**되는 잠재 누출이 이미 존재. (※ 해당 핸들러는 로깅을 하지 않으므로 *로그* 노출은 아님 — "응답 본문" 한정)
- **Q-4** (`controller/AuthController.java:90-94`, `frontend/lib/authApi.ts:22`): 효용 낮은 엔드포인트(`Boolean`)가 공개 표면으로 남아 유지보수 비용·오해 유발. 프론트 소비처도 없음(dead export).
- **Q-6** (`controller/AuthController.java:60-71`, `domain/token/Role.java:4`, `frontend/components/management/RoleGate.tsx:43`): Spring 내부 권한 접두사(`ROLE_`)가 외부 계약으로 굳어짐 → 권한 확장/표현 변경 시 프론트와 강결합.
- **Q-7** (`utils/TokenResolver.java`, `configuration/JwtFilter.java:43`): (이미 해소) 미해소로 문서에 남아 있어 작업 추적이 부정확.
- **Q-8** (`service/implementation/AuthCredentialVerifier.java:26,30`, `RefreshTokenServiceImpl.java:59,95,126`, `exception/GlobalExceptionHandler.java:43`): 어드민 단일 계정 시스템에서 **누가·어디서(IP)·무엇으로(UA)·언제·결과** 형태의 감사 추적이 비정형·부분적(여러 클래스에 분산) → 침해 사고 분석/이상 탐지 곤란.

---

## 2. 항목별 분석 및 결정 (Analyze + Decision)

### Q-3. `LoginReqDto` 메시지 ↔ 실제 검증 불일치

**선택지**

| 안 | 내용 | 장점 | 단점/리스크 |
|---|---|---|---|
| A. `@Pattern`으로 규칙 강제 | `username`/`password`에 정규식 부착 | 메시지와 동작 일치 | **로그인 DTO에 포맷 강제는 안티패턴**. 기존 어드민 비밀번호가 패턴 외 문자를 포함하면 BCrypt 비교 전에 400 → **영구 락아웃**. 회원가입/비번변경 엔드포인트가 없어(단일 어드민) 보안 이득 0 (BCrypt가 이미 임의 입력 처리) |
| B. 거짓 안내 제거 + 최소 제약 유지 | 메시지에서 "특수문자 불가" 등 허위 문구 삭제, `@NotBlank`+`@Size`만 유지 | 락아웃 위험 0, 정직한 계약 | "강한 검증"이라는 외형은 사라짐(실익 없음) |
| C. 메시지대로 느슨한 패턴 | `!@#$%` 허용 패턴으로 맞춤 | 메시지·동작 일치 | 여전히 기존 비번이 패턴 밖이면 락아웃 위험 |

**결정: B (거짓 안내 제거 + 최소 제약)** + **비밀번호 `rejectedValue` 마스킹**.

- 근거: 로그인 검증은 "포맷 게이트"가 아니라 "빈 값/극단 길이 방어"가 목적이다. 포맷 강제는 등록 시점에 두는 것이 표준이며, 본 시스템엔 등록 경로가 없다. A/C는 운영 어드민 락아웃이라는 비가역 리스크 대비 이득이 없다.
- 부수 보안 개선: `GlobalExceptionHandler.handleValidation`이 **민감 필드의 `rejectedValue`를 응답 본문에 넣지 않도록 마스킹**. `password` 단일 비교가 아니라 민감 필드명 집합(`password`, `oldPassword`, `newPassword`, `confirmPassword`, `token`, `secret`)을 판별하는 helper(`isSensitiveField(fieldName)`)로 `value`를 `"[PROTECTED]"`로 치환한다. `handleValidation`은 **전 검증 응답에 공통 적용**되는 로직이므로(현재는 `LoginReqDto.password`만 해당하나) 방어적으로 일반화해 향후 DTO 추가 시 누출을 사전 차단. 이는 Q-3 메시지 정정과 동일 맥락의 정보 누출 차단.

### Q-4. `/api/v1/token-validation` 효용

**선택지**

| 안 | 내용 | 장점 | 단점 |
|---|---|---|---|
| A. 엔드포인트 제거 | `tokenValidate()` + `AuthService.isTokenValid` 삭제 | 공개 표면·dead code 최소화. 보호 API는 어차피 401로 신호 | 향후 "사전 갱신" 용도 재구현 비용 |
| B. `expiresIn`으로 용도 변경 | `{ valid, expiresIn }` 반환 → 만료 임박 시 선제 reissue | 능동적 silent-refresh 가능 | 현재 소비처 없음 → 투기적 구현(YAGNI) |
| C. 유지(Boolean) | 변경 없음 | - | 효용 낮음·오해 지속 |

**결정: A (계약 제거)** — 단, "선제 갱신 UX를 곧 도입"할 계획이면 B, **외부 소비자 부재를 100% 확신할 수 없으면 D(`@Deprecated` 후 단계 제거)**.

- 근거: 프론트 소비처가 0건이고, 보호 API 호출 시 401 + reissue 플로우가 이미 동일 정보를 더 정확히(race-free) 제공한다. 미사용 공개 엔드포인트는 공격 표면이자 인지 부채다.
- **이것은 "제품 기능"이 아니라 "공개 API 계약"의 제거다.** 따라서 제거 범위는 코드뿐 아니라 **문서 표면 전체**를 포함한다:
  - 코드: 엔드포인트 + `isTokenValid`(인터페이스 `AuthService:20` / 구현) + 테스트(`tokenValidate`, `tokenValidateWithoutValidCookie`)
  - **Asciidoc: `src/docs/asciidoc/auth.adoc:14-15`의 "토큰 인증" 섹션**(`operation::auth-controller-test/token-validate[...]`) — 미삭제 시 스니펫 누락으로 `asciidoctor` 태스크가 **실패**한다
  - 정적 산출물(`src/main/resources/static/docs`)은 `createDocument` 태스크가 재생성하므로 별도 수정 불필요
  - 프론트 dead export `validateAccessToken` 제거는 위 "범위 외 #2"와 함께
- **D(Deprecated) 옵션**: 외부 소비자 존재가 불확실하면 즉시 제거 대신 `@Deprecated` + 문서 폐기예정 표기 → 한 사이클 관찰 후 제거.
- **이 항목은 공개 계약 변경 포인트** → 구현 착수 전 사용자 확인 권장(아래 §5 참고). 기본 권고는 A.

### Q-6. `ROLE_` 접두사 외부 노출 정리 (`-b -f`)

**선택지**

| 안 | 내용 | 장점 | 단점 |
|---|---|---|---|
| A. `/token-role`이 `"ADMIN"` 반환 | 접두사 제거(`Role.ADMIN.name()` 또는 매핑) + 프론트 `"ADMIN"` 비교 | 내부 권한 모델과 외부 계약 분리 | 백엔드·프론트 동시 배포 필요 |
| B. 구조화 응답 `{ "roles": ["ADMIN"] }` | 다중 권한 확장 대비 | 미래 확장성 | 계약 변경 폭 큼, 현재 단일 권한이라 과설계 |

**결정: A** — 단일 권한 시스템에 배열은 과설계. 응답을 `"ADMIN"`(접두사 제거 문자열)로 단순화.

- **배포 순서 리스크(핵심)**: 백엔드만 먼저 배포되면 구버전 프론트(`role !== "ROLE_ADMIN"`)가 `"ADMIN"`을 비-어드민으로 판정 → **어드민이 `/management`에서 홈으로 튕김**.
- **완화책**: 프론트 `RoleGate`를 **전이 호환**으로 먼저 배포 — `role === "ADMIN" || role === "ROLE_ADMIN"`. 이후 백엔드 배포. 안정화 후 `"ROLE_ADMIN"` 분기 제거(정리 커밋). 이렇게 하면 배포 순서 무관.
- 변환 위치·방식: `domain/token/Role.java`에 **명시 매핑**을 둔다 — `displayName()`(`ADMIN` 반환)과 역매핑 `fromAuthority(String authority)`(**정의되지 않은 authority는 예외 처리**). 컨트롤러는 `Role.fromAuthority(authority).displayName()`로 변환. **문자열 치환(`replaceFirst("ROLE_", "")`)은 금지** — 잘못된/미정의 권한 값도 그럴듯한 외부 값으로 흘려보내 계약을 오염시킬 수 있다. 내부 SecurityContext 권한(`ROLE_ADMIN`)은 절대 변경하지 않는다(Spring `hasRole` 계약 보존).

### Q-7. Bearer 파싱 중복 — **신규 작업 없음, 검증 후 종료**

- `TokenResolver` 도입으로 토큰 해석이 단일화되었고(쿠키 기반), 컨트롤러/필터의 중복 `Bearer ` 파싱과 `AuthController.getToken()`이 모두 사라졌다.
- 기존 테스트 `authorizationHeaderOnlyCannotAccessProtectedApi`가 "헤더만으로는 401"을 이미 보장.
- **조치**: 상위 리뷰 문서 표를 ✅로 갱신. 추가로, 회귀 방지를 위해 "토큰 해석은 `TokenResolver` 단일 경로" 주석/테스트가 충분한지 확인만 한다. 코드 변경 불필요.

### Q-8. 인증 audit 로그 정형화 (IP·UA·결과)

**선택지**

| 안 | 내용 | 장점 | 단점 |
|---|---|---|---|
| A. 기존 로그에 IP/UA 인자 추가 | 분산 로그에 필드만 보강 | 최소 변경 | UA는 웹 계층 관심사 → 서비스/도메인 계층까지 `HttpServletRequest` 누수. 포맷 비일관 유지 |
| B. 전용 `AuthAuditLogger` (권장) | 컨트롤러 경계(요청 객체 보유)에서 정형 이벤트 기록. 전용 logger 네임(`AUDIT`)으로 별도 appender 라우팅 가능 | 정형·단일 책임·웹 관심사 격리·운영 분리 용이 | 신규 컴포넌트 1개 추가 |
| C. MDC + 필터 | requestId/IP/UA를 MDC로 전 로그에 주입 | 전역 상관관계 | 인증 감사 목적엔 과함, 패턴 정착 비용 |

**결정: B (`AuthAuditLogger`)**.

- **이벤트 taxonomy**: `LOGIN_SUCCESS`, `LOGIN_FAILURE`, `LOGOUT`, `TOKEN_REISSUE`, `TOKEN_REISSUE_REUSE_DETECTED`, `LOGIN_LOCKED`(429).
- **공통 필드(정형, key=value 한 줄)**: `event`, `result`(SUCCESS/FAILURE), `username`(실패 포함 — 감사 목적이라 기록. **단 S-1과 무관**: S-1은 HTTP *응답* 차별 금지일 뿐 서버 로그 기록은 표준), `ip`, `ua`, `reason`(실패 사유 코드), `ts`(자동). **`password`는 절대 기록 금지**.
- **호출 위치 (이벤트별 정확한 기록 지점 — 이중 기록·오분류 방지)**:
  - `LOGIN_SUCCESS`: `AuthController.login`에서 `adminLogin` 정상 반환 후. IP·UA(`request.getHeader("User-Agent")`)는 컨트롤러에서 확보.
  - `LOGIN_FAILURE`: `AuthController.login`에서 **`catch (UnauthorizedException)`로만 한정**해 audit + rethrow. `TooManyLoginAttemptsException`도 `BusinessException` 하위이므로(`AuthServiceImpl.adminLogin:35,42`에서 발생) 넓은 `catch`로 잡으면 LOGIN_FAILURE와 LOGIN_LOCKED가 **이중 기록**된다 → 반드시 예외 타입을 좁혀 잡는다. (5회째 실패는 서비스가 `UnauthorizedException` 대신 `TooManyLoginAttemptsException`을 던지므로 LOCKED 한 건만 남음)
  - `LOGIN_LOCKED`: `GlobalExceptionHandler.handleTooManyLoginAttempts`(`:40-48`)에서만 기록. IP·UA가 필요하면 핸들러에 `HttpServletRequest` 파라미터 추가.
  - `LOGOUT`/`TOKEN_REISSUE`(정상): 해당 컨트롤러 메서드(`logout`/`reissuingAccessToken`)에서 정상 처리 후 기록.
  - `TOKEN_REISSUE_REUSE_DETECTED`: **reuse는 `RefreshTokenServiceImpl.rotate:125-127` 내부에서만 정확히 판별**되고(`InvalidateTokenException(ErrorCode.REFRESH_TOKEN_REUSE_DETECTED)`), 컨트롤러의 일반 catch(`InvalidateTokenException | ExpiredRefreshTokenException`)는 reuse·만료·위조·누락을 **구분하지 못한다**. 따라서 컨트롤러에서 추측하지 말고, **`GlobalExceptionHandler.handleInvalidateTokenException`이 `e.getErrorCode() == REFRESH_TOKEN_REUSE_DETECTED`일 때** 기록(여기서 IP·UA 확보 가능).
- **계층 책임 분리(핵심)**: 웹 계층 `AuthAuditLogger`는 요청 객체(IP·UA)를 가진 **컨트롤러/핸들러 경계**에서만 호출한다. 서비스/도메인 계층은 `HttpServletRequest`를 받지 않으며(도메인 청결 유지), 서비스 내부에서만 알 수 있는 보안 신호(reuse)는 **ErrorCode로 표현**해 핸들러가 audit하도록 위임한다. 서비스의 기존 `warn`(`:126`)은 detection-layer 보안 신호로 유지(승격이 아니라 병행).
- **중복 정리**: `AuthCredentialVerifier`의 ad-hoc `warn`(`:26,30`)과 `RefreshTokenServiceImpl`의 `info("Admin login...")`(`:59`)·`info("Admin logout...")`(`:95`)는 audit 로거로 일원화 후 `debug`로 강등하거나 제거(중복 로그 방지). 단, reuse 탐지 `warn`(`RefreshTokenServiceImpl:126`)과 잠금 `warn`(`GlobalExceptionHandler:43`)은 보안 신호라 유지하고, 웹 계층(핸들러)에서 각각 `TOKEN_REISSUE_REUSE_DETECTED`·`LOGIN_LOCKED` audit 이벤트를 **병행 기록**한다(승격이 아니라 detection-layer warn 유지 + 웹 계층 audit 추가 — 위 "호출 위치" 참조).
- **운영**: logger 네임을 `AUDIT`로 분리해 `logback`에서 별도 파일/레벨로 라우팅 가능하도록 설계(설정 추가는 선택). 최소 INFO 레벨.

---

## 3. Action (구현 계획)

### 작업 범위·순서

> 작은 것부터, 프론트 결합 항목은 뒤로.

0. **선행 핫픽스** — `frontend/lib/authApi.ts`의 `logout`/`reissuingAccessToken` GET→POST 수정(현재 405 인증 버그). [`-f`, **Q-6 이전 필수**]
1. **Q-7** — 문서만 ✅ 갱신 (코드 무변경). [백엔드 PR과 무관, 즉시]
2. **Q-3** — `LoginReqDto` 메시지 정정 + 민감 필드 `rejectedValue` 마스킹. [백엔드 단독]
3. **Q-8** — `AuthAuditLogger` 도입 + 호출부 배선(컨트롤러/핸들러 경계) + 기존 로그 정리. [백엔드 단독]
4. **Q-4** — (사용자 결정 후) 엔드포인트/`isTokenValid`/`auth.adoc` 섹션 제거 **또는** `expiresIn` 전환 **또는** `@Deprecated`. [백엔드 단독, 프론트 dead export 정리 동반]
5. **Q-6** — `/token-role` 응답 `"ADMIN"` 전환. [`-b -f`, 배포 순서 주의 — 프론트 전이 호환 먼저]

### 변경 대상 파일 목록

**백엔드**

| 파일 | 변경 | 항목 |
|---|---|---|
| `dto/auth/LoginReqDto.java` | 거짓 안내 메시지 제거, 메시지를 실제 제약(`@NotBlank`/`@Size`)과 일치 | Q-3 |
| `exception/GlobalExceptionHandler.java` | (Q-3) `handleValidation`에 민감 필드명 집합 마스킹 helper(`isSensitiveField` → `[PROTECTED]`); (Q-8) `handleInvalidateTokenException`에서 `REFRESH_TOKEN_REUSE_DETECTED` 시 `TOKEN_REISSUE_REUSE_DETECTED` audit, `handleTooManyLoginAttempts`에서 `LOGIN_LOCKED` audit | Q-3·Q-8 |
| `utils/AuthAuditLogger.java` (신규) | 정형 감사 이벤트 기록 컴포넌트 | Q-8 |
| `controller/AuthController.java` | login/logout/reissue에 audit 호출 + UA 수집, (Q-4) `tokenValidate` 제거 or 변경, (Q-6) role 표현 변환 | Q-4·Q-6·Q-8 |
| `service/AuthService.java` + `…/AuthServiceImpl.java` | (Q-4 A안) `isTokenValid` 제거 | Q-4 |
| `service/implementation/AuthCredentialVerifier.java` | ad-hoc warn → debug 강등/제거(audit로 일원화) | Q-8 |
| `service/implementation/RefreshTokenServiceImpl.java` | `info("Admin login...")` 중복 정리, reuse 탐지를 audit 이벤트로 승격 | Q-8 |
| `domain/token/Role.java` | (Q-6) 명시 매핑 헬퍼 `displayName()` + `fromAuthority(String)`(미정의 authority는 예외) 추가 — 문자열 치환 금지 | Q-6 |
| `docs/asciidoc/auth.adoc` | (Q-4 A안) `:14-15` "토큰 인증" 섹션 제거(스니펫 참조 삭제) | Q-4 |

**프론트 (`-f` 세션)**

| 파일 | 변경 | 항목 |
|---|---|---|
| `components/management/RoleGate.tsx` | 전이 호환(`=== "ADMIN" \|\| === "ROLE_ADMIN"`) → 안정화 후 `"ADMIN"`만 | Q-6 |
| `lib/authApi.ts` | (범위 외 동반) `logout`/`reissuingAccessToken` GET→POST 수정, dead `validateAccessToken` 제거 | 부수·Q-4 |

### 주요 트레이드오프

- **Q-3**: 검증 강화 외형 포기 ↔ 어드민 락아웃 비가역 리스크 회피 + 비번 누출 차단. (정직성·안전성 우선)
- **Q-4**: 확장 옵션(`expiresIn`) 보류 ↔ 표면/부채 축소. (YAGNI)
- **Q-6**: 동시 배포 제약 ↔ 내부/외부 모델 분리. (전이 호환으로 제약 완화)
- **Q-8**: 컴포넌트 1개 추가 ↔ 감사성·관심사 격리. (운영 가치 큼)

### 예상 이슈 & 대응

- **Q-6 배포 순서** → 프론트 전이 호환 먼저 배포(필수).
- **Q-8 username 로깅 vs S-1** → 응답이 아닌 *서버 로그*이므로 충돌 없음. 비번은 절대 미기록.
- **Q-8 LOGIN_FAILURE/LOGIN_LOCKED 이중 기록** → `login`의 audit catch를 `UnauthorizedException`으로만 한정(`TooManyLoginAttemptsException`은 `BusinessException` 하위라 넓은 catch에 걸림). 잠금은 핸들러에서만 `LOGIN_LOCKED` 기록.
- **Q-8 reuse 오분류** → 컨트롤러 일반 catch는 reuse를 구분 못 함. `handleInvalidateTokenException`에서 `ErrorCode == REFRESH_TOKEN_REUSE_DETECTED`로 판별해 기록(§2 호출 위치 참조).
- **Q-4 제거 시 문서 파이프라인** → 테스트(`tokenValidate`/`tokenValidateWithoutValidCookie`)와 `auth.adoc:14-15` 섹션을 **함께** 제거해야 `asciidoctor`(`dependsOn test`) → `createDocument` → `bootJar` 체인이 깨지지 않음(스니펫 누락 시 asciidoctor 실패).
- **Q-8 테스트 안정성** → 로그 어서션은 `OutputCaptureExtension`(Spring Boot) 또는 Logback `ListAppender`로 검증.

---

## 4. Result (검증 계획)

> CLAUDE.md 규칙 4: 변경과 **직접 관련된 테스트만** 선택 실행.

### 테스트

- **Q-3**: `LoginController/AuthControllerTest`에 (a) 빈 값 400, (b) 길이 위반 400, (c) **400 응답 본문에 입력 비밀번호 문자열이 포함되지 않음**(마스킹) 검증 추가.
- **Q-4 (A안)**: `tokenValidate`·`tokenValidateWithoutValidCookie` 테스트 제거 + `auth.adoc:14-15` "토큰 인증" 섹션 제거 후 **`./gradlew asciidoctor`(→ `createDocument` → `bootJar`) 문서 생성 성공까지 확인**(스니펫 누락 시 asciidoctor 실패로 회귀 감지). (B안 채택 시: `expiresIn` 필드 검증으로 교체)
- **Q-6**: `getTokenFromRole` 회귀 가드를 `content().string("ROLE_ADMIN")` → `"ADMIN"`으로 변경. 프론트는 어드민 진입 시 게이트 통과를 수동/E2E 확인(전이 호환 단계 포함).
- **Q-7**: 신규 테스트 불필요 — `authorizationHeaderOnlyCannotAccessProtectedApi`가 이미 커버. 문서 갱신만.
- **Q-8**: `OutputCaptureExtension`으로 (a) 로그인 성공/실패 시 `event=LOGIN_SUCCESS/LOGIN_FAILURE`, `ip=`, `ua=` 포함 및 `password` 미포함, (b) **잠금(5회째) 발생 시 `LOGIN_LOCKED`만 기록되고 `LOGIN_FAILURE`는 중복 기록되지 않음**, (c) reuse 탐지 시 `TOKEN_REISSUE_REUSE_DETECTED` 기록을 어서션.

```bash
# 관련 테스트만 (전체 ./gradlew test 금지)
./gradlew test --tests "com.moya.myblogboot.controller.AuthControllerTest"
# (Q-8 audit 테스트 클래스 추가 시 함께)
./gradlew test --tests "com.moya.myblogboot.*Auth*"
```

### 성공 기준

- [x] Q-3: 검증 메시지가 실제 동작과 일치(거짓 특수문자 안내 제거), 400 응답 본문에 비밀번호 미노출(`rejectedValue` 마스킹). — `loginValidationMasksSensitiveRejectedValue` 통과
- [x] Q-4: 미사용 엔드포인트·`isTokenValid`·`auth.adoc` "토큰 인증" 섹션 제거, 코드/테스트/문서에 dangling 참조 0건(`token-validate` grep 0). (A안 채택)
- [x] Q-6: 백엔드 `"ADMIN"` 반환(`Role.fromAuthority().displayName()`), 프론트 전이 호환(`"ADMIN" \|\| "ROLE_ADMIN"`). — `getTokenFromRole` → `content().string("ADMIN")` 통과
- [x] Q-7: 코드 무변경 확인, 상위 문서 표 ✅ 갱신 완료.
- [x] Q-8: 6개 이벤트(`LOGIN_SUCCESS/FAILURE/LOCKED`, `LOGOUT`, `TOKEN_REISSUE`, `TOKEN_REISSUE_REUSE_DETECTED`)를 `AUDIT` 로거로 정형 한 줄 기록, **잠금 시 FAILURE 미중복**(테스트 가드), reuse는 핸들러에서 기록, 비번 미기록. — `loginWithWrongPassword`/`loginBruteForceProtection` 통과
- [x] 변경 영향 테스트(`AuthControllerTest`) 전부 통과, 디버그 로그는 `debug` 강등 처리, 사이드 이펙트 점검 완료.

---

## 5. 구현 결정

이번 구현은 기본 권고안으로 진행한다.

1. **Q-4**: 엔드포인트 **계약 제거**.
2. **Q-6 배포**: 프론트 **전이 호환 선반영** 후 백엔드 `/token-role`을 `"ADMIN"`으로 전환.

---

## 6. 상위 리뷰 문서 갱신 사항 (구현 완료 후 반영)

- Q-7: ❌ → ✅ ("TokenResolver 단일화로 해소, 코드 무변경 확인").
- Q-8: 🟡 사유를 "성공/실패 로그는 존재했으나 IP·UA·정형성 부재" → 보강 완료로 정정.
- Q-3/Q-4/Q-6: 완료 시 상태·근거 갱신.

> ✅ 위 갱신은 `260513_auth-controller-senior-review.md`에 2026-05-25 자로 반영 완료(전 항목 ✅, 진행 상황 표/변경 영향 메모 포함).

---

## 7. 구현 결과 — 코드 검증 (2026-05-25)

> 원칙: 완료 여부는 커밋 메시지가 아니라 **실제 코드 + 테스트**로 검증한다. 아래는 본 계획서와 실제 구현의 대조 결과다.

### 항목별 구현 확인 (파일·라인)

| 항목 | 계획 | 실제 구현 (검증 위치) | 상태 |
|---|---|---|---|
| Q-3 메시지 | 거짓 안내 제거 | `LoginReqDto.java:13,16` — 특수문자 안내 삭제, `@NotBlank`/`@Size`만 | ✅ |
| Q-3 마스킹 | 민감 필드 helper | `GlobalExceptionHandler.java:34-41,158-175` — `SENSITIVE_FIELDS`(`password`/`token`/`secret` 계열) + `maskRejectedValue`/`isSensitiveField` | ✅ 계획+α |
| Q-4 엔드포인트 | 제거 | `AuthController`의 `tokenValidate` 삭제, `AuthService.java`에서 `isTokenValid` 삭제 | ✅ |
| Q-4 문서 | `auth.adoc` 섹션 삭제 | `auth.adoc`에서 "토큰 인증"(`token-validate`) 제거, dangling 참조 0건 | ✅ |
| Q-4 프론트 | dead export 제거 | `frontend/lib/authApi.ts` — `validateAccessToken` export 삭제 | ✅ |
| Q-6 변환 | `displayName`/`fromAuthority` | `Role.java:18-27`(미정의 authority → `IllegalArgumentException`), `AuthController.java:81` 변환 | ✅ |
| Q-6 프론트 | 전이 호환 | `RoleGate.tsx:43` — `role !== "ADMIN" && role !== "ROLE_ADMIN"` | ✅ |
| Q-7 | 코드 무변경 | `TokenResolver` 단일 경로 유지 | ✅ |
| Q-8 로거 | `AuthAuditLogger` 신설 | `utils/AuthAuditLogger.java` — `AUDIT` 로거, 6개 이벤트 enum, `event=...` 정형 한 줄 | ✅ |
| Q-8 호출부 | 컨트롤러/핸들러 경계 | `AuthController.java:49-56`(login, `catch (UnauthorizedException)` 한정)·`69`(LOGOUT)·`97`(TOKEN_REISSUE), `GlobalExceptionHandler.java:61`(LOGIN_LOCKED)·`89-91`(REUSE) | ✅ |
| Q-8 로그 정리 | warn→debug, info 제거 | `AuthCredentialVerifier.java:26,30`(`warn`→`debug`), `RefreshTokenServiceImpl`의 로그인/로그아웃 `info` 제거, reuse `warn`(`:124`)은 detection 신호로 유지 | ✅ |
| 선행 핫픽스 | GET→POST | `authApi.ts` — `logout`/`reissuingAccessToken` `axios.post`로 수정 | ✅ |

### 계획 대비 개선점 (구현 중 보강)

- **로그 인젝션 방어**: `AuthAuditLogger.safe()`가 공백을 `_`로 치환해 `key=value` 한 줄 포맷 붕괴·로그 위조를 차단.
- **마스킹 견고화**: `isSensitiveField`가 대소문자 무시 + 중첩 필드(`obj.password`)의 마지막 세그먼트까지 판별.
- **이중 기록 회귀 가드**: `loginBruteForceProtection` 테스트가 잠금 발생 시 `LOGIN_LOCKED` 이후 `LOGIN_FAILURE`가 추가 기록되지 않음을 단언.

### 테스트 결과

- `./gradlew test --tests "com.moya.myblogboot.controller.AuthControllerTest"` → **BUILD SUCCESSFUL**. 변경된 인증 테스트(`AuthControllerTest`·`GlobalExceptionHandlerTest`·`AuthServiceImplTest`) 전부 통과.
- 핵심 가드: `loginValidationMasksSensitiveRejectedValue`(Q-3), `getTokenFromRole`→`"ADMIN"`(Q-6), `loginWithWrongPassword`/`loginBruteForceProtection`(Q-8, `event=LOGIN_FAILURE`/`LOGIN_LOCKED` + 이중기록 가드).
- Q-4 문서 파이프라인: `src/docs/asciidoc`에 `token-validate` 참조 0건 → `asciidoctor` 스니펫 누락 위험 없음.
- **`./gradlew bootJar`(전체 테스트 동반)는 현재 BUILD FAILED** — `PostControllerTest`/`CategoryControllerTest`/`FileUploadControllerTest`에서 19건 실패(대부분 401·S3). **단, 본 작업과 무관한 기존(pre-existing) 결함**으로 확인: 변경분을 `git stash`로 제거한 HEAD 상태에서도 33건 중 19건 동일 실패 재현 → 인증 P2 변경의 회귀 아님. (정적 docs 재생성용 `bootJar` 성공은 위 비인증 테스트 선결 필요)

### 잔여 / 후속

- **배포 순서(Q-6)**: 프론트 전이 호환은 이미 반영됨 → 백엔드 `/token-role`=`"ADMIN"` 배포 후, 안정화되면 프론트에서 `"ROLE_ADMIN"` 분기 제거(정리 커밋).
- **선택 운영**: `logback`에서 `AUDIT` 로거를 별도 appender/파일로 분리(현재는 logger 네임만 분리, 설정은 미적용).

---
