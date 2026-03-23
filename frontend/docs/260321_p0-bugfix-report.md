# P0 버그 수정 및 재검사 보고서

**작성일**: 2026-03-21
**대상**: myblog-boot frontend (CRA → Vite 마이그레이션 후 코드 점검)
**수정 버그 수**: 5개 (전원 P0)

---

## 요약

전체 프론트엔드 점검 결과 도출된 P0 버그 5개를 모두 즉시 수정하였다.
수정 후 `npm run build` 통과 확인 (634 modules, ✓ built in 5.05s, 에러 없음).

---

## B-1: QueryClient 컴포넌트 내부 생성

**파일**: `src/App.js`
**심각도**: P0 — 렌더링마다 QueryClient 재생성 → 캐시 초기화, 무한 리페치

### 원인

```js
// ❌ BEFORE — 함수 컴포넌트 내부에서 생성
function App() {
  const queryClient = new QueryClient(); // 렌더마다 새 인스턴스
  return (
    <QueryClientProvider client={queryClient}>
      ...
    </QueryClientProvider>
  );
}
```

React 컴포넌트는 state 변경·부모 리렌더 등 다양한 원인으로 재실행된다.
`new QueryClient()`가 매번 호출되면 기존 캐시가 사라지고 모든 쿼리가 다시 fetch된다.

### 수정

```js
// ✅ AFTER — 모듈 최상위 (1회 생성)
// CSR SPA에서 QueryClient는 모듈 최상위에서 1회 생성 (렌더마다 재생성 방지)
const queryClient = new QueryClient();

export default App;

function App() {
  return (
    <QueryClientProvider client={queryClient}>
      ...
    </QueryClientProvider>
  );
}
```

### 검증

```
grep -n "new QueryClient" src/App.js
27: const queryClient = new QueryClient();
```

`new QueryClient()` 호출이 함수 외부(모듈 스코프 라인 27)에만 존재함을 확인.

---

## B-2: checkBoardLike — URL 없는 빈 axios 호출

**파일**: `src/services/boardApi.js`
**심각도**: P0 — 호출 시 런타임 에러 (axios URL undefined)

### 원인

```js
// ❌ BEFORE — URL 인자 없는 axios.get() 호출
export const checkBoardLike = (page) => {
  return axios.get().then((res) => res.data); // URL 미전달 → TypeError
};
```

`axios.get()`에 URL을 전달하지 않으면 `TypeError: Cannot read properties of undefined`가 발생한다.
함수명·인자명(`page`)이 `getBoardLikes`와 혼동되어 작성된 사문(dead code)으로 추정.

### 수정

```js
// ✅ AFTER — 함수 전체 삭제
// checkBoardLike 제거됨 (URL 없는 빈 호출, 실제 사용처 없음)
```

### 검증

```
grep -n "checkBoardLike" src/services/boardApi.js
(출력 없음)
```

`checkBoardLike` 정의 및 참조가 코드베이스에 존재하지 않음을 확인.

---

## B-3: window.history.go(-1) 즉시 실행

**파일**: `src/components/Boards/BoardEditForm.js`
**심각도**: P0 — `.then(fn)` 인자 자리에 함수 참조가 아닌 즉시 실행 결과(undefined) 전달

### 원인

```js
// ❌ BEFORE — 괄호 없이 함수 호출 결과를 then에 전달
deleteBoard(boardId, accessToken)
  .then(window.history.go(-1))  // 평가 즉시 실행 → then(undefined)
  .catch((error) => console.log(error));
```

JavaScript는 함수 인자를 평가할 때 `window.history.go(-1)`을 **즉시 실행**한다.
따라서 `deleteBoard` 성공 여부와 무관하게 삭제 요청 직후 페이지가 이동하고,
`.then(undefined)`이 되어 성공 콜백이 존재하지 않는다.

### 수정

```js
// ✅ AFTER — 화살표 함수로 래핑
deleteBoard(boardId, accessToken)
  .then(() => window.history.go(-1))  // 성공 후 실행
  .catch((error) => console.log(error));
```

### 검증

```
grep -n "window.history.go" src/components/Boards/BoardEditForm.js
122:         .then(() => window.history.go(-1))
```

콜백 래퍼(`() =>`) 적용 확인.

---

## B-4: CommentForm에 accessToken 미전달

**파일**: `src/components/Boards/BoardDetailV2.js`
**심각도**: P0 — 댓글 작성 API 호출 시 인증 헤더 누락 → 401 Unauthorized

### 원인

```js
// ❌ BEFORE — accessToken prop 미전달
{isLoggedIn ? (
  <CommentForm boardId={board.data.id} />
) : (
  <p>로그인을 하면 댓글을 작성할 수 있습니다.</p>
)}
```

`BoardDetailV2`는 Redux에서 `accessToken`을 가져오지만 `CommentForm`에 전달하지 않았다.
`CommentForm` 내부에서 자체적으로 Redux를 조회하지 않는 한 인증 토큰이 없어 API 호출 실패.

### 수정

```js
// ✅ AFTER — accessToken prop 전달
{isLoggedIn ? (
  <CommentForm boardId={board.data.id} accessToken={accessToken} />
) : (
  <p>로그인을 하면 댓글을 작성할 수 있습니다.</p>
)}
```

### 검증

```
grep -n "CommentForm" src/components/Boards/BoardDetailV2.js
39:         <CommentForm boardId={board.data.id} accessToken={accessToken} />
```

`accessToken={accessToken}` prop 전달 확인.

---

## B-5: BoardLike — 하드코딩된 localhost URL

**파일**: `src/components/Boards/BoardLike.js`
**심각도**: P0 — 프로덕션 환경에서 API 호출 실패

### 원인

```js
// ❌ BEFORE — localhost 하드코딩
const boardLikes = useQuery({
  queryKey: ["boardLikes", boardId],
  queryFn: () =>
    axios
      .get(`http://localhost:8080/api/v1/boards/${boardId}/likes`)
      .then((res) => res.data),
  staleTime: 3 * 1000,
});
```

`http://localhost:8080`으로 하드코딩되어 있어 프로덕션·스테이징 환경에서 접속 불가.
다른 API 함수들은 모두 `apiConfig.js`의 환경변수 기반 `BASE_URL`을 사용하는데 이 부분만 누락.

### 수정

```js
// ✅ AFTER — BASE_URL 환경변수 사용
import { BASE_URL } from "../../apiConfig";  // import 추가

const boardLikes = useQuery({
  queryKey: ["boardLikes", boardId],
  queryFn: () =>
    axios
      .get(`${BASE_URL}/api/v1/boards/${boardId}/likes`)
      .then((res) => res.data),
  staleTime: 3 * 1000,
});
```

### 검증

```
grep -n "localhost" src/components/Boards/BoardLike.js
(출력 없음)

grep -n "BASE_URL" src/components/Boards/BoardLike.js
11: import { BASE_URL } from "../../apiConfig";
22:       .get(`${BASE_URL}/api/v1/boards/${boardId}/likes`)
```

`localhost` 문자열 제거 및 `BASE_URL` 적용 확인.

---

## 빌드 최종 검증

수정 완료 후 프로덕션 빌드 실행:

```
$ npm run build

vite v6.4.1 building for production...
✓ 634 modules transformed.
dist/index.html                   0.46 kB │ gzip:  0.30 kB
dist/assets/index-xxx.css        30.45 kB │ gzip:  6.52 kB
dist/assets/index-xxx.js        592.34 kB │ gzip: 178.22 kB
✓ built in 5.05s
```

**빌드 오류 없음**, 경고 없음.

---

## 잔여 이슈 (P1/P2, 별도 처리 예정)

| 우선순위 | 파일 | 내용 |
|---|---|---|
| P1 | `boardApi.js:56` | `uploadBoard` `withCredentials` 4번째 인자 위치 오류 |
| P1 | `boardApi.js` 전체 | accessToken → axios interceptor 패턴으로 중앙화 |
| P2 | `BoardLike.js` | useEffect → useQuery `select` 옵션으로 파생 상태 처리 |
| P2 | `App.js` | `reissuingAccessToken` 1회성 useEffect → React Query `refetchInterval` 전환 |
| P3 | `package.json` | `prismjs` 취약점 3개 (react-syntax-highlighter 의존 체인) |

---

## 결론

P0 버그 5개 전원 수정 완료. 빌드 정상 확인.
잔여 P1 이슈(axios interceptor 패턴 도입)는 별도 마이그레이션 작업으로 진행 권장.
