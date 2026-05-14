# App Router 리뷰 1회차 — 점검 영역 1 (1-1 ~ 1-4) 계획서

- **작성일**: 2026-04-20
- **참조 리뷰**: `.claude/reviews/frontend-review-1-app-router.md` (2026-04-19)
- **연계 계획서**:
  - `frontend/docs/260419_public-route-group-consolidation-plan.md` (1-1)
  - `frontend/docs/260419_server-client-component-boundary-plan.md` (1-2)
  - `frontend/docs/260420_app-router-top3-implementation-plan.md` (1-3 + 2-4)
- **범위**: 리뷰 "점검 영역 1" 전 구간 (1-1 디렉토리 구조 / 1-2 서버·클라이언트 분리 / 1-3 특수 파일 / 1-4 고급 라우팅)
- **성격**: **신규 대규모 구현 계획이 아니라**, 이미 완료된 작업에 대한 **상태 검증 + 남은 gap 정리 + 회귀 테스트 설계**

---

## 1. Problem (문제 정의)

### 1.1 리뷰 1회차가 지적한 것 (요약)
| 항목 | 우선순위 | 지적 내용 |
| --- | --- | --- |
| 1-1 `(public)` 그룹 반쪽 적용 | High | 홈/카테고리/검색이 그룹 밖 + `UserLayout.tsx` 와 `(public)/layout.tsx` 중복 |
| 1-1 최상위 동적 세그먼트 | Medium | `/[categoryName]` 이 `/about`/`/tags` 등과 충돌 가능 |
| 1-1 `(admin)` 그룹 부재 | Medium | `app/management/` 가 그룹 격리 안 됨 |
| 1-1 `/boards/[id]` shim | Low | `next.config.ts` redirects 로 대체 가능 |
| 1-2 `management/layout.tsx` 전역 클라이언트화 | High | 관리자 HTML Flash, 실보안 경계 아님 |
| 1-2 `CommentSection` 불필요한 `"use client"` | Medium | 단순 래퍼인데 CC 선언 |
| 1-2 Editor dynamic loader 중복 | Medium | `new-post` / `posts/[id]` 두 곳 중복 |
| 1-3 `error.tsx` / `loading.tsx` 부재 | High | 장애 시 404 오해, Streaming 기회 상실 |
| 1-3 `posts/[slug]/not-found.tsx` 세분화 | Low | 삭제 글 안내 + 추천 목록 UX 부재 |
| 1-4 병렬/인터셉트 라우트 | Low | 현 시점 도입 불필요 (이월) |

### 1.2 왜 지금 시점에 다시 정리하는가
- 위 항목 중 **High / Medium 대부분이 이미 구현되어 master 로 머지됨**(commit `5fe9069 feat: App Router Top 1+3` + 선행 커밋).
- 그러나 **(a) 리뷰 문서의 지적 항목 → 실제 구현 파일 매핑이 문서로 정리되어 있지 않고**, **(b) Low 우선순위 1건이 누락**되어 있으며, **(c) 회귀 검증이 아직 집행되지 않음**.
- 이 계획서 목적: 완료 상태를 공식화 → 남은 Low 항목 처리 → 회귀 테스트 플랜 확정.

### 1.3 방치 시 리스크
- 리뷰 문서와 실제 코드 베이스의 불일치 → 이후 회차(2·3회차) 리뷰 시 중복 지적 발생 가능.
- 게시글 삭제 시 `/posts/존재하지-않는-슬러그` 접근이 전역 `not-found.tsx`(홈 링크만) 로 폴백 — 삭제된 글/오타/오래된 북마크 구분 불가.
- `next.config.ts` 예약어 가드(`(?!category|posts|search|login|management|boards|api|_next|...)`)가 있어도 향후 새 정적 경로를 추가할 때 이 정규식을 업데이트해야 함을 인지하는 문서가 없음.

---

## 2. Analyze (분석 및 선택지 검토)

### 2.1 현재 구현 상태 — 교차 검증

| 리뷰 항목 | 리뷰 권고 | 현재 상태 | 근거 파일 |
| --- | --- | --- | --- |
| 1-1 `(public)` 그룹 일관화 | 홈/카테고리/검색을 `(public)/` 로 이동 | ✅ 이동 완료 | `app/(public)/page.tsx`, `app/(public)/category/[name]/page.tsx`, `app/(public)/search/page.tsx` |
| 1-1 `UserLayout.tsx` 삭제 | 중복 레이아웃 제거 | ✅ 삭제됨 | `components/layout/` 에 `UserLayout.tsx` 없음 |
| 1-1 `/[categoryName]` 리팩토링 | `/category/[name]` 으로 변경 + redirects | ✅ 완료 | `app/(public)/category/[name]/page.tsx` + `next.config.ts:43-48` |
| 1-1 `(admin)` 그룹 신설 | `app/(admin)/management/...` | ✅ 완료 | `app/(admin)/management/*` |
| 1-1 `/boards/[boardId]` shim | `next.config.ts` redirects 로 대체 | ✅ 완료 | `next.config.ts:31-41`, `app/boards/` 삭제됨 |
| 1-2 `management/layout.tsx` SC 전환 | SC + `cookies()` + `redirect()` | ✅ 완료 | `app/(admin)/management/layout.tsx:1-40` (no `"use client"`) |
| 1-2 role 검증 분리 | `RoleGate` Client fragment 로 분리 | ✅ 완료 | `components/management/RoleGate.tsx` |
| 1-2 `CommentSection` SC 전환 | `"use client"` 제거 | ✅ 완료 | `components/comments/CommentSection.tsx:1-15` (import 만 존재) |
| 1-2 Editor dynamic loader 추출 | 공용 래퍼 | ✅ 완료 | `components/editor/PostEditorDynamic.tsx` |
| 1-3 `(public)/error.tsx` | 재시도 버튼 | ✅ 완료 | `app/(public)/error.tsx` |
| 1-3 `(public)/loading.tsx` | 라우트 레벨 fallback | ✅ 완료 | `app/(public)/loading.tsx` |
| 1-3 `posts/[slug]/loading.tsx` | 스켈레톤 | ✅ 완료 | `app/(public)/posts/[slug]/loading.tsx` |
| 1-3 `management/error.tsx` | 관리자 복구 UX | ✅ 완료 | `app/(admin)/management/error.tsx` |
| 1-3 `posts/[slug]/not-found.tsx` | 삭제 글 전용 안내 | ❌ **미구현** | `app/(public)/posts/[slug]/` 에 부재 |
| 1-4 `(public)` / `(admin)` 분리 | 라우트 그룹 | ✅ 완료 (1-1 에서 병합) | — |
| 1-4 병렬/인터셉트 라우트 | 이월 | ⏸️ 의도적 보류 | — |

### 2.2 남은 gap 분석 — `posts/[slug]/not-found.tsx`

#### 현재 동작
- `app/(public)/posts/[slug]/page.tsx:67-72` 에서 `getPostBySlug()` 예외 → `notFound()` 호출.
- Next.js 라우팅: **가장 가까운 `not-found.tsx`** 를 찾아 렌더. 현재 `posts/[slug]/` 에는 없고 → 부모인 `(public)/` 도 없고 → 루트 `app/not-found.tsx` 로 폴백.
- `app/not-found.tsx` 는 **"홈으로 돌아가기" 링크만 제공** (파일 내용 확인 완료).

#### 개선 옵션 비교

| 옵션 | 내용 | 장점 | 단점 |
| --- | --- | --- | --- |
| **A. 최소 문구만** | 전용 `not-found.tsx` 를 만들되 static 안내 문구 + 홈 링크 | 구현 단순, SSG 가능 | UX 개선 폭 작음 |
| **B. 최근 글 목록 포함** | 서버에서 `getPostList(1)` 호출해 최근 게시글 N개 노출 | 이탈 방지, SEO 내부 링크 증가 | Server Component 에서 `notFound()` 렌더 경로가 data fetch 를 한 번 더 함 — TTFB 영향 |
| **C. 검색창 포함** | 클라이언트 검색폼 추가 | 복구 수단 제공 | Client island 추가, bundle 증가 |

#### 결론: **옵션 B 채택**
- 근거: 리뷰 원문(187행) 권고가 **"삭제된 글 안내 + 최근 글 목록"** 으로 명시됨.
- `getPostList(1)` 은 이미 ISR 60s + `next.tags: ["posts"]` 로 캐시됨 → 추가 호출 비용은 **대부분 캐시 히트**.
- 검색창은 2회차 SEO/검색 개선에서 재검토 (과한 범위 확대 방지).

### 2.3 회귀 검증 전략
리뷰 항목 구현 후 **end-to-end 검증이 누락됨**. 다음 3가지가 동시에 정상 동작해야 함:

1. **라우팅 레벨**: `(public)` / `(admin)` 경계 정상, redirects 동작.
2. **렌더링 레벨**: Server/Client 경계가 의도대로 hydrate — 관리자 Flash 없음.
3. **오류 UX**: error.tsx / loading.tsx / not-found.tsx 가 상황별로 정확히 매칭.

각 축에 대한 수동 테스트 시나리오를 4.2 에서 정의.

---

## 3. Action (구현 계획 및 설계)

### 3.1 목표 및 범위
- **In scope**:
  1. `app/(public)/posts/[slug]/not-found.tsx` 신설 (옵션 B)
  2. 회귀 테스트 시나리오 실행 및 결과 기록
  3. `next.config.ts` 예약어 가드 주석 보강 (향후 유지보수 단서)
- **Out of scope**:
  - 병렬/인터셉트 라우트 도입 (1-4 Low, 이월 유지)
  - 관리자 페이지 서버 prefetch (2회차 TanStack Query 와 묶어 처리)
  - SEO 메타데이터 개선 (2회차)

### 3.2 구현 접근

#### 3.2.1 `app/(public)/posts/[slug]/not-found.tsx`

**설계 결정**:
- Server Component 로 작성 (특수 파일 `not-found.tsx` 는 SC 허용, async 데이터 페칭 가능).
- `getPostList(1)` 결과 중 상위 5개만 노출.
- fetch 실패 시 `try/catch` → 최근 글 없이 안내 문구만 표시 (기존 `sitemap.ts` 패턴과 동일).
- 스타일: 기존 `app/not-found.tsx` 의 monospace/accent 톤을 재사용, 별도 CSS 추가하지 않음.

**예상 파일 구조**:
```tsx
// app/(public)/posts/[slug]/not-found.tsx (Server Component)
import Link from "next/link";
import { getPostList } from "@/lib/api";
import type { Post } from "@/types";

export default async function PostNotFound() {
  let recentPosts: Post[] = [];
  try {
    const res = await getPostList(1);
    recentPosts = res.content.slice(0, 5);
  } catch {
    // 백엔드 장애 시 안내 문구만 노출
  }
  return (
    <section className="container py-5">
      <p className="mono-caption">// 404 · post</p>
      <h1>해당 게시글을 찾을 수 없습니다</h1>
      <p className="text-muted">삭제되었거나 URL이 변경되었을 수 있습니다.</p>
      {recentPosts.length > 0 && (
        <>
          <h2 className="mt-4">최근 게시글</h2>
          <ul>
            {recentPosts.map((p) => (
              <li key={p.id}>
                <Link href={`/posts/${p.slug}`}>{p.title}</Link>
              </li>
            ))}
          </ul>
        </>
      )}
      <Link href="/">← 홈으로</Link>
    </section>
  );
}
```

**검증 포인트**:
- `getPostList` 의 응답 타입이 `content` 배열을 가지는지 `lib/api.ts` 에서 확인 (구현 세션에서 재확인 필수).
- `PostListResponse` 타입이 없다면 `types/index.ts` 기존 타입과 호환 점검.

#### 3.2.2 회귀 테스트 — 수동 시나리오

| # | 시나리오 | 예상 결과 |
| --- | --- | --- |
| R1 | `/` 접속 | `(public)/layout.tsx` (Header + CategoryNav + VisitorCount) + `(public)/page.tsx` 렌더 |
| R2 | `/category/spring` 접속 | 카테고리 페이지 렌더, 사이드바 동일 |
| R3 | 구 `/spring` (카테고리명) 접속 | 301 → `/category/spring` |
| R4 | `/boards/123` 접속 | 301 → `/posts/123` |
| R5 | `/boards` 접속 | 301 → `/` |
| R6 | `/management` 접속 (로그아웃 상태, HTTP dev) | Header 표시 → RoleGate 가 `/login?from=/management` 로 replace |
| R7 | `/management` 접속 (로그아웃 상태, HTTPS prod 모드) | `redirect("/login?from=/management")` 서버에서 처리 (Flash 없음) |
| R8 | `/posts/존재하지-않는-slug` 접속 | **새 `not-found.tsx`** — 최근 글 목록 포함 |
| R9 | 게시글 페이지에서 백엔드 500 발생 | `(public)/error.tsx` — 재시도 버튼 |
| R10 | 게시글 페이지 로딩 중 | `(public)/posts/[slug]/loading.tsx` 스켈레톤 |
| R11 | 관리자 페이지 작업 중 오류 | `(admin)/management/error.tsx` |
| R12 | 댓글 섹션 렌더 | `CommentSection` 이 SC → 내부 `CommentForm`/`CommentList` 만 CC hydrate |
| R13 | 새 글 작성 `/management/new-post` | `PostEditorLazy` dynamic import, `"loading editor..."` 플레이스홀더 |

#### 3.2.3 `next.config.ts` 주석 보강
예약어 정규식이 향후 추가될 정적 경로와 연동되어야 하는 점을 코드 주석으로 명시:
```ts
// 구 카테고리 URL /:name → /category/:name
// 주의: destination 경로가 충돌하지 않도록 새로운 최상위 정적 경로(예: /about, /tags)를
// 추가할 때는 이 부정 lookahead (?!category|posts|...) 에 해당 이름을 반드시 추가할 것.
```

### 3.3 변경 대상 파일
| 파일 | 변경 유형 | 설명 |
| --- | --- | --- |
| `frontend/app/(public)/posts/[slug]/not-found.tsx` | 신규 | 삭제 글 안내 + 최근 글 목록 |
| `frontend/next.config.ts` | 주석 보강 | 예약어 정규식 유지보수 가이드 |
| `frontend/docs/260420_app-router-review-sections-1-4-plan.md` | 본 파일 | — |

### 3.4 트레이드오프
- **fetch 추가 비용**: `not-found.tsx` 에서 `getPostList(1)` 을 한 번 더 호출. → **허용**. 이미 홈에서 캐시된 결과 재사용, 실패 시에도 graceful degrade.
- **복잡도**: `try/catch` + 조건부 렌더로 10줄 미만 추가. → **수용 가능**.
- **SEO**: `not-found.tsx` 는 `status 404` 반환 — 크롤러가 제외 판단에 영향 없음.

### 3.5 예상 이슈 및 대응
| 이슈 | 대응 |
| --- | --- |
| `getPostList` 응답 타입이 `content` 가 아닌 경우 | 구현 세션에서 `lib/api.ts` 의 타입 재확인 후 mapping 조정 |
| 백엔드 미기동 시 테스트 불가 | R8 는 `getPostBySlug` 가 throw 하도록 존재하지 않는 slug 사용 → 백엔드 동작 필요. 백엔드 미기동 시 해당 케이스 skip 하고 unit 테스트 추가 검토 |
| 루트 `app/not-found.tsx` 와 UX 상이 | 의도적 차이 — 전역 404 는 일반 안내, 게시글 404 는 context-aware |

---

## 4. Result (검증 계획)

### 4.1 성공 기준
1. `app/(public)/posts/[slug]/not-found.tsx` 존재 + 옵션 B 형태 구현.
2. 회귀 시나리오 R1 ~ R13 **모두 expected 와 일치**.
3. `npm run build` 성공 + 타입 에러 0건.
4. 리뷰 문서의 1-1 ~ 1-4 항목 중 Low 1건 포함 **모든 High/Medium 해소** 문서화.

### 4.2 검증 절차
```bash
# 1. 타입/빌드 검증
cd frontend && npm run build

# 2. dev 서버 구동
npm run dev

# 3. 수동 회귀 테스트 — Section 3.2.2 의 R1 ~ R13 실행
# 각 항목 pass/fail 을 이 문서 하단 "검증 결과" 섹션에 기록
```

### 4.3 측정 지표 (선택적, 회귀 기준선)
- **Server / Client 파일 비율**: 현재 33개 중 26개 CC (≈79%) → 이번 변경 후 새 SC 1개 추가로 `/34 중 26` ≈ 76%.
- **관리자 페이지 첫 페인트**: `management/layout.tsx` SC 전환 후 Flash 제거 — DevTools Network 탭에서 `refresh_token` 없이 `/management` 접근 시 **첫 HTML 이 `/login` 인지** 확인 (HTTPS prod 모드 한정, HTTP dev 는 RoleGate 경로).

### 4.4 회귀 이슈 발견 시 대응
- 리뷰 문서의 개선안 블록을 재참조 → 구현 파일 수정 → 변경 로그를 본 계획서 하단에 기록.

---

## 5. 차기 회차로 이월 (본 계획서 범위 외)
- **2회차 SEO/TanStack Query**: 관리자 페이지 서버 prefetch + `HydrationBoundary`, `generateMetadata` 고도화, `useSuspenseQuery` 검토.
- **3회차 SSE**: `VisitorCount` / `PostEventListener` 재검토.
- **병렬 라우트**: 관리자 대시보드(통계 + 최근 댓글) 설계 시점에 부활 검토.

---

## 6. 체크리스트 (구현 세션용)
- [ ] `lib/api.ts` 의 `getPostList` / `PostListResponse` 타입 확인
- [ ] `app/(public)/posts/[slug]/not-found.tsx` 신규 작성
- [ ] `next.config.ts` 예약어 주석 보강
- [ ] R1 ~ R13 수동 회귀 실행 및 결과 기록
- [ ] `npm run build` 통과 확인
- [ ] 사용자에게 변경 요약 + 커밋 확인 요청 (자동 커밋 금지)

---

## 7. 검증 결과 (구현 세션에서 채울 것)
<!-- 구현 완료 후 채워넣기
| 시나리오 | 결과 | 비고 |
| --- | --- | --- |
| R1 | ⏳ |  |
...
-->
