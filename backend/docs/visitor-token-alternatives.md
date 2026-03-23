# 방문자 임시 토큰 방식 대안 검토 

> 작성일: 2026-03-15
> 대상: `RandomUserNumberService` + `VisitorCountRedisRepository` 기반 현행 방문자 중복 방지 구조
> 목적: Redis 토큰 저장소 의존 제거 또는 최소화를 위한 대안 방식 분석

---

## 1. 현행 방식 구조 및 문제점

### 1-1. 동작 흐름

```
[클라이언트] 요청
  → UserNumCookieInterceptor
  → user_n 쿠키 존재? → Redis Set 조회 (randomUserNum:)
      ├─ 존재 O → 이미 집계된 방문자, 통과
      └─ 존재 X → 랜덤 Long 생성 → Redis Set 저장 (TTL: 자정까지)
                  → user_n 쿠키 발급 → 방문자 수 +1
```

### 1-2. 현행 방식의 문제 요약

| 문제 | 설명 | 심각도 |
|------|------|--------|
| **모든 요청에 Redis 조회 발생** | `isExists(number)` → 인터셉터 레벨에서 100% 호출 | 높음 |
| **단일 Set 키 공유** | `randomUserNum:` 하나에 전체 방문자 번호 누적 | 중간 |
| **비원자적 저장** | `opsForSet().add()` + `expire()` 두 호출 사이에 경쟁 조건 | 낮음 |
| **Timezone 미지정** | `LocalDateTime.now()` → JVM 기본 TZ 의존, UTC 서버면 자정 불일치 | 높음 |
| **쿠키 MaxAge 없음** | 세션 쿠키라 브라우저 종료 시 즉시 소멸, 설계 의도와 불일치 | 중간 |
| **REST API에 상태 저장소 결합** | Stateless API 원칙 위반, Redis 장애 시 방문자 집계 전체 불가 | 높음 |

### 1-3. REST API 설계 원칙 위반

REST API는 **Stateless** 를 원칙으로 한다. 방문자 중복 방지를 위해 서버 측 Redis에 모든 방문자 번호를 저장하는 것은, 클라이언트 요청 검증을 위해 서버가 외부 상태를 반드시 조회해야 하는 구조를 만든다. 이는:

- 수평 확장(Scale-out) 시 Redis 클러스터 없이는 서버 간 상태 불일치 발생
- Redis 장애 → 모든 방문자가 `isExists()` false 반환 → 방문자 수 폭증
- 개인 블로그 수준의 기능에 Redis 가용성이 필수 조건이 됨

---

## 2. 대안 방식 비교

### 방식 A — HMAC 서명 쿠키 (Stateless) ✅ 추천

쿠키 값 자체에 **날짜 + 서명**을 담아 서버가 Redis 조회 없이 검증한다.
Spring Security의 `TokenBasedRememberMeServices`가 동일한 원리(Hash-Based Token)를 사용한다.

```
쿠키 값 형식: {오늘날짜}:{HMAC-SHA256(오늘날짜, secret)}
예시:         2026-03-15:a3f9c2e1b8d47...
```

**검증 로직:**
```
1. 쿠키에서 date 추출
2. date == 오늘? → 아니면 만료된 토큰 → 재발급
3. HMAC(date, secret) == 쿠키의 서명? → 위변조 여부 확인
4. 두 조건 모두 통과 → 오늘 이미 집계된 방문자
```

**쿠키 수명:** `Max-Age` = 자정까지 남은 초(TTL)로 설정 → 브라우저가 자동 만료

| 항목 | 평가 |
|------|------|
| Redis 조회 | **없음** |
| 구현 난이도 | 낮음 (Java 표준 `javax.crypto.Mac` 사용) |
| 보안 | 서명 위변조 불가, secret 키 관리 필요 |
| REST 원칙 | **완전 Stateless** |
| 장애 영향 | Redis 장애와 무관 |
| 확장성 | 서버 무한 수평 확장 가능 |
| 단점 | secret 키 로테이션 시 기존 쿠키 전체 무효화 |

---

### 방식 B — UUID 쿠키 + MaxAge (서버 저장 없음)

UUID를 쿠키에 발급하고, 쿠키 만료 자체를 자정으로 설정한다. 서버는 중복 여부를 **쿠키 존재 여부**로만 판단하고 저장소를 사용하지 않는다.

```
Set-Cookie: visitor_id=550e8400-e29b-41d4-...; Max-Age=3600; HttpOnly; Secure; SameSite=Lax
```

| 항목 | 평가 |
|------|------|
| Redis 조회 | **없음** |
| 구현 난이도 | 매우 낮음 (`UUID.randomUUID()`) |
| 보안 | **위변조 가능** — 클라이언트가 임의 UUID를 조작해도 서버가 모름 |
| REST 원칙 | Stateless |
| 단점 | 서명 없으므로 조작된 쿠키로 방문자 수 집계 왜곡 가능 |

개인 블로그 수준에서는 실용적이나, 쿠키 위변조로 방문자 수를 임의로 조작할 수 있는 보안 허점 존재.

---

### 방식 C — Bloom Filter (Redis 구조 개선)

현행 Redis Set 대신 **Bloom Filter**를 사용해 메모리를 절감하는 방식.
Redis `BF.ADD` / `BF.EXISTS` 명령어(RedisBloom 모듈 필요).

```
BF.ADD   visitor_bloom:{today} {number}
BF.EXISTS visitor_bloom:{today} {number}
```

| 항목 | 평가 |
|------|------|
| Redis 조회 | 여전히 필요 |
| 구현 난이도 | 중간 (RedisBloom 모듈 설치 필요) |
| 보안 | 동일 |
| 메모리 효율 | Set 대비 수십 배 절감 |
| 단점 | **오탐(False Positive) 가능** — 새 방문자를 기존 방문자로 오분류할 수 있음 |
| 핵심 문제 해결 여부 | ❌ Redis 의존 유지 |

Redis 의존 자체는 그대로이므로 현행 방식의 근본 문제를 해결하지 못함.

---

### 방식 D — 외부 분석 도구 위임

Google Analytics, Cloudflare Analytics 등 전문 도구에 방문자 집계를 완전히 위임.

| 항목 | 평가 |
|------|------|
| 서버 코드 | 방문자 집계 코드 전체 제거 가능 |
| 구현 난이도 | 매우 낮음 (프론트엔드 스크립트 삽입) |
| 데이터 소유권 | **외부 서비스 의존** — 서비스 종료/정책 변경 위험 |
| 개인정보 | GA의 경우 GDPR/PIPA 고려 필요 |
| 자체 DB 저장 | 현재처럼 DB에 방문자 수 누적 불가 |

자체 DB에 방문자 수를 쌓는 현재 아키텍처와 병행하기 어려움.

---

## 3. 종합 비교표

| 항목 | 현행 (Redis Set) | A: HMAC 쿠키 | B: UUID 쿠키 | C: Bloom Filter | D: 외부 도구 |
|------|:---:|:---:|:---:|:---:|:---:|
| Redis 조회 제거 | ❌ | ✅ | ✅ | ❌ | ✅ |
| 위변조 방지 | ✅ | ✅ | ❌ | ✅ | — |
| 구현 난이도 | — | 낮음 | 매우 낮음 | 중간 | 매우 낮음 |
| Stateless | ❌ | ✅ | ✅ | ❌ | ✅ |
| 자정 만료 정확성 | TZ 의존 | 명시 제어 가능 | 명시 제어 가능 | TZ 의존 | — |
| Redis 장애 영향 | 전체 중단 | **없음** | **없음** | 전체 중단 | — |
| 자체 DB 누적 | ✅ | ✅ | ✅ | ✅ | ❌ |

---

## 4. 추천 방식: A (HMAC 서명 쿠키) 상세 설계

### 4-1. 핵심 원리

Spring Security `TokenBasedRememberMeServices`의 Hash-Based Token과 동일한 원리:

```
cookie_value = date + ":" + Base64(HMAC-SHA256(date + ":" + secret_key))
```

- `date`: 오늘 날짜 문자열 (`yyyy-MM-dd`, KST 기준)
- `secret_key`: 서버 환경변수로 관리 (`VISITOR_HMAC_SECRET`)

### 4-2. 검증 알고리즘

```
request 수신
  │
  ├─ user_n 쿠키 없음 → 신규 방문자 → 서명 쿠키 발급 + 카운트 +1
  │
  └─ user_n 쿠키 있음
       ├─ 파싱 실패 → 신규 방문자로 처리
       ├─ date ≠ 오늘 → 전날 쿠키 → 신규 방문자로 처리
       └─ HMAC 불일치 → 위변조 → 신규 방문자로 처리
       └─ 모두 통과 → 기존 방문자 → 통과
```

### 4-3. 쿠키 MaxAge 설정

```java
// 한국 시간 기준 자정까지 남은 초
ZonedDateTime now = ZonedDateTime.now(ZoneId.of("Asia/Seoul"));
ZonedDateTime midnight = now.toLocalDate().plusDays(1).atStartOfDay(ZoneId.of("Asia/Seoul"));
long secondsUntilMidnight = Duration.between(now, midnight).getSeconds();
cookie.setMaxAge((int) secondsUntilMidnight);
```

- 브라우저가 자정(KST)에 쿠키를 자동 만료 → 다음날 방문 시 신규 방문자로 집계
- Redis TTL 관리 불필요

### 4-4. 제거 가능한 구성요소

HMAC 쿠키 방식 도입 시 다음 컴포넌트를 제거할 수 있다:

| 제거 대상 | 비고 |
|-----------|------|
| `RandomUserNumberService` / `Impl` | 전체 삭제 |
| `RandomUserNumberRedisRepository` / `Impl` | 전체 삭제 |
| `RandomUserNumberDto` | 전체 삭제 |
| `RedisKey.RANDOM_USER_NUM_KEY` | 상수 제거 |
| Redis Set 저장 로직 | `save()`, `isExists()` |

`UserNumCookieInterceptor`는 유지하되 검증 로직을 HMAC 기반으로 교체.

### 4-5. 보안 고려사항

| 항목 | 내용 |
|------|------|
| secret 키 길이 | 최소 256bit (32바이트) 이상 권장 |
| secret 키 저장 | 환경변수 (`VISITOR_HMAC_SECRET`), 절대 코드에 하드코딩 금지 |
| 키 로테이션 | 로테이션 시 해당일 기존 쿠키 전체 무효화 → 당일 방문자 재집계 발생 (허용 범위) |
| 알고리즘 | HmacSHA256 (Java 표준 `javax.crypto.Mac`) |
| 쿠키 속성 | `HttpOnly`, `Secure`, `SameSite=Lax` 유지 (현행과 동일) |

---

## 5. 현행 유지 vs 방식 A 전환 시 득실

### 현행 유지가 나은 경우
- Redis가 이미 다른 핵심 기능(게시글 조회수, 좋아요, 방문자 카운트 캐싱)에도 사용 중이어서 Redis 자체를 제거할 수 없는 상황
- 방문자 토큰 Redis 조회가 전체 부하에서 무시할 수준인 트래픽

### 방식 A 전환이 나은 경우
- **서버 코드를 Stateless하게 유지하고 싶은 경우** (현재 고민)
- Redis 장애 시 방문자 집계가 영향받지 않아야 하는 경우
- 수평 확장 시 Redis 클러스터 없이도 정확한 집계를 원하는 경우
- 코드베이스를 단순화하고 싶은 경우 (5개 클래스 제거 가능)

---

## 6. 결론

**방식 A (HMAC 서명 쿠키)** 전환을 권장한다.

현재 방문자 수 집계용 Redis(VisitorCount 캐싱)는 유지되지만, 방문자 **중복 방지** 목적의 Redis Set은 제거 가능하다. 구현 난이도는 낮고, 보안은 현행과 동등하며, REST API Stateless 원칙을 회복할 수 있다.

```
현행: 모든 요청 → Redis 조회(randomUserNum:) → 방문자 판별
전환: 모든 요청 → HMAC 검증(CPU 연산) → 방문자 판별
```

Redis 네트워크 I/O를 CPU 연산(HMAC)으로 대체하는 구조이며, 수 마이크로초 수준의 HMAC 연산은 Redis RTT(수 밀리초)보다 훨씬 빠르다.
