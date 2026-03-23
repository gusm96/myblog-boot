# TanStack Query 최적화 계획서
> 작성일: 2026-03-22
> 목적: 불필요한 API 트래픽 감소 및 UX 개선

---

## 1. 현황 분석

### 1-1. TanStack Query 채택률

| 분류 | 파일 수 | 비율 |
|---|---|---|
| `useQuery` 사용 | 4개 | 31% |
| `useEffect + setState` 레거시 패턴 | 5개 | 38% |
| `useQuery + useEffect` 혼합 (나쁜 패턴) | 1개 | 8% |
| `useMutation` 사용 | **0개** | 0% |
| `invalidateQueries` 사용 | **0개** | 0% |

### 1-2. staleTime 설정 분포

```
["boardList", page]      →  staleTime: 5s,   gcTime: 5s   (Home.js)
["board", boardId]       →  staleTime: 5s,   gcTime: 5s   (useQueries.js)
["comments", boardId]    →  staleTime: 5s,   gcTime: 5s   (useQueries.js)
["likeStatus", boardId]  →  staleTime: 5s,   gcTime: 5s   (useQueries.js)
["boardLikes", boardId]  →  staleTime: 3s,   gcTime: 5분  (BoardLike.js ← 비일관성)
["categories"]           →  staleTime: 10분, gcTime: 5분  (CategoryNavV2.js ← gcTime < staleTime 버그!)
QueryClient defaultOptions →  staleTime: 0    gcTime: 5분  (App.js ← 설정 없음)
```

### 1-3. 핵심 문제점

#### 🔴 Critical

**① `QueryClient` defaultOptions 미설정**
`staleTime: 0` (기본값) = 컴포넌트 마운트 시마다 서버에 재요청.
탭 전환, 뒤로가기, 포커스 복귀 때마다 불필요한 API 호출 발생.

**② `useMutation` / `invalidateQueries` 전무**
댓글 작성 후 `window.location.reload()` 호출 → **전체 페이지 재로드** (캐시 전부 파기, 모든 API 재호출).
좋아요 등록/취소도 axios 직접 호출 후 지역 state 업데이트 → 다른 컴포넌트와 캐시 불일치.

**③ 레거시 `useEffect + setState` 패턴 5곳**
`PageByCategory`, `SearchPage`, `Management`, `TemporaryStorage` 가 캐싱 전혀 없이 매번 재요청.
같은 카테고리 / 같은 검색어를 반복해서 열어도 매번 API 호출.

#### 🟡 Warning

**④ `["categories"]` gcTime < staleTime 버그**
`staleTime: 10분`, `gcTime: 5분` → 데이터가 stale 되기도 전에 캐시가 GC 됨.
gcTime은 staleTime 이상이어야 함 (TanStack Query 공식 문서 권고).

**⑤ `BoardLike.js` 혼합 패턴**
`useQuery(["boardLikes"])` + `useEffect` 안에서 `getBoardLikeStatus()` 직접 호출 = 두 개의 별개 쿼리가 동시 실행.
`["likeStatus", boardId]`와 `["boardLikes", boardId]`가 사실상 같은 데이터를 중복 조회.

**⑥ 페이지네이션 시 UI 깜빡임**
페이지 버튼 클릭 → 데이터 로딩 중 빈 화면 → `placeholderData` 미적용.

---

## 2. 개선 전략 개요

```
Phase 1  QueryClient 전역 설정        (영향: 전체 앱)
Phase 2  Query Key Factory 도입       (영향: 유지보수성)
Phase 3  레거시 useEffect → useQuery  (영향: 5개 화면)
Phase 4  useMutation + invalidate     (영향: 댓글·좋아요·편집)
Phase 5  페이지네이션 placeholderData (영향: 6개 목록 화면)
Phase 6  staleTime 도메인별 최적화    (영향: 캐싱 효율)
```

---

## 3. Phase 1 — QueryClient 전역 설정

### 3-1. 문제

```js
// 현재 App.js
const queryClient = new QueryClient();  // defaultOptions 없음 → staleTime: 0
```

### 3-2. 개선안

```js
// App.js
const queryClient = new QueryClient({
  defaultOptions: {
    queries: {
      staleTime:           1000 * 60 * 3,   // 3분: 기본 fresh 유지 시간
      gcTime:              1000 * 60 * 10,  // 10분: 미사용 캐시 보관 시간
      retry:               1,              // 실패 시 1회만 재시도 (기본 3회 → 감소)
      refetchOnWindowFocus: false,         // 탭 전환 시 자동 재요청 OFF
      refetchOnReconnect:   true,          // 네트워크 재연결 시 재요청 ON
    },
  },
});
```

### 3-3. 기대 효과

| 상황 | 현재 | 개선 후 |
|---|---|---|
| 탭 전환 후 복귀 | API 재호출 | 캐시 반환 (3분 내) |
| 뒤로가기 | API 재호출 | 캐시 반환 |
| 같은 페이지 재방문 (3분 내) | API 재호출 | 캐시 반환 |
| 실패 시 재시도 | 3회 | 1회 |

---

## 4. Phase 2 — Query Key Factory 도입

### 4-1. 문제

queryKey가 각 파일에 흩어져 있어 오타·불일치 위험.
`["boardLikes", boardId]` vs `["likeStatus", boardId]` 혼재.

### 4-2. 개선안

`src/services/queryKeys.js` 신규 생성:

```js
// src/services/queryKeys.js
export const queryKeys = {
  // 게시글
  boards: {
    all:      ()          => ['boards'],
    lists:    ()          => [...queryKeys.boards.all(), 'list'],
    list:     (page)      => [...queryKeys.boards.lists(), { page }],
    details:  ()          => [...queryKeys.boards.all(), 'detail'],
    detail:   (id)        => [...queryKeys.boards.details(), id],
    likes:    (id)        => [...queryKeys.boards.detail(id), 'likes'],
    likeStatus: (id)      => [...queryKeys.boards.detail(id), 'likeStatus'],
  },
  // 카테고리
  categories: {
    all:      ()          => ['categories'],
    list:     ()          => [...queryKeys.categories.all(), 'list'],
    boards:   (name, page) => [...queryKeys.categories.all(), name, { page }],
  },
  // 댓글
  comments: {
    all:      ()          => ['comments'],
    list:     (boardId)   => [...queryKeys.comments.all(), boardId],
    children: (parentId)  => [...queryKeys.comments.all(), 'children', parentId],
  },
  // 검색
  search: {
    all:      ()               => ['search'],
    results:  (type, contents, page) => [...queryKeys.search.all(), { type, contents, page }],
  },
  // 관리자
  admin: {
    boards:   (page) => ['admin', 'boards', { page }],
    trash:    (page) => ['admin', 'trash', { page }],
  },
};
```

### 4-3. 사용 예시

```js
// 이전: 문자열 리터럴 하드코딩
useQuery({ queryKey: ["board", boardId], ... })

// 이후: 팩토리 함수 사용
useQuery({ queryKey: queryKeys.boards.detail(boardId), ... })

// invalidate 시 계층적 무효화 가능
queryClient.invalidateQueries({ queryKey: queryKeys.boards.details() })
// → boards > detail > * 전체 무효화
```

---

## 5. Phase 3 — 레거시 useEffect → useQuery 마이그레이션

### 5-1. 대상 파일

| 파일 | API 함수 | queryKey |
|---|---|---|
| `PageByCategory.js` | `getCategoryOfBoardList(name, page)` | `queryKeys.categories.boards(name, page)` |
| `SearchPage.js` | `getSearchedBoardList(type, contents, page)` | `queryKeys.search.results(type, contents, page)` |
| `Management.js` | `getBoardList(page)` | `queryKeys.admin.boards(page)` |
| `TemporaryStorage.js` | `getDeletedBoards(page)` | `queryKeys.admin.trash(page)` |

### 5-2. PageByCategory.js 변환 예시

```js
// ❌ 현재
const [boards, setBoards] = useState([]);
const [pageCount, setPageCount] = useState(0);

useEffect(() => {
  getCategoryOfBoardList(categoryName, page)
    .then((data) => {
      setBoards(data.list);
      setPageCount(data.totalPage);
    })
    .catch(() => {});
}, [categoryName, page]);

// ✅ 개선 후
const { data, isPending, error } = useQuery({
  queryKey: queryKeys.categories.boards(categoryName, page),
  queryFn: () => getCategoryOfBoardList(categoryName, page),
  enabled: !!categoryName,
  placeholderData: keepPreviousData,   // Phase 5 - 페이지 전환 시 이전 데이터 유지
});

if (isPending) return <LoadingSpinner />;
if (error)    return <ErrorMessage message={error.message} />;
// data.list, data.totalPage 바로 사용
```

### 5-3. 캐싱 효과

```
예시: "Java" 카테고리 → 1페이지 → 2페이지 → 다시 1페이지

현재:  API ×4회 (매 전환마다 요청)
개선:  API ×2회 (1페이지 캐시 적중 → 재사용)
       → 동일 파라미터 staleTime 내 재방문 시 API 0회
```

---

## 6. Phase 4 — useMutation + invalidateQueries

### 6-1. 댓글 작성 (Comment.js / CommentForm.js)

```js
// ❌ 현재
addComment(boardId, commentData)
  .then(() => window.location.reload())  // 전체 페이지 리로드!

// ✅ 개선 후
const queryClient = useQueryClient();

const addCommentMutation = useMutation({
  mutationFn: ({ boardId, commentData }) => addComment(boardId, commentData),
  onSuccess: (_, { boardId }) => {
    // 댓글 목록 캐시만 무효화 → 댓글 API 1회 재호출, 나머지 캐시 유지
    queryClient.invalidateQueries({
      queryKey: queryKeys.comments.list(boardId),
    });
  },
});
```

**페이지 리로드 제거 효과**

| | 현재 (`window.location.reload()`) | 개선 후 |
|---|---|---|
| 재호출 API | 전체 (board, comments, likes, categories, visitors...) | comments 1개만 |
| 캐시 상태 | 전부 파기 | 나머지 유지 |
| UX | 화면 전체 깜빡임 | 댓글 목록만 갱신 |

### 6-2. 좋아요 (BoardLike.js)

```js
// ❌ 현재: useQuery + useEffect 혼합 + setState
const boardLikes = useQuery({ queryKey: ["boardLikes", boardId], ... });
useEffect(() => {
  if (!boardLikes.isLoading) setLikeCount(boardLikes.data);
  if (isLoggedIn) getBoardLikeStatus(boardId).then(...); // 추가 axios 직접 호출
}, [...]);

// ✅ 개선 후: 두 쿼리 통합 + useMutation
const { data: likeInfo } = useQuery({
  queryKey: queryKeys.boards.likes(boardId),
  queryFn: () => getBoardLikeInfo(boardId),   // { count, isLiked } 반환하도록 API 통합 or 별개 유지
  enabled: true,
});

const toggleLikeMutation = useMutation({
  mutationFn: (isCurrentlyLiked) =>
    isCurrentlyLiked ? cancelBoardLike(boardId) : addBoardLike(boardId),
  onSuccess: (newCount) => {
    // 캐시를 서버 응답값으로 즉시 업데이트 (재요청 없음)
    queryClient.setQueryData(queryKeys.boards.likes(boardId), (old) => ({
      ...old,
      count: newCount,
      isLiked: !old.isLiked,
    }));
  },
});
```

### 6-3. 게시글 편집 / 삭제 (BoardEditForm.js)

```js
// ✅ editBoard mutation
const editMutation = useMutation({
  mutationFn: ({ boardId, board, html }) => editBoard(boardId, board, html),
  onSuccess: (data, { boardId }) => {
    // 수정된 게시글 캐시 무효화
    queryClient.invalidateQueries({ queryKey: queryKeys.boards.detail(boardId) });
    // 목록도 제목이 바뀌었을 수 있으니 무효화
    queryClient.invalidateQueries({ queryKey: queryKeys.boards.lists() });
    navigate(`/management/boards/${data}`);
  },
});

// ✅ deleteBoard mutation
const deleteMutation = useMutation({
  mutationFn: (boardId) => deleteBoard(boardId),
  onSuccess: () => {
    queryClient.invalidateQueries({ queryKey: queryKeys.boards.lists() });
    queryClient.invalidateQueries({ queryKey: queryKeys.admin.boards() });
    navigate(-1);
  },
});
```

### 6-4. 자식 댓글 조회 (Comment.js)

```js
// ❌ 현재: 버튼 클릭 시마다 axios 직접 호출 (캐싱 없음)
const handleShowChild = (parentId) => {
  getChildComments(parentId).then((data) => setChild(data));
};

// ✅ 개선 후: useQuery + enabled 토글
const [showReply, setShowReply] = useState(false);

const { data: children } = useQuery({
  queryKey: queryKeys.comments.children(comment.id),
  queryFn: () => getChildComments(comment.id),
  enabled: showReply,   // 펼칠 때만 요청, 이후 캐시 재사용
  staleTime: 30 * 1000,
});
```

---

## 7. Phase 5 — 페이지네이션 placeholderData

### 7-1. 문제

페이지 버튼 클릭 → queryKey 변경 → 새 데이터 로딩 중 빈 화면 또는 스피너.

### 7-2. 개선안

```js
import { useQuery, keepPreviousData } from '@tanstack/react-query';

// 모든 목록 쿼리에 placeholderData 추가
const { data, isPlaceholderData } = useQuery({
  queryKey: queryKeys.boards.list(page),
  queryFn: () => getBoardList(page),
  placeholderData: keepPreviousData,  // 이전 페이지 데이터 유지
});

// isPlaceholderData: true인 동안 로딩 인디케이터만 옅게 표시
return (
  <div style={{ opacity: isPlaceholderData ? 0.6 : 1 }}>
    <BoardList boards={data?.list ?? []} />
    <PageButton pageCount={data?.totalPage ?? 0} />
  </div>
);
```

### 7-3. 적용 대상

- `Home.js`
- `PageByCategory.js`
- `SearchPage.js`
- `Management.js`
- `TemporaryStorage.js`

---

## 8. Phase 6 — staleTime 도메인별 최적화

### 8-1. 데이터 특성 분석

| 데이터 | 변경 빈도 | 권장 staleTime | 권장 gcTime |
|---|---|---|---|
| 카테고리 목록 | 매우 낮음 (관리자만 변경) | **30분** | **60분** |
| 게시글 목록 | 낮음 (새 글 가끔) | **5분** | **15분** |
| 게시글 상세 | 낮음 (수정 드뭄) | **5분** | **15분** |
| 좋아요 수 | 중간 | **1분** | **5분** |
| 좋아요 여부 (본인) | 낮음 (본인만 변경) | **5분** | **10분** |
| 댓글 목록 | 중간 (타인 댓글 추가 가능) | **30초** | **5분** |
| 자식 댓글 | 중간 | **30초** | **5분** |
| 검색 결과 | 중간 | **2분** | **10분** |
| 관리자 게시글 목록 | 낮음 | **3분** | **10분** |

### 8-2. 현재 vs 개선 비교

```
현재:  ["categories"] staleTime=10분, gcTime=5분   ← gcTime < staleTime 버그
개선:  ["categories"] staleTime=30분, gcTime=60분

현재:  ["board", id] staleTime=5s, gcTime=5s       ← 거의 캐싱 없음
개선:  ["boards", "detail", id] staleTime=5분, gcTime=15분

현재:  ["comments", id] staleTime=5s              ← 5초마다 재요청
개선:  ["comments", id] staleTime=30s, gcTime=5분
```

### 8-3. useQueries.js 수정안

```js
// src/hooks/useQueries.js
import { useQuery } from '@tanstack/react-query';
import { queryKeys } from '../services/queryKeys';

const STALE = {
  board:    5  * 60 * 1000,   // 5분
  comments: 30 * 1000,        // 30초
  likes:    60 * 1000,        // 1분
};
const GC = {
  board:    15 * 60 * 1000,  // 15분
  comments: 5  * 60 * 1000,  // 5분
  likes:    5  * 60 * 1000,  // 5분
};

export const useBoardQuery = (boardId) =>
  useQuery({
    queryKey:            queryKeys.boards.detail(boardId),
    queryFn:             () => getBoard(boardId),
    staleTime:           STALE.board,
    gcTime:              GC.board,
    enabled:             !!boardId,
  });

export const useCommentsQuery = (boardId) =>
  useQuery({
    queryKey:            queryKeys.comments.list(boardId),
    queryFn:             () => getComments(boardId),
    staleTime:           STALE.comments,
    gcTime:              GC.comments,
    enabled:             !!boardId,
  });

export const useLikeStatusQuery = (boardId, isLoggedIn) =>
  useQuery({
    queryKey:            queryKeys.boards.likeStatus(boardId),
    queryFn:             () => getBoardLikeStatus(boardId),
    staleTime:           STALE.likes,
    gcTime:              GC.likes,
    enabled:             !!boardId && isLoggedIn,
  });
```

---

## 9. 작업 우선순위 및 예상 효과

### 우선순위표

| Phase | 난이도 | 트래픽 감소 효과 | UX 개선 효과 | 순서 |
|---|---|---|---|---|
| **Phase 1** — QueryClient 전역 설정 | 🟢 낮음 | ⭐⭐⭐⭐⭐ 매우 높음 | ⭐⭐⭐ 중간 | **1순위** |
| **Phase 2** — Query Key Factory | 🟢 낮음 | - (유지보수) | - | **2순위** |
| **Phase 6** — staleTime 최적화 | 🟢 낮음 | ⭐⭐⭐⭐ 높음 | ⭐⭐ 낮음 | **3순위** |
| **Phase 4** — useMutation (댓글) | 🟡 중간 | ⭐⭐⭐⭐⭐ 매우 높음 | ⭐⭐⭐⭐⭐ 매우 높음 | **4순위** |
| **Phase 3** — 레거시 마이그레이션 | 🟡 중간 | ⭐⭐⭐⭐ 높음 | ⭐⭐⭐ 중간 | **5순위** |
| **Phase 5** — placeholderData | 🟢 낮음 | ⭐ 낮음 | ⭐⭐⭐⭐ 높음 | **6순위** |
| **Phase 4** — useMutation (좋아요·편집) | 🟡 중간 | ⭐⭐ 낮음 | ⭐⭐⭐ 중간 | **7순위** |

### 예상 API 호출 감소 시나리오

```
시나리오: 사용자가 홈 → Java 카테고리 → 게시글 → 뒤로가기 → Java 카테고리

현재 API 호출:
  홈 진입:           boardList, categories, visitorCount     = 3회
  Java 카테고리:     categoryBoards                           = 1회
  게시글 진입:       board, comments, likeStatus              = 3회
  뒤로가기:          board, comments (재호출)                  = 2회  ← 불필요
  Java 카테고리:     categoryBoards (재호출)                   = 1회  ← 불필요
  합계: 10회

개선 후 API 호출:
  홈 진입:           boardList, categories, visitorCount     = 3회
  Java 카테고리:     categoryBoards                           = 1회
  게시글 진입:       board, comments, likeStatus              = 3회
  뒤로가기:          (캐시 적중)                               = 0회  ✅
  Java 카테고리:     (캐시 적중)                               = 0회  ✅
  합계: 7회 → 30% 감소
```

---

## 10. 주의사항

### 10-1. gcTime > staleTime 규칙
```
gcTime은 반드시 staleTime 이상으로 설정할 것.
gcTime < staleTime → 데이터가 stale 되기 전에 캐시가 삭제되는 버그.
```

### 10-2. `enabled` 옵션 활용
```js
// 로그인한 경우에만 좋아요 상태 조회
enabled: !!boardId && isLoggedIn

// 검색어가 있는 경우에만 검색 실행
enabled: !!contents && contents.length >= 1
```

### 10-3. `window.location.reload()` 완전 제거
`useMutation` + `invalidateQueries` 패턴으로 모두 대체.
현재 `Comment.js`, `CommentForm.js` 확인 필요.

### 10-4. 서버 응답 구조에 따른 queryFn 설계
API가 `{ list, totalPage }` 구조를 반환하는 경우, queryFn에서 그대로 반환하고 컴포넌트에서 구조 분해:
```js
queryFn: () => getCategoryOfBoardList(categoryName, page),
// data.list, data.totalPage 로 접근
```

---

## 11. 변경 파일 목록

```
신규 생성:
  src/services/queryKeys.js                    ← Query Key Factory

수정:
  src/App.js                                   ← QueryClient defaultOptions
  src/hooks/useQueries.js                      ← queryKeys 적용, staleTime 조정
  src/screens/Home.js                          ← placeholderData, queryKeys
  src/screens/PageByCategory.js               ← useEffect → useQuery, placeholderData
  src/screens/SearchPage.js                   ← useEffect → useQuery, placeholderData
  src/screens/Management.js                   ← useEffect → useQuery, placeholderData
  src/screens/TemporaryStorage.js             ← useEffect → useQuery, placeholderData
  src/components/Boards/BoardLike.js          ← useMutation, 혼합 패턴 제거
  src/components/Comments/CommentForm.js      ← useMutation, window.reload 제거
  src/components/Comments/Comment.js          ← useMutation, window.reload 제거, 자식 댓글 useQuery
  src/components/Boards/BoardEditForm.js      ← useMutation (edit, delete)
  src/components/Navbar/CategoryNavV2.js      ← queryKeys 적용, gcTime 수정
```
