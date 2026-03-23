# 방문자 토큰 Redis → HMAC 서명 쿠키 전환

> 작성일: 2026-03-15

---

## 1. 배경 및 목표

### 현행 문제

| 문제 | 근거 |
|------|------|
| 모든 요청마다 Redis 네트워크 I/O 발생 | `UserNumCookieInterceptor` → `randomUserNumberService.isRandomUserNumberInRedis()` |
| REST Stateless 원칙 위반 | 방문자 식별 근거가 서버(Redis)에 존재 |
| Timezone 미지정 | `LocalDateTime.now()` → JVM 기본 TZ, UTC 서버 배포 시 KST 자정 불일치 |
| `@Scheduled` cron `zone` 미설정 | Spring Framework 공식 문서 — `zone` 속성 미지정 시 JVM 기본 TZ 사용 |
| 쿠키 `MaxAge` 없음 | 세션 쿠키로 발급, 자정 만료 제어 불가 |

### 전환 목표

- **방문자 중복 방지 로직을 완전 Stateless**로 전환
- Redis는 방문자 **카운트 캐싱** 역할만 유지 (기존 `VisitorCountRedisRepository` 그대로)
- KST 자정을 모든 시간 연산의 기준으로 명시적 고정
- Spring Security `TokenBasedRememberMeServices`와 동일한 **Hash-Based Token 원리** 적용

### HMAC 쿠키 원리 (context7 — Spring Security 6.5 공식 문서 기반)

Spring Security의 Hash-Based 토큰 패턴:
```
field1 + ":" + field2 + ":" + algorithmName + ":" + HMAC(field1:field2:key)
```

이를 방문자 토큰에 맞게 단순화:
```
{date}:{Base64(HmacSHA256(date, SECRET_KEY))}
예시: 2026-03-15:aB3fK9mXpQ...
```

검증 절차:
```
1. 쿠키 파싱 → date : signature 분리
2. date == 오늘(KST)? → 아니면 만료 토큰
3. HmacSHA256(date, SECRET_KEY) == signature? → 아니면 위변조
4. 두 조건 모두 통과 → 오늘 이미 집계된 방문자
```

---

## 2. 변경 범위 전체 요약

### 2-1. 삭제 (6개 파일)

| 파일 경로 | 삭제 이유 |
|-----------|-----------|
| `dto/member/RandomUserNumberDto.java` | HMAC 방식에서 랜덤 번호 DTO 불필요 |
| `repository/RandomUserNumberRedisRepository.java` | Redis Set 조회 인터페이스 제거 |
| `repository/implementation/RandomUserNumberRedisRepositoryImpl.java` | 구현체 제거 |
| `service/RandomUserNumberService.java` | 서비스 인터페이스 제거 |
| `service/implementation/RandomUserNumberServiceImpl.java` | 서비스 구현체 제거 |
| `test/.../VisitorCountServiceV2ImplTest.java` | 비어있는 stub 테스트, 더 이상 유효하지 않음 |

### 2-2. 신규 생성 (2개 파일)

| 파일 경로 | 역할 |
|-----------|------|
| `service/VisitorHmacService.java` | HMAC 토큰 생성/검증 인터페이스 |
| `service/implementation/VisitorHmacServiceImpl.java` | `HmacSHA256` 기반 구현체 |

### 2-3. 수정 (7개 파일)

| 파일 경로 | 수정 내용 요약 |
|-----------|----------------|
| `utils/DateUtil.java` | `ZoneId("Asia/Seoul")` 명시 |
| `utils/TTLCalculator.java` | KST 기준 자정까지 초 계산 |
| `utils/CookieUtil.java` | `maxAge` 파라미터 오버로드 추가 |
| `interceptor/UserNumCookieInterceptor.java` | HMAC 검증 방식으로 전면 교체 |
| `scheduler/VisitorCountScheduledTask.java` | `zone = "Asia/Seoul"` 추가 |
| `domain/keys/RedisKey.java` | `RANDOM_USER_NUM_KEY` 상수 제거 |
| `application.yaml` | `visitor.hmac.secret` 프로퍼티 추가 |

---

## 3. 신규 파일 상세 설계

### 3-1. `VisitorHmacService.java` (인터페이스)

```java
package com.moya.myblogboot.service;

public interface VisitorHmacService {
    /** 오늘(KST) 날짜 기반 HMAC 서명 토큰 생성 */
    String generateToken();

    /** 쿠키 값이 오늘(KST) 날짜 기준 유효한 토큰인지 검증 */
    boolean isValid(String cookieValue);

    /** 쿠키 Max-Age: 현재 시각 기준 KST 자정까지 남은 초 */
    int secondsUntilMidnight();
}
```

### 3-2. `VisitorHmacServiceImpl.java` (구현체)

**의존성**: `javax.crypto.Mac`, `java.time.ZoneId` — 외부 라이브러리 추가 없음

```java
package com.moya.myblogboot.service.implementation;

@Service
public class VisitorHmacServiceImpl implements VisitorHmacService {

    private static final String ALGORITHM = "HmacSHA256";
    private static final ZoneId KST = ZoneId.of("Asia/Seoul");
    private static final String DELIMITER = ":";

    private final String secretKey; // application.yaml → visitor.hmac.secret

    // @Value("${visitor.hmac.secret}") 또는 @ConfigurationProperties 바인딩
    public VisitorHmacServiceImpl(@Value("${visitor.hmac.secret}") String secretKey) {
        this.secretKey = secretKey;
    }

    @Override
    public String generateToken() {
        String today = todayKst();
        return today + DELIMITER + sign(today);
    }

    @Override
    public boolean isValid(String cookieValue) {
        if (cookieValue == null || cookieValue.isBlank()) return false;
        String[] parts = cookieValue.split(DELIMITER, 2);
        if (parts.length != 2) return false;

        String date = parts[0];
        String signature = parts[1];

        return todayKst().equals(date) && sign(date).equals(signature);
    }

    @Override
    public int secondsUntilMidnight() {
        ZonedDateTime now = ZonedDateTime.now(KST);
        ZonedDateTime midnight = now.toLocalDate().plusDays(1).atStartOfDay(KST);
        return (int) Duration.between(now, midnight).getSeconds();
    }

    private String todayKst() {
        return ZonedDateTime.now(KST).format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
    }

    private String sign(String data) {
        try {
            Mac mac = Mac.getInstance(ALGORITHM);
            mac.init(new SecretKeySpec(secretKey.getBytes(StandardCharsets.UTF_8), ALGORITHM));
            return Base64.getEncoder().encodeToString(
                mac.doFinal(data.getBytes(StandardCharsets.UTF_8))
            );
        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            throw new IllegalStateException("HMAC 서명 실패", e);
        }
    }
}
```

**핵심 설계 결정:**
- `sign()` 내부에서 예외는 `IllegalStateException`으로 래핑 — JVM에 `HmacSHA256`은 항상 존재하므로 실제 발생하지 않음
- `split(DELIMITER, 2)` — 서명 내 `:`(Base64에는 없지만) 안전하게 2분할
- `secretKey`는 생성자 주입 → 테스트 시 직접 주입 가능

---

## 4. 기존 파일 수정 상세

### 4-1. `DateUtil.java`

**변경 이유**: `LocalDateTime.now()`는 JVM 기본 TZ 의존 — UTC 서버에서 KST 날짜 불일치 발생

```java
// Before
public static String getToday() {
    return LocalDateTime.now().format(DateTimeFormatter.ofPattern(DATE_PATTERN));
}

// After
private static final ZoneId KST = ZoneId.of("Asia/Seoul");

public static String getToday() {
    return ZonedDateTime.now(KST).format(DateTimeFormatter.ofPattern(DATE_PATTERN));
}
```

`getPreviousDay()`, `getTodayAndTime()` 도 동일하게 `ZonedDateTime.now(KST)` 기반으로 수정.

---

### 4-2. `TTLCalculator.java`

**변경 이유**: 자정 계산이 JVM 기본 TZ 기준 — KST 명시 필요

```java
// Before
public static long calculateSecondsUntilMidnight() {
    LocalDateTime now = LocalDateTime.now();
    LocalDateTime midnight = now.toLocalDate().plusDays(1).atStartOfDay();
    return Duration.between(now, midnight).getSeconds();
}

// After
private static final ZoneId KST = ZoneId.of("Asia/Seoul");

public static long calculateSecondsUntilMidnight() {
    ZonedDateTime now = ZonedDateTime.now(KST);
    ZonedDateTime midnight = now.toLocalDate().plusDays(1).atStartOfDay(KST);
    return Duration.between(now, midnight).getSeconds();
}
```

> `VisitorHmacServiceImpl.secondsUntilMidnight()`와 로직이 동일하지만,
> `TTLCalculator`는 다른 곳(방문자 카운트 Redis 캐싱 TTL 등)에서도 사용될 수 있으므로 유지.

---

### 4-3. `CookieUtil.java`

**변경 이유**: 기존 `addCookie()`는 `MaxAge` 없이 세션 쿠키 발급 — HMAC 쿠키는 KST 자정 기준 `MaxAge` 필요

```java
// 기존 메서드 — AuthController(refresh_token) 용도로 그대로 유지
public static Cookie addCookie(String cookieName, String cookieValue) { ... }

// 신규 오버로드 추가 — visitor HMAC 쿠키용
public static Cookie addCookie(String cookieName, String cookieValue, int maxAge) {
    Cookie cookie = new Cookie(cookieName, cookieValue);
    cookie.setPath("/");
    cookie.setHttpOnly(true);
    cookie.setSecure(true);
    cookie.setAttribute("SameSite", "Lax");
    cookie.setMaxAge(maxAge);  // 추가
    return cookie;
}
```

기존 `addCookie(name, value)` 시그니처는 **그대로 유지** → `AuthController` 수정 불필요.

---

### 4-4. `UserNumCookieInterceptor.java`

**핵심 변경** — `RandomUserNumberService` 의존 제거, `VisitorHmacService` 주입

```java
// Before (의존성)
private final RandomUserNumberService randomUserNumberService;
private final VisitorCountService visitorCountService;

// After (의존성)
private final VisitorHmacService visitorHmacService;
private final VisitorCountService visitorCountService;

// Before (preHandle 핵심 로직)
Cookie userNumCookie = CookieUtil.findCookie(request, USER_NUM_COOKIE);
String userNumValue = userNumCookie != null ? userNumCookie.getValue() : "";

if (!validateUserNum(userNumValue)) {
    RandomUserNumberDto userNumberDto = randomUserNumberService.getRandomUserNumber();
    Cookie newUserNumCookie = createUserNumCookie(userNumberDto);
    response.addCookie(newUserNumCookie);
    userNumValue = String.valueOf(userNumberDto.getNumber());
    visitorCountService.incrementVisitorCount(DateUtil.getToday());
}
request.setAttribute(USER_NUM_COOKIE, userNumValue);

// After (preHandle 핵심 로직)
Cookie userNumCookie = CookieUtil.findCookie(request, USER_NUM_COOKIE);
String cookieValue = userNumCookie != null ? userNumCookie.getValue() : null;

if (!visitorHmacService.isValid(cookieValue)) {
    String newToken = visitorHmacService.generateToken();
    int maxAge = visitorHmacService.secondsUntilMidnight();
    response.addCookie(CookieUtil.addCookie(USER_NUM_COOKIE, newToken, maxAge));
    visitorCountService.incrementVisitorCount(DateUtil.getToday());
}
```

`validateUserNum()`, `createUserNumCookie()` private 메서드 — **삭제**

---

### 4-5. `VisitorCountScheduledTask.java`

**변경 이유**: context7 Spring Framework 공식 문서 — `zone` 속성 미지정 시 JVM 기본 TZ 사용

```java
// Before
@Scheduled(cron = "0 0 0 * * ?")
public void updateVisitorCountAtMidnight() { ... }

@Scheduled(cron = "0 1 0 * * ?")
public void createTodayVisitorCountAtMidnight() { ... }

// After
@Scheduled(cron = "0 0 0 * * ?", zone = "Asia/Seoul")
public void updateVisitorCountAtMidnight() { ... }

@Scheduled(cron = "0 1 0 * * ?", zone = "Asia/Seoul")
public void createTodayVisitorCountAtMidnight() { ... }
```

`fixedDelay` 방식의 `updateVisitorCountEveryTenMinutes()`는 절대 시각 기반이 아니므로 변경 불필요.

---

### 4-6. `RedisKey.java`

```java
// Before
public static final String RANDOM_USER_NUM_KEY = "randomUserNum:";

// After — 해당 상수 삭제
// (참조 클래스 RandomUserNumberRedisRepositoryImpl도 함께 삭제되므로 컴파일 오류 없음)
```

---

### 4-7. `application.yaml`

**변경 이유**: context7 Spring Boot 공식 문서 — `@ConfigurationProperties` 또는 `@Value`로 외부 환경변수 바인딩

```yaml
# 추가
visitor:
  hmac:
    secret: ${VISITOR_HMAC_SECRET}
```

- `VISITOR_HMAC_SECRET`: 운영 환경변수로 관리 (`.env`, Docker `--env`, K8s Secret 등)
- 최소 32바이트(256bit) 이상의 무작위 문자열 권장
- 기존 `JWT_SECRET_KEY` 관리 방식과 동일한 패턴 유지

---

## 5. 작업 순서

```
Step 1 — application.yaml 수정 (secret 프로퍼티 추가)
Step 2 — DateUtil, TTLCalculator KST 수정
Step 3 — CookieUtil maxAge 오버로드 추가
Step 4 — VisitorHmacService 인터페이스 생성
Step 5 — VisitorHmacServiceImpl 구현체 생성
Step 6 — UserNumCookieInterceptor 교체
Step 7 — VisitorCountScheduledTask zone 추가
Step 8 — RedisKey RANDOM_USER_NUM_KEY 제거
Step 9 — 불필요 파일 6개 삭제
Step 10 — 컴파일 확인 및 테스트 실행
```

> Step 1~3은 순서 무관하게 병렬 진행 가능.
> Step 6(Interceptor 교체)은 Step 4~5(VisitorHmacService 완성) 이후에 진행.
> Step 9(파일 삭제)는 Step 6 완료 후 컴파일 확인 후 진행.

---

## 6. 테스트 계획

### 6-1. 기존 테스트 영향

| 테스트 클래스 | 영향 | 조치 |
|---------------|------|------|
| `VisitorCountServiceImplTest` | `VisitorCountRedisRepository` 직접 사용 → 변경 없음 | 그대로 유지 |
| `VisitorCountServiceV2ImplTest` | 빈 stub | **삭제** |
| 나머지 18개 `@SpringBootTest` | `RandomUserNumberService` 미사용 → 영향 없음 | 그대로 유지 |

### 6-2. 신규 단위 테스트 — `VisitorHmacServiceImplTest`

| 테스트 케이스 | 검증 내용 |
|---------------|-----------|
| `generateToken_형식_검증` | `{date}:{signature}` 형식, date == 오늘(KST) |
| `isValid_정상_토큰` | 오늘 날짜 + 올바른 서명 → `true` |
| `isValid_날짜_만료` | 어제 날짜 토큰 → `false` |
| `isValid_서명_위변조` | 날짜는 오늘, 서명 변조 → `false` |
| `isValid_null_빈값` | `null`, `""`, `":"` → `false` |
| `isValid_잘못된_형식` | 구분자 없음, 3분할 → `false` |
| `secondsUntilMidnight_범위` | `0 < result <= 86400` |

> `VisitorHmacServiceImpl`은 Redis 불필요 → `AbstractContainerBaseTest` 상속 없이 순수 단위 테스트 가능

### 6-3. `UserNumCookieInterceptor` 통합 검증 체크리스트

- [ ] 쿠키 없는 첫 방문자 → 새 토큰 쿠키 발급, 방문자 수 +1
- [ ] 유효한 HMAC 쿠키 재방문 → 쿠키 재발급 없음, 방문자 수 유지
- [ ] 전날 날짜 쿠키 → 만료 처리, 새 토큰 발급, 방문자 수 +1
- [ ] 서명 위변조 쿠키 → 거부, 새 토큰 발급, 방문자 수 +1
- [ ] 발급된 쿠키의 `Max-Age` > 0 확인

---

## 7. 보안 체크리스트

| 항목 | 기준 |
|------|------|
| `VISITOR_HMAC_SECRET` 길이 | 최소 32바이트 (256bit) |
| secret 코드 하드코딩 | 금지 — 환경변수 전용 |
| secret 키 로테이션 | 로테이션 시 당일 기존 쿠키 전체 무효화 허용 범위 내 |
| 쿠키 속성 | `HttpOnly`, `Secure`, `SameSite=Lax` 유지 (현행 동일) |
| HMAC 알고리즘 | `HmacSHA256` — JVM 표준, 별도 의존성 없음 |

---

## 8. 변경 전/후 아키텍처 비교

```
[Before]
요청 → Interceptor → Redis SET 조회 (randomUserNum:)
                          ↓
                    존재 O → 통과
                    존재 X → 랜덤 Long 생성 → Redis SET 저장 + 쿠키 발급

[After]
요청 → Interceptor → HMAC 검증 (CPU 연산, 네트워크 없음)
                          ↓
                    유효 → 통과
                    무효 → HMAC 서명 쿠키 생성 + 발급
```

| 지표 | Before | After |
|------|--------|-------|
| 방문자 판별 비용 | Redis RTT (수 ms) | HMAC 연산 (수 μs) |
| Redis 의존 (중복방지) | 필수 | **제거** |
| Redis 의존 (카운트 캐싱) | 유지 | 유지 |
| 수평 확장 | Redis 클러스터 필요 | 서버 간 상태 공유 불필요 |
| Stateless | ❌ | ✅ |
| KST 자정 보장 | JVM TZ 의존 | `ZoneId("Asia/Seoul")` 명시 |
