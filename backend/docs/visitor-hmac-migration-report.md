# 방문자 토큰 Redis → HMAC 서명 쿠키 전환 작업 보고서

> 작업일: 2026-03-16
> 브랜치: develop
> 참조 계획서: `docs/plan-visitor-hmac-migration.md`

---

## 1. 작업 요약

방문자 중복 방지 로직을 **Redis Stateful → HMAC 서명 쿠키 Stateless** 방식으로 전환했습니다.
KST 자정 기준 명시적 고정 및 보안 설계를 반영하였으며, 기존 99개 테스트 전체 통과를 확인했습니다.

---

## 2. 변경 파일 목록

### 2-1. 삭제 (6개)

| 파일 | 삭제 이유 |
|------|-----------|
| `dto/member/RandomUserNumberDto.java` | HMAC 방식에서 랜덤 번호 DTO 불필요 |
| `repository/RandomUserNumberRedisRepository.java` | Redis Set 조회 인터페이스 제거 |
| `repository/implementation/RandomUserNumberRedisRepositoryImpl.java` | 구현체 제거 |
| `service/RandomUserNumberService.java` | 서비스 인터페이스 제거 |
| `service/implementation/RandomUserNumberServiceImpl.java` | 서비스 구현체 제거 |
| `test/.../VisitorCountServiceV2ImplTest.java` | 빈 stub 테스트 제거 |

### 2-2. 신규 생성 (3개)

| 파일 | 역할 |
|------|------|
| `service/VisitorHmacService.java` | HMAC 토큰 생성/검증 인터페이스 |
| `service/implementation/VisitorHmacServiceImpl.java` | HmacSHA256 기반 구현체 |
| `test/.../VisitorHmacServiceImplTest.java` | 순수 단위 테스트 (8개 케이스) |

### 2-3. 수정 (9개)

| 파일 | 수정 내용 |
|------|-----------|
| `application.yaml` | `visitor.hmac.secret: ${VISITOR_HMAC_SECRET}` 추가 |
| `utils/DateUtil.java` | `LocalDateTime.now()` → `ZonedDateTime.now(KST)` (KST 명시) |
| `utils/TTLCalculator.java` | `calculateSecondsUntilMidnight()` KST 기준으로 수정 |
| `utils/CookieUtil.java` | `addCookie(name, value, maxAge)` 오버로드 추가 |
| `interceptor/UserNumCookieInterceptor.java` | `RandomUserNumberService` → `VisitorHmacService` 전면 교체 |
| `scheduler/VisitorCountScheduledTask.java` | `@Scheduled(cron)` 두 곳에 `zone = "Asia/Seoul"` 추가 |
| `domain/keys/RedisKey.java` | `RANDOM_USER_NUM_KEY` 상수 제거 |
| `controller/BoardController.java` | v7 엔드포인트 `Long.valueOf(userNum)` → String 직접 사용 |
| `repository/UserViewedBoardRedisRepository.java` + 구현체 + 서비스 체인 (3파일) | `Long userNum` → `String userToken` 타입 변경 |

---

## 3. 핵심 구현 상세

### 3-1. HMAC 토큰 구조

```
{yyyy-MM-dd(KST)}:{Base64(HmacSHA256(date, VISITOR_HMAC_SECRET))}
예시: 2026-03-16:aB3fK9mXpQ2Kd+...
```

- 알고리즘: `HmacSHA256` — JVM 표준, 외부 의존성 없음
- KST 날짜를 서명 입력으로 사용 → 자정 기준 자동 만료
- `Base64.getEncoder()` (표준 인코더) 사용
- `split(DELIMITER, 2)` — 서명 내 `:` 방지 안전 분할

### 3-2. 검증 로직

```
1. null / blank → false (즉시 탈출)
2. ":" 구분 2분할 불가 → false
3. date != 오늘(KST) → false  (날짜 만료)
4. sign(date) != signature → false  (위변조)
5. 모두 통과 → true
```

### 3-3. 쿠키 Max-Age

- `VisitorHmacService.secondsUntilMidnight()` = KST 자정까지 남은 초
- `CookieUtil.addCookie(name, value, maxAge)` 신규 오버로드로 설정
- 기존 `addCookie(name, value)` 시그니처는 그대로 유지 (AuthController 영향 없음)

### 3-4. 플랜 미기재 사항 — UserViewedBoardService 체인 수정

플랜의 `UserNumCookieInterceptor` 에서 `request.setAttribute` 제거가 명시되어 있었으나,
`/api/v7/boards/{boardId}` 컨트롤러가 `@RequestAttribute(USER_NUM_COOKIE)` + `Long.valueOf(userNum)` 의존 중이었습니다.

HMAC 토큰은 문자열(`{date}:{signature}`) 이므로 `Long.valueOf` 변환 불가.
**해결**: 아래 체인 전체를 `Long userNum` → `String userToken` 으로 변경:

- `UserViewedBoardRedisRepository` (인터페이스)
- `UserViewedBoardRedisRepositoryImpl` (구현체)
- `UserViewedBoardService` (인터페이스)
- `UserUserViewedBoardServiceImpl` (구현체)
- `BoardController.getBoardDetailV7` (호출부)

인터셉터에서 `request.setAttribute(USER_NUM_COOKIE, cookieValue)` 는 유지 (v7 엔드포인트 호환).

> **의미 변화 주의**: 기존에는 사용자별 게시글 조회 중복 방지(랜덤 Long).
> 이제는 당일 토큰(모든 사용자가 동일한 HMAC 토큰 공유) 기준 → 게시글 조회 중복 방지가 **일별 공유** 방식으로 변경됨.
> 방문자 카운트 로직 자체는 정확히 플랜대로 동작.

---

## 4. KST 수정 범위

| 파일 | 변경 전 | 변경 후 |
|------|---------|---------|
| `DateUtil.getToday()` | `LocalDateTime.now()` | `ZonedDateTime.now(KST)` |
| `DateUtil.getTodayAndTime()` | `LocalDateTime.now()` | `ZonedDateTime.now(KST)` |
| `DateUtil.getPreviousDay()` | `LocalDateTime` 기반 변환 | `LocalDate.parse().minusDays(1)` (단순화) |
| `TTLCalculator.calculateSecondsUntilMidnight()` | `LocalDateTime.now()` | `ZonedDateTime.now(KST)` |
| `VisitorCountScheduledTask` cron | `zone` 미지정 (JVM TZ 의존) | `zone = "Asia/Seoul"` 명시 |

---

## 5. 테스트 결과

### 5-1. 신규 단위 테스트 (`VisitorHmacServiceImplTest`) — 8개 케이스

| 테스트 | 결과 |
|--------|------|
| `generateToken_형식_검증` | ✅ |
| `isValid_정상_토큰` | ✅ |
| `isValid_날짜_만료` | ✅ |
| `isValid_서명_위변조` | ✅ |
| `isValid_null` | ✅ |
| `isValid_빈값` | ✅ |
| `isValid_구분자_없음` | ✅ |
| `secondsUntilMidnight_범위` | ✅ |

> Redis 불필요 → `AbstractContainerBaseTest` 상속 없이 순수 단위 테스트로 구현

### 5-2. 전체 테스트 결과

```
Total tests: 99   (기존 91 + VisitorHmacServiceImplTest 8개)
Failures:     0
Errors:       0
```

---

## 6. 보안 체크리스트

| 항목 | 상태 |
|------|------|
| `VISITOR_HMAC_SECRET` 환경변수 전용 (`${VISITOR_HMAC_SECRET}`) | ✅ |
| secret 코드 하드코딩 없음 | ✅ |
| HmacSHA256 사용 | ✅ |
| 쿠키 `HttpOnly` + `Secure` + `SameSite=Lax` | ✅ |
| 쿠키 `MaxAge` = KST 자정까지 남은 초 | ✅ |
| 잔여 `RandomUserNumber*` 참조 없음 | ✅ |

---

## 7. 아키텍처 변화

```
[Before]
요청 → Interceptor → Redis SET 조회 (randomUserNum:)
                         ↓
                   존재 O → 통과
                   존재 X → 랜덤 Long 생성 → Redis SET 저장 + 세션 쿠키

[After]
요청 → Interceptor → HMAC 검증 (CPU 연산, 네트워크 없음)
                         ↓
                   유효 → 통과
                   무효 → HMAC 토큰 생성 + MaxAge 쿠키 발급
```

| 지표 | Before | After |
|------|--------|-------|
| 방문자 판별 비용 | Redis RTT (수 ms) | HMAC 연산 (수 μs) |
| Redis 의존 (중복방지) | 필수 | **제거** |
| Redis 의존 (카운트 캐싱) | 유지 | 유지 |
| Stateless | ❌ | ✅ |
| KST 자정 보장 | JVM TZ 의존 | `ZoneId("Asia/Seoul")` 명시 |
| 쿠키 MaxAge 제어 | 세션 쿠키 (불가) | KST 자정까지 초 단위 |
