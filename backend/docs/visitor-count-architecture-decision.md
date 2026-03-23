# 방문자 수 집계 아키텍처 개선

> 작성일: 2026-03-14
> 관련 파일: `UserNumCookieInterceptor`, `VisitorCountService`, `CommonController`, `WebConfig`

---

## 1. 문제 인식

### 1-1. 방문 집계 범위가 특정 API 경로에 종속

현재 `UserNumCookieInterceptor`는 다음 두 경로에만 등록되어 있다.

```java
// WebConfig.java
registry.addInterceptor(userNumCookieInterceptor)
    .addPathPatterns("/api/v2/visitor-count")
    .addPathPatterns("/api/v7/boards/**");
```

SPA(Single Page Application) 구조에서 사용자는 브라우저 주소창에 임의의 URL을 직접 입력하거나, 외부 링크를 통해 특정 페이지로 바로 진입할 수 있다. 이 경우 위 두 경로 중 하나를 반드시 거친다는 보장이 없으며, 결과적으로 **방문자 수가 집계되지 않을 수 있다.**

```
사용자가 /post/42 로 직접 진입
  → React App 로드
  → 게시글 상세 API 호출 (/api/v7/boards/42)  ← 인터셉터 적용됨 (우연히 집계)

사용자가 /search?q=spring 으로 직접 진입
  → React App 로드
  → 검색 API 호출 (/api/v1/search?...)        ← 인터셉터 미적용, 집계 누락
```

이처럼 어떤 경로로 진입하느냐에 따라 집계 여부가 달라지는 **비결정적 동작**이 발생한다.

---

### 1-2. GET 요청에서 Side Effect 발생 — REST 원칙 위반

현재 방문자 수 집계(카운트 증가)는 `GET /api/v2/visitor-count` 요청에 의해 트리거된다.

```
GET /api/v2/visitor-count
  → UserNumCookieInterceptor.preHandle()
      → 신규 방문자 판별 시 visitorCountService.incrementVisitorCount() 호출
  → VisitorCountService.getVisitorCount() 호출 후 응답 반환
```

HTTP 명세(RFC 7231)에서 GET 메서드는 **안전(Safe)하고 멱등(Idempotent)** 해야 한다. 즉, 같은 요청을 몇 번 보내더라도 서버 상태가 변경되어서는 안 된다. 하지만 현재 설계에서는 GET 요청이 Redis 카운터를 증가시키는 부작용(Side Effect)을 일으킨다.

```
RFC 7231, Section 4.2.1:
"Request methods are considered 'safe' if their defined semantics are
essentially read-only; i.e., the client does not request, and does not
expect, any state change on the origin server as a result of applying
a safe method to a target resource."
```

이로 인해 다음과 같은 부작용이 생긴다.

- **캐시 오염**: 브라우저나 CDN이 GET 응답을 캐싱하면 카운트 증가가 발생하지 않는다.
- **재시도 위험**: 네트워크 오류로 인한 클라이언트 자동 재시도 시 카운트가 중복 증가할 수 있다.
- **의미 불명확**: 조회 API인지 집계 API인지 코드만으로 판단하기 어렵다.

---

### 1-3. 방문 등록과 카운트 조회의 관심사 미분리

현재 하나의 `GET /api/v2/visitor-count` 엔드포인트가 두 가지 책임을 동시에 진다.

| 책임 | 내용 |
|---|---|
| 방문 등록 | 쿠키 검증 → 신규 방문자 판별 → Redis 카운트 증가 |
| 카운트 조회 | Redis / DB에서 오늘·어제·누적 방문자 수 반환 |

단일 책임 원칙(SRP) 관점에서 두 관심사는 분리되어야 한다. 특히 카운트 조회는 방문 등록 여부와 무관하게 독립적으로 호출될 수 있어야 한다(예: 관리자 대시보드, 주기적 폴링 등).

---

## 2. 설계 대안 검토

### Option A — 프론트엔드 루트에서 명시적 Visit 등록 API 호출

```
[ 모든 경로로 진입 ]
       ↓
[ React App.js 마운트 (루트 컴포넌트) ]
       ↓
 useEffect (최초 1회)
       ├─ POST /api/v1/visit  → 방문 등록 (쿠키 검증 + 카운트 증가)
       └─ GET  /api/v2/visitor-count  → 순수 조회
```

**장점**
- SPA 특성상 어떤 URL로 진입해도 루트 컴포넌트는 반드시 마운트되므로 **모든 경로에서 집계 보장**
- POST 메서드 사용으로 **REST 시맨틱 준수** (상태 변경은 POST)
- 방문 등록과 카운트 조회가 완전히 분리되어 **단일 책임 원칙 준수**
- 인터셉터가 `/api/v1/visit` 하나만 감시하면 되므로 **성능 부하 최소화**

**단점**
- 프론트엔드 코드 변경 필요 (App.js 루트에 POST 호출 추가)
- JavaScript를 비활성화한 환경에서는 집계 불가 (현재도 동일한 한계)

---

### Option B — 서버 Filter를 전체 API 경로에 적용

```
모든 요청 → OncePerRequestFilter (/api/**)
              ↓
         쿠키 검증 → 신규 방문자면 Redis increment
```

**장점**
- 프론트엔드 변경 없이 서버 단에서 완전 처리

**단점**
- SPA에서 한 페이지 로드 시 API 호출이 다수 발생 → 매 요청마다 Redis 쿠키 조회 실행, **불필요한 I/O 부하**
- 정적 자원, 헬스체크 등 집계가 불필요한 경로까지 필터링 대상이 되어 **관리 복잡도 증가**
- Spring MVC Interceptor에서 Servlet Filter로의 전환 필요

---

### Option C — 현재 방식 유지 + 프론트 루트에서 GET 강제 호출

```
App.js useEffect → GET /api/v2/visitor-count (항상 호출)
```

**장점**
- 백엔드 변경 없음

**단점**
- **GET의 Side Effect 문제 그대로 유지**
- visitor-count 조회 없이 다른 경로만 이용하는 사용자는 집계 누락 가능성 잔존
- 근본적인 설계 문제를 해결하지 않고 우회하는 방식

---

## 3. 결정: Option A 채택

### 선택 이유

| 기준 | A | B | C |
|---|:---:|:---:|:---:|
| 모든 경로에서 집계 보장 | ✅ | ✅ | △ |
| REST 시맨틱 준수 | ✅ | ✅ | ❌ |
| 단일 책임 원칙 | ✅ | △ | ❌ |
| 성능 (Redis 조회 최소화) | ✅ | ❌ | ✅ |
| 구현 변경 범위 | 중 | 대 | 소 |

Option A는 SPA 아키텍처의 특성(루트 컴포넌트 단일 진입)을 설계에 그대로 활용하면서, REST 원칙과 SRP를 모두 만족하는 가장 균형 잡힌 방안이다.

---

## 4. 목표 아키텍처 (Option A)

```
┌─────────────────────────────────────────────────────────┐
│                  React SPA (모든 경로)                   │
│                                                         │
│  App.js (루트 컴포넌트)                                  │
│    useEffect(() => {                                    │
│      POST /api/v1/visit          ← 방문 등록 (1회)      │
│      GET  /api/v2/visitor-count  ← 카운트 조회          │
│    }, [])                                               │
└───────────────┬─────────────────────────────────────────┘
                │
                ▼
┌─────────────────────────────────────────────────────────┐
│                   Spring Boot Backend                    │
│                                                         │
│  POST /api/v1/visit                                     │
│    → UserNumCookieInterceptor (쿠키 검증 + 중복 방지)    │
│        → VisitorCountService.incrementVisitorCount()    │
│            → Redis HINCRBY (total, today)               │
│    → 200 OK (body 없음)                                 │
│                                                         │
│  GET /api/v2/visitor-count                              │
│    → VisitorCountService.getVisitorCount()              │
│        → Redis 조회 (miss 시 DB fallback)               │
│    → { total, today, yesterday }                        │
│                                                         │
│  [Scheduler]                                            │
│    10분마다: Redis → DB 동기화                           │
│    자정 00:00: 전날 최종 동기화                           │
│    자정 00:01: 오늘 날짜 레코드 생성                     │
└─────────────────────────────────────────────────────────┘
```

---

## 5. 구현 변경 계획

### 5-1. Backend

#### `VisitController` 신설 (또는 `CommonController` 활용)

```java
// POST /api/v1/visit — 방문 등록 전용, 응답 바디 없음
@PostMapping("/api/v1/visit")
public ResponseEntity<Void> registerVisit() {
    // 실제 카운팅은 UserNumCookieInterceptor가 preHandle에서 처리
    return ResponseEntity.ok().build();
}
```

인터셉터가 `preHandle` 단계에서 쿠키를 검증하고 카운트를 증가시키므로, 컨트롤러 메서드 자체에는 별도 로직이 필요 없다.

#### `WebConfig` — 인터셉터 경로 변경

```java
// 변경 전
registry.addInterceptor(userNumCookieInterceptor)
    .addPathPatterns("/api/v2/visitor-count")
    .addPathPatterns("/api/v7/boards/**");

// 변경 후
registry.addInterceptor(userNumCookieInterceptor)
    .addPathPatterns("/api/v1/visit");  // 방문 등록 전용 경로만
```

#### `GET /api/v2/visitor-count` — Side Effect 제거

기존 GET 엔드포인트는 그대로 유지하되, 인터셉터 적용 경로에서 제외한다. 이로써 순수 조회 역할만 담당한다.

---

### 5-2. Frontend

#### `App.js` 루트 컴포넌트

```javascript
// App.js
useEffect(() => {
  // 방문 등록 — 모든 경로 진입 시 1회 실행
  api.post('/api/v1/visit').catch(() => {});

  // 방문자 수 조회
  dispatch(fetchVisitorCount());
}, []);
```

`POST /api/v1/visit`는 응답 결과를 UI에 반영할 필요가 없으므로 오류가 발생해도 무시한다 (`.catch(() => {})`). 집계 실패가 사용자 경험에 영향을 주어서는 안 된다.

---

## 6. 변경 전후 비교

| 항목 | 변경 전 | 변경 후 |
|---|---|---|
| 방문 등록 트리거 | `GET /api/v2/visitor-count` 인터셉터 | `POST /api/v1/visit` 인터셉터 |
| 집계 보장 범위 | visitor-count, boards/** 경로만 | **모든 경로** (App.js 루트 보장) |
| GET 멱등성 | ❌ Side Effect 존재 | ✅ 순수 조회만 |
| 인터셉터 적용 경로 | 2개 | 1개 |
| 관심사 분리 | ❌ 등록 + 조회 혼재 | ✅ 등록 / 조회 분리 |

---

## 7. 고려 사항 및 한계

### 쿠키 기반 중복 방지의 한계
현재 `user_n` 쿠키는 브라우저 쿠키에 의존한다. 시크릿 모드, 쿠키 차단, 브라우저 초기화 시 동일 사용자가 재집계된다. 이는 완벽한 UV(Unique Visitor) 집계가 아니라 **근사치**임을 전제로 한다. 개인 블로그 규모에서는 이 수준의 정확도로 충분하다.

### Bot/크롤러 트래픽
`POST /api/v1/visit`는 `user_n` 쿠키가 없는 봇의 첫 요청에서 카운트가 증가할 수 있다. 필요 시 User-Agent 기반 필터링을 인터셉터에 추가할 수 있다.

### 단일 인스턴스 환경 전제
현재 `synchronized` 키워드 기반 동시성 제어는 단일 JVM에서만 유효하다. 수평 확장이 필요한 경우 Redis 기반 분산 락(예: Redisson)으로 교체가 필요하다.
