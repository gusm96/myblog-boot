# RFC-BE-005: 조회수 업데이트 스케줄러 안정성 개선

> 작업일: 2026-02-25
> 상태: 완료 (1단계)
> 대상 파일: `BoardScheduledTask.java`

---

## 배경

`BoardScheduledTask.updateFromRedisStoreToDB()`는 10분마다 Redis 캐시에 누적된 게시글 조회수/좋아요수를 DB에 동기화하는 스케줄러이다.

기존 코드에는 다음과 같은 문제가 있었다:

| 문제 | 설명 | 위험도 |
|------|------|--------|
| 동시 실행 보호 없음 | `VisitorCountScheduledTask`와 달리 Lock 없이 동작 | 중 |
| 전체 실패 전파 | 하나의 게시글 동기화 실패 시 `RuntimeException`을 던져 전체 작업 중단 | 상 |
| 에러 로깅 부족 | 어떤 게시글에서 실패했는지 식별 불가 | 중 |
| 캐시 삭제 실패 미처리 | `deleteFromCache`에서 예외 발생 시 상위로 전파 | 중 |

---

## 변경 내용

### 1. ReentrantLock 도입

```java
private final Lock lock = new ReentrantLock();

public void updateFromRedisStoreToDB() {
    if (!lock.tryLock()) {
        log.debug("게시글 캐시 동기화 스킵: 이전 작업 진행 중");
        return;
    }
    try {
        // 동기화 로직
    } finally {
        lock.unlock();
    }
}
```

- `VisitorCountScheduledTask`와 동일한 패턴 적용
- `tryLock()` 사용으로 논블로킹 — 이전 작업이 진행 중이면 스킵
- 자정 작업(`deleteExpiredBoards`)과의 동시 실행 방지

### 2. 개별 게시글 단위 에러 핸들링

```java
for (Long boardId : keys) {
    try {
        // 개별 게시글 동기화
        successCount++;
    } catch (Exception e) {
        failCount++;
        log.error("게시글 캐시 동기화 실패 [boardId={}]: {}", boardId, e.getMessage());
    }
}
```

- **변경 전**: 하나의 게시글 실패 → 전체 `RuntimeException` → 나머지 게시글 조회수 유실
- **변경 후**: 하나의 게시글 실패 → 해당 게시글만 건너뜀 → 나머지 정상 처리

### 3. 캐시 삭제 실패 시 안전 처리

```java
private void deleteFromCache(Long boardId, BoardForRedis boardForRedis) {
    try {
        boardRedisRepository.delete(boardForRedis);
    } catch (Exception e) {
        log.error("캐시 삭제 실패 [boardId={}]: {}", boardId, e.getMessage());
    }
}
```

- DB 업데이트 성공 후 캐시 삭제 실패 시에도 예외 전파하지 않음
- 다음 주기에서 해당 캐시를 다시 처리 (멱등성 보장 — `totalViews()`는 절대값 기반)

### 4. 처리 결과 로깅

```
INFO  - 게시글 캐시 → DB 동기화 완료: 5건 처리
WARN  - 게시글 캐시 → DB 동기화 완료: 성공 4건, 실패 1건
```

- 성공/실패 건수를 구분하여 모니터링 용이하게 개선
- 실패 발생 시 WARN 레벨로 기록

---

## 기타 수정

| 항목 | 변경 전 | 변경 후 |
|------|--------|--------|
| 상수명 오타 | `SECONDS_INT_15DAYS` | `SECONDS_IN_15DAYS` |
| 메서드명 | `updateBoards` (복수형) | `updateBoard` (단수형 — 단일 게시글 처리) |

---

## 테스트

`BoardScheduledTaskTest.java` 신규 작성 (Mockito 기반 단위 테스트)

| 테스트 케이스 | 검증 내용 |
|-------------|----------|
| `updateFromRedisStoreToDB_success` | 캐시 데이터가 있으면 DB 동기화 + 캐시 삭제 |
| `updateFromRedisStoreToDB_emptyKeys` | 캐시가 비어있으면 아무 작업도 하지 않음 |
| `updateFromRedisStoreToDB_partialFailure` | 개별 실패 시 나머지 게시글 계속 처리 |
| `updateFromRedisStoreToDB_nullCache` | null 캐시는 건너뜀 |

---

## 향후 과제 (2단계)

현재 `ReentrantLock`은 **단일 JVM 인스턴스** 내에서만 유효하다.
다중 인스턴스 배포 시 아래 방안을 검토해야 한다:

| 방안 | 장점 | 단점 | 의존성 |
|------|------|------|--------|
| **ShedLock** | DB 기반 분산 락, 설정 간단 | 테이블 추가 필요 | `shedlock-spring` + `shedlock-provider-jdbc-template` |
| **Redisson** | Redis 기반 분산 락, 이미 Redis 사용 중 | Lettuce와 별개 클라이언트 | `redisson-spring-boot-starter` |
| **Redis SET NX** | 추가 의존성 없음 | 직접 구현 필요, 타임아웃 관리 | 없음 (현재 Lettuce 활용) |

**권장**: 현재 단일 인스턴스 운영이므로 ReentrantLock으로 충분하며,
다중 인스턴스 확장 시점에 ShedLock 또는 Redis SET NX 방식 도입을 권장한다.
