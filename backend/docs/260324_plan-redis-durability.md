# 계획서: Redis 데이터 유실 방지 — Graceful Shutdown + AOF 영속성

> 작성일: 2026-03-24
> 브랜치: develop
> 대상 파일:
> - `backend/src/main/java/.../scheduler/VisitorCountScheduledTask.java`
> - `docker-compose.yaml` (루트)
> - `docker-compose.dev.yaml` (루트)

---

## 1. 문제 정의

### 현재 데이터 흐름

```
방문자 요청
    ↓
Redis (Write Buffer)  ←→  10분/자정 스케줄러  →  MariaDB (영구 저장)
```

방문자 수는 Redis에 먼저 누적되고, 스케줄러가 주기적으로 DB에 동기화한다.
스케줄러 주기(10분) 사이에 서버가 종료되면 **최대 10분치 방문자 데이터가 유실**된다.

### 유실 시나리오 두 가지

| 시나리오 | 설명 | 현재 대응 |
|----------|------|-----------|
| 정상 종료 | 배포 재시작, 수동 `docker compose down` | ❌ 없음 |
| 비정상 종료 | OOM Kill, 전원 차단, 컨테이너 강제 중지 | ❌ 없음 |

---

## 2. 해결 전략

두 시나리오를 각각 다른 방법으로 처리한다.

```
정상 종료  →  Graceful Shutdown Hook  →  종료 직전 DB flush
비정상 종료  →  Redis AOF 영속성  →  재시작 시 Redis 데이터 복원
```

두 전략을 조합하면 사실상 완전한 안전망이 된다.
(단, 방문자 수는 비정합 허용 데이터이므로 1초 이내 유실은 허용 가능한 수준)

---

## 3. Phase 1 — Graceful Shutdown Hook

### 3-1. 구현 방향

`VisitorCountScheduledTask`에 `ApplicationListener<ContextClosedEvent>`를 구현한다.

- Spring은 SIGTERM 수신 시 `ContextClosedEvent`를 발행한다.
- 기존 `syncVisitorCountToDb()` 로직을 그대로 재사용하므로 신규 코드 최소화.
- `@PreDestroy`보다 `ContextClosedEvent`가 타이밍이 명확하고 Spring 컨텍스트가 아직 살아 있는 상태에서 실행된다.

### 3-2. 변경 대상

**`VisitorCountScheduledTask.java`**

현재 클래스 선언:
```java
public class VisitorCountScheduledTask {
```

변경 후:
```java
public class VisitorCountScheduledTask implements ApplicationListener<ContextClosedEvent> {
```

추가할 메서드:
```java
@Override
public void onApplicationEvent(ContextClosedEvent event) {
    lock.lock();
    try {
        String today = getToday();
        visitorCountService.syncVisitorCountToDb(today);
        log.info("Shutdown: Redis → DB 동기화 완료 ({})", today);
    } catch (Exception e) {
        log.error("Shutdown: 동기화 실패 — AOF 복원에 의존. 원인: {}", e.getMessage());
    } finally {
        lock.unlock();
    }
}
```

### 3-3. 기존 Lock 활용

기존 `ReentrantLock`을 그대로 사용한다.
종료 직전 10분 스케줄러가 실행 중이더라도 Lock이 해제될 때까지 대기 후 flush한다.
`tryLock()` 대신 `lock()`을 사용해 반드시 실행되도록 보장한다.

### 3-4. 주의사항

- `@Profile("!test")`가 이미 적용되어 있으므로 테스트 환경에서는 실행되지 않는다.
- Docker `SIGTERM` → Spring에 전달되려면 `docker-compose.yaml`의 `stop_grace_period`가 충분해야 한다.

```yaml
# docker-compose.yaml backend 서비스에 추가
stop_grace_period: 30s   # 기본값 10s → 30s로 연장
```

DB flush는 보통 1~2초 이내 완료되므로 30초면 충분하다.

---

## 4. Phase 2 — Redis AOF 영속성 설정

### 4-1. AOF란

AOF(Append Only File): Redis의 모든 쓰기 명령을 파일에 순차 기록한다.
서버 재시작 시 AOF를 재실행해 데이터를 복원한다.

### 4-2. fsync 옵션 비교

| 옵션 | 동작 | 최대 유실 | 성능 영향 |
|------|------|-----------|-----------|
| `always` | 매 쓰기마다 fsync | 0 | 높음 (느림) |
| `everysec` | 1초마다 fsync | 최대 1초 | 낮음 (권장) |
| `no` | OS 판단에 맡김 | 수 초 | 없음 (위험) |

→ **`everysec` 선택**: 성능과 안전성의 균형점.
방문자 수 특성상 1초치 유실은 허용 가능.

### 4-3. 변경 대상

**`docker-compose.yaml`** (운영) — redis 서비스:

```yaml
redis:
  image: redis:7-alpine
  command: redis-server --appendonly yes --appendfsync everysec
  volumes:
    - redis-data:/data   # AOF 파일 영속 저장
  # ... 기존 설정 유지
```

**`docker-compose.dev.yaml`** (개발) — redis 서비스:

```yaml
redis:
  image: redis:7-alpine
  command: redis-server --appendonly yes --appendfsync everysec
  volumes:
    - redis-data:/data
```

개발 환경도 동일하게 적용해 운영과 동작 차이를 없앤다.

### 4-4. 기존 volumes 선언 확인

`docker-compose.yaml` 하단 `volumes:` 블록에 `redis-data`가 선언되어 있는지 확인 후 없으면 추가:

```yaml
volumes:
  redis-data:
```

---

## 5. 검증 계획

### Phase 1 검증 — Graceful Shutdown

1. 서버 기동 후 방문자 API를 몇 차례 호출해 Redis에 카운트 누적
2. 스케줄러 10분 주기 전에 `docker compose stop backend` (SIGTERM 발송)
3. 로그에서 `"Shutdown: Redis → DB 동기화 완료"` 확인
4. DB에서 방문자 수 조회해 Redis 값과 일치 여부 확인

### Phase 2 검증 — AOF 복원

1. Redis에 방문자 카운트 누적
2. `docker compose kill redis` (SIGKILL, 강제 종료)
3. `docker compose start redis`
4. Redis에서 방문자 수 조회 — 종료 전 값이 복원되었는지 확인

---

## 6. 트레이드오프 정리

| 항목 | 내용 |
|------|------|
| AOF 디스크 I/O | `everysec`는 1초마다 fsync → 개인 블로그 수준에서는 무시 가능 |
| AOF 파일 크기 | 시간이 지나면 커질 수 있음 → `redis.conf`의 `auto-aof-rewrite` 기본값으로 자동 압축 |
| Shutdown Hook 실패 | DB 연결 끊김 등으로 flush 실패 시 AOF가 보조 안전망 역할 수행 |
| 완벽한 무결성 여부 | 비정상 종료 + AOF 파일 손상이 동시에 발생하면 유실 가능 — 방문자 수 특성상 허용 범위 |

---

## 7. 구현 순서

1. [ ] `VisitorCountScheduledTask`에 `ApplicationListener<ContextClosedEvent>` 구현
2. [ ] `docker-compose.yaml` redis 서비스에 AOF 옵션 + volumes 추가
3. [ ] `docker-compose.dev.yaml` 동일 적용
4. [ ] `docker-compose.yaml` backend 서비스에 `stop_grace_period: 30s` 추가
5. [ ] Phase 1 로컬 검증 (Graceful Shutdown 로그 확인)
6. [ ] Phase 2 로컬 검증 (AOF 복원 확인)
