# 방문자 수 API 설계 검토 보고서

> 작성일: 2026-03-14
> 검토 범위: `VisitorCount` 도메인 전체 (Entity, DTO, Repository, Service, Scheduler, Interceptor, Controller)
> 참고 문서: Spring Data Redis 공식 문서, Spring Boot Scheduling 공식 문서 (context7)

---

## 1. 전체 아키텍처 요약

```
요청 → UserNumCookieInterceptor
         ├─ 기존 user_n 쿠키 유효 → 통과
         └─ 쿠키 없음/유효하지 않음 → 새 쿠키 발급 + incrementVisitorCount()
                                               ↓
                                        Redis HINCRBY (total, today)
                                               ↓ (10분마다)
                                        VisitorCountScheduledTask → DB 동기화
                                               ↓ (자정)
                                        updateAtMidnight + createTodayRecord

GET /api/v2/visitor-count → VisitorCountService.getVisitorCount()
                              ├─ Redis 조회 성공 → 반환
                              └─ Redis 미스 → DB 조회 → Redis 저장 → 반환
```

**긍정적 설계 요소:**
- Redis를 Write-Through Cache로 활용하여 실시간 카운팅 부하를 DB에서 분리한 점
- 일별 통계를 별도 엔티티(`VisitorCount`)로 분리한 점
- `@DynamicPropertySource`를 이용한 테스트 환경 Redis 격리

---

## 2. 결함 목록

### 🔴 심각 (Critical)

#### C-1. `@Transactional` 자기 호출(Self-invocation) 무효화

**위치:** `VisitorCountServiceImpl.java:70`

```java
// @Transactional이 선언되어 있지만 같은 클래스 내 호출이라 프록시를 거치지 않음
@Transactional
public VisitorCount retrieveVisitorCountFromDB(String formattedDate) { ... }

// 호출 경로:
// incrementVisitorCount() → retrieveAndSaveVisitorCount() → retrieveVisitorCountDto()
//   → retrieveVisitorCountFromDB()  ← 같은 빈 내 호출, AOP 프록시 미적용
```

Spring의 `@Transactional`은 프록시 기반 AOP로 동작한다. 같은 클래스 내부에서 호출되면 프록시를 우회하므로 `@Transactional` 어노테이션이 **완전히 무시**된다. 결과적으로 `retrieveVisitorCountFromDB`의 `save()` 호출은 트랜잭션 없이 실행될 수 있다.

**영향:** 데이터 저장 도중 예외 발생 시 롤백 불가, 데이터 불일치 위험

**권고:** 별도 `@Component` 빈으로 분리하거나, `ApplicationContext`를 통해 자기 자신의 프록시를 주입받아 호출한다.

---

#### C-2. `synchronized` + `@Transactional` 동시성 제어 충돌

**위치:** `VisitorCountServiceImpl.java:55`, `VisitorCountScheduledTask.java:38`

```java
// Service
private synchronized VisitorCountDto retrieveVisitorCountDto(String date) { ... }

// Scheduler
@Transactional
@Scheduled(cron = "0 0 0 * * ?")
public void updateVisitorCountAtMidnight() {
    lock.lock();
    try { ... }
    finally { lock.unlock(); }  // ← 락 해제 후 트랜잭션 커밋 전 다른 스레드 진입 가능
}
```

두 가지 문제가 있다:

1. **`synchronized`는 단일 JVM에서만 유효**하다. 다중 인스턴스(수평 확장) 환경에서는 동시성 보호가 전혀 작동하지 않는다.

2. **스케줄러에서 `ReentrantLock`이 `finally` 블록에서 해제되는 시점은 메서드 반환 전**이지만, Spring의 트랜잭션 커밋은 메서드 반환 **이후** 프록시 레이어에서 일어난다. 따라서 락 해제 → 다음 스레드 진입 → 첫 번째 스레드 커밋 사이의 짧은 구간에서 데이터 경쟁이 발생할 수 있다.

**권고:** Redisson 등 Redis 기반 분산 락으로 교체하거나, 스케줄러를 `@Transactional` 없이 작성하고 서비스 레이어에서만 트랜잭션을 관리한다.

---

#### C-3. 자정 스케줄 두 메서드의 실행 순서 미보장

**위치:** `VisitorCountScheduledTask.java:37, 84`

```java
@Scheduled(cron = "0 0 0 * * ?")  // 전날 통계 DB 동기화
public void updateVisitorCountAtMidnight() { ... }

@Scheduled(cron = "0 0 0 * * ?")  // 오늘 날짜 레코드 생성
public void createTodayVisitorCountAtMidnight() { ... }
```

두 메서드 모두 `0 0 0 * * ?`으로 동시에 트리거된다. **`updateVisitorCountAtMidnight`가 먼저 완료되어야** `createTodayVisitorCountAtMidnight`가 올바른 `totalVisitors`를 참조할 수 있다. 스케줄러가 단일 스레드라면 등록 순서로 실행되지만, 멀티스레드 스케줄러 설정 시 순서가 보장되지 않는다.

**권고:** 두 로직을 하나의 스케줄 메서드로 통합하거나, `createTodayVisitorCountAtMidnight`를 `0 1 0 * * ?` (자정 1분 후)로 변경하여 순서를 명시적으로 보장한다.

---

### 🟠 경고 (Warning)

#### W-1. JPQL 명명 파라미터 구문 오류

**위치:** `VisitorCountCustomRepositoryImpl.java:25`

```java
em.createQuery("select v from VisitorCount v where v.date =: date", VisitorCount.class)
//                                                           ^^
//                                                    콜론과 파라미터명 사이 공백
```

JPQL 명명 파라미터는 `:paramName` (공백 없음)이 표준이다. `: date`는 일부 JPA 구현체에서 파싱 오류를 일으킬 수 있다. Hibernate에서는 우연히 동작할 수 있으나 스펙을 벗어난 코드이다.

**권고:** `v.date = :date`로 수정한다. (등호와 콜론 사이 공백, 콜론과 파라미터명은 공백 없음)

---

#### W-2. 센티넬 값(-1L) 안티패턴

**위치:** `VisitorCountRedisRepositoryImpl.java:36`, `VisitorCountServiceImpl.java:42`

```java
// Repository: 키 미존재 시 -1L로 채운 DTO 반환
return findByDate(keyDate).orElseGet(
    () -> VisitorCountDto.builder().total(-1L).today(-1L).yesterday(-1L).build()
);

// Service: -1L 여부로 존재 확인
if (visitorCountDto == null || visitorCountDto.getTotal() < 0) { ... }
```

`-1L`을 "데이터 없음" 신호로 사용하는 방식은 코드 의미를 불명확하게 만들고 버그를 유발할 수 있다. 이미 `Optional<VisitorCountDto>`를 반환하는 `findByDate()`가 존재하므로 일관성도 없다.

**권고:** `increment()` 반환 타입을 `Optional<VisitorCountDto>`로 변경하여 명시적으로 표현한다.

---

#### W-3. `fixedRate` 대신 `fixedDelay` 권장

**위치:** `VisitorCountScheduledTask.java:61`

```java
@Scheduled(fixedRate = 600000)  // 10분 간격, 이전 실행과 무관하게 시작
public void updateVisitorCountEveryTenMinutes() { ... }
```

`fixedRate`는 이전 실행이 완료되지 않아도 다음 실행을 스케줄한다. DB 동기화 작업이 10분을 초과하면 실행이 중첩될 수 있다. `ReentrantLock.tryLock()`으로 보호하고 있지만, `fixedDelay`를 사용하면 이 문제를 구조적으로 방지할 수 있다.

**권고:**
```java
@Scheduled(fixedDelay = 600000, initialDelay = 600000)
```

---

#### W-4. `VisitorCountDto.increment()` 미사용 메서드

**위치:** `VisitorCountDto.java:33`

```java
public void increment() {
    this.total++;
    this.today++;
}
```

실제 카운팅은 Redis `HINCRBY`로 직접 처리하므로 이 메서드는 사용되지 않는다. 미사용 코드는 유지보수 혼란을 야기한다.

**권고:** 사용하지 않는다면 제거한다.

---

#### W-5. Redis 증가 시 TTL 미갱신

**위치:** `VisitorCountRedisRepositoryImpl.java:31`

```java
public VisitorCountDto increment(String keyDate) {
    String key = getKey(keyDate);
    redisTemplate.opsForHash().increment(key, TOTAL_COUNT_KEY, 1L);
    redisTemplate.opsForHash().increment(key, TODAY_COUNT_KEY, 1L);
    // TTL 갱신 없음
    return findByDate(keyDate).orElseGet(...);
}
```

`increment()` 호출 시 TTL을 갱신하지 않는다. 키가 7일 TTL의 만료 직전에 있다면, 증가 작업 이후에도 TTL이 연장되지 않아 조만간 데이터가 유실될 수 있다. 실제로는 오늘 날짜 키가 7일 내에 만료될 일이 거의 없지만, 방어적 코드 관점에서 개선이 필요하다.

**권고:** `increment()` 내에서 `redisTemplate.expire(key, 7, TimeUnit.DAYS)`를 호출한다.

---

#### W-6. `@Service` 어노테이션을 스케줄러에 사용

**위치:** `VisitorCountScheduledTask.java:16`

```java
@Service  // ← 스케줄러 클래스에 부적절
public class VisitorCountScheduledTask { ... }
```

`@Service`는 비즈니스 로직을 담은 서비스 레이어 컴포넌트에 사용하는 의미론적 어노테이션이다. 스케줄러는 `@Component`가 적합하다.

**권고:** `@Component`로 변경한다.

---

### 🟡 개선 권고 (Suggestion)

#### S-1. 방문자 카운팅 범위가 2개 경로로 한정됨

**위치:** `WebConfig.java:17`

```java
registry.addInterceptor(userNumCookieInterceptor)
    .addPathPatterns("/api/v2/visitor-count")
    .addPathPatterns("/api/v7/boards/**");
    // 다른 API 경로 (댓글, 카테고리, 검색 등)는 미적용
```

사용자가 게시글 목록 외 다른 기능만 사용할 경우 방문으로 집계되지 않는다. 의도된 설계라면 주석으로 명시할 필요가 있다.

**권고:** 모든 API에 방문자를 집계하려면 인터셉터 경로를 `/api/**`로 확장하고 중복 카운팅을 Redis Set으로 방지한다. 현재 설계를 유지한다면 코드 주석으로 의도를 명시한다.

---

#### S-2. `user_n` 쿠키에 보안 속성 누락

**위치:** `UserNumCookieInterceptor.java:47`

```java
private static Cookie createUserNumCookie(RandomUserNumberDto userNumberDto) {
    return CookieUtil.addCookie(USER_NUM_COOKIE, String.valueOf(userNumberDto.getNumber()));
}
```

`CookieUtil.addCookie` 구현에 따라 다르지만, 쿠키에 `HttpOnly`, `SameSite`, `Secure` 속성이 빠져 있을 가능성이 있다. `user_n` 쿠키 값이 탈취되면 방문자 집계를 우회할 수 있다.

**권고:** 쿠키 생성 시 다음 속성을 명시적으로 설정한다.
```java
cookie.setHttpOnly(true);
cookie.setSecure(true);  // HTTPS 환경
cookie.setAttribute("SameSite", "Lax");
```

---

#### S-3. API 버전 번호 비일관성

| 엔드포인트 | 버전 |
|---|---|
| `GET /api/v2/visitor-count` | v2 |
| `GET /api/v7/boards/**` | v7 |

버전 번호가 기능별로 크게 다르다. 이는 일관된 API 진화 전략 없이 버전이 부여됐음을 나타낸다. 외부 클라이언트와의 계약(Contract)이 명확하지 않으면 향후 유지보수가 어렵다.

**권고:** 전체 API의 버전 전략을 문서화하고, 신규 API는 단일 기준(예: `v1`)으로 통일한다.

---

#### S-4. `retrieveAndSaveRecentVisitorCount`의 날짜 불일치 가능성

**위치:** `VisitorCountServiceImpl.java:113`

```java
private VisitorCountDto retrieveAndSaveRecentVisitorCount() {
    VisitorCount visitorCount = retrieveRecentVisitorCount();
    // ...
    visitorCountRedisRepository.save(getToday(), visitorCountDto);  // ← 오늘 날짜 키로 저장
    return visitorCountDto;
}
```

`getVisitorCount(date)`를 호출할 때 `date` 파라미터를 사용하지 않고 `getToday()`로 강제 저장한다. 만약 날짜가 달라지는 엣지 케이스(자정 전후)에 다른 날짜로 조회가 들어오면 오늘 키에 잘못된 데이터가 저장될 수 있다.

**권고:** `save(getToday(), ...)` 대신 `save(date, ...)`를 사용하여 조회 날짜와 저장 키를 일치시킨다.

---

#### S-5. Redis TTL 7일은 과도하게 길다

**위치:** `VisitorCountRedisRepositoryImpl.java:27`

```java
redisTemplate.expire(key, 7, TimeUnit.DAYS);
```

방문자 카운터에서 실질적으로 필요한 키는 오늘과 어제 두 가지다. 7일치 데이터를 Redis에 유지하는 것은 메모리 낭비이며, 스케줄러가 DB에 주기적으로 동기화하므로 Redis는 단기 캐시 역할만 하면 충분하다.

**권고:** TTL을 2~3일로 줄인다.

---

## 3. 종합 평가

| 구분 | 내용 |
|---|---|
| **전체 설계 방향** | Redis 캐시 + DB 영속화 2계층 구조는 적절하다 |
| **동시성 처리** | `synchronized`/`ReentrantLock` 사용은 단일 인스턴스 환경에서만 유효, 분산 환경 미지원 |
| **트랜잭션 관리** | 자기 호출로 인한 `@Transactional` 무효화가 핵심 결함 |
| **코드 품질** | 미사용 메서드, 센티넬 값(-1L), 비표준 JPQL 등 소규모 문제 다수 |
| **운영 안정성** | 자정 스케줄 순서 미보장이 데이터 정합성 위험 요소 |

---

## 4. 우선순위별 수정 권고

| 우선순위 | ID | 제목 | 난이도 |
|---|---|---|---|
| 1순위 | C-1 | `@Transactional` 자기 호출 무효화 수정 | 중 |
| 2순위 | C-3 | 자정 스케줄 순서 보장 | 하 |
| 3순위 | W-1 | JPQL `: date` 공백 제거 | 하 |
| 4순위 | C-2 | 스케줄러 `@Transactional` + Lock 분리 | 중 |
| 5순위 | W-2 | 센티넬 값 → `Optional` 교체 | 중 |
| 6순위 | W-4 | 미사용 `increment()` 메서드 제거 | 하 |
| 7순위 | S-4 | `retrieveAndSaveRecentVisitorCount` 날짜 키 수정 | 하 |
| 8순위 | W-3 | `fixedRate` → `fixedDelay` 변경 | 하 |
| 9순위 | W-6 | `@Service` → `@Component` 변경 | 하 |
| 10순위 | S-2 | 쿠키 보안 속성 추가 | 하 |
