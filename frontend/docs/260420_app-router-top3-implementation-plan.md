# App Router 점검 Top 1~3 구현 계획서

- **작성일**: 2026-04-20
- **근거 문서**: `.claude/reviews/frontend-review-1-app-router.md` (2026-04-19)
- **작업 범위**: frontend (`-f`)
- **대상 영역**: 점검 1회차 App Router 구조 / 렌더링 전략 Top 3 우선순위
- **선행 완료**: `260419_public-route-group-consolidation-plan.md`, `260419_server-client-component-boundary-plan.md`

---

## 0. 선행 작업 현황 점검 (리뷰 시점 vs 현재)

리뷰 작성(2026-04-19) 이후 일부 항목이 이미 반영된 상태이므로, 본 계획서는 **잔여 작업**에 집중한다.

### 0-1. Top 2 — `(public)` 그룹 일관화 / `UserLayout` 중복 제거

리뷰 2026-04-19 시점 → 현재(2026-04-20)까지 아래 항목이 이미 반영 완료:

| 항목 | 리뷰 지적 | 현재 상태 |
| --- | --- | --- |
| `(public)` 그룹 범위 확장 | 홈/카테고리/검색이 그룹 밖 | ✅ `app/(public)/{page.tsx, posts/[slug], category/[name], search}` 모두 그룹 내 |
| `UserLayout.tsx` 중복 제거 | 동일 JSX 2곳 중복 | ✅ `components/layout/UserLayout.tsx` 삭제 완료 |
| `(admin)` 그룹 신설 | `app/management/` 최상위 노출 | ✅ `app/(admin)/management/` 로 격리 |
| `management/layout.tsx` Server Component화 | 전역 `"use client"` | ✅ Server Component + 본문 영역만 `<RoleGate>` Client로 분리 |
| 최상위 동적 세그먼트 변경 | `/[categoryName]` 충돌 위험 | ✅ `/category/[name]` 이동 + `next.config.ts` redirect로 구 URL 호환 |
| `/boards/[boardId]` shim 제거 | 도메인 라우트처럼 보임 | ✅ `next.config.ts` redirects 에 흡수 |
| `CommentSection` Server Component 전환 | 불필요 Client | ✅ `"use client"` 제거 완료 |
| `PostEditorDynamic.tsx` 공용 래퍼 | 코드 중복 | ✅ `components/editor/PostEditorDynamic.tsx` 추출 완료 |

**⇒ Top 2 에서 남은 잔여 작업**: **`app/sitemap.ts` 상단에 `export const revalidate = 3600;` 명시 추가** (리뷰 2-1 Medium, 1줄 보강).

### 0-2. Top 1 / Top 3

| Top | 현황 |
| --- | --- |
| Top 1 — on-demand revalidation | 🔴 **완전 미구현** (`revalidateTag`/`revalidatePath` 호출 0건, `next.tags` 0건, Route Handler 없음) |
| Top 3 — `error.tsx` / `loading.tsx` | 🔴 **완전 미구현** (`error.tsx`/`loading.tsx` 각 0개, `not-found.tsx` 는 루트 1개만) |

본 계획서의 실질 구현 대상은 **Top 1 + Top 3 + Top 2 잔여(sitemap revalidate 명시)** 이다.

---

## 1. Top 1 — On-demand Revalidation 연결

### 1-1. Problem (문제 정의)

**무엇이 문제인가?**
- 홈 `/`, 카테고리 `/category/[name]`, 상세 `/posts/[slug]` 는 모두 ISR 로 60초 주기 재생성.
- `sitemap.xml` 의 slug 목록은 `getAllSlugs()` 의 `revalidate: 3600` 에 따라 최대 1시간 stale.
- 그런데 관리자 쓰기 경로(`PostEditorClient.tsx`)는 `queryClient.invalidateQueries()` 만 호출 → **TanStack Query 캐시는 리셋되지만 Next.js ISR 캐시(fs on disk / CDN edge)는 그대로**.

**왜 중요한가?**
- 관리자가 새 글을 올려도 **다른 사용자 / 검색엔진 크롤러** 는 최대 60초 ~ 1시간 동안 구식 HTML 을 받는다.
- Next.js 마이그레이션의 원래 동기(SEO 색인 / SSR 첫 페인트) 가 **반쪽 구현**에 머무름.
- 리뷰 2-2 의 "`next.tags` 전무" 와 짝을 이루는 구조적 결손: 태그가 없어 `revalidateTag("posts")` 한 줄로 관련 페이지를 한꺼번에 무효화할 수 있는 메커니즘 자체가 봉인됨.

**해결하지 않으면?**
- SEO 측면: 신규 글이 sitemap 에 반영되는 데 최대 1시간 지연 → 색인 지연.
- UX 측면: 관리자가 "발행" 을 눌렀는데 공개 페이지가 그대로 → 이중 확인 / 새로고침 반복.
- ISR TTL 을 완화(60s → 600s) 하고 싶어도 on-demand 가 없으면 늘릴 수 없음 → 백엔드 호출 빈도 고착.

---

### 1-2. Analyze (분석 및 선택지 검토)

#### 선택지 A. `revalidateTag` 만 사용

- **장점**: 의미 단위 라벨(`posts`, `categories`) 로 여러 페이지 일괄 무효화, 유연함.
- **단점**: `sitemap.xml` / `robots.txt` 같은 **특수 파일은 tag 부여가 불가** — `MetadataRoute.Sitemap` 반환 함수는 `fetch` 가 아니라 직접 호출이므로 태그 미적용.

#### 선택지 B. `revalidatePath` 만 사용

- **장점**: URL 패턴 명확, 특수 파일(`/sitemap.xml`) 직접 지정 가능.
- **단점**: 게시글 1건 변경 시 `/posts/:slug` / `/category/:name` / `/` 세 경로를 **호출자가 일일이 명시**해야 함 → 백엔드가 프론트 라우팅을 알아야 하는 결합 발생.

#### 선택지 C. **혼합 — 태그(주력) + 경로(특수 파일 보강)** ✅ 채택

- 의미 단위는 태그로 일괄 무효화: `posts`, `post:{slug}`, `categories`, `slugs`.
- 태그 부여가 불가능한 `sitemap.xml` 은 경로로 백업 무효화: `revalidatePath("/sitemap.xml")`.
- **근거**: Next.js 공식 문서 패턴, 리뷰 2-3 개선안과 동일, 프론트/백 결합도 최소화.

#### 호출 주체 선택지

| 방식 | 장점 | 단점 | 채택 |
| --- | --- | --- | --- |
| Server Action | 같은 Next.js 앱 내 쓰기 경로면 간편 | Spring Boot 가 별도 서비스인 현 구조에선 불가 | ✗ |
| 백엔드 → Route Handler (webhook) | 프론트 배포만 갱신, 결합도 낮음 | secret 관리 필요 | ✅ |
| 프론트 직접 호출 | 단순 | 관리자 세션이 있어야만 무효화 → 크론/백오피스 불가 | ✗ |

#### 이벤트 타이밍

- **`@TransactionalEventListener(AFTER_COMMIT)`** 으로 DB 커밋 후에만 webhook 호출.
- 이유: 트랜잭션 롤백 시 캐시만 무효화되어 "없는 글" 을 stale-less 로 노출하는 상황 방지.

#### 보안

- `x-revalidate-secret` 헤더 + `process.env.REVALIDATE_SECRET` (프론트/백 공유 비밀) 검증.
- 운영: 백엔드 컨테이너에서만 호출되므로 Docker 내부 네트워크 + secret 으로 충분.
- 로컬 개발: `.env.local` 에 secret 동일 설정.

---

### 1-3. Action (구현 계획 및 설계)

#### 1-3-1. 작업 범위

- **Frontend (`-f`)**:
  1. `lib/api.ts` 모든 서버 fetch 함수에 `next.tags` 부여.
  2. `app/api/revalidate/route.ts` Route Handler 신설 (secret 검증 포함).
  3. `revalidate` TTL 완화 검토 — 본 계획서에서는 **값만 변경 없이 유지**하고 Top 1 완료 후 별도 튜닝(후속 계획서).
  4. `.env.local.example` 에 `REVALIDATE_SECRET` 항목 추가.
- **Backend (`-b`, 본 계획서 범위 밖 — 후속 계획서로 분리)**:
  - `PostChangeEvent` + `@TransactionalEventListener(AFTER_COMMIT)` + `RevalidateClient` (WebClient) 구현.
  - **본 계획서에서는 프론트 측 엔드포인트 스펙만 확정**하고 백엔드 작업은 `-b` 별도 세션에서 진행.

#### 1-3-2. 태그 설계

| 태그 | 의미 | 부여 대상 |
| --- | --- | --- |
| `posts` | 게시글 목록/상세 전체 | `getPostList`, `getPostBySlug`, `getCategoryPostList` |
| `post:{slug}` | 특정 게시글 1건 | `getPostBySlug` (동적) |
| `slugs` | sitemap 용 slug 목록 | `getAllSlugs` |
| `categories` | 카테고리 네비게이션 | `getCategoriesV2`, `getCategories` |

#### 1-3-3. 무효화 시나리오 매핑

| 이벤트 | 호출할 태그/경로 |
| --- | --- |
| 게시글 생성 | tags: `["posts", "slugs"]` + paths: `["/sitemap.xml"]` |
| 게시글 수정 | tags: `["posts", "post:{slug}"]` |
| 게시글 삭제 | tags: `["posts", "post:{slug}", "slugs"]` + paths: `["/sitemap.xml"]` |
| 카테고리 CUD | tags: `["categories"]` |

#### 1-3-4. Route Handler 스펙

```ts
// app/api/revalidate/route.ts
POST /api/revalidate

Headers:
  content-type: application/json
  x-revalidate-secret: <REVALIDATE_SECRET>

Body:
  {
    "tags"?: string[]   // 예: ["posts", "post:hello-world"]
    "paths"?: string[]  // 예: ["/sitemap.xml"]
  }

Response (200):
  { "ok": true, "tags": string[], "paths": string[], "revalidatedAt": number }

Response (401):
  { "ok": false, "error": "invalid secret" }

Response (400):
  { "ok": false, "error": "tags or paths required" }
```

#### 1-3-5. 변경 대상 파일 목록

| 파일 | 변경 내용 | 라인 변동 |
| --- | --- | --- |
| `frontend/lib/api.ts` | 6개 함수에 `next.tags` 추가 + 정책 주석 블록 | +~15 |
| `frontend/app/api/revalidate/route.ts` | **신규** Route Handler | +50 |
| `frontend/.env.local.example` | `REVALIDATE_SECRET` 추가 | +1 |
| `frontend/docs/260420_app-router-top3-implementation-plan.md` | 본 문서 | +N |

> 백엔드 측 webhook 호출자 구현은 `-b` 별도 계획서에서 다룬다. 본 계획서는 **프론트 엔드포인트가 `-b` 작업의 contract** 가 된다.

#### 1-3-6. 트레이드오프

| 트레이드오프 | 선택 |
| --- | --- |
| 세밀함(tag 다수) vs 단순함 | 4개 태그 + 개별 slug 태그 — 유지보수 비용 낮고 무효화 범위 명확 |
| 동기 webhook vs 비동기 큐 | 현 규모(일 쓰기 수 건)는 동기로 충분. 큐는 YAGNI |
| fire-and-forget vs 실패 시 재시도 | 백엔드에서 1회 재시도 + 실패 로그만 — 캐시는 어차피 TTL 이 있음 |

#### 1-3-7. 예상 이슈 및 대응

| 이슈 | 대응 |
| --- | --- |
| secret 유출 | 환경변수로만 주입, 커밋 금지(`.env.local.example` 에 placeholder 만) |
| 과도한 무효화 폭증 | tag 를 `posts` 같은 광범위 레벨로 묶었으므로 호출 빈도 자체가 낮음. 필요 시 debounce 는 백엔드 |
| Route Handler 의 fetch 옵션 누락 | `dynamic = "force-dynamic"` + `runtime = "nodejs"` 명시 |
| 로컬 개발 시 백엔드에서 호출 실패 | 백엔드 로거만 남기고 프론트는 영향 없음 |
| `getAllSlugs` 내부 `fetch` 에 태그가 없어 `sitemap.xml` 이 sync 안 됨 | `getAllSlugs` 에 tag 추가 + `/sitemap.xml` 경로 무효화 병행 |

---

### 1-4. Result (검증 계획)

#### 검증 시나리오

1. **단위**: `curl -X POST http://localhost:3000/api/revalidate -H "x-revalidate-secret: ..." -d '{"tags":["posts"]}'` → `{ ok: true }` 응답 확인.
2. **secret 오검증**: 잘못된 secret → 401.
3. **빈 body**: → 400.
4. **태그 무효화 E2E** (백엔드 webhook 연동 후 별도 세션):
   - 홈 접속 → HTML 캐시 확인 (x-nextjs-cache: HIT).
   - 관리자 로그인 → 신규 글 발행.
   - 홈 재접속 → 60초 이내라도 **새 글이 즉시 노출**.
5. **sitemap 무효화**: `/sitemap.xml` 에 신규 slug 가 1시간 내 반영 → on-demand 로 즉시.

#### 성공 기준

- [ ] Route Handler 가 200/401/400 을 정상 반환.
- [ ] `lib/api.ts` 6개 함수 모두 `next.tags` 보유.
- [ ] 수동 curl 테스트 3종 통과.
- [ ] `REVALIDATE_SECRET` 이 `.env.local.example` 에 명시됨.

#### 모니터링

- Route Handler 에 간단 로그 (`console.info("[revalidate] tags=%o paths=%o"`, ...)) 추가 — 운영에서 호출 빈도 추적.

---

## 2. Top 2 (잔여) — `sitemap.ts` revalidate 명시

### 2-1. Problem

`app/sitemap.ts` 는 내부적으로 `getAllSlugs(3600)` 을 호출해 사실상 1시간 캐시되지만, **파일 상단에 `export const revalidate = N` 가 없어** 동작이 구두 계약에 의존. 향후 유지보수자가 TTL 을 오해하기 쉽다.

### 2-2. Analyze

- Next.js 의 `sitemap.ts` 는 **Route Segment Config** 를 지원 (`export const revalidate`).
- `getAllSlugs` 의 fetch 는 자체 `revalidate: 3600` 을 가지지만, 이는 **fetch 캐시** TTL. sitemap 라우트 자체의 재생성 주기와 별개라 2겹으로 명시하는 것이 의도를 분명히 한다.
- 다만 중복 명시가 혼란을 줄 수도 있으므로 **fetch TTL 과 동일한 3600 을 sitemap 라우트에도 명시** + **주석으로 의도 설명**.

### 2-3. Action

- `app/sitemap.ts` 최상단에 다음 추가:
  ```ts
  // sitemap 자체를 1시간 단위로 재생성. 신규 글 즉시 반영은 on-demand (revalidatePath("/sitemap.xml")) 가 담당.
  export const revalidate = 3600;
  ```
- 라인 변동: +2

### 2-4. Result

- 빌드 성공 (`npm run build`) 확인.
- `.next/server/app/sitemap.xml.body` 재생성 주기 변화 없음 — 회귀 없음.

---

## 3. Top 3 — `error.tsx` / `loading.tsx` 특수 파일 도입

### 3-1. Problem (문제 정의)

**무엇이 문제인가?**
- `loading.tsx` / `error.tsx` 가 **전 프로젝트에 0개** — 라우트 레벨 Error Boundary 와 Streaming Fallback 이 전혀 선언되지 않음.
- 상세 페이지(`app/(public)/posts/[slug]/page.tsx`) 는 백엔드 fetch 실패 시 `notFound()` 만 호출 → **500/502 장애도 404 로 표출**되어 사용자는 "게시글 없음" 으로 오해.
- 라우트 전환 시 Streaming SSR 이 활성화되지 않아 **전체 HTML 대기** → TTFB 체감 저하.

**왜 중요한가?**
- 장애 복구 UX 부재: 사용자는 재시도 경로가 없고, 서비스 안정성에 대한 신뢰 저하.
- `loading.tsx` 는 자동으로 `<Suspense>` 경계를 만들어 **부분 렌더링** 을 활성화 — Next.js App Router 의 핵심 이점이 봉인됨.
- 관리자 영역은 더 심각: 글 발행 중 401/403 이 나면 **빈 페이지 + 콘솔 에러** 만 남음 (현재 `RoleGate` 외 복구 수단 없음).

**해결하지 않으면?**
- 일시적 백엔드 장애 → 404 오인 → 사용자 이탈.
- Streaming SSR 미활용 → 느린 백엔드 응답이 그대로 TTFB 로 전파.
- 관리자가 글 작성 중 토큰 만료 → 재시도 불가, 내용 소실 가능.

---

### 3-2. Analyze (분석 및 선택지 검토)

#### 배치 전략 선택지

##### 선택지 A. 루트에 `error.tsx` 하나만

- **장점**: 단순.
- **단점**: 전체 트리를 잡기 때문에 layout 자체의 에러까지 가려져 디버깅 힘듦. 공개/관리자 UX 차별 불가.

##### 선택지 B. 그룹별 `error.tsx` + 세그먼트별 `loading.tsx` ✅ 채택

- `(public)/error.tsx` — 공개 영역: "일시 오류 + 재시도".
- `(admin)/management/error.tsx` — 관리자 영역: "세션 만료 가능성 + 로그인 이동".
- `(public)/posts/[slug]/loading.tsx` — 상세 페이지 스켈레톤 (최대 병목 구간).
- **근거**: App Router 의 error/loading 파일은 **세그먼트 내 자식 트리만** 감싼다. 공개/관리자의 UX 가 달라야 하고, loading 은 핵심 경로에만 두는 것이 비용 대비 이득 최적.

#### `not-found.tsx` 세분화 여부

- 루트 `app/not-found.tsx` 는 이미 존재. `(public)/posts/[slug]/not-found.tsx` 는 리뷰에서 **Low** 로 분류 — 본 계획서 **범위 제외**(후속 SEO 회차와 함께 다루는 것이 자연스러움).

#### `loading.tsx` 세분화 범위

| 라우트 | 체감 이득 | 본 계획서 포함 |
| --- | --- | --- |
| `app/(public)/posts/[slug]/loading.tsx` | 🔴 High (상세 페이지 = 외부 유입 첫 페이지) | ✅ |
| `app/(public)/loading.tsx` | 🟡 Medium (홈/카테고리/검색 공통) | ✅ |
| `app/(admin)/management/loading.tsx` | 🔵 Low (내부 사용, 스켈레톤보다 실제 데이터 선호) | ✗ |

#### `error.tsx` 의 Client Component 제약

- React Error Boundary 는 클래스 컴포넌트 `componentDidCatch` 에 의존 → 반드시 `"use client"`.
- `reset: () => void` 콜백은 stateful → Client 여야 호출 가능.
- `digest` 는 서버 에러의 디버깅 해시 (prod 에서 상세 메시지 노출 방지).

#### 상호작용 UX 설계

| 버튼 | 동작 |
| --- | --- |
| `(public)/error.tsx` · "다시 시도" | `reset()` 호출 |
| `(admin)/management/error.tsx` · "다시 시도" / "로그인 페이지로" | `reset()` / `router.push("/login?from=...")` |

---

### 3-3. Action (구현 계획 및 설계)

#### 3-3-1. 변경 대상 파일 목록

| 파일 | 역할 | 타입 |
| --- | --- | --- |
| `frontend/app/(public)/error.tsx` | 공개 영역 에러 폴백 | **신규** Client |
| `frontend/app/(public)/loading.tsx` | 공개 영역 공용 스켈레톤 | **신규** Server |
| `frontend/app/(public)/posts/[slug]/loading.tsx` | 상세 페이지 세분화 스켈레톤 | **신규** Server |
| `frontend/app/(admin)/management/error.tsx` | 관리자 영역 에러 폴백 | **신규** Client |
| `frontend/app/globals.css` | 스켈레톤 CSS (shimmer 애니메이션) | 수정 |

#### 3-3-2. `(public)/error.tsx` 스펙

```tsx
"use client";

import { useEffect } from "react";

export default function PublicError({
  error,
  reset,
}: {
  error: Error & { digest?: string };
  reset: () => void;
}) {
  useEffect(() => {
    // 운영에서는 Sentry 등으로 대체. 현재는 콘솔 기록만.
    console.error("[PublicError]", error);
  }, [error]);

  return (
    <div role="alert" className="container py-5 text-center">
      <h2 className="mb-3">잠시 불러올 수 없습니다</h2>
      <p className="text-muted mb-4">
        네트워크 또는 서버 오류가 발생했어요. 잠시 후 다시 시도해 주세요.
      </p>
      <button className="btn btn-primary" onClick={reset}>
        다시 시도
      </button>
      {error.digest && (
        <p className="text-muted small mt-3">오류 코드: {error.digest}</p>
      )}
    </div>
  );
}
```

#### 3-3-3. `(admin)/management/error.tsx` 스펙

```tsx
"use client";

import { useEffect } from "react";
import { useRouter } from "next/navigation";

export default function ManagementError({
  error,
  reset,
}: {
  error: Error & { digest?: string };
  reset: () => void;
}) {
  const router = useRouter();

  useEffect(() => {
    console.error("[ManagementError]", error);
  }, [error]);

  return (
    <div role="alert" className="container py-5 text-center">
      <h2 className="mb-3">요청을 처리할 수 없습니다</h2>
      <p className="text-muted mb-4">
        세션이 만료되었거나 일시적 오류가 발생했을 수 있어요.
      </p>
      <div className="d-flex justify-content-center gap-2">
        <button className="btn btn-outline-secondary" onClick={reset}>
          다시 시도
        </button>
        <button
          className="btn btn-primary"
          onClick={() => router.push("/login?from=/management")}
        >
          로그인 페이지로
        </button>
      </div>
    </div>
  );
}
```

#### 3-3-4. `(public)/posts/[slug]/loading.tsx` 스펙

```tsx
// Server Component — static HTML 로 즉시 스트리밍되는 스켈레톤
export default function PostDetailLoading() {
  return (
    <article className="post-skeleton" aria-busy="true" aria-live="polite">
      <div className="skeleton skeleton-title" />
      <div className="skeleton skeleton-meta" />
      <div className="skeleton skeleton-block" />
      <div className="skeleton skeleton-line" />
      <div className="skeleton skeleton-line" />
      <div className="skeleton skeleton-line skeleton-line-short" />
    </article>
  );
}
```

#### 3-3-5. `(public)/loading.tsx` 스펙

```tsx
export default function PublicLoading() {
  return (
    <div className="post-list-skeleton" aria-busy="true" aria-live="polite">
      {Array.from({ length: 4 }).map((_, i) => (
        <div key={i} className="post-card-skeleton">
          <div className="skeleton skeleton-title" />
          <div className="skeleton skeleton-line" />
          <div className="skeleton skeleton-line skeleton-line-short" />
        </div>
      ))}
    </div>
  );
}
```

#### 3-3-6. `globals.css` — shimmer 애니메이션

```css
.skeleton {
  background: linear-gradient(
    90deg,
    var(--bs-gray-800, #2a2a2a) 0%,
    var(--bs-gray-700, #3a3a3a) 50%,
    var(--bs-gray-800, #2a2a2a) 100%
  );
  background-size: 200% 100%;
  animation: skeleton-shimmer 1.4s ease-in-out infinite;
  border-radius: 4px;
}
@keyframes skeleton-shimmer {
  0%   { background-position: 200% 0; }
  100% { background-position: -200% 0; }
}
.skeleton-title       { height: 28px; width: 70%; margin-bottom: 12px; }
.skeleton-meta        { height: 14px; width: 40%; margin-bottom: 20px; }
.skeleton-block       { height: 200px; width: 100%; margin-bottom: 16px; }
.skeleton-line        { height: 14px; width: 100%; margin-bottom: 8px; }
.skeleton-line-short  { width: 60%; }
.post-skeleton        { padding: 1rem 0; }
.post-list-skeleton   { display: flex; flex-direction: column; gap: 1rem; }
.post-card-skeleton   { padding: 1rem; border: 1px solid var(--bs-border-color, #333); border-radius: 6px; }
```

> 다크 테마 기준 색상. 실제 CSS 변수는 `globals.css` 현행 값 확인 후 맞춤(구현 시 재검토).

#### 3-3-7. 트레이드오프

| 트레이드오프 | 선택 |
| --- | --- |
| 전역 단일 error vs 그룹별 | 그룹별 (공개/관리자 UX 다름) |
| 세분화 loading vs 공용 loading | 둘 다 — 상세는 전용, 그 외는 공용 |
| skeleton 정밀도 (1:1 구조 복제) vs 단순 블록 | 단순 블록(hydration 부담 최소화, 유지보수 용이) |
| `global-error.tsx` 추가 | **범위 외** — `(public)/error.tsx` 로 공개 영역 충분 커버, root layout 에러는 현재 드물어 후속 과제 |

#### 3-3-8. 예상 이슈 및 대응

| 이슈 | 대응 |
| --- | --- |
| `error.tsx` 가 layout 자신의 에러는 못 잡음 | Layout 은 최소화되어 있어 현실적 위험 낮음. 필요 시 `global-error.tsx` 추후 도입 |
| skeleton CSS 가 다크/라이트 혼용 환경에서 깨짐 | 현 프로젝트는 다크 전용 (커밋 `9c7b8e1`) — 현재 문제 없음 |
| `notFound()` 호출이 `error.tsx` 로 떨어짐 | `notFound()` 는 별도 경로로 `not-found.tsx` 에 매핑됨. 루트 `not-found.tsx` 로 커버 |
| 상세 페이지에서 `loading.tsx` 로 인해 Client Hydration 지연 체감 | skeleton 은 SC 고정, 본문 교체는 Streaming 청크로 처리 — 오히려 TTFB 이득 |
| `aria-live` 로 인한 스크린리더 과다 알림 | `polite` + `aria-busy` 조합으로 과잉 읽기 방지 |

---

### 3-4. Result (검증 계획)

#### 검증 시나리오

1. **`error.tsx` 수동 트리거**:
   - `app/(public)/posts/[slug]/page.tsx` 에 임시로 `throw new Error("test");` 삽입 → 브라우저 새로고침 → `(public)/error.tsx` 폴백 노출 확인 → 원복.
   - `reset` 버튼 클릭 → 재시도 동작 확인.
2. **`loading.tsx` 스트리밍 확인**:
   - Next.js DevTools / Network 탭에서 `/posts/[slug]` 진입 시 `content-type: text/html; charset=utf-8` + `transfer-encoding: chunked` 확인.
   - 백엔드에 `Thread.sleep(2000)` 임시 지연 삽입 시 skeleton 이 2초 표시 → 원복.
3. **관리자 error 폴백**:
   - 관리자 페이지에서 token 만료 상태로 접근 → `management/error.tsx` 노출 → "로그인 페이지로" 이동 동작 확인.
4. **접근성**:
   - 스크린리더(NVDA 등)로 skeleton 진입 시 "바쁨" 알림 확인, 에러 시 "경고" 역할 인식.

#### 성공 기준

- [ ] 4개 신규 파일(`(public)/error.tsx`, `(public)/loading.tsx`, `(public)/posts/[slug]/loading.tsx`, `(admin)/management/error.tsx`) 생성.
- [ ] `globals.css` 에 skeleton 스타일 반영.
- [ ] 상세 페이지 진입 시 스켈레톤 가시 확인 (백엔드 지연 시뮬레이션).
- [ ] 에러 시 재시도/로그인 이동 버튼 동작.
- [ ] `npm run build` 성공 + `npm run dev` 워크플로 회귀 없음.

#### 모니터링

- `console.error("[PublicError]", ...)` / `console.error("[ManagementError]", ...)` 로그로 수집.
- 장기: Sentry 연동 시 `error.digest` 를 키로 사용 (후속).

---

## 4. 전체 구현 순서 (권장)

본 계획서는 **Sonnet 세션** 에서 순서대로 구현을 권장.

1. **Step 1 — Top 3 먼저 (낮은 리스크, 즉시 가시 효과)**
   1. `globals.css` skeleton 스타일 추가.
   2. `(public)/loading.tsx` / `(public)/posts/[slug]/loading.tsx` / `(public)/error.tsx` / `(admin)/management/error.tsx` 신규 생성.
   3. 수동 트리거로 에러 폴백 / 스트리밍 동작 확인.
2. **Step 2 — Top 2 잔여**
   1. `app/sitemap.ts` 상단에 `export const revalidate = 3600;` + 의도 주석 추가.
3. **Step 3 — Top 1 (Frontend 측)**
   1. `lib/api.ts` 6개 함수에 `next.tags` 부여 + 정책 주석 블록.
   2. `app/api/revalidate/route.ts` Route Handler 신설.
   3. `.env.local.example` 에 `REVALIDATE_SECRET` 추가.
   4. curl 수동 테스트 3종 (정상/401/400).
4. **Step 4 — Top 1 (Backend 측, 별도 `-b` 세션)**
   1. `PostChangeEvent` + `@TransactionalEventListener(AFTER_COMMIT)` + `RevalidateClient` (WebClient) 구현.
   2. 통합 테스트: 게시글 CUD → 프론트 캐시 무효화 확인.
5. **Step 5 — 검토 / 커밋**
   1. 재검토 체크리스트 적용.
   2. 커밋은 사용자 확인 후 진행.

---

## 5. Context7 활용 포인트 (구현 시)

구현 세션에서 다음 라이브러리의 **Context7 MCP 문서 조회** 를 우선 수행할 것:

- Next.js 16.x — `revalidateTag`, `revalidatePath` 시그니처 및 Route Handler 규약.
- Next.js 16.x — `error.tsx` / `loading.tsx` / `not-found.tsx` Route Segment Config.
- Next.js 16.x — `Route Segment Config` (`export const revalidate`, `dynamic`, `runtime`).

---

## 6. 재검토 체크리스트 (완료 시)

- [ ] 각 신규 파일이 리뷰 개선안 스펙과 일치
- [ ] `lib/api.ts` 6개 함수 모두 `next.tags` 보유
- [ ] `app/api/revalidate/route.ts` — secret 검증, 400/401/200 분기
- [ ] `app/sitemap.ts` `revalidate` 명시
- [ ] 4개 특수 파일 (`error.tsx` x 2, `loading.tsx` x 2) 생성
- [ ] `globals.css` skeleton 스타일 추가
- [ ] `npm run build` 성공
- [ ] 수동 검증 4종 (error 폴백, streaming, 관리자 error, curl) 완료
- [ ] 디버그용 임시 코드(`throw new Error`, `Thread.sleep`) 모두 원복
- [ ] 커밋 메시지 초안 사용자 확인

---

## 7. 범위 제외 / 후속 계획서로 이월

| 항목 | 이월 사유 |
| --- | --- |
| 백엔드 `@TransactionalEventListener(AFTER_COMMIT)` webhook 호출자 | `-b` 작업. 본 계획서는 프론트 엔드포인트 스펙만 확정. 별도 `backend/docs/YYMMDD_revalidate-webhook-plan.md` 로 분리 |
| `revalidate` TTL 완화 (60s → 600s) | 운영 모니터링 후 수치 결정. Top 1 완료 후 성능 튜닝 회차 |
| `(public)/posts/[slug]/not-found.tsx` 커스텀 (추천글/돌아가기) | 리뷰 Low. SEO 회차 (2회차) 와 함께 진행 |
| `global-error.tsx` | 현재 root layout 에러 발생 사례 없음. 후속 안정화 회차 |
| Sentry 연동 | 별도 관측성 회차 |
