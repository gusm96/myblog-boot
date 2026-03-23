# 방문자 수 2씩 증가 버그 분석 및 수정 계획서

## 1. 버그 현상

Chrome DevTools에서 `user_n` 쿠키를 삭제한 후 페이지를 새로고침하면, 방문자 수(today/total)가 **1이 아닌 2씩 증가**한다.

---

## 2. 근본 원인 분석

### 2.1 아키텍처 흐름 요약

```
Browser Refresh
  ├── GET /api/v2/visitor-count     ← VisitorCount 컴포넌트 (UserLayout 사이드바)
  └── GET /api/v7/boards/{boardId}  ← BoardDetailV2 컴포넌트 (Outlet)
          ↓ 둘 다
    UserNumCookieInterceptor.preHandle()
          ↓ 쿠키 없으면
    visitorCountService.incrementVisitorCount()
```

### 2.2 인터셉터 등록 범위 (WebConfig.java)

```java
registry.addInterceptor(userNumCookieInterceptor)
    .addPathPatterns("/api/v2/visitor-count")   // VisitorCount 조회
    .addPathPatterns("/api/v7/boards/**");       // 게시글 상세 조회
```

**두 개의 엔드포인트** 모두에 인터셉터가 등록되어 있다.

### 2.3 동시 요청 경합 (Race Condition) — 핵심 원인

게시글 상세 페이지(`/boards/:boardId`)에 있을 때, 브라우저는 **두 요청을 거의 동시에 전송**한다:

```
시간축 →

Browser:  ──[DELETE cookie]──[Refresh]──┬── GET /api/v2/visitor-count (쿠키 없음) ──────→
                                        └── GET /api/v7/boards/42    (쿠키 없음) ──────→

Server:   Request 1 도착 → 쿠키 없음 → increment (+1) → Set-Cookie 응답
          Request 2 도착 → 쿠키 없음 → increment (+1) → Set-Cookie 응답
                                                          ↑
                                            Request 2는 Request 1의 Set-Cookie를
                                            아직 받지 못한 상태에서 전송됨

결과: today +2, total +2
```

**핵심**: HTTP는 비연결(stateless) 프로토콜이므로, 브라우저가 첫 번째 응답의 `Set-Cookie`를 수신하기 **전에** 두 번째 요청을 이미 전송한다. 따라서 두 요청 모두 쿠키 없이 서버에 도착하여, 인터셉터가 **각각 독립적으로 방문자 수를 증가**시킨다.

### 2.4 영향 범위

| 페이지 | 인터셉터 적용 요청 수 | 증가량 |
|--------|----------------------|--------|
| 홈 (`/boards`) | 1개 (`visitor-count`만) | +1 (정상) |
| 게시글 상세 (`/boards/:id`) | **2개** (`visitor-count` + `v7/boards/{id}`) | **+2 (버그)** |
| 카테고리 페이지 | 1개 (`visitor-count`만) | +1 (정상) |
| 검색 페이지 | 1개 (`visitor-count`만) | +1 (정상) |

**게시글 상세 페이지에서만 재현되는 버그**이다. 사용자가 게시글 상세 페이지에서 쿠키를 삭제하거나, 쿠키 없이 게시글 상세 페이지에 최초 진입할 때 발생한다.

---

## 3. 설계 결함 분석

현재 `UserNumCookieInterceptor`가 **두 가지 책임**을 동시에 지고 있다:

1. **쿠키 검증/발급** — 모든 인터셉터 대상 요청에서 필요
2. **방문자 수 증가** — 하루에 한 번만 실행되어야 함

이 두 책임이 하나의 인터셉터에 결합되어 있기 때문에, 인터셉터가 실행되는 **모든 경로**에서 중복 증가가 발생할 수 있다.

---

## 4. 수정 방안

### 방안 A: 인터셉터에서 increment 분리 (권장)

**원칙**: 인터셉터는 쿠키 검증/발급만 담당하고, 방문자 수 증가는 `getVisitorCountV2` 컨트롤러에서만 수행한다.

#### 변경 파일

| 파일 | 변경 내용 |
|------|-----------|
| `UserNumCookieInterceptor.java` | `incrementVisitorCount()` 호출 제거, 신규 방문자 여부를 `request.setAttribute`로 전달 |
| `CommonController.java` | 신규 방문자일 때만 `incrementVisitorCount()` 호출 |

#### 변경 코드

**UserNumCookieInterceptor.java**
```java
@Override
public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
    Cookie userNumCookie = CookieUtil.findCookie(request, USER_NUM_COOKIE);
    String cookieValue = userNumCookie != null ? userNumCookie.getValue() : null;

    boolean isNewVisitor = false;
    if (!visitorHmacService.isValid(cookieValue)) {
        String newToken = visitorHmacService.generateToken();
        int maxAge = visitorHmacService.secondsUntilMidnight();
        response.addCookie(CookieUtil.addCookie(USER_NUM_COOKIE, newToken, maxAge));
        // ❌ 제거: visitorCountService.incrementVisitorCount(DateUtil.getToday());
        cookieValue = newToken;
        isNewVisitor = true;
    }

    request.setAttribute(USER_NUM_COOKIE, cookieValue);
    request.setAttribute("IS_NEW_VISITOR", isNewVisitor);  // ✅ 추가
    return true;
}
```

**CommonController.java**
```java
@GetMapping("/api/v2/visitor-count")
public ResponseEntity<VisitorCountDto> getVisitorCountV2(HttpServletRequest request) {
    String today = DateUtil.getToday();
    Boolean isNewVisitor = (Boolean) request.getAttribute("IS_NEW_VISITOR");
    if (Boolean.TRUE.equals(isNewVisitor)) {
        // ✅ visitor-count 엔드포인트에서만 1회 증가
        VisitorCountDto dto = visitorCountService.incrementVisitorCount(today);
        return ResponseEntity.ok().body(dto);
    }
    return ResponseEntity.ok().body(visitorCountService.getVisitorCount(today));
}
```

#### 장점
- 최소 변경 (2개 파일)
- 인터셉터의 책임을 쿠키 관리로 단일화 (SRP)
- 방문자 수 증가가 `visitor-count` API에서만 1회 발생하도록 보장
- `/api/v7/boards/**` 경로에서는 쿠키만 발급하고 increment하지 않음

#### 단점
- `IS_NEW_VISITOR` attribute를 통한 인터셉터↔컨트롤러 간 암묵적 결합

---

### 방안 B: 서버 측 멱등성 보장 (Redis 플래그)

인터셉터에서 increment하되, 동일 토큰으로 중복 increment를 방지한다.

```java
// UserNumCookieInterceptor.java
if (!visitorHmacService.isValid(cookieValue)) {
    String newToken = visitorHmacService.generateToken();
    // Redis에 짧은 TTL로 토큰 저장, 이미 있으면 increment 스킵
    if (incrementGuardRedisRepository.tryAcquire(newToken)) {
        visitorCountService.incrementVisitorCount(DateUtil.getToday());
    }
    response.addCookie(CookieUtil.addCookie(USER_NUM_COOKIE, newToken, maxAge));
    cookieValue = newToken;
}
```

#### 장점
- 인터셉터 구조 유지
- 어떤 경로 조합에서든 멱등성 보장

#### 단점
- 새 Redis 키/레포지토리 추가 필요
- HMAC 토큰이 날짜 기반이므로 같은 날 동일 토큰 생성 → 별도 고유값(UUID 등) 추가 필요
- 과도한 엔지니어링

---

## 5. 권장 수정 방안: 방안 A

**이유**:
1. **최소 변경**: 2개 파일, 약 10줄 수정
2. **근본 해결**: increment 호출 지점을 단일화하여 경합 조건 자체를 제거
3. **명확한 책임 분리**: 인터셉터 = 쿠키, 컨트롤러 = 비즈니스 로직
4. **추가 인프라 불필요**: Redis 키나 새 레포지토리 없음

---

## 6. 추가 고려사항

### 6.1 인터셉터에서 `VisitorCountService` 의존성 제거

방안 A 적용 후, `UserNumCookieInterceptor`는 더 이상 `VisitorCountService`를 주입받을 필요가 없다. 의존성을 제거하여 결합도를 낮춘다.

### 6.2 테스트 수정

- `UserNumCookieInterceptor` 단위 테스트: `incrementVisitorCount` 호출 검증 제거
- `CommonController` 통합 테스트: 신규 방문자일 때 increment 호출 검증 추가
- 기존 `VisitorCountServiceImplTest`: 변경 불필요 (서비스 로직 자체는 동일)

### 6.3 엣지 케이스

| 시나리오 | 방안 A 동작 |
|----------|------------|
| 쿠키 없이 홈 접속 | `visitor-count` API에서 1회 increment → 정상 |
| 쿠키 없이 게시글 상세 접속 | 인터셉터 2회 실행(쿠키만 발급), `visitor-count` API에서 1회 increment → **정상** |
| 쿠키 있는 상태에서 재접속 | increment 없음 → 정상 |
| `visitor-count` API를 호출하지 않는 시나리오 (게시글 상세 직접 접근) | increment 안 됨 — 하지만 `VisitorCount` 컴포넌트가 `UserLayout`에 항상 존재하므로 사실상 불가능 |

---

## 7. 구현 순서

1. `UserNumCookieInterceptor`에서 `VisitorCountService` 의존성 및 `incrementVisitorCount` 호출 제거
2. `IS_NEW_VISITOR` attribute 추가
3. `CommonController.getVisitorCountV2()`에 `HttpServletRequest` 파라미터 추가 및 분기 로직 구현
4. 기존 테스트 수정 및 신규 테스트 작성
5. 수동 검증: Chrome DevTools에서 쿠키 삭제 후 게시글 상세 페이지에서 방문자 수 확인
