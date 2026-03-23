# React Router v7 마이그레이션 계획서 (3단계)

> 작성일: 2026-03-20
> 완료일: 2026-03-20
> 대상 브랜치: `develop`
> 참고: context7 `/websites/reactrouter` (v7 공식 문서), 코드베이스 직접 분석

---

## 1. 2단계 완료 상태 확인

### 패키지 버전 (package.json 기준)

| 패키지 | 목표 | 실제 설치 | 상태 |
|--------|------|-----------|------|
| `@reduxjs/toolkit` | ^2.11.0 | ^2.11.2 | ✅ |
| `react-redux` | ^9.x | ^9.2.0 | ✅ |
| `redux` | ^5.x | ^5.0.1 | ✅ |
| `react-cookie` | ^8.x | ^8.0.1 | ✅ |

### store.js 수정 사항

```js
// ignoredActions 키 + 6개 액션 상수 등록 ✅
ignoredActions: [FLUSH, REHYDRATE, PAUSE, PERSIST, PURGE, REGISTER],

// redux-logger dev-only ✅
process.env.NODE_ENV === "development" ? [logger] : []
```

**2단계 이상 없음. 3단계 진행.**

---

## 2. 목표 버전

| 패키지 | 이전 | 이후 | 비고 |
|--------|------|------|------|
| `react-router-dom` | ^6.30.3 | 제거됨 | v7에서 통합 패키지로 폐기 |
| `react-router` | (미설치) | ^7.13.1 | v7 단일 통합 패키지 |

---

## 3. 현재 코드 분석

### 3.1 사용 중인 react-router-dom API 전체 목록

| API | 사용 파일 | v7 영향 |
|-----|----------|---------|
| `BrowserRouter as Router` | App.js | import 경로만 변경 |
| `Routes`, `Route` | App.js | import 경로만 변경 |
| `useParams` | BoardDetail.js, BoardDetailV2.js, BoardEditForm.js | import 경로만 변경 |
| `useNavigate` | Redirect.js, NavigateBack.js, LoginForm.js | import 경로만 변경 |
| `useSearchParams` | Management.js, Home.js, TemporaryStorage.js, PageByCategory.js | import 경로만 변경 |
| `useLocation` | SearchPage.js, ProtectedRoute.js, LoginForm.js | import 경로만 변경 |
| `Navigate` | ProtectedRoute.js | import 경로만 변경 |
| `Outlet` | ProtectedRoute.js, UserLayout.js | import 경로만 변경 |
| `Link` | CategoryNavV2.js, AdminNavBar.js, CategoryNav.js | import 경로만 변경 |

**`RouterProvider` 미사용** — `from "react-router/dom"` 분리 경로 적용 불필요.

### 3.2 Splat Route (`path="*"`) 분석

```jsx
// App.js — UserLayout 내부
<Route path="*" element={<NotFound />} />

// App.js — ProtectedRoute 내부
<Route path="*" element={<NotFound />} />
```

두 곳 모두 **단일 세그먼트 와일드카드**(`*`)이며, v7 breaking change 대상인
멀티 세그먼트 splat (`dashboard/*` 형태) **아님** → 경로 수정 불필요.

### 3.3 `BrowserRouter` alias 패턴

```jsx
// App.js
import { BrowserRouter as Router, Routes, Route } from "react-router-dom";
// ...
<Router>
```

v7에서도 `BrowserRouter`는 동일하게 존재하므로 alias 유지 가능.
`future` 플래그 미사용 → 제거할 것 없음.

---

## 4. Breaking Changes 체크리스트

### react-router v7 (context7 `/websites/reactrouter` 문서 기반)

| # | Breaking Change | 현재 코드 영향 | 조치 |
|---|----------------|--------------|------|
| R-1 | `react-router-dom` 패키지 폐기, `react-router`로 통합 | **전체 16개 파일, 17개 import 라인** | import 경로 일괄 변경 ✅ |
| R-2 | `RouterProvider`는 `"react-router/dom"`으로 분리 | `RouterProvider` 미사용 | 없음 |
| R-3 | `v7_relativeSplatPath` 기본 활성화 — 멀티 세그먼트 splat 동작 변경 | 단순 `*` wildcard만 사용 | 없음 |
| R-4 | `v7_startTransition` 기본 활성화 | JS 프로젝트 — 런타임 무영향 | 없음 |
| R-5 | Node.js 20+ 필수 | v22.17.1 확인 ✅ | 없음 |
| R-6 | React 18+ 필수 | React 18.2.0 사용 중 | 없음 |
| R-7 | `future` prop 제거됨 | `future` prop 미사용 | 없음 |

---

## 5. 실행 계획 (순서)

### Step 1 — Node.js 버전 확인 ✅

```bash
node -v  # v22.17.1 — 20+ 요건 충족
```

### Step 2 — 패키지 교체 ✅

```bash
npm uninstall react-router-dom
npm install react-router@latest
# → react-router@^7.13.1 설치됨
```

### Step 3 — import 일괄 변경 ✅

변경된 파일 목록 (16개 파일, 17개 라인):

| 파일 | 변경 전 | 변경 후 |
|------|---------|---------|
| `src/App.js` | `"react-router-dom"` | `"react-router"` |
| `src/components/Boards/BoardDetail.js` | 동일 | 동일 |
| `src/components/Boards/BoardDetailV2.js` | 동일 | 동일 |
| `src/components/Boards/BoardEditForm.js` | 동일 | 동일 |
| `src/components/Redirect.js` | 동일 | 동일 |
| `src/screens/Management.js` | 동일 | 동일 |
| `src/screens/Home.js` | 동일 | 동일 |
| `src/screens/TemporaryStorage.js` | 동일 | 동일 |
| `src/screens/PageByCategory.js` | 동일 | 동일 |
| `src/screens/SearchPage.js` | 동일 | 동일 |
| `src/components/Layout/ProtectedRoute.js` | 동일 | 동일 |
| `src/screens/Member/LoginForm.js` | 동일 | 동일 |
| `src/components/Layout/UserLayout.js` | 동일 | 동일 |
| `src/components/Navbar/CategoryNavV2.js` | 동일 | 동일 |
| `src/components/Navbar/AdminNavBar.js` | 동일 | 동일 |
| `src/components/Navbar/CategoryNav.js` | 동일 | 동일 |
| `src/screens/error/NavigateBack.js` | 동일 | 동일 |

변경 패턴:
```diff
-import { ... } from "react-router-dom";
+import { ... } from "react-router";
```

> `RouterProvider` 미사용이므로 `"react-router/dom"` 분리 import 불필요.

### Step 4 — 동작 검증 ✅

```bash
npm run build
# → Compiled successfully. 308.77 kB (+7.18 kB gzip, 2026-03-20)
```

체크포인트:
- [x] ✅ Node.js 버전 확인 — v22.17.1 (20+ 요건 충족, 2026-03-20)
- [x] ✅ `npm run build` 성공 — `Compiled successfully` (빌드 +7.18 kB, CRA 경고는 react-scripts 기존 이슈로 무관, 2026-03-20)
- [x] ✅ `react-router-dom` 잔여 import 없음 — `src/` 디렉토리 전체 grep 확인 (2026-03-20)
- [x] ✅ `/` (Home) 정상 렌더링 — 게시글 목록, 카테고리 사이드바, 방문자 수 표시 (Playwright 확인, 2026-03-20)
- [x] ✅ `/login` 접근 → LoginForm 정상 렌더링 — 아이디/비밀번호 입력 폼 표시 (Playwright 확인, 2026-03-20)
- [x] ✅ `/management` 접근 → 비로그인 시 `/login` 리다이렉트 — URL `http://localhost:3000/login`으로 전환 확인 (`Navigate` + `useLocation` 정상 동작, Playwright 확인, 2026-03-20)
- [x] ✅ `/boards/24` → `useParams` 정상 동작 — 게시글 제목·본문·댓글 영역 렌더링 (Playwright 확인, 2026-03-20)
- [x] ✅ `/Java` → `useParams(categoryName)` 정상 동작 — Java 카테고리 게시글 목록 렌더링 (Playwright 확인, 2026-03-20)
- [x] ✅ `/search?keyword=자바` → `useLocation` 정상 동작 — SearchPage 렌더링 (API 500은 백엔드 미기동 문제, React Router 무관, Playwright 확인, 2026-03-20)
- [x] ✅ `Link` 컴포넌트 네비게이션 — `Java (3)` 링크 클릭 시 `/Java`로 SPA 이동 (Playwright 확인, 2026-03-20)
- [x] ✅ 히스토리 뒤로가기 (`navigate(-1)`) — `/Java` → `/` 복귀 정상 (Playwright `goBack()` 확인, 2026-03-20)
- [x] ✅ 브라우저 콘솔 에러 0건 — React Router 관련 에러 없음 (Playwright 확인, 2026-03-20)

---

## 6. 변경 파일 목록

| 파일 | 변경 내용 |
|------|----------|
| `package.json` | `react-router-dom` 제거, `react-router@^7.13.1` 추가 |
| 위 17개 소스 파일 | import 경로 변경 (`"react-router-dom"` → `"react-router"`) |

**총 수정 파일: 18개** (package.json + 17개 소스 파일)
**코드 로직 변경: 0건** — import 경로 교체만 해당

---

## 7. 빌드 결과

| 항목 | 2단계 이전 (v6) | 3단계 완료 (v7) |
|------|----------------|----------------|
| react-router | (react-router-dom ^6.30.3) | react-router ^7.13.1 |
| 빌드 크기 (gzip) | 약 301.59 kB | 308.77 kB (+7.18 kB) |
| 취약점 | 30개 | 30개 (변화 없음) |
| 빌드 성공 | ✅ | ✅ |

> 빌드 크기 증가(+7.18 kB)는 react-router v7 패키지 구조 통합에 따른 것으로 정상 범위.
> 취약점은 react-scripts 체인에 기인하며 4단계(CRA → Vite)에서 해결.

---

## 8. 취약점 현황

| 단계 | 취약점 수 |
|------|---------|
| 1단계 완료 후 | 32개 |
| 2단계 완료 후 | 30개 |
| 3단계 완료 후 | **30개** (변화 없음) |

`react-router-dom`/`react-router` 자체에 등록된 취약점 없음.
잔여 취약점 30개 전부 `react-scripts` 체인에 기인 → 4단계에서 해결.

---

## 9. 후속 작업

이 마이그레이션 완료 후 다음 단계:

- **4단계**: CRA → Vite 마이그레이션 (`react-scripts` 취약점 ~30개 근본 해결)
  - 참고: `docs/cra-to-vite-migration-analysis.md`
