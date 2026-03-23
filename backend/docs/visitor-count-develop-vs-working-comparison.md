# VisitorCount 로직 비교: develop 브랜치(커밋) vs 워킹 트리(수정)

> 2026-03-17

---

## 개요

develop 브랜치에 커밋된 VisitorCount 관련 코드와 현재 워킹 트리의 수정 사항을 비교한다.
변경의 핵심은 **방문자 식별 방식의 Stateful → Stateless 전환**과 **Cold Cache 버그 수정**이다.

---

## 1. 아키텍처 변경 요약

### develop (커밋 상태)

```
방문자 요청 → UserNumCookieInterceptor
    → 쿠키 없음 → RandomUserNumberService.getRandomUserNumber()
        → ThreadLocalRandom으로 Long 난수 생성
        → Redis SET에 저장 (TTL: 자정까지)
        → 쿠키에 난수 저장
    → 쿠키 있음 → Redis SET에서 존재 여부 검증
        → 없으면 재생성

→ BoardController (v7)
    → Long userNum = Long.valueOf(cookieValue)
    → UserViewedBoardService.isViewedBoard(Long userNum, boardId)
    → VisitorCountService.incrementVisitorCount(date)
```

### 워킹 트리 (수정 상태)

```
방문자 요청 → UserNumCookieInterceptor
    → 쿠키 없음 → VisitorHmacService.generateToken()
        → HMAC-SHA256(오늘 날짜, secret) 서명 생성
        → "yyyy-MM-dd:base64(signature)" 형식 토큰
        → 쿠키에 토큰 저장 (maxAge: 자정까지)
    → 쿠키 있음 → VisitorHmacService.isValid(cookieValue)
        → 날짜가 오늘(KST)인지 + 서명 일치 검증
        → 무효하면 재생성

→ BoardController (v7)
    → String userToken = cookieValue (타입 변환 없음)
    → UserViewedBoardService.isViewedBoard(String userToken, boardId)
    → VisitorCountService.incrementVisitorCount(date)
```

---

## 2. 파일별 상세 비교

### 2.1 삭제된 파일 (5개) — RandomUserNumber 체인

| 파일 | 역할 |
|------|------|
| `RandomUserNumberService.java` | 인터페이스: `getRandomUserNumber()`, `isRandomUserNumberInRedis(long)` |
| `RandomUserNumberServiceImpl.java` | 구현체: `ThreadLocalRandom`으로 Long 난수 생성, Redis SET 저장, 중복 시 재생성 |
| `RandomUserNumberRedisRepository.java` | 인터페이스: `isExists(long)`, `save(long, long)` |
| `RandomUserNumberRedisRepositoryImpl.java` | 구현체: `opsForSet().add()`, `opsForSet().isMember()` |
| `RandomUserNumberDto.java` | DTO: `number(long)`, `expireTime(long)` |

**삭제 이유**: 방문자 식별에 Redis 상태 저장이 불필요해짐. HMAC 서명으로 쿠키 자체에서 유효성 검증 가능.

### 2.2 신규 파일 (2개) — VisitorHmac 체인

| 파일 | 역할 |
|------|------|
| `VisitorHmacService.java` | 인터페이스: `generateToken()`, `isValid(String)`, `secondsUntilMidnight()` |
| `VisitorHmacServiceImpl.java` | 구현체: `HmacSHA256` 서명, KST 기준 날짜 토큰 생성/검증 |

**토큰 형식**: `yyyy-MM-dd:Base64(HMAC-SHA256(date, secret))`

**검증 로직**:
1. `:`로 분리 → `[date, signature]`
2. `date`가 오늘(KST)과 일치하는지 확인
3. `HMAC(date, secret)`이 `signature`와 일치하는지 확인

### 2.3 UserNumCookieInterceptor

| 항목 | develop (커밋) | 워킹 트리 (수정) |
|------|---------------|-----------------|
| 의존성 | `RandomUserNumberService` | `VisitorHmacService` |
| 검증 방식 | Redis SET 조회 (`isRandomUserNumberInRedis(long)`) | HMAC 서명 검증 (`isValid(String)`) — Redis 불필요 |
| 토큰 생성 | `ThreadLocalRandom.nextLong()` → Redis 저장 | `HMAC-SHA256(today, secret)` → 서명만 계산 |
| 쿠키 maxAge | 미지정 (세션 쿠키) | `secondsUntilMidnight()` (KST 자정까지) |

### 2.4 UserViewedBoard 체인 (파라미터 타입 변경)

**변경**: `Long userNum` → `String userToken` (4개 파일)

| 파일 | 변경 내용 |
|------|----------|
| `UserViewedBoardService.java` | 인터페이스 파라미터 `Long userNum` → `String userToken` |
| `UserUserViewedBoardServiceImpl.java` | 구현체 파라미터 동일 변경 |
| `UserViewedBoardRedisRepository.java` | 인터페이스 파라미터 동일 변경 |
| `UserViewedBoardRedisRepositoryImpl.java` | 구현체 파라미터 동일 변경, Redis 키 생성도 `String` 기반 |

**Redis 키 변경**:
- develop: `userViewedBoard:{Long 난수}` (예: `userViewedBoard:8234567890123`)
- 수정: `userViewedBoard:{HMAC 토큰}` (예: `userViewedBoard:2026-03-17:abc123...`)

### 2.5 BoardController (v7 엔드포인트)

```java
// develop (커밋)
@RequestAttribute(USER_NUM_COOKIE) String userNum
Long userN = Long.valueOf(userNum);
userViewedBoardService.isViewedBoard(userN, boardId);
userViewedBoardService.addViewedBoard(userN, boardId);

// 워킹 트리 (수정)
@RequestAttribute(USER_NUM_COOKIE) String userToken
userViewedBoardService.isViewedBoard(userToken, boardId);  // Long 변환 제거
userViewedBoardService.addViewedBoard(userToken, boardId);
```

### 2.6 VisitorCountRedisRepositoryImpl — Cold Cache 버그 수정

```java
// develop (커밋) — 버그 코드
public VisitorCountDto increment(String keyDate) {
    String key = getKey(keyDate);
    redisTemplate.opsForHash().increment(key, TOTAL_COUNT_KEY, 1L);  // 키 없어도 자동 생성
    redisTemplate.opsForHash().increment(key, TODAY_COUNT_KEY, 1L);
    Map<Object, Object> entries = redisTemplate.opsForHash().entries(key);
    if (entries.isEmpty()) {  // ← HINCRBY가 이미 키를 생성했으므로 절대 true가 되지 않음
        return VisitorCountDto.builder()
                .total(-1L).today(-1L).yesterday(-1L).build();  // 죽은 코드
    }
    return VisitorCountDto.builder()
            .total(getLongValue(entries.get(TOTAL_COUNT_KEY)))
            .today(getLongValue(entries.get(TODAY_COUNT_KEY)))
            .yesterday(getLongValue(entries.get(YESTERDAY_COUNT_KEY)))
            .build();
}
```

```java
// 워킹 트리 (수정) — 수정 코드
public Optional<VisitorCountDto> increment(String keyDate) {
    String key = getKey(keyDate);
    // Cold Cache 감지: 키가 없으면 서비스 레이어의 DB 복구 블록으로 위임
    if (Boolean.FALSE.equals(redisTemplate.hasKey(key))) {
        return Optional.empty();
    }
    redisTemplate.opsForHash().increment(key, TOTAL_COUNT_KEY, 1L);
    redisTemplate.opsForHash().increment(key, TODAY_COUNT_KEY, 1L);
    redisTemplate.expire(key, 7, TimeUnit.DAYS);
    return findByDate(keyDate);
}
```

| 항목 | develop (커밋) | 워킹 트리 (수정) |
|------|---------------|-----------------|
| 반환 타입 | `VisitorCountDto` | `Optional<VisitorCountDto>` |
| Cold Cache 감지 | `entries.isEmpty()` (HINCRBY 후 → 항상 false) | `hasKey()` 선검사 (HINCRBY 전 → 정확한 감지) |
| 감지 실패 시 | 센티넬 값 `{-1, -1, -1}` 반환 (도달 불가) | `Optional.empty()` 반환 |
| TTL 갱신 | 없음 | `expire(key, 7, DAYS)` 추가 |

### 2.7 VisitorCountRedisRepository (인터페이스)

```java
// develop: VisitorCountDto increment(String keyDate);
// 수정:   Optional<VisitorCountDto> increment(String keyDate);
```

### 2.8 VisitorCountServiceImpl

```java
// develop (커밋)
public VisitorCountDto incrementVisitorCount(String date) {
    VisitorCountDto visitorCountDto = visitorCountRedisRepository.increment(date);
    if (visitorCountDto.getTotal() < 0) {  // 센티넬 값 검사 (도달 불가)
        retrieveAndSaveVisitorCount(date);
        return visitorCountRedisRepository.increment(date);
    }
    return visitorCountDto;
}
```

```java
// 워킹 트리 (수정)
public VisitorCountDto incrementVisitorCount(String date) {
    return visitorCountRedisRepository.increment(date).orElseGet(() -> {
        retrieveAndSaveVisitorCount(date);
        return visitorCountRedisRepository.increment(date)
                .orElseThrow(() -> new IllegalStateException("방문자 수 증가 실패"));
    });
}
```

| 항목 | develop (커밋) | 워킹 트리 (수정) |
|------|---------------|-----------------|
| Cold Cache 분기 | `if (total < 0)` 센티넬 검사 (죽은 코드) | `orElseGet()` — Optional 기반 |
| DB 복구 흐름 | 실행 불가 (센티넬 도달 안 됨) | 정상 작동 |
| 실패 처리 | 없음 | `orElseThrow(IllegalStateException)` |
| `syncVisitorCountToDb()` | 없음 (스케줄러에 직접 구현) | 서비스에 추가 (스케줄러 위임 대상) |

### 2.9 VisitorCountScheduledTask

| 항목 | develop (커밋) | 워킹 트리 (수정) |
|------|---------------|-----------------|
| 어노테이션 | `@Service` | `@Component` |
| 의존성 | `VisitorCountService` + `VisitorCountRepository` | `VisitorCountService`만 |
| DB 동기화 | 스케줄러에서 직접 엔티티 빌드 + 저장 | `visitorCountService.syncVisitorCountToDb()` 위임 |
| 10분 주기 | `fixedRate = 600000` | `fixedDelay = 600000, initialDelay = 600000` |
| 자정 동기화 크론 | `0 0 0 * * ?` | `0 0 0 * * ?`, `zone = "Asia/Seoul"` |
| 당일 생성 크론 | `0 0 0 * * ?` (자정 동기화와 동시) | `0 1 0 * * ?`, `zone = "Asia/Seoul"` (1분 후) |
| `@Transactional` | 스케줄러에 직접 부착 | 제거 (서비스 레이어에서 관리) |

**`fixedRate` → `fixedDelay` 변경 이유**:
- `fixedRate`: 이전 실행 완료와 무관하게 고정 간격으로 실행 → DB 동기화가 오래 걸리면 중첩 실행 가능
- `fixedDelay`: 이전 실행 완료 후부터 간격 측정 → 중첩 방지
- `initialDelay`: 서버 시작 직후 실행 방지 (Redis가 아직 비어있을 수 있음)

### 2.10 VisitorCountDto

```java
// develop (커밋) — increment() 메서드 포함
public VisitorCountDto increment() {
    return VisitorCountDto.builder()
            .total(this.total + 1)
            .today(this.today + 1)
            .yesterday(this.yesterday)
            .build();
}

// 워킹 트리 (수정) — increment() 메서드 삭제
// Redis HINCRBY로 원자적 증가하므로 DTO 레벨 increment 불필요
```

### 2.11 DateUtil

| 항목 | develop (커밋) | 워킹 트리 (수정) |
|------|---------------|-----------------|
| 시간 기준 | `LocalDateTime.now()` (서버 타임존 의존) | `ZonedDateTime.now(KST)` (`Asia/Seoul` 명시) |
| `getToday()` | `LocalDateTime.now().format(...)` | `ZonedDateTime.now(KST).format(...)` |
| `getPreviousDay()` | `LocalDate.parse().atStartOfDay().minusDays(1).format()` | `LocalDate.parse().minusDays(1).format()` (단순화) |
| `getTodayAndTime()` | `LocalDateTime.now().format(...)` | `ZonedDateTime.now(KST).format(...)` |

### 2.12 TTLCalculator

| 항목 | develop (커밋) | 워킹 트리 (수정) |
|------|---------------|-----------------|
| 자정 계산 기준 | `LocalDateTime.now()` (서버 타임존 의존) | `ZonedDateTime.now(KST)` (`Asia/Seoul` 명시) |

### 2.13 CookieUtil

```java
// 워킹 트리 — 신규 오버로드 메서드 추가
public static void addCookie(HttpServletResponse response, String name, String value, int maxAge) {
    Cookie cookie = new Cookie(name, value);
    cookie.setPath("/");
    cookie.setHttpOnly(true);
    cookie.setMaxAge(maxAge);
    response.addCookie(cookie);
}
```

### 2.14 RedisKey

```java
// develop: RANDOM_USER_NUM_KEY = "randomUserNum" 포함
// 수정:   RANDOM_USER_NUM_KEY 삭제
```

### 2.15 WebSecurityConfig

| 항목 | develop (커밋) | 워킹 트리 (수정) |
|------|---------------|-----------------|
| Security DSL | 체이닝 방식 (deprecated) | 람다 DSL (`AbstractHttpConfigurer::disable`) |

### 2.16 VisitorCountCustomRepositoryImpl

```java
// develop: "v.date =: date"  (공백 오류, JPA 파서 관용으로 동작)
// 수정:   "v.date = :date"   (올바른 JPQL 문법)
```

### 2.17 InitDb

| 항목 | develop (커밋) | 워킹 트리 (수정) |
|------|---------------|-----------------|
| `@Component` | 활성 | 주석 처리 (`//@Component`) |
| `InitService` 내부 클래스 | 어노테이션 없음 | `//@Service` 주석 처리 상태 |

---

## 3. 변경 영향 분석

### 3.1 해결된 문제

| 문제 | 원인 | 수정 |
|------|------|------|
| Cold Cache 시 방문자 수 0 초기화 | `HINCRBY`의 auto-create로 `Optional.empty()` 미반환 | `hasKey()` 선검사 추가 |
| DB 복구 코드 미실행 | 센티넬 값(-1) 도달 불가 | `Optional` 기반 흐름 전환 |
| 서버 타임존 의존 | `LocalDateTime.now()` 사용 | KST(`Asia/Seoul`) 명시 |
| 스케줄러 중첩 실행 가능 | `fixedRate` 사용 | `fixedDelay` + `initialDelay` 전환 |
| 자정 동기화/생성 동시 실행 | 둘 다 `0 0 0 * * ?` | 생성을 `0 1 0 * * ?`로 1분 지연 |
| 방문자 식별에 Redis 상태 의존 | `RandomUserNumber` Redis SET | HMAC 서명 쿠키 (Stateless) |

### 3.2 삭제/추가 파일 요약

| 구분 | 파일 수 | 대상 |
|------|---------|------|
| 삭제 | 5 | `RandomUserNumber{Service, ServiceImpl, RedisRepository, RedisRepositoryImpl, Dto}` |
| 추가 | 2 | `VisitorHmac{Service, ServiceImpl}` |
| 수정 | 14 | 상기 2.3 ~ 2.17 항목 |

### 3.3 Redis 사용량 변화

| 항목 | develop (커밋) | 워킹 트리 (수정) |
|------|---------------|-----------------|
| `randomUserNum` SET | 사용 (방문자별 Long 저장) | 삭제 |
| `visitorCount:{date}` HASH | 사용 | 사용 (변경 없음) |
| `userViewedBoard:{id}` SET | 키에 Long 사용 | 키에 HMAC 토큰 String 사용 |

RandomUserNumber Redis SET이 제거되어 Redis 메모리 사용량이 감소한다.

---

## 4. 정리

이번 변경은 단순한 버그 픽스를 넘어, 방문자 추적 아키텍처의 **Stateful → Stateless 전환**이다.

1. **방문자 식별**: Redis 난수 저장 → HMAC 서명 쿠키 (서버 재시작 영향 없음)
2. **Cold Cache 복구**: 센티넬 값 기반 → `Optional` + `hasKey()` 선검사 (실제 동작하는 복구)
3. **시간 처리**: 서버 타임존 의존 → KST 명시 (배포 환경 무관)
4. **스케줄러**: 책임 분리 + 실행 순서 보장 + 중첩 방지
