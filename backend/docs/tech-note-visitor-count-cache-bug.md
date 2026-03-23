# 서버 재시작 시 Redis Cold Cache 미처리로 인한 방문자 조회수 초기화 버그 수정

> 2026-03-16

---

## 배경

myblog-boot 프로젝트는 방문자 수를 **Redis(캐시)와 DB(영구 저장)**의 이중 구조로 관리한다.
Redis에 당일 방문자 수(`today`, `total`, `yesterday`)를 Hash로 캐싱하고,
10분 주기 스케줄러가 Redis 값을 DB에 동기화하는 구조다.

이 구조에서 **서버 재시작 등으로 Redis가 비어있을 때(Cold Cache) 방문자 수가 0부터 다시 시작**되고,
이후 스케줄러가 그 잘못된 값을 DB에 덮어쓰는 데이터 유실 버그가 있었다.

---

## 설계 의도 vs 실제 동작

### 설계 의도

```
신규 방문자 → increment(date)
    → Redis 키 있음 → HINCRBY 실행 (정상 카운트)
    → Redis 키 없음 → Optional.empty() 반환
                      → DB에서 기존 데이터 로드 후 Redis에 적재
                      → increment 재실행
```

서비스 레이어에도 이 의도가 코드로 명확히 표현되어 있었다.

```java
// VisitorCountServiceImpl.java
public VisitorCountDto incrementVisitorCount(String date) {
    return visitorCountRedisRepository.increment(date).orElseGet(() -> {
        retrieveAndSaveVisitorCount(date);   // DB 복구
        return visitorCountRedisRepository.increment(date)
                .orElseThrow(() -> new IllegalStateException("방문자 수 증가 실패"));
    });
}
```

`orElseGet` 블록이 실행되어야 DB 복구가 이루어지는 구조다.

### 실제 동작

문제는 `increment()` 내부에서 호출하는 Redis 명령어에 있었다.

```java
// VisitorCountRedisRepositoryImpl.java (버그 코드)
public Optional<VisitorCountDto> increment(String keyDate) {
    String key = getKey(keyDate);
    redisTemplate.opsForHash().increment(key, TOTAL_COUNT_KEY, 1L);  // ①
    redisTemplate.opsForHash().increment(key, TODAY_COUNT_KEY, 1L);  // ②
    redisTemplate.expire(key, 7, TimeUnit.DAYS);
    return findByDate(keyDate);  // ③
}
```

`HashOperations.increment()`는 내부적으로 Redis `HINCRBY` 명령을 사용한다.
Spring Data Redis 공식 문서(context7)에 따르면 `HINCRBY`는:

> **존재하지 않는 키/필드에 대해 0으로 초기화한 뒤 증가시킨다.**

즉 ①에서 키가 없어도 Redis가 `{total: 1}` Hash를 자동으로 생성하고,
②에서 `{total: 1, today: 1}`이 된다.

③의 `findByDate()`는 내부에서 `HGETALL`을 호출한다.
마찬가지로 공식 문서에 따르면 `HGETALL`은:

> **존재하지 않는 키에 대해 빈 Map을 반환한다. (null 아님)**

그런데 이 시점에는 ①②가 이미 키를 생성해뒀으므로 `entries()` 결과는 비어있지 않다.
따라서 `findByDate()`는 항상 `Optional.of({total:1, today:1, yesterday:0})`을 반환한다.

결과적으로 `increment()`는 Cold Cache 상황에서도 절대 `Optional.empty()`를 반환하지 않으며,
서비스 레이어의 `orElseGet` 블록은 **영원히 실행되지 않는 죽은 코드**가 된다.

---

## 어떤 피해가 발생하는가

서버 재시작 시나리오를 예시로 살펴보자.

```
[재시작 전 DB 상태]
  date: 2026-03-16, totalVisitors: 15420, todayVisitors: 87

[서버 재시작 → Redis 플러시]

[첫 방문자 요청]
  increment("2026-03-16")
  → HINCRBY total 1  → {total: 1}  (키 없어도 자동 생성)
  → HINCRBY today 1  → {total: 1, today: 1}
  → findByDate() → {total:1, today:1, yesterday:0}
  → orElseGet 미실행 (DB 복구 없음)

[10분 후 스케줄러: syncVisitorCountToDb 실행]
  → getVisitorCount() → Redis에서 {total:1, today:1} 조회
  → visitorCountRepository.save({total:1, today:1})

[최종 DB 상태]
  date: 2026-03-16, totalVisitors: 1, todayVisitors: 1
  → 15,419건 누적 방문자 데이터 영구 유실
```

스케줄러가 잘못된 Redis 값을 DB에 덮어씀으로써 복구 불가능한 상태가 된다.

---

## 수정 방법

`increment()` 메서드에서 `HINCRBY`를 호출하기 전에 먼저 키 존재 여부를 확인하면 된다.

`RedisTemplate.hasKey(key)`는 내부적으로 Redis `EXISTS` 명령을 사용하며,
존재하지 않는 키에 대해 `Boolean.FALSE`를 반환한다.

단, 공식 문서에 따르면 반환 타입이 `Boolean`(박싱 타입)이므로
파이프라인/트랜잭션 컨텍스트에서 이론적으로 `null`이 될 수 있다.
일반 동기 호출에서는 발생하지 않지만, `Boolean.FALSE.equals()`를 사용하면
null-safe하게 처리할 수 있다.

```java
// VisitorCountRedisRepositoryImpl.java (수정 코드)
public Optional<VisitorCountDto> increment(String keyDate) {
    String key = getKey(keyDate);

    if (Boolean.FALSE.equals(redisTemplate.hasKey(key))) {
        return Optional.empty();  // Cold Cache → 서비스 레이어의 DB 복구 블록으로 위임
    }

    redisTemplate.opsForHash().increment(key, TOTAL_COUNT_KEY, 1L);
    redisTemplate.opsForHash().increment(key, TODAY_COUNT_KEY, 1L);
    redisTemplate.expire(key, 7, TimeUnit.DAYS);
    return findByDate(keyDate);
}
```

이 수정으로 `hasKey()`가 `false`를 반환하면 `Optional.empty()`가 반환되고,
서비스 레이어의 `orElseGet` 블록이 활성화되어 DB에서 기존 데이터를 로드한 뒤
Redis에 적재하고 다시 increment를 수행한다.

```
[수정 후: 서버 재시작 시나리오]

첫 방문자 요청
  → increment("2026-03-16")
      → hasKey("visitorCount:2026-03-16") → false
      → return Optional.empty()           ← 이제 정상 신호
  → orElseGet() 실행                      ← 활성화됨
      → DB에서 {today:87, total:15420} 로드
      → Redis에 {today:87, total:15420, yesterday:X} 적재
      → increment("2026-03-16") 재실행
          → hasKey() → true
          → HINCRBY total → 15421
          → HINCRBY today → 88
  → 최종: {total:15421, today:88}         ← DB 데이터 보존
```

서비스 레이어는 변경하지 않았다. 처음부터 올바르게 설계되어 있었고,
Repository 단 1개 메서드의 수정만으로 죽어있던 복구 코드가 되살아났다.

---

## 경쟁 조건(Race Condition) 검토

`hasKey()` 와 `HINCRBY` 사이에는 짧은 TOCTOU(Time Of Check To Time Of Use) 구간이 존재한다.
두 스레드가 동시에 `hasKey() → false`를 확인하고 둘 다 `Optional.empty()`를 반환할 수 있다.

이 경우 두 스레드가 모두 `retrieveAndSaveVisitorCount()`를 호출하게 되는데,
내부에서 호출하는 `retrieveVisitorCountDto()`에 `synchronized` 키워드가 있어 동시 DB 읽기가 직렬화되어 있다.
두 번째 스레드가 호출할 때는 첫 번째 스레드가 이미 Redis에 적재를 마쳤으므로
같은 DB 값을 두 번 쓰는 것에 그친다 — 멱등성이 보장된다.

이후 각자의 `increment()` 재실행에서 둘 다 `hasKey() → true`를 확인하고
각각 +1씩 증가한다. 결과적으로 동시 방문자 2명이 2번 카운트되므로 정확하다.

방문자 수는 금융 거래가 아니므로 이 수준의 허용 범위는 실용적으로 문제없다.
완전한 원자성이 필요하다면 Lua 스크립트로 `EXISTS + HINCRBY`를 단일 명령으로 묶을 수 있지만,
현재 요구사항에서는 과잉 설계다.

---

## 검증

수정의 정확성을 검증하기 위해 두 가지 테스트를 작성했다.

**`VisitorCountRedisRepositoryImplTest` — Repository 단위 동작 검증**

기존에 Repository 구현체에 대한 테스트가 없었다.
Cold/Warm Cache 각 경우의 `increment()` 동작과 `save/findByDate` 기본 동작을 검증한다.

```java
@Test
void increment_ColdCache_returnsEmpty() {
    redisTemplate.delete(REDIS_KEY);  // Cold 상태 보장
    Optional<VisitorCountDto> result = visitorCountRedisRepository.increment(TEST_DATE);
    assertThat(result).isEmpty();
    assertThat(redisTemplate.hasKey(REDIS_KEY)).isFalse();  // 키가 생성되지 않아야 함
}

@Test
void increment_WarmCache_incrementsCorrectly() {
    visitorCountRedisRepository.save(TEST_DATE, VisitorCountDto.builder()
            .total(100L).today(10L).yesterday(8L).build());
    Optional<VisitorCountDto> result = visitorCountRedisRepository.increment(TEST_DATE);
    assertThat(result.get().getTotal()).isEqualTo(101L);
    assertThat(result.get().getYesterday()).isEqualTo(8L);  // yesterday는 변경 없어야 함
}
```

**`VisitorCountServiceImplTest` — Cold Cache DB 복구 시나리오 검증**

DB에 누적 방문자 수(total: 1050, today: 20)를 저장하고 Redis를 비운 뒤
`increment`를 호출했을 때 1051이 반환되는지 확인한다.

```java
@Test
void incrementVisitorCount_ColdCache_loadsFromDb() {
    // DB에 1050 저장, Redis cold
    VisitorCountDto result = increment(today);
    assertThat(result.getTotal()).isEqualTo(1051L);  // 0+1이 아닌 1050+1
}
```

**최종 테스트 결과**: 104개 전체 통과 (기존 99개 + 신규 5개)

---

## 정리

| 항목 | 내용 |
|------|------|
| 버그 위치 | `VisitorCountRedisRepositoryImpl.increment()` |
| 원인 | Redis `HINCRBY`의 auto-create 동작 — Cold Cache에서도 키를 자동 생성하여 `Optional.empty()` 미반환 |
| 결과 | 서비스 레이어의 DB 복구 블록(orElseGet)이 죽은 코드가 됨 → 서버 재시작 시 데이터 유실 |
| 수정 | `hasKey()` 선검사 추가 — Cold Cache 시 `Optional.empty()` 반환으로 서비스 복구 블록 활성화 |
| 변경 파일 | `VisitorCountRedisRepositoryImpl.java` 1개 |
| 신규 테스트 | `VisitorCountRedisRepositoryImplTest` 4개, `VisitorCountServiceImplTest` 1개 |
