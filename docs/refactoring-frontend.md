# 프론트엔드 리팩토링 계획

---

## RFC-FE-001: 데이터 페칭 방식 통일 (React Query)

**우선순위**: P2
**대상 파일**: `src/screens/PageByCategory.js`

### 현황
- `Home.js`: TanStack React Query 사용 (일관성 있음)
- `PageByCategory.js`: `useState` + `useEffect` + `axios` 직접 사용

### 개선 방향
`PageByCategory.js`도 React Query 기반으로 변경하여 일관성 확보.

```javascript
// 현재 방식 (PageByCategory.js)
const [boards, setBoards] = useState([]);
useEffect(() => {
  axios.get(url).then(res => setBoards(res.data));
}, [categoryName, page]);

// 개선 방향
const { data, isLoading, isError } = useQuery({
  queryKey: ['boards', 'category', categoryName, page],
  queryFn: () => getBoardListByCategory(categoryName, page),
});
```

### 기대 효과
- 캐싱 자동 처리
- 로딩/에러 상태 일관된 처리
- 코드 중복 제거

---

## RFC-FE-002: BoardForm / BoardEditor 중복 컴포넌트 통합

**우선순위**: P2
**대상 파일**: `src/components/Boards/BoardForm.js`, `src/components/Boards/BoardEditor.js`

### 현황
두 컴포넌트가 거의 동일한 기능을 중복 구현:
- Draft.js WYSIWYG 에디터
- 카테고리 선택
- 이미지 업로드
- 게시글 저장 로직

### 개선 방향
하나의 공통 `BoardFormBase` 컴포넌트로 통합하고, 작성/수정 모드를 props로 구분.

```
components/Boards/
  ├── BoardFormBase.js     ← 공통 에디터/폼 UI
  ├── BoardCreatePage.js   ← 작성 전용 (submit 로직만)
  └── BoardEditPage.js     ← 수정 전용 (기존 데이터 로드 + submit 로직)
```

---

## RFC-FE-003: React Query 캐시 설정 최적화

**우선순위**: P2
**대상 파일**: `src/hooks/useQueries.js`

### 현황
`staleTime: 5초`, `gcTime: 5초` 로 설정되어 있어 사실상 캐싱 효과가 없음.
페이지 전환 시마다 서버 재요청 발생.

```javascript
// 현재
{
  staleTime: 5 * 1000,   // 5초
  gcTime: 5 * 1000,      // 5초
}
```

### 개선 방향
콘텐츠 성격에 따라 캐시 시간 구분.

```javascript
// 게시글 상세 (자주 바뀌지 않음)
{
  staleTime: 60 * 1000,        // 1분
  gcTime: 5 * 60 * 1000,       // 5분
  refetchOnWindowFocus: false,
}

// 댓글 (실시간성 중요)
{
  staleTime: 30 * 1000,        // 30초
  gcTime: 2 * 60 * 1000,       // 2분
  refetchOnWindowFocus: true,
}

// 좋아요 상태
{
  staleTime: 10 * 1000,        // 10초
  gcTime: 60 * 1000,           // 1분
}
```

---

## RFC-FE-004: 토큰 갱신 로직 커스텀 훅 분리

**우선순위**: P2
**대상 파일**: `src/App.js`

### 현황
`App.js`의 `useEffect` 내에 토큰 유효성 검증 및 갱신 로직이 직접 작성됨.
컴포넌트 역할이 섞여 있음 (라우팅 + 인증 처리).

### 개선 방향
`useTokenRefresh` 커스텀 훅으로 분리.

```javascript
// src/hooks/useTokenRefresh.js
export const useTokenRefresh = () => {
  const dispatch = useDispatch();
  const accessToken = useSelector(state => state.user.accessToken);

  useEffect(() => {
    if (!accessToken) return;
    // 토큰 유효성 검사 및 갱신 로직
  }, [accessToken]);
};

// App.js에서는 훅만 호출
const App = () => {
  useTokenRefresh();
  return <RouterProvider ... />;
};
```

---

## RFC-FE-005: 에러 처리 개선 (alert → Toast)

**우선순위**: P3
**대상 파일**: `src/App.js`, 각종 API 호출 파일

### 현황
오류 발생 시 브라우저 기본 `alert()` 사용.

```javascript
// 현재
alert("토큰 갱신에 실패하였습니다. 재로그인 해주세요.");
```

### 개선 방향
React 기반 Toast 알림으로 교체.

```bash
npm install react-toastify
```

```javascript
// 개선 후
import { toast } from 'react-toastify';
toast.error("세션이 만료되었습니다. 다시 로그인해주세요.");
```

또는 자체 Toast 컴포넌트 구현 (외부 라이브러리 최소화 방침이라면).

---

## RFC-FE-006: apiConfig.js 구조 개선

**우선순위**: P3
**대상 파일**: `src/apiConfig.js`

### 현황
- 일부는 상수(문자열), 일부는 함수 형태로 혼재
- API 버전별 엔드포인트가 같은 파일에 나열됨
- 의도가 불명확한 API 버전들 (v5, v6, v7)

```javascript
// 현재 (혼재된 방식)
export const BOARD_GET = (boardId) => `/api/v7/boards/${boardId}`;  // 함수
export const BOARD_LIST = '/api/v1/boards';                         // 상수
```

### 개선 방향
도메인별로 분리하고 일관된 형태로 통일.

```javascript
// src/apiConfig.js
const API_BASE = '/api';

export const boardApi = {
  list: (page) => `${API_BASE}/v1/boards?p=${page}`,
  detail: (boardId) => `${API_BASE}/v1/boards/${boardId}`,
  create: `${API_BASE}/v1/boards`,
  update: (boardId) => `${API_BASE}/v1/boards/${boardId}`,
  delete: (boardId) => `${API_BASE}/v1/boards/${boardId}`,
};

export const categoryApi = {
  list: `${API_BASE}/v1/categories`,
  create: `${API_BASE}/v1/categories`,
  delete: (id) => `${API_BASE}/v1/categories/${id}`,
};
```

---

## RFC-FE-007: ErrorBoundary 개선

**우선순위**: P3
**대상 파일**: `src/components/ErrorBoundary.js`

### 현황
- 에러 발생 시 단순 에러 메시지만 표시
- 개발/프로덕션 환경 구분 없음
- 에러 로깅 없음

### 개선 방향
```javascript
componentDidCatch(error, info) {
  // 개발 환경: 상세 에러 출력
  if (process.env.NODE_ENV === 'development') {
    console.error('Error details:', error, info);
  }
  // 프로덕션: 에러 리포팅 서비스 전송 (Sentry 등)
  // Sentry.captureException(error);
}
```

---

## RFC-FE-008: 로딩 스피너 오타 수정

**우선순위**: P4
**대상 파일**: 다수

### 현황
여러 파일에서 `"Loding..."` 오타 사용.

```javascript
// 현재
if (isLoading) return <div>Loding...</div>;

// 수정
if (isLoading) return <div>Loading...</div>;
```

또는 공통 `LoadingSpinner` 컴포넌트 제작 권장.

---

## RFC-FE-009: 공통 로딩/에러 컴포넌트 제작

**우선순위**: P3

### 개선 방향
```
components/
  ├── LoadingSpinner.js    ← 공통 로딩 UI
  ├── ErrorMessage.js      ← 기존 (활용)
  └── EmptyState.js        ← 데이터 없음 상태 UI
```

각 페이지에서 중복으로 작성하던 로딩/에러 처리를 공통 컴포넌트로 통일.

---

## 작업 진행 체크리스트

- [ ] RFC-FE-001: PageByCategory.js React Query 방식으로 전환
- [ ] RFC-FE-002: BoardForm / BoardEditor 통합
- [ ] RFC-FE-003: React Query 캐시 설정 최적화
- [ ] RFC-FE-004: 토큰 갱신 로직 훅 분리
- [ ] RFC-FE-005: alert → Toast 에러 처리 개선
- [ ] RFC-FE-006: apiConfig.js 구조 개선
- [ ] RFC-FE-007: ErrorBoundary 개선
- [ ] RFC-FE-008: 로딩 오타 수정
- [ ] RFC-FE-009: 공통 로딩/에러 컴포넌트 제작
