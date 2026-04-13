# 프론트엔드 보완 계획서

> 작성일: 2026-03-26
> 대상: `frontend/src` 전체
> 점검 기준: React Router v7, TanStack Query v5 공식 문서 (context7 MCP 참조)

---

## 점검 요약

| 우선순위 | 항목 수 | 설명 |
|---------|---------|------|
| P0 — 즉시 수정 | 2 | 성능·UX에 직접적 영향 |
| P1 — 이번 작업 | 5 | 잘못된 패턴·미사용 코드 |
| P2 — 다음 작업 | 4 | 품질 개선 |
| P3 — 나중에 | 2 | 최적화 |

---

## P0 — 즉시 수정

### P0-1. 코드 스플리팅 미적용 (번들 936KB)

**위치**: `App.js` 상단 import 전체

**문제**: 모든 페이지 컴포넌트가 정적 import로 한 번에 번들링됨.
Vite 빌드 시 다음 경고가 매번 발생:
```
(!) Some chunks are larger than 500 kB after minification.
```
초기 로드 시 불필요한 관리자 페이지(Tiptap 에디터 포함) 코드를 일반 사용자도 다운받게 됨.

**현재 코드**:
```js
// App.js — 정적 import (모두 번들에 포함)
import Home from "./screens/Home";
import { Management } from "./screens/Management";
import { BoardEditForm } from "./components/Boards/BoardEditForm";
import BoardEditor from "./components/Boards/BoardEditor";
import { CategoryList } from "./components/Category/CategoryList";
import { TemporaryStorage } from "./screens/TemporaryStorage";
// ... 나머지 전부
```

**수정 방향**: React Router의 `lazy` 라우팅 + `React.lazy` + `Suspense` 조합.
관리자 전용 페이지(BoardEditor, BoardEditForm, CategoryList 등)와 일반 페이지를 분리하여 동적 로드.

```js
// 변경 후 예시
import React, { lazy, Suspense } from "react";

const Home           = lazy(() => import("./screens/Home"));
const Management     = lazy(() => import("./screens/Management"));
const BoardEditor    = lazy(() => import("./components/Boards/BoardEditor"));
const BoardEditForm  = lazy(() => import("./components/Boards/BoardEditForm"));
const CategoryList   = lazy(() => import("./components/Category/CategoryList"));
const TemporaryStorage = lazy(() => import("./screens/TemporaryStorage"));
// ... 나머지 페이지 컴포넌트

// Route를 Suspense로 감싸기
<Suspense fallback={<div>Loading...</div>}>
  <Routes>
    ...
  </Routes>
</Suspense>
```

**예상 효과**: 관리자 페이지 코드(Tiptap 에디터 ~250KB)가 별도 청크로 분리.
초기 번들 500KB 이하로 감소 예상.

---

### P0-2. Axios Response 인터셉터 없음 — 세션 중 401 무처리

**위치**: `services/apiClient.js`

**문제**: 현재 `apiClient`에는 요청 인터셉터(Authorization 헤더 주입)만 있음.
응답 인터셉터가 없으므로, 세션 중간에 토큰이 만료되어 서버가 401을 반환해도 아무 처리 없이 API 호출이 조용히 실패함.

앱 마운트 시 `reissuingAccessToken()`을 1회 호출하지만, 그 이후 세션 내에서
accessToken이 만료되는 경우(예: 장시간 사용)는 대응 불가.

```js
// 현재 apiClient.js — 요청 인터셉터만 존재
apiClient.interceptors.request.use((config) => {
  if (_token) {
    config.headers.Authorization = `bearer ${_token}`;
  }
  return config;
});
// 응답 인터셉터 없음 → 401 응답 시 조용히 실패
```

**수정 방향**: 응답 인터셉터 추가. 401 수신 시 `reissuingAccessToken()` 자동 재시도.
재발급 실패 시 Redux logout 디스패치.

```js
// 변경 후 예시
import { store } from "../redux/store";
import { userLogout, updateUserAccessToken } from "../redux/authAction";
import { reissuingAccessToken } from "./authApi";

apiClient.interceptors.response.use(
  (response) => response,
  async (error) => {
    const originalRequest = error.config;
    if (error.response?.status === 401 && !originalRequest._retry) {
      originalRequest._retry = true;
      try {
        const newToken = await reissuingAccessToken();
        store.dispatch(updateUserAccessToken(newToken));
        setAuthToken(newToken.accessToken);
        originalRequest.headers.Authorization = `bearer ${newToken.accessToken}`;
        return apiClient(originalRequest);
      } catch {
        store.dispatch(userLogout());
      }
    }
    return Promise.reject(error);
  }
);
```

> **주의**: `reissuingAccessToken()`은 별도 axios 인스턴스로 호출해야 무한 루프 방지.
> `store`를 직접 import하는 방식 또는 콜백 DI 패턴 사용.

---

## P1 — 이번 작업 (잘못된 패턴·미사용 코드)

### P1-1. CategoryList.js — `useEffect + setState` + `window.location.reload()`

**위치**: `components/Category/CategoryList.js`

**문제**:
1. 데이터 페칭을 TanStack Query 없이 직접 `useEffect`로 처리 — 로딩/에러 상태 없음
2. 카테고리 삭제 성공 후 `window.location.reload()` 사용 → TanStack Query 캐시 전체 날림
3. 카테고리 추가 후 목록이 갱신되지 않음 (추가 성공 후 `setCategory("")`만 함, 목록 재조회 없음)

```js
// 현재
useEffect(() => {
  getCategoriesForAdmin().then((data) => setCategories(data)); // TQ 미사용
}, []);

const handleDeleteCategory = (e) => {
  deleteCategory(e.target.value)
    .then((data) => {
      alert(data);
      window.location.reload(); // ❌ 전체 리로드
    });
};
```

**수정 방향**:
```js
// 변경 후 예시
const { data: categories = [], isLoading } = useQuery({
  queryKey: queryKeys.admin.categories(),   // queryKeys에 admin.categories 추가 필요
  queryFn:  getCategoriesForAdmin,
  staleTime: 5 * 60 * 1000,
});

const addMutation = useMutation({
  mutationFn: addNewCategory,
  onSuccess: () => {
    queryClient.invalidateQueries({ queryKey: queryKeys.admin.categories() });
    queryClient.invalidateQueries({ queryKey: queryKeys.categories.all() }); // 사이드바도 갱신
    setCategory("");
  },
});

const deleteMutation = useMutation({
  mutationFn: deleteCategory,
  onSuccess: () => {
    queryClient.invalidateQueries({ queryKey: queryKeys.admin.categories() });
    queryClient.invalidateQueries({ queryKey: queryKeys.categories.all() });
  },
});
```

---

### P1-2. VisitorCount.js — 원시 axios + useState 데이터 페칭

**위치**: `components/VisitorCount.js`

**문제**:
1. `axios`를 직접 import해서 사용 → `apiClient`의 인터셉터(인증 헤더, 응답 처리) 우회
2. TanStack Query 없이 `useEffect + useState` — 로딩 상태 수동 관리, 캐싱 없음
3. 방문할 때마다 매번 새로 요청 (캐시 활용 불가)

```js
// 현재
import axios from "axios"; // ❌ 원시 axios 직접 사용

useEffect(() => {
  const fetchVisitorData = async () => {
    const response = await axios.get(`${BASE_URL}/api/v2/visitor-count`, ...);
    setVisitor({ ... });
  };
  fetchVisitorData();
}, []);
```

**수정 방향**:
```js
// 변경 후 예시
import apiClient from "../services/apiClient"; // apiClient 사용
import { useQuery } from "@tanstack/react-query";

// visitorApi.js에 추가
export const getVisitorCount = () =>
  apiClient.get("/api/v2/visitor-count").then((res) => res.data);

// VisitorCount.js
const { data: visitor, isPending } = useQuery({
  queryKey: ["visitor"],
  queryFn:  getVisitorCount,
  staleTime: 5 * 60 * 1000, // 5분 캐시
});
```

---

### P1-3. Redirect.js — anti-pattern (useEffect + navigate)

**위치**: `components/Redirect.js`

**문제**: React Router v7에서 `useEffect` 안에서 `navigate()`를 호출하는 패턴은 anti-pattern.
렌더링이 한 번 발생한 뒤 effect가 실행되어 불필요한 리렌더가 생김.

```js
// 현재 — anti-pattern
const Redirect = ({ to }) => {
  const navigate = useNavigate();
  useEffect(() => {
    navigate(to);
  }, [navigate, to]);
  return null;
};
```

**수정 방향**: `<Navigate>` 컴포넌트로 교체. 사용처 확인 후 직접 `<Navigate>`로 대체하거나 파일 삭제.

```js
// 변경 후 — 해당 컴포넌트 삭제하고 사용처에서 직접 사용
import { Navigate } from "react-router";
<Navigate to="/some-path" replace />
```

현재 `Redirect.js`가 어디서 사용되는지 확인 후 제거.

---

### P1-4. CategoryNav.js — 미사용 데드 코드

**위치**: `components/Navbar/CategoryNav.js`

**문제**: `UserLayout`에서는 `CategoryNavV2`를 사용하며, `CategoryNav`는 어디서도 import되지 않음.
또한 `CategoryNavV2`와 달리:
- 활성 카테고리 강조 없음
- TanStack Query 미사용 (직접 `useEffect + setState`)
- 더 기능이 적은 구버전

**수정 방향**: 파일 삭제.

---

### P1-5. BoardEditor.js — useMutation 미사용, 에러 처리 없음

**위치**: `components/Boards/BoardEditor.js`

**문제**: `uploadBoard()`를 `.then().catch()` 패턴으로 직접 호출.
- 제출 중 로딩 상태 없음 (버튼이 계속 클릭 가능 → 중복 제출 위험)
- catch 블록이 비어 있음 → 실패해도 사용자에게 아무 피드백 없음
- `BoardEditForm`은 이미 `useMutation` 패턴으로 구현됨 → 일관성 없음

```js
// 현재 — BoardEditor.js
const handleSubmit = (e) => {
  e.preventDefault();
  uploadBoard(formData, editor.getHTML())
    .then((res) => { ... alert("게시글을 등록하였습니다."); navigate(...); })
    .catch(() => {}); // ❌ 비어있는 catch
};
```

**수정 방향**: `useMutation`으로 전환하여 `BoardEditForm`과 패턴 통일.

```js
// 변경 후 예시
const uploadMutation = useMutation({
  mutationFn: ({ formData, html }) => uploadBoard(formData, html),
  onSuccess: (res) => {
    queryClient.invalidateQueries({ queryKey: queryKeys.boards.lists() });
    queryClient.invalidateQueries({ queryKey: queryKeys.categories.all() });
    alert("게시글을 등록하였습니다.");
    navigate(`/management/boards/${res.data}`);
  },
  onError: () => {
    alert("게시글 등록에 실패했습니다. 다시 시도해주세요.");
  },
});

// 버튼에 isPending 연결
<button type="submit" disabled={uploadMutation.isPending}>
  {uploadMutation.isPending ? "등록 중..." : "작성하기"}
</button>
```

---

## P2 — 다음 작업 (품질 개선)

### P2-1. BoardLike.js — Optimistic Update 롤백 미구현

**위치**: `components/Boards/BoardLike.js`

**문제**: 좋아요/취소 성공 시 `setQueryData()`로 캐시를 수동 업데이트하지만,
실패 시 롤백 로직이 없음. 서버 에러 발생 시 UI 상태와 서버 상태가 불일치.

```js
// 현재 — 실패 롤백 없음
const addLikeMutation = useMutation({
  mutationFn: () => addBoardLike(boardId),
  onSuccess: (newCount) => {
    queryClient.setQueryData(queryKeys.boards.likes(boardId),      newCount);
    queryClient.setQueryData(queryKeys.boards.likeStatus(boardId), true);
  },
  // onError 없음 → 실패 시 UI가 그대로 남음
});
```

**수정 방향**: `onMutate`로 이전 상태 스냅샷, `onError`에서 롤백, `onSettled`에서 서버 상태 동기화.

```js
// 변경 후 예시
const addLikeMutation = useMutation({
  mutationFn: () => addBoardLike(boardId),
  onMutate: async () => {
    await queryClient.cancelQueries({ queryKey: queryKeys.boards.likes(boardId) });
    const previousCount  = queryClient.getQueryData(queryKeys.boards.likes(boardId));
    const previousLiked  = queryClient.getQueryData(queryKeys.boards.likeStatus(boardId));
    queryClient.setQueryData(queryKeys.boards.likes(boardId), (old) => (old ?? 0) + 1);
    queryClient.setQueryData(queryKeys.boards.likeStatus(boardId), true);
    return { previousCount, previousLiked };
  },
  onError: (err, variables, context) => {
    queryClient.setQueryData(queryKeys.boards.likes(boardId),      context.previousCount);
    queryClient.setQueryData(queryKeys.boards.likeStatus(boardId), context.previousLiked);
  },
  onSettled: () => {
    queryClient.invalidateQueries({ queryKey: queryKeys.boards.likes(boardId) });
  },
});
```

---

### P2-2. 미사용 패키지 제거

**위치**: `package.json`

| 패키지 | 문제 |
|--------|------|
| `ws` (^8.19.0) | WebSocket 라이브러리. 프론트엔드 코드 어디서도 사용되지 않음 |
| `react-session-api` (^1.1.0) | 세션 API. 사용 흔적 없음 |

```bash
npm uninstall ws react-session-api
```

번들 크기 감소 + 의존성 공격 표면 축소.

---

### P2-3. CategoryList.js — 수정 기능 없는 빈 버튼

**위치**: `components/Category/CategoryList.js:81`

**문제**: "수정" 버튼이 존재하지만 `onClick` 핸들러가 없음 → 사용자 혼란 유발.

```js
// 현재
<Button className="edit-btn">수정</Button>  // ❌ 아무 동작 없음
```

**수정 방향** (둘 중 선택):
- A. 버튼 제거 (카테고리 수정 기능 미구현 확정 시)
- B. 인라인 편집 기능 구현 (카테고리명 수정 API가 백엔드에 있는 경우)

---

### P2-4. queryKeys — 관리자 카테고리 키 누락

**위치**: `services/queryKeys.js`

**문제**: `CategoryList`를 TanStack Query로 전환(P1-1)하려면 `admin.categories()` 키가 필요하지만 현재 없음.

```js
// 현재 admin 키
admin: {
  all:       () => ['admin'],
  boardsAll: () => [...queryKeys.admin.all(), 'boards'],
  boards:    (page) => [...queryKeys.admin.boardsAll(), { page: String(page) }],
  trashAll:  () => [...queryKeys.admin.all(), 'trash'],
  trash:     (page) => [...queryKeys.admin.trashAll(), { page: String(page) }],
  board:     (id)  => [...queryKeys.admin.all(), 'board', id],
  // categories 없음
}
```

**수정 방향**:
```js
admin: {
  // ... 기존 키들
  categoriesAll: () => [...queryKeys.admin.all(), 'categories'],
}
```

---

## P3 — 나중에 (최적화)

### P3-1. Vite manualChunks 번들 청크 분리

**위치**: `vite.config.js`

코드 스플리팅(P0-1)과 함께 vendor 청크를 명시적으로 분리하면 브라우저 캐시 효율이 높아짐.

```js
// vite.config.js 추가
build: {
  rollupOptions: {
    output: {
      manualChunks: {
        'vendor-react':    ['react', 'react-dom', 'react-router'],
        'vendor-query':    ['@tanstack/react-query', '@tanstack/react-query-persist-client'],
        'vendor-redux':    ['@reduxjs/toolkit', 'redux', 'react-redux', 'redux-persist'],
        'vendor-editor':   ['@tiptap/react', '@tiptap/starter-kit', '@tiptap/extension-image'],
        'vendor-ui':       ['react-bootstrap', 'bootstrap'],
      },
    },
  },
},
```

---

### P3-2. ErrorBoundary + TanStack Query 연동

**위치**: `components/ErrorBoundary.js`

**현재**: 클래스형 ErrorBoundary가 TanStack Query의 에러 상태와 독립적으로 동작.
`useQueryErrorResetBoundary`와 연동되어 있지 않아 "재시도" 버튼 클릭 시 query가 리셋되지 않음.

**수정 방향**: `react-error-boundary` 패키지 도입 + `useQueryErrorResetBoundary` 연동.

```js
// 변경 후 예시
import { useQueryErrorResetBoundary } from "@tanstack/react-query";
import { ErrorBoundary } from "react-error-boundary";

const AppErrorBoundary = ({ children }) => {
  const { reset } = useQueryErrorResetBoundary();
  return (
    <ErrorBoundary
      onReset={reset}
      fallbackRender={({ resetErrorBoundary }) => (
        <div className="container text-center mt-5">
          <div className="alert alert-danger">
            <h4>오류가 발생했습니다</h4>
            <button className="btn btn-primary" onClick={resetErrorBoundary}>
              다시 시도
            </button>
          </div>
        </div>
      )}
    >
      {children}
    </ErrorBoundary>
  );
};
```

---

## 작업 순서 요약

```
Phase 1 (P0)
  └── P0-1: App.js 코드 스플리팅 (React.lazy + Suspense)
  └── P0-2: apiClient.js response 인터셉터 추가

Phase 2 (P1)
  └── P1-4: CategoryNav.js 삭제 (가장 단순)
  └── P1-3: Redirect.js 교체 및 삭제
  └── P1-1: CategoryList.js → TanStack Query 전환
           └── P2-4: queryKeys admin.categoriesAll() 추가 (선행 필요)
  └── P1-2: VisitorCount.js → TanStack Query 전환
  └── P1-5: BoardEditor.js → useMutation 전환

Phase 3 (P2)
  └── P2-1: BoardLike Optimistic Update 롤백 구현
  └── P2-2: 미사용 패키지 제거 (ws, react-session-api)
  └── P2-3: CategoryList 수정 버튼 처리

Phase 4 (P3)
  └── P3-1: Vite manualChunks 설정
  └── P3-2: ErrorBoundary + useQueryErrorResetBoundary 연동
```

---

## 참조

- React Router v7 lazy loading: https://reactrouter.com/start/data/custom#lazy-loading
- TanStack Query v5 optimistic updates: https://tanstack.com/query/v5/docs/react/guides/optimistic-updates
- TanStack Query v5 useQueryErrorResetBoundary: https://tanstack.com/query/v5/docs/react/guides/suspense
