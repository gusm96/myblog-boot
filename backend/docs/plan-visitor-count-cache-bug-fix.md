# 방문자 수 Redis Cache-Aside 버그 수정 계획서

> 작성일: 2026-03-16
> 참조: `docs/visitor-count-api-review.md`, context7 Spring Data Redis 공식 문서

---

## 1. 버그 요약

`VisitorCountRedisRepositoryImpl.increment()` 가 Redis 캐시가 비어 있을 때(Cold Cache)
DB에서 기존 데이터를 복구하지 않고 **0부터 카운트를 시작**하는 심각한 데이터 유실 버그가 존재합니다.

### 영향 범위

| 상황 | 결과 |
|------|------|
| 서버 재시작 | Redis 플러시 → 첫 방문자가 누적 total / today 를 0→1 로 덮어씀 |
| Redis 장애 복구 | 동일 |
| 새벽 자정 후 신규 날짜 첫 방문자 | yesterday 가 0 으로 초기화됨 |
| 10분 뒤 `syncVisitorCountToDb` 실행 | **DB의 실제 누적 데이터를 잘못된 값(1 등)으로 덮어씀 → 영구 유실** |

---

## 2. 버그 원인 상세 분석

### 2-1. Redis `HINCRBY` 의 auto-create 동작 (context7 검증)

context7 Spring Data Redis 공식 문서에 따르면:

> `HashOperations.increment()` 는 내부적으로 Redis `HINCRBY` 명령을 사용하며,
> **존재하지 않는 키/필드에 대해 0으로 초기화한 뒤 증가시킨다. 오류 없이 자동 생성.**

```
HINCRBY visitorCount:2026-03-16 total 1
→ 키가 없어도 → {total: 1} 로 자동 생성
```

`entries()` (`HGETALL`) 는 존재하지 않는 키에 대해 **빈 Map** 을 반환한다 (null 아님).
따라서 `increment()` 실행 후 `findByDate()` 는 **항상 non-empty** 를 반환한다.

### 2-2. 현재 코드 흐름 (버그 경로)

```
신규 방문자 요청
  → incrementVisitorCount("2026-03-16")
      → visitorCountRedisRepository.increment("2026-03-16")
          → HINCRBY total +1   ← ❌ 키 없어도 {total:1} 자동 생성
          → HINCRBY today +1   ← ❌ {today:1} 자동 생성
          → expire 7days
          → findByDate() → {total:1, today:1, yesterday:0}  ← 항상 non-empty
          → return Optional.of(...)
      → orElseGet() 블록 ← ❌ 절대 실행되지 않음 (dead code)
          (DB 복구 로직 → 영원히 건너뜀)
  → 결과: {total:1, today:1, yesterday:0}  ← DB의 실제 값 무시
```

### 2-3. 올바른 의도 vs 실제 동작

```java
// VisitorCountServiceImpl.java:38 — 서비스 레이어의 의도는 올바름
public VisitorCountDto incrementVisitorCount(String date) {
    return visitorCountRedisRepository.increment(date).orElseGet(() -> {
        // ← 이 복구 로직이 실행되어야 하지만, 절대 실행되지 않음
        retrieveAndSaveVisitorCount(date);
        return visitorCountRedisRepository.increment(date).orElseThrow();
    });
}
```

`orElseGet` 블록은 `increment()` 가 `Optional.empty()` 를 반환할 때만 실행되는데,
현재 `increment()` 는 Cold Cache 상황에서도 `Optional.of(...)` 를 반환하므로
**복구 블록은 죽은 코드(dead code)** 입니다.

### 2-4. 데이터 유실 시나리오 예시

```
[서버 재시작 전 DB 상태]
  date: 2026-03-16, totalVisitors: 15420, todayVisitors: 87

[재시작 → Redis 플러시]

[첫 방문자 요청]
  increment() → {total:1, today:1, yesterday:0}  ← 실제 DB 값 무시

[10분 후 syncVisitorCountToDb 실행]
  getVisitorCount() → Redis 에서 {total:1, today:1} 가져옴
  visitorCountRepository.save({total:1, today:1})  ← ❌ DB 영구 덮어씀

[최종 DB 상태]
  date: 2026-03-16, totalVisitors: 1, todayVisitors: 1  ← 15419 방문자 유실
```

---

## 3. `getVisitorCount` 경로 비교 — 정상 동작 확인

```java
// VisitorCountServiceImpl.java:29 — READ 경로는 정상
public VisitorCountDto getVisitorCount(String date) {
    return visitorCountRedisRepository.findByDate(date).orElseGet(
        () -> retrieveAndSaveRecentVisitorCount()  // ← Cache Miss 시 정상 복구
    );
}
```

`findByDate()` 는 `entries()` 의 `isEmpty()` 로 Cache Miss 를 감지하며,
HINCRBY 를 호출하지 않으므로 키를 자동 생성하지 않습니다. **이 경로는 버그 없음.**

---

## 4. 수정 방안 비교

### Option A — `hasKey()` 선검사 후 increment (채택)

```java
// VisitorCountRedisRepositoryImpl.increment()
public Optional<VisitorCountDto> increment(String keyDate) {
    String key = getKey(keyDate);

    if (Boolean.FALSE.equals(redisTemplate.hasKey(key))) {
        return Optional.empty();  // Cold Cache 신호 — 서비스 레이어가 DB 복구 처리
    }

    redisTemplate.opsForHash().increment(key, TOTAL_COUNT_KEY, 1L);
    redisTemplate.opsForHash().increment(key, TODAY_COUNT_KEY, 1L);
    redisTemplate.expire(key, 7, TimeUnit.DAYS);
    return findByDate(keyDate);
}
```

**장점**: 변경 범위 최소 (Repository 1개 메서드만 수정), 서비스 코드 변경 불필요
**단점**: `hasKey()` 와 `increment()` 사이의 TOCTOU(Time Of Check To Time Of Use) 경쟁 조건 이론적 존재

> **TOCTOU 위험도 평가**: `retrieveVisitorCountDto()` 에 이미 `synchronized` 가 걸려 있어
> 동시 DB 읽기는 직렬화됨. 극히 짧은 경쟁 창(수 μs)에서 발생하는 중복 카운트는
> 방문자 수 특성상 허용 범위 내이며 금융 데이터가 아니므로 이 옵션을 채택.

---

### Option B — `returnValue == 1L` 감지 후 롤백 (미채택)

```java
Long newTotal = (Long) redisTemplate.opsForHash().increment(key, TOTAL_COUNT_KEY, 1L);
if (newTotal == 1L) {          // Cold Cache: 0→1 이 된 것 = 첫 번째 write
    redisTemplate.delete(key); // 잘못 생성된 키 제거
    return Optional.empty();   // DB 복구 위임
}
```

context7: "Redis initializes missing field to 0 before incrementing — getting 1L back means the field was absent."

**장점**: 원자적 감지 (EXISTS + HINCRBY 를 단일 명령으로 대체)
**단점**: `TOTAL_COUNT_KEY` 만 increment 후 `delete` → `TODAY_COUNT_KEY` 미처리 상태에서 삭제.
두 번째 `increment()` (DB 복구 후) 가 정상 실행되므로 결과적으로 맞지만 로직이 불명확.

---

### Option C — Lua 스크립트 완전 원자화 (미채택)

```lua
if redis.call('EXISTS', KEYS[1]) == 0 then
    return -1
else
    local t = redis.call('HINCRBY', KEYS[1], ARGV[1], 1)
    redis.call('HINCRBY', KEYS[1], ARGV[2], 1)
    redis.call('EXPIRE', KEYS[1], 604800)
    return t
end
```

**장점**: 완전한 원자성
**단점**: Lua 스크립트 관리 복잡도 증가, 현재 TOCTOU 위험이 허용 범위이므로 과잉

---

## 5. 수정 범위 (Option A 기준)

### 5-1. 수정 파일 (1개)

| 파일 | 수정 내용 |
|------|-----------|
| `repository/implementation/VisitorCountRedisRepositoryImpl.java` | `increment()` 메서드 — `hasKey()` 선검사 추가 |

**서비스 / 인터페이스 레이어 수정 불필요** — 기존 `orElseGet` 복구 블록이 그대로 활성화됨.

### 5-2. 수정 후 정상 흐름

```
신규 방문자 요청 (Cold Cache)
  → incrementVisitorCount("2026-03-16")
      → increment("2026-03-16")
          → hasKey("visitorCount:2026-03-16") → false
          → return Optional.empty()          ← ✅ Cache Miss 정상 신호
      → orElseGet() 블록 실행               ← ✅ 이제 실행됨
          → retrieveAndSaveVisitorCount("2026-03-16")
              → DB에서 {todayVisitors:87, totalVisitors:15420} 로드
              → DB에서 어제 데이터 로드 → yesterday 계산
              → Redis에 save({total:15420, today:87, yesterday:X})
          → increment("2026-03-16") (2차)
              → hasKey() → true             ← ✅ 이제 정상 경로
              → HINCRBY total +1 → 15421
              → HINCRBY today +1 → 88
              → return Optional.of({total:15421, today:88, yesterday:X})
  → 결과: DB 데이터 보존, 정확한 카운트
```

---

## 6. 신규 테스트 계획

### 6-1. `VisitorCountRedisRepositoryImplTest` (신규)

현재 `VisitorCountRedisRepositoryImpl` 에 대한 단위 테스트가 없습니다.
`AbstractContainerBaseTest` 기반 통합 테스트로 작성합니다.

| 테스트 케이스 | 검증 내용 |
|---------------|-----------|
| `increment_ColdCache_returnsEmpty` | 키 없을 때 `increment()` → `Optional.empty()` 반환 확인 |
| `increment_WarmCache_returnsIncremented` | 키 있을 때 `increment()` → `today+1`, `total+1` 확인 |
| `save_thenFindByDate` | `save()` → `findByDate()` 정상 조회 확인 |
| `findByDate_noKey_returnsEmpty` | 키 없을 때 `findByDate()` → `Optional.empty()` 확인 |

### 6-2. `VisitorCountServiceImplTest` 기존 테스트 추가

| 테스트 케이스 | 검증 내용 |
|---------------|-----------|
| `incrementVisitorCount_ColdCache_loadsFromDb` | Redis cold → DB 값으로 복구 후 increment 확인 |

---

## 7. 작업 순서

```
Step 1 — VisitorCountRedisRepositoryImpl.increment() 수정
Step 2 — VisitorCountRedisRepositoryImplTest 신규 작성 (Cold/Warm Cache 케이스)
Step 3 — VisitorCountServiceImplTest 에 Cold Cache 복구 케이스 추가
Step 4 — 전체 테스트 실행 확인
```

---

## 8. 보충 검토: 관련 메서드 이상 없음

| 메서드 | 판정 | 비고 |
|--------|------|------|
| `getVisitorCount()` | ✅ 정상 | `findByDate().isEmpty()` → Cache Miss 정확히 감지 |
| `retrieveAndSaveVisitorCount()` | ✅ 정상 | DB 로드 후 `save()` → `putAll()` + `expire()` |
| `retrieveVisitorCountDto()` | ✅ 정상 | `synchronized` 로 동시 DB 읽기 직렬화 |
| `syncVisitorCountToDb()` | ✅ 정상 (수정 후) | 버그 수정 시 올바른 Redis 값을 DB 에 쓰게 됨 |
| `createTodayVisitorCount()` | ✅ 정상 | DB에만 레코드 생성, Redis 는 첫 방문 시 lazy 로드 |
| `save()` (Redis) | ✅ 정상 | `putAll()` + `expire(7days)` |
| `findByDate()` (Redis) | ✅ 정상 | `entries().isEmpty()` 로 Cache Miss 올바르게 감지 |
| `increment()` (Redis) | ❌ **버그** | 본 계획서 수정 대상 |

---

## 9. 수정 전/후 `increment()` 비교

```java
// ===== Before (버그) =====
public Optional<VisitorCountDto> increment(String keyDate) {
    String key = getKey(keyDate);
    redisTemplate.opsForHash().increment(key, TOTAL_COUNT_KEY, 1L);  // ❌ 키 없어도 자동 생성
    redisTemplate.opsForHash().increment(key, TODAY_COUNT_KEY, 1L);  // ❌ 키 없어도 자동 생성
    redisTemplate.expire(key, 7, TimeUnit.DAYS);
    return findByDate(keyDate);  // 항상 non-empty → orElseGet 절대 미실행
}

// ===== After (수정) =====
public Optional<VisitorCountDto> increment(String keyDate) {
    String key = getKey(keyDate);

    if (Boolean.FALSE.equals(redisTemplate.hasKey(key))) {  // ✅ Cold Cache 선검사
        return Optional.empty();  // ✅ 서비스 레이어의 DB 복구 블록 활성화
    }

    redisTemplate.opsForHash().increment(key, TOTAL_COUNT_KEY, 1L);
    redisTemplate.opsForHash().increment(key, TODAY_COUNT_KEY, 1L);
    redisTemplate.expire(key, 7, TimeUnit.DAYS);
    return findByDate(keyDate);
}
```
