# BoardList 캐시 영속화 최적화

**작성일**: 2026-03-23
**대상**: `myblog-boot` frontend — BoardList 불필요 API 요청 개선

---

## 배경

블로그를 운영하다 보니 신경 쓰이는 부분이 하나 있었다. 페이지를 새로고침할 때마다 게시글 목록 API를 다시 호출하는 것이었다. TanStack Query를 도입하면서 `staleTime: 5분`을 설정해 두었고, "이 정도면 재요청을 줄일 수 있겠다"고 생각했는데 실제로는 새로고침할 때마다 요청이 나가고 있었다. 왜 그런지 원인을 파악하고, 어떻게 해결했는지 기록으로 남긴다.

---

## 문제 원인 분석

### TanStack Query 캐시는 메모리에만 존재한다

TanStack Query의 캐시는 기본적으로 **JavaScript 런타임 메모리(힙)** 에만 저장된다. `staleTime`과 `gcTime`은 캐시의 신선도와 수명을 제어하지만, 브라우저 새로고침(F5) 또는 탭 닫기·열기 시 JavaScript 런타임이 초기화되면서 캐시 전체가 사라진다.

```
staleTime: 5분  → 5분 이내 재요청 없음 (같은 세션 내에서만)
gcTime:   15분  → 마지막 구독자가 사라진 후 15분 뒤 캐시 GC
```

즉, 같은 탭 안에서 5분 내에 페이지를 이동했다가 돌아오는 경우에는 캐시가 동작한다. 하지만 **새로고침 한 번이면 캐시는 완전히 초기화**되고, 캐시가 존재하지 않으므로 `staleTime`도 의미가 없어진다.

### 적용 전 코드

```js
// src/screens/Home.js
const { data, isPending, error, isPlaceholderData } = useQuery({
  queryKey: queryKeys.boards.list(page.toString()),
  queryFn:  () => getBoardList(page),
  staleTime: 5  * 60 * 1000,   // 5분
  gcTime:    15 * 60 * 1000,   // 15분
  placeholderData: keepPreviousData,
});
```

설정은 되어 있지만, 새로고침 시 캐시가 없으니 `staleTime`은 동작하지 않는다.

### refetchOnWindowFocus 기본값 문제

TanStack Query는 `refetchOnWindowFocus`의 기본값이 `true`다. 다른 탭을 보다가 블로그로 돌아오면, 해당 쿼리의 데이터가 stale 상태인 경우 자동으로 백그라운드 재요청이 발생한다. 블로그 특성상 게시글이 그 사이에 바뀌는 일은 거의 없으므로 이 동작은 불필요한 트래픽을 만들 뿐이다.

---

## 해결 방향 검토

세 가지 선택지를 검토했다.

| 방식 | 특징 | 적합성 |
|---|---|---|
| **SSE (Server-Sent Events)** | 게시글 작성 시 서버 → 클라이언트 푸시, 즉시 갱신 | 복잡도 대비 효용 낮음. 개인 블로그에서 게시글 작성은 하루에 몇 번 없음 |
| **HTTP ETag / 304 Not Modified** | 변경 없으면 응답 바디 없이 304 반환, 대역폭 절약 | 네트워크 요청 자체는 줄지 않음. 백엔드 추가 작업 필요 |
| **persistQueryClient + localStorage** | 캐시를 localStorage에 저장, 새로고침 후에도 복원 | 구현 간단, 요청 자체를 없앰, TanStack Query 공식 지원 |

결론적으로 **`persistQueryClient` + `createSyncStoragePersister`** 방식을 선택했다. 가장 간단하면서도 새로고침 후 요청 자체를 없앨 수 있고, 게시글 작성·수정·삭제 mutation에서 `invalidateQueries`로 캐시를 무효화하면 "언제 최신화하는가"의 문제도 명확하게 제어할 수 있다.

---

## 동작 원리

### persistQueryClient란

`@tanstack/react-query-persist-client`는 TanStack Query의 공식 플러그인으로, QueryClient의 캐시 상태를 외부 스토리지(localStorage, AsyncStorage 등)에 직렬화(dehydrate)해 저장하고, 앱 초기화 시 역직렬화(hydrate)해 복원한다.

```
[첫 방문]
  useQuery 실행 → API 요청 → 캐시 저장(메모리) → localStorage에 직렬화 저장

[새로고침 후]
  앱 초기화 → localStorage에서 캐시 복원(hydrate) → staleTime 이내면 API 요청 없음
                                                    → staleTime 초과면 백그라운드 재요청
```

### maxAge와 gcTime의 관계

`persistQueryClient`에는 `maxAge` 옵션이 있다. 저장된 캐시가 이 시간을 초과하면 복원하지 않고 파기한다. 기본값은 **24시간**이다.

여기서 중요한 점이 하나 있다. **`gcTime`은 `maxAge`보다 크거나 같아야 한다.** `gcTime`이 `maxAge`보다 짧으면, 캐시가 메모리에서 GC로 제거될 때 persist 플러그인이 그 변경(쿼리 삭제)을 즉시 localStorage에 동기화하기 때문에, localStorage에 저장해 둔 데이터까지 함께 지워진다. 결국 maxAge가 아직 남아 있어도 이미 localStorage에 데이터가 없는 상태가 된다.

```js
// gcTime < maxAge → GC로 메모리에서 제거 → 그 변경이 localStorage에 동기화 → 저장된 캐시도 삭제 (X)
// gcTime >= maxAge → maxAge 만료 전까지 메모리·localStorage 모두 유지 (O)
```

---

## 적용 방법

### 패키지 설치

```bash
npm install @tanstack/react-query-persist-client @tanstack/query-sync-storage-persister
```

### QueryClient 설정 변경

`gcTime`을 24시간으로 상향하고, `staleTime`을 10분으로 늘린다.
`refetchOnWindowFocus`는 전역으로 비활성화한다.

```js
// src/App.js (또는 QueryClient 생성 위치)
const queryClient = new QueryClient({
  defaultOptions: {
    queries: {
      staleTime: 10 * 60 * 1000,    // 10분 — 이 시간 안에는 캐시를 그대로 사용
      gcTime:    24 * 60 * 60 * 1000, // 24시간 — maxAge와 맞춤
      refetchOnWindowFocus: false,   // 탭 전환 시 자동 재요청 비활성화
    },
  },
});
```

### PersistQueryClientProvider로 교체

기존 `QueryClientProvider`를 `PersistQueryClientProvider`로 교체한다.

게시글 본문 HTML처럼 용량이 큰 데이터가 localStorage에 쌓이지 않도록, `dehydrateOptions.shouldDehydrateQuery`로 board list와 category 쿼리만 선별해서 저장한다.

```js
// src/App.js
import { PersistQueryClientProvider } from '@tanstack/react-query-persist-client';
import { createSyncStoragePersister } from '@tanstack/query-sync-storage-persister';

const PERSIST_MAX_AGE = 1000 * 60 * 60 * 24; // 24시간

const persister = createSyncStoragePersister({
  storage: window.localStorage,
});

// QueryClientProvider → PersistQueryClientProvider 교체
<PersistQueryClientProvider
  client={queryClient}
  persistOptions={{
    persister,
    maxAge: PERSIST_MAX_AGE,
    dehydrateOptions: {
      // 게시글 목록·카테고리만 localStorage에 저장
      // 게시글 본문 HTML·댓글·좋아요 등 대용량 or 변동성 높은 데이터는 제외
      shouldDehydrateQuery: (query) => {
        const key = query.queryKey;
        if (key[0] === 'boards' && key[1] === 'list') return true;
        if (key[0] === 'categories') return true;
        return false;
      },
    },
  }}
>
  <App />
</PersistQueryClientProvider>
```

### 게시글 작성·수정·삭제 시 invalidateQueries 연결

캐시를 영속화하면 "새 게시글을 써도 목록이 안 바뀐다"는 문제가 생길 수 있다. mutation 완료 후 게시글 목록 쿼리를 무효화하면 해결된다.

```js
// 게시글 작성 mutation 예시
const uploadMutation = useMutation({
  mutationFn: ({ formData, htmlString }) => uploadBoard(formData, htmlString),
  onSuccess: () => {
    // 게시글 목록 전체 무효화 → 다음 방문 시 fresh 데이터 요청
    queryClient.invalidateQueries({ queryKey: queryKeys.boards.lists() });
    queryClient.invalidateQueries({ queryKey: queryKeys.categories.all() });
  },
});
```

`invalidateQueries`를 호출하면 해당 키의 캐시가 stale로 표시된다. localStorage에 저장된 캐시도 함께 갱신되므로, 다음 방문 시 fresh 데이터를 요청하게 된다.

---

## 효과 정리

| 상황 | 적용 전 | 적용 후 |
|---|---|---|
| 같은 세션 내 5분 이내 재방문 | 요청 없음 (기존 동작) | 요청 없음 |
| 같은 세션 내 5~10분 사이 재방문 | 재요청 발생 | 요청 없음 (staleTime 10분) |
| **F5 새로고침 후 10분 이내** | **재요청 발생** | **요청 없음 (localStorage 복원)** |
| 탭 전환 후 돌아올 때 | stale이면 재요청 발생 | 재요청 없음 |
| 게시글 작성·수정·삭제 후 | 자동 갱신 안 됨 | invalidate로 즉시 갱신 |
| 24시간 이후 방문 | 재요청 발생 | 재요청 발생 (maxAge 만료) |

---

## 주의할 점

**localStorage 용량**: localStorage는 브라우저당 보통 5~10MB를 제공한다. 게시글 목록(제목, 날짜, 요약 텍스트)은 수백 개 단위라도 수백 KB 이내이므로 충분하다. 단, 전체 본문을 포함한 상세 페이지 캐시까지 쌓이면 용량을 초과할 수 있다. `dehydrateOptions`로 캐시에 저장할 쿼리 키를 선별적으로 지정하는 것도 방법이다.

**SSR 환경**: 이 블로그는 CSR(CRA → Vite) 구조이므로 `window.localStorage`를 직접 사용해도 무방하다. Next.js처럼 서버에서 렌더링하는 환경이라면 `window` 접근 시 `typeof window !== 'undefined'` 가드가 필요하다.

---

## 마치며

사실 `staleTime`만 설정해 두면 캐시가 잘 동작할 것이라고 생각했는데, TanStack Query 캐시가 인메모리라는 사실을 간과하고 있었다. `staleTime`은 "이미 캐시가 있을 때 재요청을 막는" 옵션이지, "캐시를 영속화하는" 옵션이 아니다. 두 가지는 다르다.

`persistQueryClient`는 캐시를 localStorage에 직렬화해 저장하는 역할이고, `staleTime`은 그 캐시가 얼마나 신선한 것으로 취급될지를 결정하는 역할이다. 두 개념을 조합하면 새로고침 후에도 캐시가 살아있고, staleTime 이내라면 요청이 발생하지 않는다.

개인 블로그는 게시글 작성 빈도가 낮고 독자 입장에서 실시간 갱신이 크게 중요하지 않다. SSE 같은 복잡한 구조를 도입하기보다, 이처럼 클라이언트 캐시를 잘 활용하는 것이 지금 단계에서 가장 합리적인 선택이라고 판단했다.
