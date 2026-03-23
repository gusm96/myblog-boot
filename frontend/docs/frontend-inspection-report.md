# 프론트엔드 전반 점검 보고서

> 작성일: 2026-03-21
> 점검 기준: 코드베이스 직접 분석 + context7 (`/tanstack/query`, `/axios/axios-docs`, `/websites/vite_dev`, `/websites/reactrouter`)
> 4단계 마이그레이션(CRA→Vite) 완료 후 기준

---

## 목차

1. [버그 (즉시 수정 필요)](#1-버그-즉시-수정-필요)
2. [TanStack Query 패턴](#2-tanstack-query-패턴)
3. [Axios 패턴](#3-axios-패턴)
4. [React 패턴](#4-react-패턴)
5. [보안](#5-보안)
6. [성능](#6-성능)
7. [코드 품질](#7-코드-품질)
8. [접근성 (a11y)](#8-접근성-a11y)
9. [Vite 설정](#9-vite-설정)
10. [종합 평가 및 우선순위](#10-종합-평가-및-우선순위)

---

## 1. 버그 (즉시 수정 필요)

### B-1 ⛔ `QueryClient` 컴포넌트 내부 생성 — 매 렌더마다 캐시 유실

**파일**: `src/App.js:28`

```js
// 현재 — 컴포넌트 본문 안에서 생성 (렌더마다 새 인스턴스)
function App() {
  const queryClient = new QueryClient();   // ❌
```

React는 상태 변경마다 `App`을 재렌더하므로 `QueryClient`가 새로 생성되어 모든 캐시가 초기화된다.
context7 TanStack Query 공식 문서는 `QueryClient`를 **컴포넌트 외부**에서 생성할 것을 명시한다.

```js
// 수정
const queryClient = new QueryClient();   // ✅ 모듈 최상위

function App() { ... }
```

---

### B-2 ⛔ `checkBoardLike` — 빈 `axios.get()` 호출

**파일**: `src/services/boardApi.js:115-117`

```js
export const checkBoardLike = (page) => {
  return axios.get();   // ❌ URL 없음 — 호출 시 런타임 오류
};
```

함수 본문이 미완성 상태다. 사용처가 없으면 삭제, 필요하면 URL을 추가해야 한다.

---

### B-3 ⛔ `handleDelete` — `.then()` 콜백 누락

**파일**: `src/components/Boards/BoardEditForm.js:122`

```js
deleteBoard(boardId, accessToken)
  .then(window.history.go(-1))    // ❌ 즉시 실행됨 (콜백 아님)
  .catch(...)
```

`window.history.go(-1)`이 `.then()`에 전달되는 순간 **즉시 실행**된다. 삭제 API 응답을 기다리지 않고 바로 뒤로 이동한다.

```js
// 수정
deleteBoard(boardId, accessToken)
  .then(() => window.history.go(-1))   // ✅ 콜백으로 전달
  .catch(...)
```

---

### B-4 ⛔ `CommentForm` — `accessToken` prop 미전달

**파일**: `src/components/Boards/BoardDetailV2.js:39`, `src/components/Comments/CommentForm.js:14`

`CommentForm`은 `accessToken` prop을 파라미터로 선언하지만 호출부에서 전달하지 않는다.

```js
// BoardDetailV2.js — accessToken 없음
<CommentForm boardId={board.data.id} />   // ❌

// CommentForm.js — prop으로 받지만 실제로는 undefined
export const CommentForm = ({ boardId, accessToken }) => {
  ...
  addComment(boardId, commentData, accessToken)  // accessToken = undefined
```

댓글 작성 API가 항상 인증 오류(401)를 반환한다.

---

### B-5 ⛔ `BoardLike` — 하드코딩된 `localhost:8080` URL

**파일**: `src/components/Boards/BoardLike.js:21`

```js
queryFn: () =>
  axios.get(`http://localhost:8080/api/v1/boards/${boardId}/likes`)  // ❌
```

`apiConfig.js`의 `BASE_URL`을 사용하지 않아 배포 환경에서 동작하지 않는다.

```js
// 수정
import { BASE_URL } from "../../apiConfig";
queryFn: () =>
  axios.get(`${BASE_URL}/api/v1/boards/${boardId}/likes`)   // ✅
```

---

## 2. TanStack Query 패턴

context7 `/tanstack/query` 공식 문서 기반으로 분석.

### Q-1 ⚠️ `staleTime` / `gcTime` 5초 — 과도하게 짧음

**파일**: `src/hooks/useQueries.js`, `src/screens/Home.js`

```js
staleTime: 5 * 1000,  // 5초
gcTime:    5 * 1000,  // 5초
```

`gcTime`이 `staleTime`과 동일하게 5초로 설정되어 있다. 이는 캐시가 5초 후 **즉시 가비지 컬렉션**된다는 의미다. 뒤로가기로 돌아오면 항상 fresh 요청을 보낸다.

context7 TanStack Query 문서 권장:
- `staleTime`: 서버 데이터가 얼마나 자주 바뀌는지에 따라 설정 (블로그 게시글 → 1~5분)
- `gcTime`: `staleTime`보다 **충분히 길게** 설정 (기본값 5분 유지 권장)

```js
// 권장
staleTime: 60 * 1000,    // 1분
// gcTime: default (5분) — 명시하지 않아도 됨
```

---

### Q-2 ⚠️ `queryOptions` 헬퍼 미사용 — 중복 설정

**파일**: `src/hooks/useQueries.js`

context7 TanStack Query v5 문서는 `queryOptions()` 헬퍼를 사용해 queryKey·queryFn을 재사용 가능한 단위로 분리하는 패턴을 권장한다.

```js
// 현재 — 각 hook마다 설정 반복
export const useBoardQuery = (boardId) =>
  useQuery({ queryKey: ['board', boardId], queryFn: ..., staleTime: 5000, gcTime: 5000, ... });

// 권장 — queryOptions 헬퍼로 분리
import { queryOptions } from '@tanstack/react-query';

export const boardQueryOptions = (boardId) =>
  queryOptions({
    queryKey: ['board', boardId],
    queryFn: () => getBoard(boardId),
    staleTime: 60 * 1000,
  });

// 사용
export const useBoardQuery = (boardId) => useQuery(boardQueryOptions(boardId));
// queryClient.prefetchQuery(boardQueryOptions(id)) 도 동일 옵션 재사용 가능
```

---

### Q-3 ⚠️ `BoardLike` — TanStack Query 데이터를 `useState`에 복사

**파일**: `src/components/Boards/BoardLike.js:15-32`

```js
const [likeCount, setLikeCount] = useState("");
const boardLikes = useQuery({ ... });

useEffect(() => {
  if (!boardLikes.isLoading) setLikeCount(boardLikes.data);  // ❌ 복사
}, [boardLikes, ...]);
```

TanStack Query의 `data`를 `useState`에 복사하는 것은 안티패턴이다. `boardLikes.data`를 직접 사용하면 된다. 또한 좋아요 상태(`isLiked`)도 `useEffect + setState` 대신 `useLikeStatusQuery`(이미 존재)를 활용할 수 있다.

---

### Q-4 ⚠️ `Home.js` — `useSearchParams` 초기값 설정 오류

**파일**: `src/screens/Home.js:12`

```js
const [page] = useSearchParams("p");   // ❌ "p"는 초기값 파라미터가 아님
```

`useSearchParams(init)`의 인자는 초기 URLSearchParams 객체 또는 문자열 초기값이다. `"p"`를 넘기면 `p`가 **키이자 값**으로 파싱되어 의도와 다르게 동작할 수 있다.

`page`가 `URLSearchParams` 객체로 `getBoardList(page)`에 전달되는 구조를 활용하고 있으나, 초기화 의도를 명확히 해야 한다.

---

## 3. Axios 패턴

context7 `/axios/axios-docs` 문서 기반으로 분석.

### A-1 ⚠️ Axios 인스턴스 및 인터셉터 미사용

**관련 파일**: `src/services/boardApi.js`, `src/services/authApi.js`

모든 API 호출마다 `Authorization` 헤더를 **수동으로** 전달한다:

```js
// boardApi.js — 반복 패턴
headers: { Authorization: getToken(accessToken) }
```

context7 Axios 문서 권장 패턴: **Axios 인스턴스 + 요청 인터셉터**로 헤더를 중앙 관리.

```js
// 권장 — src/services/axiosInstance.js
const api = axios.create({ baseURL: import.meta.env.VITE_API_URL || 'http://localhost:8080' });

api.interceptors.request.use((config) => {
  const token = store.getState().user.accessToken;
  if (token) config.headers.Authorization = `Bearer ${token}`;
  return config;
});

api.interceptors.response.use(undefined, async (error) => {
  if (error.response?.status === 401) {
    // 토큰 재발급 로직
  }
  return Promise.reject(error);
});
```

---

### A-2 ⚠️ `VisitorCount.js` — 컴포넌트 내부에서 전역 axios 설정 변경

**파일**: `src/components/VisitorCount.js:15`

```js
const VisitorCount = () => {
  axios.defaults.withCredentials = true;   // ❌ 렌더마다 전역 설정 오염
```

`axios.defaults`는 전역 상태다. 컴포넌트 렌더마다 실행되어 불필요한 부작용을 일으킨다.
`withCredentials`는 `authApi.js`의 로그인/로그아웃 호출에서만 개별 설정되어 있으므로, 이 전역 설정은 의도가 불분명하고 다른 API 요청에도 영향을 준다. 모듈 최상위 1회 설정 또는 Axios 인스턴스로 관리해야 한다.

---

### A-3 ⚠️ `Bearer` 오타 — 소문자 `bearer`

**파일**: `src/services/boardApi.js:192`, `src/services/authApi.js:47,56`

```js
return `bearer ${accessToken}`;   // ⚠️ 소문자
```

HTTP 인증 스펙(RFC 7235)에서 `Bearer`는 대소문자 구분이 없지만, 일부 서버 구현은 대문자를 기대할 수 있다. 일관성을 위해 `Bearer`로 통일을 권장한다.

---

### A-4 ⚠️ 401 오류 처리 — `App.js`에만 산재

**파일**: `src/App.js:40-43`

```js
.catch((error) => {
  if (error.response.status === 401 || error.response.status === 500) {
    alert("토큰이 만료되어 로그아웃 합니다.");
    dispatch(userLogout());
  }
});
```

401 처리가 `App.js`에만 있고, 다른 API 호출의 인증 오류는 각 컴포넌트에서 개별 처리하거나 무시된다. Axios 인터셉터로 중앙화하면 모든 API 호출에서 일관된 처리가 가능하다.

---

### A-5 ℹ️ `uploadBoard` — `withCredentials` 인자 위치 오류

**파일**: `src/services/boardApi.js:54-57`

```js
axios.post(url, data, headers, { withCredentials: true })  // ❌ 4번째 인자 무시됨
```

`axios.post(url, data, config)`는 3개의 인자를 받는다. `withCredentials`는 3번째 `config` 객체에 포함해야 한다.

```js
// 수정
axios.post(url, data, {
  headers: { Authorization: getToken(accessToken) },
  withCredentials: true,   // ✅
})
```

---

## 4. React 패턴

### R-1 ⚠️ `window.location.reload()` — TanStack Query 캐시 미활용

**파일**: `src/components/Comments/CommentForm.js:45`

```js
addComment(boardId, commentData, accessToken)
  .then(() => window.location.reload());   // ❌ 전체 페이지 새로고침
```

TanStack Query를 사용 중임에도 `window.location.reload()`로 전체 새로고침한다.
`queryClient.invalidateQueries`를 사용하면 관련 캐시만 무효화해 UX를 개선할 수 있다.

```js
// 권장
const queryClient = useQueryClient();
addComment(boardId, commentData, accessToken)
  .then(() => queryClient.invalidateQueries({ queryKey: ['comments', boardId] }));
```

---

### R-2 ⚠️ `ErrorBoundary` — `window.location.href` 사용

**파일**: `src/components/ErrorBoundary.js:19`

```js
handleGoHome = () => {
  this.setState({ hasError: false, error: null });
  window.location.href = "/";   // 전체 페이지 리로드
};
```

Class Component에서 `useNavigate` 사용 불가라는 제약이 있으나, React Router v7의 `<Link>`나 `history` 객체를 활용하거나 ErrorBoundary를 함수형 래퍼로 감쌀 수 있다. 현재 동작은 전체 페이지 새로고침이어서 Redux 상태(로그인 정보 등)가 일시적으로 소실 후 rehydrate된다.

---

### R-3 ⚠️ `BoardDetailV2` — `likeStatus.isLoading` 로딩 블록

**파일**: `src/components/Boards/BoardDetailV2.js:22`

```js
if (board.isLoading || likeStatus.isLoading) return <div>Loading...</div>;
```

`likeStatus`는 `enabled: isLoggedIn`으로 비로그인 시 실행되지 않는다. 비로그인 사용자는 게시글 본문 로딩이 완료되어도 `likeStatus.isLoading`이 `false`로 고정되지 않을 경우 무한 로딩 상태에 빠질 수 있다.

> 실제로는 `enabled: false`인 쿼리의 `isLoading`이 `false`로 반환되므로 현재 동작은 정상이나, 명시적으로 `board.isLoading`만 체크하는 것이 의도를 더 명확히 한다.

---

### R-4 ℹ️ `VisitorCount` — 로딩 Spinner가 `height: 100vh` 점유

**파일**: `src/components/VisitorCount.js:36-45`

```js
<div style={{ height: "100vh" }}>
  <Spinner ... />
</div>
```

VisitorCount는 사이드바 위젯이지만 로딩 중에 화면 전체 높이를 점유한다. 작은 인라인 스피너로 교체해야 한다.

---

## 5. 보안

### S-1 ✅ XSS 방어 — DOMPurify 적용 확인

**파일**: `src/components/Boards/BoardDetailV2.js:28`

```js
{Parser(DOMPurify.sanitize(board.data.content))}   // ✅
```

서버에서 받은 HTML을 렌더링 전에 DOMPurify로 sanitize. 적절한 XSS 방어가 적용되어 있다.

---

### S-2 ⚠️ accessToken — Redux persist로 localStorage 저장

**파일**: `src/redux/store.js`

```js
whitelist: ["user"],   // isLoggedIn, accessToken이 localStorage에 저장됨
```

JWT accessToken이 `localStorage`에 저장되면 XSS 공격으로 탈취 위험이 있다. 일반적으로 accessToken은 메모리(Redux 비영속화)에, refreshToken은 `httpOnly cookie`에 저장하는 패턴이 권장된다.

현재 아키텍처(refreshToken은 쿠키, accessToken은 메모리/Redux)로 이전을 고려할 수 있다.

---

### S-3 ⚠️ `marked.parse` — HTML sanitize 없이 에디터에 삽입

**파일**: `src/components/Boards/BoardEditor.js:102`

```js
const html = marked.parse(event.target.result);
editor.commands.setContent(html, { emitUpdate: false });  // ⚠️ sanitize 없음
```

업로드된 Markdown 파일을 HTML로 변환하여 에디터에 삽입할 때 sanitize 과정이 없다. 악의적인 Markdown 파일로 `<script>` 태그 등이 에디터 내부에 삽입될 수 있다. 관리자 전용 기능이어서 실제 위험도는 낮으나, DOMPurify를 추가하는 것이 권장된다.

```js
// 권장
import DOMPurify from 'dompurify';
const html = DOMPurify.sanitize(marked.parse(event.target.result));
```

---

## 6. 성능

### P-1 ⚠️ 코드 스플리팅 미적용 — 975 kB 단일 번들

**파일**: `vite.config.js`, `src/App.js`

Vite 빌드 결과:
```
dist/assets/index-BxVvEtNr.js  975.13 kB │ gzip: 318.38 kB
```

관리자 페이지(`/management/*`) 코드가 일반 사용자 번들에 포함되어 있다. React.lazy + Suspense로 관리자 경로를 분리하면 초기 번들을 줄일 수 있다.

```js
// 권장 — App.js
const Management = React.lazy(() => import('./screens/Management'));
const BoardEditor = React.lazy(() => import('./components/Boards/BoardEditor'));
```

---

### P-2 ⚠️ `moment.js` — 유지보수 종료 라이브러리

**파일**: `src/components/Boards/BoardDetailV2.js:11,35`

```js
import moment from "moment";
moment(board.data.createDate).format("YYYY-MM-DD")
```

`moment.js`는 공식적으로 유지보수 종료 상태다. 번들 크기도 크다 (~70 kB gzip). `date-fns` 또는 `dayjs`로 교체를 권장한다.

```js
// dayjs 대체 (2 kB gzip)
import dayjs from 'dayjs';
dayjs(board.data.createDate).format('YYYY-MM-DD')
```

---

### P-3 ℹ️ `QueryClient` — 기본 옵션 미설정

**파일**: `src/App.js`

```js
const queryClient = new QueryClient();  // 기본값만 사용
```

QueryClient 생성 시 `defaultOptions`로 전역 staleTime을 설정하면 각 useQuery마다 반복 설정이 불필요해진다.

```js
const queryClient = new QueryClient({
  defaultOptions: {
    queries: {
      staleTime: 60 * 1000,
      retry: 1,
    },
  },
});
```

---

## 7. 코드 품질

### C-1 ℹ️ 오타 — `"Loding..."`

**파일**: `src/screens/Home.js:20`

```js
if (isPending) return <div>Loding...</div>;   // ❌ 오타
```

---

### C-2 ℹ️ `BoardDetail.js` — 사용하지 않는 구버전 컴포넌트

**파일**: `src/components/Boards/BoardDetail.js`

`BoardDetailV2.js`가 실제로 사용되고 있으며, `BoardDetail.js`는 라우팅에서 참조되지 않는다. 제거를 검토해야 한다.

---

### C-3 ℹ️ `EditorToolbar` — `BoardEditor.js`와 `BoardEditForm.js`에서 코드 중복

두 파일에 동일한 `EditorToolbar` 컴포넌트가 복사되어 있다. 공통 컴포넌트로 분리를 권장한다.

---

### C-4 ℹ️ `authAction.js` — 불필요한 thunk 래퍼

**파일**: `src/redux/authAction.js`

```js
export const userLogin = (accessToken) => (dispatch) => dispatch(login({ accessToken }));
```

단순히 dispatch를 한 번 더 감싸는 thunk로, 추가 비동기 로직이 없다. RTK의 `login` 액션을 직접 dispatch해도 동일하다.

---

### C-5 ℹ️ `CategoryNav.js` — 사용 여부 불명확

`CategoryNav.js`와 `CategoryNavV2.js` 두 버전이 존재한다. `App.js`나 `UserLayout.js`에서 어느 버전을 사용하는지 확인하고 미사용 버전을 제거해야 한다.

---

## 8. 접근성 (a11y)

### AC-1 ⚠️ `BoardLike` — 클릭 이벤트 아이콘에 키보드 접근성 없음

**파일**: `src/components/Boards/BoardLike.js:62-73`

```jsx
<i className="fa-solid fa-heart" onClick={handleBoardLikeCancel} />   // ❌
<i className="fa-regular fa-heart" onClick={handleBoardLike} />       // ❌
```

`<i>` 요소는 의미론적 요소가 아니다. 키보드 사용자가 접근할 수 없다.

```jsx
// 권장
<button
  onClick={handleBoardLike}
  aria-label="게시글 좋아요"
  className="btn-icon"
>
  <i className="fa-regular fa-heart" />
</button>
```

---

### AC-2 ℹ️ `LoginForm.js` — `<a href>` 대신 `<Link>` 사용 권장

**파일**: `src/screens/Member/LoginForm.js:79`

```jsx
<a href="/join"><Button>회원가입</Button></a>   // ⚠️ 전체 페이지 새로고침
```

React Router `<Link>` 대신 `<a href>`를 사용해 SPA 네비게이션이 아닌 전체 페이지 이동이 발생한다.

---

## 9. Vite 설정

### V-1 ✅ proxy, jsx 처리, test 설정 정상

`vite.config.js` — 현재 설정 이상 없음. (4단계 마이그레이션 결과 검증 완료)

---

### V-2 ℹ️ `.js` 확장자 — 장기적으로 `.jsx`로 표준화 권장

현재 `esbuild.include: /src\/.*\.js$/` 설정으로 `.js` JSX를 처리하고 있다.
파일 확장자를 `.jsx`로 변경하면 이 설정이 불필요해지고 IDE 지원이 향상된다.

---

### V-3 ℹ️ 번들 청크 분리 미적용

코드 스플리팅(P-1)과 함께 `build.rollupOptions.output.manualChunks`로 vendor 청크를 분리하면 캐시 효율이 높아진다.

```js
build: {
  rollupOptions: {
    output: {
      manualChunks: {
        vendor: ['react', 'react-dom', 'react-router'],
        redux:  ['@reduxjs/toolkit', 'react-redux', 'redux-persist'],
        query:  ['@tanstack/react-query'],
        editor: ['@tiptap/react', '@tiptap/starter-kit'],
      },
    },
  },
},
```

---

## 10. 종합 평가 및 우선순위

### 우선순위별 정리

| 우선순위 | ID | 항목 | 파일 |
|---------|----|------|------|
| ⛔ P0 (버그) | B-1 | `QueryClient` 컴포넌트 내부 생성 — 캐시 유실 | `App.js` |
| ⛔ P0 (버그) | B-2 | `checkBoardLike` 빈 axios.get() — 런타임 오류 | `boardApi.js` |
| ⛔ P0 (버그) | B-3 | `handleDelete` 콜백 누락 — 삭제 전 뒤로가기 | `BoardEditForm.js` |
| ⛔ P0 (버그) | B-4 | `CommentForm` accessToken 미전달 — 댓글 작성 불가 | `BoardDetailV2.js` |
| ⛔ P0 (버그) | B-5 | `BoardLike` 하드코딩 URL — 배포 환경 오작동 | `BoardLike.js` |
| ⚠️ P1 | A-5 | `uploadBoard` withCredentials 위치 오류 | `boardApi.js` |
| ⚠️ P1 | A-2 | 전역 `axios.defaults` 컴포넌트 내부 설정 | `VisitorCount.js` |
| ⚠️ P1 | S-3 | marked.parse 결과 sanitize 누락 | `BoardEditor.js` |
| ⚠️ P1 | Q-1 | staleTime/gcTime 과도하게 짧음 | `useQueries.js` |
| ⚠️ P1 | R-1 | `window.location.reload()` → invalidateQueries | `CommentForm.js` |
| ⚠️ P2 | A-1 | Axios 인스턴스/인터셉터 미사용 | 서비스 전체 |
| ⚠️ P2 | S-2 | accessToken localStorage 저장 | `store.js` |
| ⚠️ P2 | P-1 | 코드 스플리팅 미적용 — 975 kB 번들 | `App.js` |
| ⚠️ P2 | P-2 | moment.js → dayjs 교체 | `BoardDetailV2.js` |
| ⚠️ P2 | AC-1 | 좋아요 아이콘 키보드 접근성 | `BoardLike.js` |
| ℹ️ P3 | Q-2 | queryOptions 헬퍼 미사용 | `useQueries.js` |
| ℹ️ P3 | C-3 | EditorToolbar 중복 코드 | `BoardEditor.js`, `BoardEditForm.js` |
| ℹ️ P3 | C-4 | 불필요한 thunk 래퍼 | `authAction.js` |
| ℹ️ P3 | V-3 | 번들 청크 분리 | `vite.config.js` |
| ℹ️ P3 | C-2 | 미사용 `BoardDetail.js` 제거 | `BoardDetail.js` |

### 전체 점수

| 영역 | 점수 | 비고 |
|------|------|------|
| 보안 | 7/10 | DOMPurify ✅, accessToken localStorage ⚠️ |
| TanStack Query | 5/10 | QueryClient 버그, staleTime 짧음 |
| Axios | 4/10 | 인터셉터 없음, 전역 설정 오염 |
| React 패턴 | 6/10 | ErrorBoundary ✅, reload 안티패턴 ⚠️ |
| 성능 | 5/10 | 코드 스플리팅 없음, moment.js |
| 접근성 | 5/10 | 좋아요 버튼, a 태그 |
| 코드 품질 | 6/10 | 중복 코드, 미사용 파일 |
| **종합** | **5.4/10** | P0 버그 5건 수정 후 7점대 예상 |
