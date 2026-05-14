# 관리자 로그인 인증 버그 수정 계획서

## Problem (문제 정의)

### 증상
1. **`GET /api/v1/posts?p=1` → 401 Unauthorized**: 공개 API인데 로그인 상태에서 토큰이 만료되면 401 발생
2. **AccessToken 만료 시 자동 갱신 없음**: RefreshToken이 유효해도 AccessToken을 재발급받는 로직이 없어, 만료 즉시 세션이 끊김

### 원인 분석

**버그 1 — JwtFilter가 공개 API에서 만료 토큰을 거부**

요청 흐름:
```
[apiClient] Authorization: Bearer <expired_token>
    ↓
[JwtFilter.shouldNotFilter()] → /api/v1/posts는 EXCLUDE_PATHS에 없음 → 필터 실행
    ↓
[JwtFilter.doFilterInternal()] → 토큰 검증 실패 → ExpiredTokenException
    ↓
response.sendError(401) ← 여기서 끝. SecurityConfig의 permitAll()까지 도달 못함
```

- `ShouldNotFilterPath.EXCLUDE_PATHS`에 `/api/v8/posts`는 있지만 `/api/v1/posts`는 없음
- `apiClient.ts` 인터셉터가 Redux에 토큰이 있으면 **모든 요청**에 Bearer 헤더를 부착
- 결과: 공개 API도 만료 토큰과 함께 보내면 401 발생

**버그 2 — 토큰 자동 갱신 로직 부재**

- `authApi.ts`에 `reissuingAccessToken()` 함수 존재 (`GET /api/v1/reissuing-token`)
- `userSlice.ts`에 `updateAccessToken` 액션 존재
- **하지만** `apiClient.ts`에 401 응답 인터셉터가 없음 → 만료 시 갱신 시도 자체를 하지 않음
- `management/layout.tsx`는 401 시 바로 `userLogout()` → 로그인 페이지 리다이렉트

### 해결하지 않을 경우
- 관리자가 AccessToken 유효기간(보통 30분~1시간)이 지나면 매번 재로그인 필요
- 일반 사용자도 로그인 후 토큰 만료 시 공개 페이지(홈, 게시글 목록)에서 데이터 로딩 실패
- UX 심각 저하

---

## Analyze (분석 및 선택지 검토)

### 버그 1: JwtFilter 만료 토큰 처리

| 선택지 | 설명 | 장점 | 단점 |
|--------|------|------|------|
| **A. JwtFilter에서 만료 시 인증 없이 통과** | `ExpiredTokenException` catch 후 `filterChain.doFilter()` 호출 (인증 세팅 없이) | 공개 API는 정상 동작, 인증 필요 API는 SecurityConfig가 403 반환 | 만료 토큰으로 공개 API 접근 시 인증 정보 없음 (의도된 동작) |
| B. ShouldNotFilterPath에 공개 경로 추가 | `/api/v1/posts` 등을 EXCLUDE_PATHS에 추가 | 구현 간단 | 경로가 늘어날 때마다 수동 추가 필요, SecurityConfig와 이중 관리 |
| C. 프론트에서 공개 API는 토큰 미부착 | apiClient 대신 일반 axios 사용 | 백엔드 수정 불필요 | 기존 코드 대량 변경, apiClient 사용 목적 퇴색 |

**선택: A안**
- 근본 원인 해결: JwtFilter는 "인증 정보 추출" 역할이지, "공개 API 차단" 역할이 아님
- 만료 토큰 → 인증 없이 통과 → SecurityConfig의 `permitAll()`/`hasRole()`이 최종 판단
- OWASP 관점에서도 인증 결정은 SecurityFilterChain에 위임하는 것이 정석

### 버그 2: 토큰 자동 갱신

| 선택지 | 설명 | 장점 | 단점 |
|--------|------|------|------|
| **A. Axios 응답 인터셉터 + 큐 패턴** | 401 → refresh 요청 → 성공 시 Redux 업데이트 + 원래 요청 재시도 | 표준 패턴, 동시 요청 처리 가능 | 구현 복잡도 중간 |
| B. 요청 전 토큰 만료 사전 체크 | 요청 인터셉터에서 JWT 디코딩 → 만료 임박 시 미리 갱신 | 401 자체를 방지 | JWT 디코딩 로직 필요, 시간 동기화 이슈 |
| C. 주기적 타이머로 갱신 | setInterval로 N분마다 갱신 | 단순 | 불필요한 네트워크 요청, 탭 비활성 시 낭비 |

**선택: A안**
- 업계 표준 패턴 (Axios interceptor + refresh queue)
- 동시에 여러 요청이 401을 받아도 refresh 요청은 1번만 수행
- refresh 실패 시 깔끔하게 로그아웃 처리

---

## Action (구현 계획)

### 작업 범위
- **백엔드**: `JwtFilter.java` 1개 파일 수정
- **프론트엔드**: `apiClient.ts` 1개 파일 수정

### Phase 1: JwtFilter 만료 토큰 처리 수정 (백엔드)

**파일**: `backend/src/main/java/com/moya/myblogboot/configuration/JwtFilter.java`

**변경 내용**: `ExpiredTokenException` 발생 시 401 반환 대신, 인증 정보 없이 `filterChain.doFilter()` 호출

```java
// Before
} catch (ExpiredTokenException e) {
    response.sendError(HttpServletResponse.SC_UNAUTHORIZED, e.getMessage());
    return;
}

// After
} catch (ExpiredTokenException e) {
    log.debug("Expired token — proceeding without authentication: {}", request.getRequestURI());
    filterChain.doFilter(request, response);
    return;
}
```

**동작 변화**:
- 공개 API (`anyRequest().permitAll()`) → 인증 없이 정상 응답
- 인증 필요 API (`hasRole("ADMIN")`) → SecurityConfig가 403 Forbidden 반환
- 프론트에서는 403을 받으면 토큰 갱신 시도 (Phase 2에서 처리)

### Phase 2: Axios 응답 인터셉터 — 토큰 자동 갱신 (프론트엔드)

**파일**: `frontend/lib/apiClient.ts`

**구현 설계**:

```
[API 요청] → 401 또는 403 응답
    ↓
[이미 갱신 중?]
    ├── Yes → 큐에 대기 (Promise)
    └── No  → isRefreshing = true
              ↓
         [GET /api/v1/reissuing-token] (withCredentials: refresh_token 쿠키)
              ├── 성공 → Redux 업데이트 + 대기 큐 전부 재시도
              └── 실패 → Redux logout + /login 리다이렉트 + 대기 큐 reject
```

**핵심 포인트**:
- `isRefreshing` 플래그로 동시 refresh 방지
- `failedQueue` 배열로 대기 중인 요청 관리
- refresh 대상 응답 코드: **401 (만료 토큰)** + **403 (JwtFilter 통과 후 권한 부족 = 만료로 인한)**
  - 단, 403은 실제 권한 부족일 수도 있으므로, refresh 1회 시도 후 재실패 시 그대로 reject
- `/api/v1/reissuing-token`, `/api/v1/login`, `/api/v1/logout` 요청은 인터셉터 대상에서 제외 (무한루프 방지)

### 변경 대상 파일 목록

| 파일 | 변경 유형 |
|------|-----------|
| `backend/.../configuration/JwtFilter.java` | 수정 — ExpiredTokenException 처리 변경 |
| `frontend/lib/apiClient.ts` | 수정 — 401/403 응답 인터셉터 + refresh 로직 추가 |

### 트레이드오프

| 항목 | 결정 | 이유 |
|------|------|------|
| 만료 토큰 → 401 vs 통과 | 통과 (인증 없이) | 인가 판단은 SecurityConfig 책임, Filter는 인증 정보 추출만 담당 |
| refresh 실패 시 동작 | 즉시 로그아웃 + 리다이렉트 | RefreshToken까지 만료 = 재로그인 필수 |
| 403도 refresh 시도 | 1회만 시도 | Phase 1 변경으로 만료 토큰 + 인증 필요 API = 403이 되므로 |

### 예상 이슈 및 대응

| 이슈 | 대응 |
|------|------|
| refresh 중 동시 요청 race condition | `isRefreshing` + `failedQueue` 큐 패턴으로 직렬화 |
| refresh 요청 자체가 401 → 무한루프 | `/api/v1/reissuing-token` URL은 인터셉터에서 제외 |
| SSR(Server Component)에서의 영향 | `api.ts`(fetch 기반)는 토큰 미사용 → 영향 없음 |

---

## Result (검증 계획)

### 테스트 시나리오

| # | 시나리오 | 기대 결과 |
|---|---------|-----------|
| 1 | 비로그인 상태에서 홈페이지 접근 | 게시글 목록 정상 로딩 |
| 2 | 로그인 후 AccessToken 만료 → 홈페이지 접근 | 게시글 목록 정상 로딩 (토큰 없이 통과) |
| 3 | 로그인 후 AccessToken 만료 → 관리자 페이지 접근 | 자동 토큰 갱신 → 정상 접근 |
| 4 | AccessToken + RefreshToken 모두 만료 → 관리자 페이지 | 갱신 실패 → 로그인 페이지 리다이렉트 |
| 5 | 동시 3개 API 요청 중 토큰 만료 | refresh 1회만 발생, 3개 모두 재시도 성공 |
| 6 | 비로그인 상태에서 관리자 페이지 직접 URL 접근 | 로그인 페이지 리다이렉트 (기존 동작 유지) |

### 검증 방법
- 브라우저 개발자 도구 Network 탭에서 요청/응답 흐름 확인
- Application 탭에서 refresh_token 쿠키 존재 확인
- Console 탭에서 불필요한 401 에러 미노출 확인
- AccessToken 만료 시뮬레이션: Redux DevTools에서 accessToken 값을 임의의 만료 토큰으로 교체

### 성공 기준
- 공개 API에서 401 에러 0건
- 관리자 페이지에서 AccessToken 만료 시 자동 갱신되어 재로그인 불필요
- RefreshToken 만료 시 깔끔하게 로그인 페이지 이동
- 콘솔에 불필요한 에러 로그 미노출
