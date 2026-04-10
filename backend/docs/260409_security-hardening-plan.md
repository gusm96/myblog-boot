# 관리자 인증 보안 강화 계획서

> 작성일: 2026-04-09  
> 목적: 광고 수익 모델 도입에 앞서 관리자 계정 탈취를 방지하기 위한 다층 보안 적용  
> 대상: `POST /api/v1/login`, `GET /api/v1/reissuing-token`, `/management/**`

---

## 1. 현재 인증 구조 분석

### 1.1 인증 흐름

```
[클라이언트]
  POST /api/v1/login (username + password)
    → AuthServiceImpl.adminLogin()
      → AdminRepository.findByUsername()
      → BCryptPasswordEncoder.matches()
    → JwtUtil.createToken() → Access Token + Refresh Token 발급
    → Refresh Token은 HttpOnly 쿠키로 전달

[이후 요청]
  Authorization: Bearer {accessToken}
    → JwtFilter.doFilterInternal()
      → JwtUtil.validateToken() → SecurityContext에 인증 정보 설정
```

### 1.2 현재 보안 취약점

| 취약점 | 위험도 | 설명 |
|--------|--------|------|
| 로그인 시도 무제한 | **높음** | 브루트 포스 공격에 완전 노출 |
| Rate Limiting 없음 | **높음** | IP당 요청 제한이 없어 자동화 공격 가능 |
| 단일 인증 요소 | **중간** | 비밀번호 유출 시 즉시 계정 탈취 |
| Refresh Token 재사용 가능 | **중간** | 탈취된 Refresh Token으로 무한 Access Token 재발급 |
| 디바이스 검증 없음 | **낮음** | 어떤 기기에서든 로그인 가능 |

---

## 2. 보안 강화 아키텍처

### 2.1 방어 계층 구조

```
요청 수신
  │
  ▼
[계층 1] RateLimitFilter (OncePerRequestFilter)
  │  IP당 /api/v1/login 요청 횟수 제한 (1분당 10회)
  │  Redis key: "rate:login:{ip}" (TTL 60s)
  │  초과 시 → 429 Too Many Requests
  │
  ▼
[계층 2] LoginAttemptService
  │  계정별 실패 횟수 추적
  │  Redis key: "login:fail:{username}" (TTL 15분)
  │  5회 실패 → 계정 잠금 (15분)
  │  성공 시 → 카운터 초기화
  │
  ▼
[계층 3] AuthServiceImpl.adminLogin()
  │  1차 인증: username + BCrypt password 검증
  │  인증 성공 → MFA 필요 여부 판단
  │
  ▼
[계층 4] TOTP MFA (2차 인증)
  │  1차 인증 성공 시 임시 토큰(pre-auth token) 발급
  │  클라이언트가 TOTP 코드와 함께 2차 인증 요청
  │  검증 성공 → JWT(Access + Refresh) 발급
  │
  ▼
[계층 5] Device Trust (디바이스 신뢰)
  │  TOTP 인증 성공 시 HMAC 서명 쿠키 발급
  │  신뢰된 디바이스는 이후 TOTP 생략
  │  IP 변경 시 신뢰 무효화 → TOTP 재요구
  │
  ▼
[계층 6] Refresh Token Rotation
     Access Token 재발급 시 Refresh Token도 함께 교체
     사용된 Refresh Token은 Redis에서 즉시 폐기
     폐기된 토큰 재사용 감지 → 전체 세션 무효화
```

---

## 3. Phase별 상세 구현 계획

### Phase 1: Rate Limiting + 로그인 시도 제한

**목표**: 브루트 포스 / 자동화 공격 차단

#### 3.1.1 Rate Limiting Filter

**새 파일**: `configuration/RateLimitFilter.java`

```java
// OncePerRequestFilter 확장
// /api/v1/login 경로에만 적용
// Redis INCR + EXPIRE로 슬라이딩 윈도우 구현
```

**동작 방식**:

```
1. 요청 IP 추출 (X-Forwarded-For 헤더 우선)
2. Redis key: "rate:login:{ip}"
3. INCR → 현재 카운트 확인
   - 첫 요청이면 EXPIRE 60초 설정
   - 10회 초과 → 429 응답 + Retry-After 헤더
4. 통과 시 다음 필터로 위임
```

**설정값**:

| 항목 | 값 | 설명 |
|------|----|------|
| 최대 요청 | 10회 | 1분당 IP별 로그인 시도 |
| 윈도우 | 60초 | Redis TTL |
| 응답 코드 | 429 | Too Many Requests |

**SecurityFilterChain 등록 위치**:

```
RateLimitFilter → JwtFilter → UsernamePasswordAuthenticationFilter
```

#### 3.1.2 로그인 시도 제한 서비스

**새 파일**: `service/LoginAttemptService.java`

```java
public interface LoginAttemptService {
    void loginFailed(String username);      // 실패 카운트 증가
    void loginSucceeded(String username);   // 카운터 초기화
    boolean isBlocked(String username);     // 잠금 여부 확인
    int getFailCount(String username);      // 현재 실패 횟수
    long getBlockTTL(String username);      // 잠금 남은 시간(초)
}
```

**Redis 키 설계**:

| Key | Value | TTL | 설명 |
|-----|-------|-----|------|
| `login:fail:{username}` | 실패 횟수 (int) | 15분 | 5 도달 시 잠금 상태 |

**동작 흐름**:

```
로그인 요청
  → isBlocked(username) 확인
     ├─ true → 423 Locked 응답 + 남은 시간 반환
     └─ false → 비밀번호 검증 진행
        ├─ 실패 → loginFailed(username) → 카운트 +1
        └─ 성공 → loginSucceeded(username) → 카운트 초기화
```

#### 3.1.3 ErrorCode 추가

```java
// 인증/인가 섹션에 추가
ACCOUNT_LOCKED(HttpStatus.LOCKED, "A007", "로그인 시도 횟수를 초과했습니다. 잠시 후 다시 시도해주세요."),
TOO_MANY_REQUESTS(HttpStatus.TOO_MANY_REQUESTS, "A008", "요청이 너무 많습니다. 잠시 후 다시 시도해주세요."),
```

#### 3.1.4 AuthController 변경

```java
@PostMapping("/api/v1/login")
public ResponseEntity<?> login(@RequestBody @Valid MemberLoginReqDto loginReqDto,
                                HttpServletResponse response) {
    // 1. 계정 잠금 확인
    if (loginAttemptService.isBlocked(loginReqDto.getUsername())) {
        throw new BusinessException(ErrorCode.ACCOUNT_LOCKED);
    }
    
    try {
        Token newToken = authService.adminLogin(loginReqDto);
        // 2. 성공 → 카운터 초기화
        loginAttemptService.loginSucceeded(loginReqDto.getUsername());
        response.addCookie(...);
        return ResponseEntity.ok().body(newToken.getAccess_token());
    } catch (BusinessException e) {
        // 3. 실패 → 카운터 증가
        loginAttemptService.loginFailed(loginReqDto.getUsername());
        throw e;
    }
}
```

#### 3.1.5 변경 파일 목록

| 파일 | 작업 |
|------|------|
| `configuration/RateLimitFilter.java` | **신규** — IP 기반 요청 제한 필터 |
| `service/LoginAttemptService.java` | **신규** — 인터페이스 |
| `service/implementation/LoginAttemptServiceImpl.java` | **신규** — Redis 기반 구현 |
| `exception/ErrorCode.java` | **수정** — ACCOUNT_LOCKED, TOO_MANY_REQUESTS 추가 |
| `controller/AuthController.java` | **수정** — 로그인 시도 제한 로직 통합 |
| `configuration/WebSecurityConfig.java` | **수정** — RateLimitFilter 등록 |
| `application.yaml` | **수정** — rate-limit 설정값 추가 |

---

### Phase 2: Refresh Token Rotation

**목표**: 토큰 탈취 시 자동 감지 및 전체 세션 무효화

#### 3.2.1 개념

```
[정상 흐름]
  1. 로그인 → Access Token + Refresh Token(v1) 발급
  2. Access Token 만료 → Refresh Token(v1)으로 재발급 요청
  3. 새 Access Token + 새 Refresh Token(v2) 발급, v1은 폐기
  4. 다음 재발급 시 v2 사용

[탈취 감지]
  공격자가 v1을 탈취하여 재발급 시도
  → v1은 이미 폐기됨 → "재사용 감지"
  → 해당 Admin의 모든 Refresh Token 무효화
  → 정상 사용자도 재로그인 필요 (안전을 위해)
```

#### 3.2.2 Redis 키 설계

| Key | Value | TTL | 설명 |
|-----|-------|-----|------|
| `refresh:{tokenId}` | `{adminId, role, version}` (JSON) | 14일 | 유효한 Refresh Token |
| `refresh:revoked:{tokenId}` | `"revoked"` | 14일 | 폐기된 토큰 (재사용 감지용) |

#### 3.2.3 JWT Claims 변경

```java
// 기존: memberPrimaryKey, role
// 추가: tokenId (UUID), tokenVersion (Long)
.claim("tokenId", UUID.randomUUID().toString())
.claim("tokenVersion", version)
```

#### 3.2.4 RefreshTokenService

**새 파일**: `service/RefreshTokenService.java`

```java
public interface RefreshTokenService {
    String store(Long adminId, String role);         // 저장 후 tokenId 반환
    TokenInfo validate(String tokenId);              // 유효성 검증 + 정보 반환
    String rotate(String oldTokenId);                // 기존 폐기 + 새 토큰 저장
    void revokeAll(Long adminId);                    // 전체 무효화 (탈취 감지 시)
}
```

#### 3.2.5 재발급 흐름 변경

```
GET /api/v1/reissuing-token
  1. Refresh Token에서 tokenId 추출
  2. Redis에서 "refresh:{tokenId}" 조회
     ├─ 존재 → 정상, rotate() 실행
     ├─ "refresh:revoked:{tokenId}"에 존재 → 재사용 감지!
     │   → revokeAll(adminId) → 401 응답
     └─ 어디에도 없음 → 유효하지 않은 토큰 → 401 응답
  3. 새 Access Token + 새 Refresh Token 쿠키 발급
```

#### 3.2.6 변경 파일 목록

| 파일 | 작업 |
|------|------|
| `service/RefreshTokenService.java` | **신규** — 인터페이스 |
| `service/implementation/RefreshTokenServiceImpl.java` | **신규** — Redis 기반 구현 |
| `utils/JwtUtil.java` | **수정** — tokenId claim 추가 |
| `domain/token/Token.java` | **수정** — tokenId 필드 추가 |
| `domain/token/TokenInfo.java` | **수정** — tokenId 필드 추가 |
| `service/implementation/AuthServiceImpl.java` | **수정** — Rotation 연동 |
| `controller/AuthController.java` | **수정** — 재발급 시 쿠키 교체 |

---

### Phase 3: TOTP MFA

**목표**: 비밀번호 유출 시에도 2차 인증으로 방어

#### 3.3.1 라이브러리

```groovy
// build.gradle
implementation 'dev.samstevens.totp:totp:1.7.1'
```

이 라이브러리가 제공하는 기능:
- Secret 키 생성 (`SecretGenerator`)
- QR 코드 URI 생성 (`QrDataFactory` → otpauth:// URI)
- TOTP 코드 검증 (`CodeVerifier`, 시간 허용 범위 설정 가능)

#### 3.3.2 Admin 엔티티 변경

```java
@Entity
public class Admin {
    // 기존 필드...

    @Column(length = 64)
    private String totpSecret;       // Base32 인코딩된 TOTP 시크릿

    @Column(nullable = false)
    private Boolean mfaEnabled = false;  // MFA 활성화 여부

    public void enableMfa(String totpSecret) {
        this.totpSecret = totpSecret;
        this.mfaEnabled = true;
    }

    public void disableMfa() {
        this.totpSecret = null;
        this.mfaEnabled = false;
    }
}
```

#### 3.3.3 인증 흐름 변경

```
POST /api/v1/login (username + password)
  → 1차 인증 성공
  → MFA 활성화 여부 확인
     ├─ mfaEnabled = false → JWT 즉시 발급 (기존과 동일)
     └─ mfaEnabled = true  → pre-auth 토큰 발급
        {
          "mfaRequired": true,
          "preAuthToken": "임시토큰(5분 TTL)"
        }

POST /api/v1/mfa/verify (preAuthToken + totpCode)
  → pre-auth 토큰 검증 (Redis)
  → TOTP 코드 검증
  → 성공 → JWT(Access + Refresh) 발급
  → 실패 → 401 + 남은 시도 횟수
```

#### 3.3.4 Pre-Auth Token

1차 인증은 통과했지만 MFA 미완료 상태를 나타내는 임시 토큰:

| 항목 | 값 |
|------|----|
| Redis key | `preauth:{uuid}` |
| Value | `{adminId, username}` (JSON) |
| TTL | 5분 |
| 용도 | MFA 검증 요청 시 본인 확인 |

JWT가 아닌 Redis 기반 불투명 토큰을 사용하는 이유:
- 5분 후 자동 소멸 (JWT는 만료되어도 클라이언트에 남아있음)
- 서버 측에서 즉시 무효화 가능

#### 3.3.5 TOTP 관리 API

```
[MFA 등록]
POST /api/v1/mfa/setup          → TOTP secret 생성 + QR 코드 URI 반환
POST /api/v1/mfa/confirm-setup  → TOTP 코드로 검증 후 활성화 확정

[MFA 검증]
POST /api/v1/mfa/verify         → 로그인 시 2차 인증

[MFA 해제]
DELETE /api/v1/mfa              → TOTP 코드 확인 후 비활성화
```

#### 3.3.6 TotpService

**새 파일**: `service/TotpService.java`

```java
public interface TotpService {
    TotpSetupDto generateSecret(Long adminId);            // secret + QR URI 생성
    void confirmSetup(Long adminId, String totpCode);     // 등록 확정
    boolean verifyCode(Long adminId, String totpCode);    // 코드 검증
    void disable(Long adminId, String totpCode);          // MFA 해제
}
```

#### 3.3.7 응답 DTO

```java
// MFA 설정 응답
public class TotpSetupDto {
    private String secret;       // 수동 입력용 Base32 시크릿
    private String qrCodeUri;    // otpauth://totp/MyBlog:admin?secret=...&issuer=MyBlog
}

// 로그인 응답 (MFA 필요 시)
public class LoginResponseDto {
    private boolean mfaRequired;
    private String preAuthToken;  // mfaRequired=true일 때만 포함
    private String accessToken;   // mfaRequired=false일 때만 포함
}
```

#### 3.3.8 ErrorCode 추가

```java
MFA_REQUIRED(HttpStatus.OK, "A009", "2차 인증이 필요합니다."),
INVALID_TOTP_CODE(HttpStatus.UNAUTHORIZED, "A010", "인증 코드가 올바르지 않습니다."),
PRE_AUTH_EXPIRED(HttpStatus.UNAUTHORIZED, "A011", "인증 세션이 만료되었습니다. 다시 로그인해주세요."),
MFA_ALREADY_ENABLED(HttpStatus.CONFLICT, "A012", "이미 MFA가 활성화되어 있습니다."),
```

#### 3.3.9 변경 파일 목록

| 파일 | 작업 |
|------|------|
| `build.gradle` | **수정** — totp 라이브러리 추가 |
| `domain/admin/Admin.java` | **수정** — totpSecret, mfaEnabled 필드 추가 |
| `service/TotpService.java` | **신규** — 인터페이스 |
| `service/implementation/TotpServiceImpl.java` | **신규** — TOTP 생성/검증 구현 |
| `controller/MfaController.java` | **신규** — MFA 관련 API 엔드포인트 |
| `domain/token/LoginResponseDto.java` | **신규** — 로그인 응답 DTO |
| `domain/mfa/TotpSetupDto.java` | **신규** — TOTP 설정 DTO |
| `controller/AuthController.java` | **수정** — 로그인 응답 분기 처리 |
| `service/implementation/AuthServiceImpl.java` | **수정** — MFA 흐름 통합 |
| `exception/ErrorCode.java` | **수정** — MFA 관련 에러 코드 추가 |
| `configuration/WebSecurityConfig.java` | **수정** — MFA API 권한 설정 |
| `application.yaml` | **수정** — TOTP issuer 설정 추가 |

---

### Phase 4: 디바이스 신뢰 쿠키

**목표**: 신뢰된 디바이스에서는 TOTP 생략하여 UX 개선

#### 3.4.1 신뢰 쿠키 구조

기존 프로젝트의 HMAC 쿠키 패턴을 재활용:

```
쿠키명: DEVICE_TRUST
값: {deviceId}.{HMAC-SHA256(deviceId + clientIP, secret)}
Max-Age: 30일
HttpOnly: true
Secure: true (운영 환경)
SameSite: Strict
```

#### 3.4.2 검증 로직

```
TOTP 검증 직전 확인:
  1. DEVICE_TRUST 쿠키 존재?
     ├─ 없음 → TOTP 요구
     └─ 있음 → 쿠키 파싱
  2. deviceId 추출 + 현재 IP로 HMAC 재계산
  3. 서명 일치?
     ├─ 일치 → TOTP 생략, JWT 즉시 발급
     └─ 불일치 (IP 변경 등) → 쿠키 삭제 + TOTP 요구
```

#### 3.4.3 DeviceTrustService

**새 파일**: `service/DeviceTrustService.java`

```java
public interface DeviceTrustService {
    Cookie createTrustCookie(String clientIp);       // 신뢰 쿠키 생성
    boolean isTrustedDevice(Cookie cookie, String clientIp);  // 검증
}
```

#### 3.4.4 인증 흐름 최종

```
POST /api/v1/login
  → 1차 인증 (username + password) 성공
  → MFA 활성화?
     ├─ NO → JWT 즉시 발급
     └─ YES → 디바이스 신뢰 쿠키 확인
        ├─ 신뢰됨 → JWT 즉시 발급 (TOTP 생략)
        └─ 미신뢰 → pre-auth 토큰 발급 → TOTP 요구
           → TOTP 성공 → JWT 발급 + 디바이스 신뢰 쿠키 발급
```

#### 3.4.5 변경 파일 목록

| 파일 | 작업 |
|------|------|
| `service/DeviceTrustService.java` | **신규** — 인터페이스 |
| `service/implementation/DeviceTrustServiceImpl.java` | **신규** — HMAC 기반 구현 |
| `controller/AuthController.java` | **수정** — 디바이스 신뢰 분기 추가 |
| `controller/MfaController.java` | **수정** — TOTP 성공 시 신뢰 쿠키 발급 |
| `application.yaml` | **수정** — device trust HMAC secret, TTL 설정 |

---

### Phase 5: 프론트엔드 연동

#### 3.5.1 로그인 플로우 UI

```
[로그인 페이지]
  username + password 입력 → POST /api/v1/login
    ├─ mfaRequired: false → accessToken 저장 → 대시보드 이동
    └─ mfaRequired: true  → TOTP 입력 화면으로 전환
       → 6자리 코드 입력 → POST /api/v1/mfa/verify
       → 성공 → accessToken 저장 → 대시보드 이동

[MFA 설정 페이지] (/management/mfa)
  MFA 등록 → QR 코드 표시 → 인증 앱으로 스캔 → 코드 입력으로 확정
  MFA 해제 → 코드 입력으로 확인 후 해제
```

#### 3.5.2 에러 처리

| HTTP Status | ErrorCode | 프론트 동작 |
|-------------|-----------|------------|
| 423 | ACCOUNT_LOCKED | "계정이 잠겼습니다. {N}분 후 다시 시도해주세요." |
| 429 | TOO_MANY_REQUESTS | "요청이 너무 많습니다. 잠시 후 다시 시도해주세요." |
| 401 (A010) | INVALID_TOTP_CODE | "인증 코드가 올바르지 않습니다." |
| 401 (A011) | PRE_AUTH_EXPIRED | 로그인 페이지로 리다이렉트 |

---

## 4. 환경 변수 추가 목록

```yaml
# application.yaml에 추가될 설정
security:
  rate-limit:
    max-attempts: 10          # IP당 분당 최대 로그인 시도
    window-seconds: 60        # 윈도우 크기
  login-attempt:
    max-failures: 5           # 계정 잠금 임계값
    lock-duration-minutes: 15 # 잠금 지속 시간
  device-trust:
    hmac-secret: ${DEVICE_TRUST_HMAC_SECRET}
    max-age-days: 30          # 신뢰 쿠키 유효 기간

# TOTP
totp:
  issuer: MyBlog              # 인증 앱에 표시될 이름
```

---

## 5. DB 변경 사항

### Admin 테이블

```sql
ALTER TABLE admin ADD COLUMN totp_secret VARCHAR(64) NULL;
ALTER TABLE admin ADD COLUMN mfa_enabled BOOLEAN NOT NULL DEFAULT FALSE;
```

---

## 6. 구현 순서 및 의존 관계

```
Phase 1 (Rate Limiting + 로그인 시도 제한)
  │  독립적, 기존 코드 변경 최소
  │
Phase 2 (Refresh Token Rotation)
  │  JWT 구조 변경 포함, Phase 3보다 먼저 해야 함
  │  (MFA 흐름에서 토큰 발급 시 Rotation이 적용되어야 하므로)
  │
Phase 3 (TOTP MFA)
  │  Phase 2의 토큰 구조 위에 MFA 분기 추가
  │
Phase 4 (디바이스 신뢰 쿠키)
  │  Phase 3의 TOTP 흐름에 신뢰 분기 추가
  │
Phase 5 (프론트엔드 연동)
     Phase 1~4 API 완성 후 UI 연동
```

---

## 7. 테스트 계획

### Phase별 테스트 항목

#### Phase 1

| 테스트 | 검증 내용 |
|--------|-----------|
| `RateLimitFilterTest` | 10회 초과 시 429 응답 |
| `RateLimitFilterTest` | 윈도우 만료 후 재요청 허용 |
| `LoginAttemptServiceTest` | 5회 실패 시 잠금 |
| `LoginAttemptServiceTest` | 성공 시 카운터 초기화 |
| `LoginAttemptServiceTest` | 잠금 TTL 만료 후 해제 |
| `AuthControllerTest` | 잠금 상태에서 423 응답 |

#### Phase 2

| 테스트 | 검증 내용 |
|--------|-----------|
| `RefreshTokenServiceTest` | 토큰 저장/조회/삭제 |
| `RefreshTokenServiceTest` | Rotation 시 기존 토큰 폐기 |
| `RefreshTokenServiceTest` | 폐기 토큰 재사용 시 전체 무효화 |
| `AuthControllerTest` | 재발급 시 새 Refresh Token 쿠키 반환 |

#### Phase 3

| 테스트 | 검증 내용 |
|--------|-----------|
| `TotpServiceTest` | secret 생성 + QR URI 형식 |
| `TotpServiceTest` | 유효 코드 검증 성공 |
| `TotpServiceTest` | 무효 코드 검증 실패 |
| `MfaControllerTest` | MFA 설정 → 확인 → 활성화 |
| `MfaControllerTest` | 로그인 시 pre-auth 토큰 발급 |
| `MfaControllerTest` | TOTP 검증 후 JWT 발급 |
| `AuthControllerTest` | MFA 미설정 계정은 기존 흐름 유지 |

#### Phase 4

| 테스트 | 검증 내용 |
|--------|-----------|
| `DeviceTrustServiceTest` | 쿠키 생성 + HMAC 서명 검증 |
| `DeviceTrustServiceTest` | IP 변경 시 검증 실패 |
| `AuthControllerTest` | 신뢰 디바이스에서 TOTP 생략 |
| `AuthControllerTest` | 미신뢰 디바이스에서 TOTP 요구 |

---

## 8. 보안 공격별 방어 매핑

| 공격 유형 | 방어 계층 | 동작 |
|-----------|-----------|------|
| 브루트 포스 (무차별 시도) | Phase 1 | IP Rate Limit + 계정 잠금 |
| Credential Stuffing (유출 DB 이용) | Phase 1 + 3 | 잠금 + TOTP 필수 |
| 비밀번호 피싱 | Phase 3 | 비밀번호만으론 로그인 불가 |
| Refresh Token 탈취 | Phase 2 | Rotation + 재사용 감지 → 전체 무효화 |
| 세션 하이재킹 | Phase 2 + 4 | 짧은 Access TTL + IP 바인딩 |
| 새 기기에서 탈취 계정 사용 | Phase 4 | 디바이스 미신뢰 → TOTP 요구 |
| 자동화 봇 공격 | Phase 1 | Rate Limiting |
