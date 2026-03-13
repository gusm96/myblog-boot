# Frontend 프로젝트 코드 검토 보고서

> 최초 작성: 2026-03-12 | 최종 업데이트: 2026-03-13
> 대상 경로: `myblog-boot/frontend/`
> 검토 도구: context7 MCP (TanStack Query v5 docs, React Router v6 docs)

### 변경 이력

| 날짜 | 내용 |
|------|------|
| 2026-03-12 | 최초 작성 |
| 2026-03-13 | 라이브러리 1단계 업그레이드 완료 (13개 패키지), P1 버그 3개 수정 완료, 취약점 65→39개 반영 |
| 2026-03-13 | Draft.js → Tiptap 교체 완료, Context7 재검토 반영, 취약점 39→32개 반영 |

---

## 목차

1. [프로젝트 개요](#1-프로젝트-개요)
2. [기술 스택 분석](#2-기술-스택-분석)
3. [아키텍처 구조](#3-아키텍처-구조)
4. [컴포넌트 검토](#4-컴포넌트-검토)
5. [상태 관리 검토](#5-상태-관리-검토)
6. [API 서비스 레이어 검토](#6-api-서비스-레이어-검토)
7. [보안 검토](#7-보안-검토)
8. [성능 검토](#8-성능-검토)
9. [발견된 버그 및 문제점](#9-발견된-버그-및-문제점)
10. [개선 권고사항 (우선순위별)](#10-개선-권고사항-우선순위별)
11. [종합 평가](#11-종합-평가)

---

## 1. 프로젝트 개요

개인 블로그 서비스의 React SPA 프론트엔드. 일반 사용자 화면(블로그 열람, 댓글)과 관리자 화면(게시글 작성·수정·삭제, 카테고리 관리)으로 구성되어 있다.

| 항목 | 내용 |
|------|------|
| 빌드 도구 | Create React App (CRA) 5.0.1 |
| 언어 | JavaScript (JSX) |
| 소스 파일 | 53개 `.js` 파일 |
| CSS | 295줄 (9개 파일) |
| 컴포넌트 수 | ~38개 |
| 서비스/훅 | services 3개, hooks 1개 |
| 배포 | Docker multi-stage build + Nginx |

---

## 2. 기술 스택 분석

### 2.1 핵심 라이브러리

| 라이브러리 | 사용 버전 | 최신 안정 버전 | 상태 |
|-----------|----------|--------------|------|
| React | 18.2.0 | 18.x | ✅ 정상 |
| React Router | ~~6.11.1~~ → **6.30.3** | 7.x | ⚠️ v7 업그레이드 가능 (3단계) |
| Redux Toolkit | 1.9.7 | 2.x | ⚠️ 업그레이드 필요 (2단계) |
| TanStack Query | ~~5.17.15~~ → **5.90.21** | 5.x | ✅ 최신 (2026-03-13 업데이트) |
| ~~Draft.js~~ → **Tiptap** | ~~0.11.7~~ → **3.20.1** | 3.x | ✅ Tiptap으로 교체 완료 (2026-03-13) |
| styled-components | ~~6.0.0-rc.1~~ → **6.3.11** | 6.x (stable) | ✅ stable 버전 (2026-03-13 업데이트) |
| moment.js | 2.29.4 | 유지보수 모드 | ⚠️ 대체 권고 (4단계) |
| Bootstrap | ~~5.2.3~~ → **5.3.8** | 5.3.x | ✅ 최신 (2026-03-13 업데이트) |
| Axios | ~~1.3.5~~ → **1.13.6** | 1.x | ✅ 최신 (2026-03-13 업데이트) |
| redux-persist | 6.0.0 | 6.0.0 | ✅ 최신 |

### 2.2 CRA (Create React App) 사용 문제

`react-scripts 5.0.1`은 2022년 이후 **사실상 유지보수가 중단된 상태**다. 주요 문제:

- Webpack 5 기반이나, 설정이 내부에 고정되어 있어 커스터마이징이 불가능하고 Vite 대비 개발 서버 HMR 속도가 느림
- 최신 React/TypeScript와의 호환 문제 누적
- [공식적으로 Create React App 폐기 권고](https://react.dev/learn/start-a-new-react-project)
- `DEPRECATED` 경고가 npm install 시 출력됨

**권고**: Vite 또는 Next.js로 마이그레이션.

---

## 3. 아키텍처 구조

### 3.1 디렉토리 구조

```
src/
├── apiConfig.js          # API 엔드포인트 상수 모음
├── App.js                # 라우팅 설정
├── components/
│   ├── Boards/           # 게시글 관련 컴포넌트
│   ├── Category/         # 카테고리 관련 컴포넌트
│   ├── Comments/         # 댓글 관련 컴포넌트
│   ├── Layout/           # 레이아웃 (Header, ProtectedRoute, UserLayout)
│   ├── Navbar/           # 내비게이션 바
│   └── Styles/           # CSS-in-JS 및 CSS 파일
├── hooks/
│   └── useQueries.js     # React Query 커스텀 훅
├── redux/
│   ├── store.js
│   ├── userSlice.js
│   └── authAction.js
├── screens/              # 페이지 단위 컴포넌트
└── services/             # API 호출 함수
```

### 3.2 아키텍처 평가

**강점:**
- 서비스(API 호출) / 컴포넌트 / 상태 관리가 명확히 분리되어 있음
- React Query로 서버 상태를 별도 관리하는 패턴 적용 시작
- 인증 흐름이 Redux + token refresh로 일관성 있게 구성됨
- Dockerfile multi-stage build로 배포 이미지가 최소화됨

**약점:**
- **미완성 마이그레이션**: v1 컴포넌트와 v2 컴포넌트가 혼재 (하단 [섹션 4.1](#41-미완성-마이그레이션-v1v2-컴포넌트-혼재) 참고)
- `App.js`에 React Query `QueryClientProvider` 래핑이 누락 — 현재 `UserLayout` 또는 상위에서 설정하고 있을 가능성 있으나 `index.js`에서 확인 필요
- TypeScript 미사용으로 런타임 에러 탐지가 늦어짐

---

## 4. 컴포넌트 검토

### 4.1 미완성 마이그레이션 (v1/v2 컴포넌트 혼재)

| 구버전 | 신버전 | 상태 |
|--------|--------|------|
| `BoardDetail.js` (useState + useEffect) | `BoardDetailV2.js` (React Query) | v1 미삭제 |
| `CategoryNav.js` | `CategoryNavV2.js` | v1 미삭제 |

`App.js`는 이미 `BoardDetailV2`를 사용하고 있으나 `BoardDetail.js`가 삭제되지 않아 코드베이스를 오염시키고 있다.

**조치**: `BoardDetail.js`, `CategoryNav.js` 삭제.

### 4.2 `VisitorCount.js` — 하드코딩된 localhost ✅ 수정 완료 (2026-03-13)

~~`apiConfig.js`의 `BASE_URL`을 사용하지 않아 **프로덕션 배포 시 동작 불가**.~~

`fetch` → `axios`로 교체하면서 `BASE_URL`을 올바르게 적용:
```javascript
// 수정 후
import { BASE_URL } from "../apiConfig";
const response = await axios.get(`${BASE_URL}/api/v2/visitor-count`);
```

### 4.3 `BoardList.js` — propTypes 오타 ✅ 수정 완료 (2026-03-13)

~~`prototype`으로 작성하면 prop 타입 검증이 전혀 동작하지 않는다.~~

`BoardList.prototype` → `BoardList.propTypes`로 수정되었다.

### 4.4 `CommentForm.js` / `Comment.js` — window.location.reload() 남용

```javascript
// 댓글 제출 후 전체 페이지 새로고침
window.location.reload();
```

React Query v5의 `useMutation` + `invalidateQueries`를 사용하면 전체 새로고침 없이 댓글 목록만 갱신할 수 있다.

**권고 패턴 (context7 React Query v5 문서 기반):**
```javascript
const queryClient = useQueryClient();

const addCommentMutation = useMutation({
  mutationFn: (commentData) => addComment(commentData),
  onSettled: () => {
    queryClient.invalidateQueries({ queryKey: ['comments', boardId] });
  },
});
```

### 4.5 `PageButton.js` — window.location 직접 조작

```javascript
window.location.href = `...`;
```

React Router의 `useNavigate`를 사용해야 SPA 히스토리가 정상 동작한다.

### 4.6 `ProtectedRoute.js` — 하드코딩된 역할 검사

```javascript
if (role === "ROLE_ADMIN") { ... }
```

역할 값을 상수로 추출하거나 `userSlice`에 셀렉터로 정의하는 것이 좋다.

### 4.7 `Header.js` — 정적 링크 하드코딩

```javascript
// 방명록 링크가 정적 문자열로 하드코딩
```

존재하지 않는 라우트로 연결될 경우 404가 발생할 수 있다.

### 4.8 `ErrorBoundary.js` — console.error만 사용

에러 경계에서 에러를 `console.error`로만 처리하고 있다. 프로덕션에서는 Sentry 같은 외부 에러 추적 서비스에 전송해야 한다.

---

## 5. 상태 관리 검토

### 5.1 Redux 구성

```
store.js → configureStore (redux-logger 포함)
userSlice.js → isLoggedIn, accessToken
authAction.js → thunk wrapping
```

**문제: redux-logger가 프로덕션에도 포함됨**

```javascript
// src/redux/store.js
import logger from "redux-logger";
// ...
middleware: (getDefaultMiddleware) =>
  getDefaultMiddleware().concat(logger),
```

로거는 개발 환경에서만 활성화해야 한다:
```javascript
middleware: (getDefaultMiddleware) => {
  const middlewares = getDefaultMiddleware();
  if (process.env.NODE_ENV === "development") {
    return middlewares.concat(logger);
  }
  return middlewares;
},
```

### 5.2 redux-persist 보안 고려사항

`accessToken`이 `localStorage`에 영구 저장됨. XSS 공격 시 토큰 탈취 가능. **httpOnly 쿠키를 사용한 토큰 관리**가 보안상 더 안전하다.

### 5.3 React Query 설정

`useQueries.js`의 설정:
```javascript
staleTime: 5000,   // 5초
gcTime: 5000,      // 5초
```

`gcTime`이 `staleTime`과 동일하게 설정되어 있다. `gcTime`은 일반적으로 `staleTime`보다 크게 설정해야 캐시를 충분히 활용할 수 있다. 권고: `gcTime: 10 * 60 * 1000` (10분).

### 5.4 `App.js` — QueryClient 컴포넌트 내부 생성 (신규)

`App.js:28`에서 `QueryClient`를 컴포넌트 함수 **내부**에서 생성하고 있다.

```javascript
// src/App.js (현재 — 문제 있음)
function App() {
  const queryClient = new QueryClient();  // ❌ 렌더마다 새 인스턴스
  ...
}
```

`App`이 리렌더링될 때마다 `QueryClient`가 새로 생성되어 **기존 캐시 전체가 초기화**된다. TanStack Query 공식 문서(stable-query-client 규칙)는 컴포넌트 외부 또는 `useState`로 한 번만 생성할 것을 명시하고 있다.

```javascript
// 권고: 모듈 레벨로 이동
const queryClient = new QueryClient();

function App() {
  ...
}
```

> `QueryClientProvider`의 존재 자체는 `App.js:49`에 정상적으로 래핑되어 있어 문제없다.

---

## 6. API 서비스 레이어 검토

### 6.1 에러 처리 불일치

대부분의 API 함수가:
```javascript
.catch((error) => console.log(error))
```
만 수행하고 있어 사용자에게 에러 피드백이 전혀 없는 경우가 많다.

**권고**: axios interceptor에서 공통 에러 처리:
```javascript
axios.interceptors.response.use(
  (response) => response,
  (error) => {
    if (error.response?.status === 401) {
      // 토큰 재발급 또는 로그아웃
    }
    return Promise.reject(error);
  }
);
```

### 6.2 API 버전 혼재

`apiConfig.js`를 보면 같은 리소스에 여러 API 버전이 혼재:

```javascript
CATEGORY_CRUD: "/api/v1/categories"
CATEGORIES: "/api/v2/categories"
CATEGORIES_FOR_ADMIN: "/api/v1/categories-management"

BOARD_CRUD: "/api/v1/boards"
BOARD_GET: "/api/v7/boards/{id}"   // ← v7까지 올라간 것은 이례적
```

특히 `BOARD_GET`이 `v7`인 것은 백엔드와의 정합성을 별도로 확인해야 한다.

### 6.3 인증 토큰 전달 방식 불일치

일부 API 함수는 `accessToken`을 파라미터로 받고, 일부는 쿠키에 의존한다. 전달 방식이 일관되지 않아 유지보수 시 혼동이 발생한다.

---

## 7. 보안 검토

### 7.1 DOMPurify 적용 — 양호

`BoardDetailV2.js`에서 백엔드로부터 받은 HTML을 렌더링할 때 DOMPurify를 적용하고 있다. XSS 방지 처리가 되어 있다.

```javascript
const sanitized = DOMPurify.sanitize(content);
```

### 7.2 accessToken localStorage 저장 — 위험

위 [5.2 섹션](#52-redux-persist-보안-고려사항) 참고.

### 7.3 `public/index.html` CDN + npm 패키지 중복 로드 ✅ 수정 완료 (2026-03-13)

~~Bootstrap이 CDN과 npm 두 곳에서 모두 로드됨. 번들 크기 증가 + 스타일 충돌 가능성.~~

`public/index.html`에서 Bootstrap CDN 태그 및 React/ReactDOM CDN 스크립트가 모두 제거되었다. 현재 Google Fonts, FontAwesome 등 필요한 외부 리소스만 유지 중이다.

### 7.4 `.env` 파일 관리

`.env.example`이 제공되고 `.gitignore`에 `.env`가 포함되어 있어 기본 보안 관리는 적절하다.

---

## 8. 성능 검토

### 8.1 번들 크기 우려 항목

| 라이브러리 | 크기 기여 | 대안 |
|-----------|----------|------|
| moment.js | ~230KB (gzip ~70KB) | `date-fns` 또는 `day.js` (<5KB) |
| Draft.js | ~60KB gzip | Tiptap, Quill, Plate |
| Bootstrap (CDN + npm 중복) | 이중 로드 | npm 단일 사용 |

### 8.2 로딩 UX

현재 로딩 상태 표시:
```javascript
if (isLoading) return <div>Loading...</div>;
```

단순 텍스트 표시로 레이아웃 이동(CLS)이 발생한다. Skeleton UI 도입 권고.

### 8.3 이미지 최적화 미흡

이미지 업로드/표시에서 `loading="lazy"`, `width`/`height` 명시 등의 최적화 없이 원본 크기 그대로 서빙된다. CRA 환경에서는 `react-lazyload` 또는 Intersection Observer API 활용을 권고한다.

### 8.4 코드 스플리팅 미적용

`React.lazy` / `Suspense`를 사용한 라우트 기반 코드 스플리팅이 없다. 초기 번들에 모든 페이지가 포함된다.

---

## 9. 발견된 버그 및 문제점

### 9.1 심각도 높음 (High)

| # | 위치 | 문제 | 영향 | 상태 |
|---|------|------|------|------|
| H-1 | `VisitorCount.js:19` | localhost 하드코딩 | 프로덕션에서 동작 불가 | ✅ 수정 완료 (2026-03-13) |
| H-2 | `public/index.html` | CDN + npm Bootstrap 중복 로드 | 스타일 충돌, 번들 낭비 | ✅ 수정 완료 (2026-03-13) |
| H-3 | `BoardList.js` | `BoardList.prototype` (propTypes 오타) | prop 검증 완전 무효화 | ✅ 수정 완료 (2026-03-13) |

### 9.2 심각도 중간 (Medium)

| # | 위치 | 문제 | 영향 |
|---|------|------|------|
| M-1 | `store.js` | redux-logger 프로덕션 포함 | 콘솔 오염, 성능 저하 |
| M-2 | `CommentForm.js`, `Comment.js` | window.location.reload() | UX 저하, SPA 의미 상실 |
| M-3 | `PageButton.js` | window.location.href 직접 조작 | React Router 히스토리 무력화 |
| M-4 | `userSlice.js` | accessToken을 localStorage에 persist | XSS 시 토큰 탈취 위험 |
| M-5 | `useQueries.js` | gcTime이 staleTime과 동일 | 캐시 효율 저하 |
| M-6 | `App.js:28` | QueryClient를 컴포넌트 내부에서 생성 | 리렌더 시 캐시 전체 초기화 |

### 9.3 심각도 낮음 (Low)

| # | 위치 | 문제 | 영향 |
|---|------|------|------|
| L-1 | `BoardDetail.js`, `CategoryNav.js` | 사용되지 않는 v1 컴포넌트 잔류 | 코드 혼란 |
| L-2 | 다수 API 함수 | `.catch((e) => console.log(e))` 만 처리 | 사용자 에러 피드백 없음 |
| L-3 | `ErrorBoundary.js` | 에러 외부 전송 없음 | 프로덕션 에러 추적 불가 |
| L-4 | `apiConfig.js` | BOARD_GET이 v7 | 백엔드 버전 혼재 확인 필요 |
| L-5 | 전체 | TypeScript 미사용 | 컴파일 타임 타입 안전성 없음 |

---

## 10. 개선 권고사항 (우선순위별)

### P1 — 즉시 수정 (버그 / 프로덕션 영향) ✅ 전체 완료 (2026-03-13)

1. ✅ **`VisitorCount.js` localhost 제거** → `axios` + `BASE_URL` 사용으로 수정
2. ✅ **`public/index.html` CDN Bootstrap/React 스크립트 제거** → npm 패키지만 사용
3. ✅ **`BoardList.js` propTypes 오타 수정** (`prototype` → `propTypes`)

### P2 — 단기 개선 (코드 품질)

4. **redux-logger 개발 환경 한정 활성화**
5. **`window.location.reload()` → React Query `invalidateQueries`로 교체**
   ```javascript
   // CommentForm.js 개선 예시
   const queryClient = useQueryClient();
   const mutation = useMutation({
     mutationFn: addComment,
     onSuccess: () => queryClient.invalidateQueries({ queryKey: ['comments', boardId] }),
   });
   ```
6. **v1 컴포넌트 삭제** (`BoardDetail.js`, `CategoryNav.js`)
7. **gcTime 수정**: `gcTime: 10 * 60 * 1000`

### P3 — 중기 개선 (아키텍처)

8. **axios interceptor 도입** — 401 처리, 토큰 재발급, 공통 에러 핸들링
9. ✅ **Draft.js → Tiptap 교체 완료** (2026-03-13) — `BoardEditor.js`, `BoardEditForm.js` 교체, 패키지 6개 제거
10. **moment.js → day.js 교체** — 번들 크기 ~225KB 절감
11. **코드 스플리팅 도입** — React.lazy + Suspense

### P4 — 장기 개선 (기술 부채)

12. **CRA → Vite 마이그레이션** — 빌드 속도 10배 향상, 최신 생태계 지원
13. **TypeScript 점진적 도입** — `.js` → `.tsx` 마이그레이션
14. **Skeleton UI 도입** — 로딩 UX 개선
15. **테스트 코드 작성** — 현재 `setupTests.js`만 존재하고 실제 테스트 파일 없음

---

## 11. 종합 평가

### 긍정적 측면

- **서비스/컴포넌트/상태 분리**가 명확하며 코드 가독성이 좋음
- **React Query 도입이 진행 중** — 신규 컴포넌트는 올바른 패턴을 사용하고 있음
- **DOMPurify로 XSS 방어** 처리됨
- **Docker multi-stage build**로 배포 이미지 최소화
- **인증 흐름** (토큰 재발급, Redux persist) 이 체계적으로 설계됨
- ✅ **P1 버그 3개 수정 완료** (2026-03-13) — localhost 하드코딩, CDN 중복 로드, propTypes 오타
- ✅ **라이브러리 13개 업그레이드** (2026-03-13) — 취약점 65개 → 39개 감소
- ✅ **Draft.js → Tiptap 교체 완료** (2026-03-13) — 취약점 39개 → 32개 감소, Vite 호환 확보, Markdown 파일 업로드 기능 추가

### 주요 우려 사항

- **CRA 사용 지속 불가** — 생태계에서 공식 폐기됨
- **v1/v2 마이그레이션 미완** — 일관성 없는 패턴이 혼재함
- **RTK 1.x → 2.x 마이그레이션 미완** — 2단계 진행 예정
- **잔여 취약점 32개** — react-scripts 체인(~23개)은 CRA→Vite 전환 전까지 해결 불가

### 종합 점수

| 분야 | 점수 | 변화 |
|------|------|------|
| 아키텍처 설계 | 7/10 | — |
| 코드 품질 | 6/10 | — |
| 보안 | 7/10 | ↑ (CDN 제거, 취약점 65→32 감소) |
| 성능 | 5/10 | — |
| 테스트 | 2/10 | — |
| 유지보수성 | 7/10 | ↑ (Draft.js 유지보수 종료 라이브러리 제거) |
| **종합** | **5.7/10** | ↑ (P1 수정 + Draft.js 교체 반영) |

> P1 버그 수정, 1단계 라이브러리 업그레이드, Draft.js → Tiptap 교체까지 완료되었다. 다음 우선순위는 RTK 2.x 마이그레이션(2단계) → React Router v7(3단계) → CRA→Vite(4단계) 순서로 진행하면 단기간에 점수를 크게 올릴 수 있다.
