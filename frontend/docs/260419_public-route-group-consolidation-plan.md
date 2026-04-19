# (public) 라우트 그룹 일관화 + (admin) 그룹 신설 계획서

- **작성일**: 2026-04-19
- **작업 범위**: `frontend/` (Next.js 16.2.3, App Router)
- **근거 리뷰**: `.claude/reviews/frontend-review-1-app-router.md` § **1-1 디렉토리 구조**
- **관련 이월 작업**: 1-2 (management Server Component 전환) / 1-3 (error/loading 특수 파일) / 2-3 (on-demand revalidation) — **본 계획서 범위 외**

---

## 1. Problem (문제 정의)

### 1.1 현상

App Router 라우트 그룹이 "반쪽짜리"로 적용되어 있다.

- `app/(public)/layout.tsx` 와 `components/layout/UserLayout.tsx` 가 **거의 동일한 JSX**(`<PostEventListener /> + <Header /> + Container/Row/Col + CategoryNav + VisitorCount`)를 **중복** 선언.
- 공개 페이지 3개(`/`, `/[categoryName]`, `/search`)가 `(public)` 그룹 **바깥**에 있어 `<UserLayout>` 을 **컴포넌트 레벨로 수동 래핑**.
- `(admin)` 그룹이 없어 관리자 경로(`app/management/**`)와 공개 경로가 레이아웃 경계에서 구분되지 않음.
- 최상위 동적 세그먼트 `app/[categoryName]/page.tsx` 때문에 향후 `/about`, `/tags` 같은 정적 경로와 **URL 충돌 위험** + 예약어 방어 로직 부재.
- `app/boards/[boardId]/page.tsx` 는 백엔드 조회 후 `/posts/{slug}` 로 리다이렉트만 수행하는 shim — 라우트 1개를 소비.

### 1.2 왜 중요한가

- **DRY 위반**: 레이아웃을 수정하려면 `(public)/layout.tsx` 와 `UserLayout.tsx` 두 곳을 동시에 고쳐야 한다. 한쪽만 수정되면 홈/카테고리/검색과 게시글 상세의 레이아웃이 달라지는 시각적 회귀가 발생한다.
- **App Router 의도와 어긋남**: `(group)` 폴더의 핵심 이점은 "URL 에 영향을 주지 않고 레이아웃 경계를 트리 레벨에서 격리" 하는 것. 현재 구조는 그룹의 이름만 차용했을 뿐 실질적 효용이 없다.
- **라우트 충돌 잠재 리스크**: `/[categoryName]` 이 최상위에 열려 있어, 카테고리 이름이 우연히 미래의 정적 경로와 겹치면 정적 경로가 가려지거나 반대로 카테고리가 404 처리된다.
- **면접 관점**: 중첩 레이아웃·라우트 그룹은 Pages Router 대비 App Router 의 **대표적인 설명 포인트**인데, 현 구조로는 "활용하지 못했다" 는 인상을 준다.

### 1.3 해결하지 않으면

- 향후 레이아웃 기능 추가(스크롤 복원, 광고 슬롯, 공지 배너 등) 시 두 파일을 동시에 건드려야 하며 누락 시 홈/카테고리/상세 페이지 간 UI 불일치 발생.
- 새 정적 경로 추가 요청(`/about`, `/tags`) 시 "카테고리 이름 예약어 리스트" 를 별도 관리해야 하는 기술 부채 누적.
- 관리자 페이지에 공통 레이아웃 요소를 추가할 때 `(admin)` 그룹이 없어 트리 레벨 분기가 불가능 → Client Component 조건 분기에 계속 의존.

---

## 2. Analyze (선택지 검토)

### 2.1 선택지 A — 현 상태 유지

- **장점**: 변경 없음, 기존 URL 100% 호환.
- **단점**: 위 모든 문제가 지속. 1-2 (관리자 Server Component 전환) 및 1-3 (에러/로딩 특수 파일) 작업이 구조적으로 얹기 어려움.
- **판정**: 부적합. 후속 회차에서 동일 문제가 반복적으로 발목을 잡는다.

### 2.2 선택지 B — `UserLayout` 제거만 (라우트 이동 없음)

- 공개 3페이지에서 `<UserLayout>` 래퍼를 제거하고, `app/layout.tsx` 에서 직접 Header + Sidebar 를 렌더.
- **장점**: 파일 이동 없음, 중복 JSX 제거 달성.
- **단점**: 그러면 **모든 경로**(로그인, 관리자, 404)가 동일 레이아웃을 상속받게 되어 관리자·로그인 페이지 구조가 오히려 망가짐. `(public)` 그룹의 존재 이유도 사라짐.
- **판정**: 부적합. App Router 가 라우트 그룹을 제공하는 이유(레이아웃 분기)를 거스름.

### 2.3 선택지 C — `(public)` 그룹으로 공개 경로 전면 이전 + `UserLayout` 삭제 [채택]

- `/`, `/[categoryName]`, `/search` 를 `app/(public)/` 하위로 이동.
- `components/layout/UserLayout.tsx` 삭제 → `(public)/layout.tsx` 하나가 단일 소스.
- **장점**: App Router 중첩 레이아웃의 정석. DRY 확보. 그룹 이름이 실제 역할과 일치. 1-2 (관리자 Server Component), 1-3 (세그먼트별 `error.tsx`/`loading.tsx`) 로의 확장 기반 마련.
- **단점**: 파일 이동 + 각 페이지의 `<UserLayout>` 래퍼 제거 필요. 이동량은 페이지 3개 + 각 페이지당 import 3~4 줄 수준.
- **판정**: **채택**.

### 2.4 `[categoryName]` 동적 세그먼트 — 선택지

- **A**. 현 상태 유지(`app/(public)/[categoryName]/page.tsx` 로 이동만).
- **B**. `/category/[name]` 으로 명시화 + 구 URL 은 `next.config.ts` `redirects()` 에서 301 처리 [채택].
- **판정**: B 채택. 정적 경로와의 충돌 리스크를 구조적으로 제거하고 URL 의도가 명확해진다. SEO 영향은 **301 permanent** 로 대부분 보존되며(크롤러가 신 URL 로 전파), `CategoryNav` 링크와 sitemap 만 업데이트하면 된다.

### 2.5 `(admin)` 그룹 신설 — 선택지

- **A**. `app/management/` 그대로 유지.
- **B**. `app/(admin)/management/` 로 이동 [채택].
- **판정**: B. URL(`/management/**`)은 변하지 않으므로 `proxy.ts` matcher·외부 링크 영향 0. 대신 레이아웃 트리가 `(public)` 과 동급 경계로 분리되어 1-2 회차에서 Server Component 전환 시 자연스럽게 연결된다.

### 2.6 `/boards/[boardId]` shim — 선택지

- **A**. 현 페이지 유지(백엔드 조회 후 실제 slug 로 리다이렉트).
- **B**. `next.config.ts` `redirects()` 로 `/boards/:boardId` → `/posts/:boardId` 단순 치환 [채택].
- **근거**: 백엔드 `/api/v1/posts/{boardId}` 는 **숫자 ID 도 처리**(파일 상단 주석 기준). slug 해석을 서버에 맡겨도 되고, SSR 없이 정적 redirect 로 1 홉 감소 + 불필요한 API 호출 제거.
- **판정**: B. 다만 "정말로 숫자 ID 접근이 백엔드에서 항상 slug URL 과 동일 컨텐츠를 돌려주는가?" 를 **실측 검증 후** 삭제(§ 4.2 검증 항목에 포함).

---

## 3. Action (구현 계획 및 설계)

### 3.1 작업 목표

- App Router 라우트 그룹을 **기능적으로** 적용: 공개(`(public)`) / 관리자(`(admin)`) / 최상위(로그인·404·sitemap·robots) 3 계층으로 정리.
- 중복 레이아웃 제거(`UserLayout.tsx` 삭제).
- 최상위 동적 세그먼트 제거(`[categoryName]` → `category/[name]`).
- `/boards/:boardId` SSR shim 제거(정적 redirect 로 치환).

### 3.2 범위

#### 포함

- [x] `app/(public)/` 하위로 홈/카테고리/검색 페이지 이전
- [x] `app/(admin)/management/` 그룹 신설 + `management/**` 이동
- [x] `components/layout/UserLayout.tsx` 삭제 + 각 페이지의 래퍼 제거
- [x] `[categoryName]` → `category/[name]` 리네임 + `next.config.ts` 301 리다이렉트
- [x] `/boards/[boardId]` 페이지 삭제 + `next.config.ts` 301 리다이렉트
- [x] `CategoryNav` 링크·`sitemap.ts`·내부 Link href 업데이트

#### 제외 (차기 회차 또는 별도 계획서)

- [ ] **1-2** `app/(admin)/management/layout.tsx` 를 Server Component + `cookies()` + `redirect()` 로 전환 → 별도 계획서
- [ ] **1-3** `app/(public)/error.tsx`, `app/(public)/posts/[slug]/loading.tsx` 등 특수 파일 신설 → 별도 계획서
- [ ] **2-3** `revalidateTag` 기반 on-demand revalidation → 별도 계획서

### 3.3 목표 디렉토리 구조

```
frontend/app/
├── layout.tsx                             ← Root (변경 없음)
├── not-found.tsx                          ← 글로벌 404 (변경 없음)
├── robots.ts, sitemap.ts                  ← 변경 없음
├── login/page.tsx                         ← 변경 없음
├── (public)/
│   ├── layout.tsx                         ← 단일 소스 (기존 유지)
│   ├── page.tsx                           ← [이동] app/page.tsx
│   ├── category/[name]/page.tsx           ← [이동+리네임] app/[categoryName]/page.tsx
│   ├── search/page.tsx                    ← [이동] app/search/page.tsx
│   └── posts/[slug]/page.tsx              ← (기존)
└── (admin)/
    └── management/                        ← [이동] app/management/**
        ├── layout.tsx
        ├── page.tsx
        ├── categories/page.tsx
        ├── new-post/page.tsx
        ├── posts/[id]/page.tsx
        └── temporary-storage/page.tsx
```

제거 대상:
- `app/page.tsx`, `app/[categoryName]/`, `app/search/`, `app/management/`, `app/boards/`
- `components/layout/UserLayout.tsx`

### 3.4 구현 접근 방식 및 설계 결정

#### 3.4.1 파일 이동 순서(작은 단위·독립 단계)

라우트 그룹은 URL 에 영향이 없으므로, 각 단계 이후 `npm run build` 가 통과하는 "가역 가능한" 체크포인트로 설계한다.

1. **Step 1 — 관리자 그룹 신설**  
   - `app/management/` → `app/(admin)/management/` 이동.  
   - `proxy.ts` matcher (`/management/:path*`) 는 **변경 불필요** (URL 동일).  
   - 검증: `npm run build` → 생성 라우트 목록에서 `/management/**` 유지 확인.

2. **Step 2 — 공개 페이지 3종 이전**  
   - `app/page.tsx` → `app/(public)/page.tsx` (내용 동일, `<UserLayout>` 래퍼만 제거).  
   - `app/search/page.tsx` → `app/(public)/search/page.tsx` (래퍼 제거).  
   - `app/[categoryName]/page.tsx` → `app/(public)/category/[name]/page.tsx`:
     - param 이름 `categoryName` → `name` 으로 변경 (URL 세그먼트 이름과 일치).  
     - `params.categoryName` 참조 3곳 수정.  
     - `generateStaticParams` 반환 키도 `name` 으로.  
   - 검증: 빌드 후 `/`, `/search`, `/category/Java` 등 접근 시 200.

3. **Step 3 — `UserLayout.tsx` 삭제**  
   - `components/layout/UserLayout.tsx` 삭제.  
   - Step 2 에서 이미 래퍼를 제거했으므로 참조 0건 상태여야 함(빌드로 확인).

4. **Step 4 — 구 URL 301 리다이렉트**  
   - `next.config.ts` `redirects()` 보강 (§ 3.4.2).  
   - `app/boards/[boardId]/page.tsx` 및 `app/boards/` 디렉토리 삭제.

5. **Step 5 — 내부 링크 업데이트**  
   - `components/layout/CategoryNav.tsx:77` — `href={`/${c.name}`}` → `href={`/category/${c.name}`}`.  
   - `app/sitemap.ts` — 카테고리 엔트리는 현재 없으므로 변경 불필요. 단, **추후 카테고리 URL 을 sitemap 에 추가할 여지**가 생겼다는 점은 1-3/2 회차 계획서에서 연결.  
   - 그 외 `/${...}` 형태로 카테고리 URL 을 만드는 코드 grep → 치환.

#### 3.4.2 `next.config.ts` redirects 추가안

```ts
// next.config.ts
async redirects() {
  return [
    // (기존) /boards → /
    { source: "/boards", destination: "/", permanent: true },

    // [신규] /boards/:boardId → /posts/:boardId
    //   이유: /boards/[boardId] SSR shim 제거. 백엔드가 숫자 ID 도 /posts/{id} 에서 처리.
    { source: "/boards/:boardId", destination: "/posts/:boardId", permanent: true },

    // [신규] 구 카테고리 URL /:categoryName → /category/:categoryName
    //   예약어(기존 정적 경로) 는 매칭에서 제외한다.
    //   /api 는 백엔드 프록시 대상이 아니지만 방어적으로 포함.
    {
      source: "/:categoryName((?!category|posts|search|login|management|boards|api|_next|favicon\\.ico|robots\\.txt|sitemap\\.xml).+)",
      destination: "/category/:categoryName",
      permanent: true,
    },
  ];
}
```

- `(?!...)` negative lookahead: 예약어는 리다이렉트하지 않고 통과 → Next 의 라우트 매처가 정적 경로를 우선 적용.
- `.+` (한 문자 이상) 사용: `/` 빈 경로는 홈이므로 매칭 제외.
- **주의**: 정규식에 의한 매칭은 Next 가 **path-to-regexp** 로 빌드 시 변환하므로, 빌드 로그에서 해당 리다이렉트 규칙이 등록되었는지 반드시 확인.

#### 3.4.3 변경 대상 파일 목록

| 분류 | 파일 | 작업 |
| --- | --- | --- |
| 이동 | `app/page.tsx` | → `app/(public)/page.tsx` (래퍼 제거) |
| 이동+리네임 | `app/[categoryName]/page.tsx` | → `app/(public)/category/[name]/page.tsx` (param 이름 변경) |
| 이동 | `app/search/page.tsx` | → `app/(public)/search/page.tsx` (래퍼 제거) |
| 이동 | `app/management/**` | → `app/(admin)/management/**` (내용 동일) |
| 삭제 | `components/layout/UserLayout.tsx` | — |
| 삭제 | `app/boards/[boardId]/page.tsx`, `app/boards/` | — |
| 수정 | `next.config.ts` | `redirects()` 3종 추가 |
| 수정 | `components/layout/CategoryNav.tsx` | `href` 경로 `/category/${name}` 로 변경 |
| 미변경 | `proxy.ts` | matcher (`/management/:path*`) URL 동일 |
| 미변경 | `app/sitemap.ts`, `app/robots.ts`, `app/layout.tsx`, `app/not-found.tsx`, `app/login/page.tsx` | — |

### 3.5 주요 트레이드오프

| 항목 | Trade-off |
| --- | --- |
| 카테고리 URL 변경 | SEO 재색인 비용 ↑ (단기) / 정적 경로 충돌 리스크 제거 (장기) — **301 permanent 로 완화** |
| `/boards/:boardId` shim 제거 | SSR 한 번 감소 (latency ↓) / 백엔드가 숫자 ID 접근을 영구 지원해야 함 — **실측 검증 필요** |
| `(admin)` 그룹 신설 (이번 회차는 디렉토리만) | 1-2 회차에서 Server Component 전환 기반 확보 / 지금 당장은 시각적 변화 0 — **수용 가능** |
| `UserLayout` 삭제 | 향후 "게시글 상세와 홈이 다른 레이아웃이어야 하는" 요구가 생기면 다시 분리 필요 — **가능성 낮음, YAGNI 원칙 적용** |

### 3.6 예상 이슈 및 대응

| 이슈 | 대응 |
| --- | --- |
| 카테고리명에 `/` 나 URL 예약 문자가 포함되어 `encodeURIComponent` 이슈 재발 | 기존 `/[categoryName]/page.tsx` 의 `decodeURIComponent(categoryName)` 로직을 그대로 이전. 링크 측(CategoryNav)도 기존과 동일하게 `c.name` 그대로 사용. |
| `redirects()` 정규식 매칭이 의도치 않게 정적 리소스(`/favicon.ico`) 를 잡음 | negative lookahead 에 명시. 또한 Next 는 `_next/**`·`/api/**` 을 기본적으로 redirect 매칭에서 제외하지만, 방어적으로 lookahead 포함. |
| `/management/**` proxy 매칭 누락 | URL 이 바뀌지 않으므로 영향 없음. 그래도 빌드 후 개발 서버에서 `/management` 접근 → `/login` 리다이렉트 확인. |
| TanStack Query / 기타 컴포넌트의 `/{category}` 하드코딩 | grep 으로 전수 확인(§ 3.4.1 Step 5). 확인 범위: `components/`, `app/`, `lib/`. |
| Next.js 빌드가 `(public)` 와 루트의 page.tsx 동시 존재로 라우트 충돌 경고 | Step 2 완료 전까지는 루트 `app/page.tsx` 를 **먼저 이동**한 뒤 `(public)/page.tsx` 를 추가(동시에 존재하지 않도록). 단일 커밋이 아니라 Step 별로 체크포인트. |

---

## 4. Result (검증 계획)

### 4.1 빌드·타입 검증

- `npm run build` 성공 (경고 수 기존 대비 증가 없음).
- `npx tsc --noEmit` 통과.
- 빌드 로그에서 **라우트 목록**을 캡처해 기대 라우트와 대조:
  - 포함: `/`, `/posts/[slug]`, `/category/[name]`, `/search`, `/login`, `/management`, `/management/categories`, `/management/new-post`, `/management/posts/[id]`, `/management/temporary-storage`, `/sitemap.xml`, `/robots.txt`, `/_not-found`
  - 포함 안 됨: `/[categoryName]`, `/boards/[boardId]`

### 4.2 경로별 수동 QA (개발 서버)

| 시나리오 | 입력 URL | 기대 결과 |
| --- | --- | --- |
| 홈 | `/` | 200, Header + Sidebar 정상 표시 |
| 카테고리 (신) | `/category/Java` | 200, 기존 카테고리 페이지와 동일 UI |
| 카테고리 (구) | `/Java` | 301 → `/category/Java` |
| 검색 | `/search?q=test` | 200, SearchContent 렌더 |
| 게시글 상세 | `/posts/{slug}` | 200 (변경 없음 확인) |
| 구 게시글 URL (숫자) | `/boards/1` | 301 → `/posts/1` → 백엔드가 숫자 ID 로 글 조회 → 200 |
| 구 게시글 URL (slug) | `/boards/hello-world` | 301 → `/posts/hello-world` |
| 관리자 비인증 (HTTPS) | `/management` | `proxy.ts` 에 의해 `/login?from=/management` 리다이렉트 |
| 관리자 비인증 (HTTP dev) | `/management` | proxy 통과 후 `management/layout.tsx` 의 Client 역할 검증으로 리다이렉트 (기존 동작 유지) |
| 예약어 방어 | `/api/v1/posts` | redirect 되지 않고 그대로 백엔드 프록시로 전달 |

### 4.3 정적 검증

- `rg -n "UserLayout"` → 0 건.
- `rg -n "/boards/"` (shim 참조) → `next.config.ts` 의 redirect 설정 외 0 건.
- `rg -n "\\[categoryName\\]"` → 0 건.
- `rg -n "href={\`/\\${"` → 카테고리명을 직접 최상위 경로로 넣는 코드 0 건.

### 4.4 성공 기준

- [ ] 위 4.1·4.2·4.3 전부 통과
- [ ] 레이아웃 변경이 한 파일(`app/(public)/layout.tsx`)만 고쳐도 홈·카테고리·검색·게시글 상세에 모두 반영됨을 **직접 시연**하여 확인
- [ ] 시각적 회귀 없음: 기존 홈/카테고리/검색의 Header·Sidebar·게시글 목록 레이아웃이 동일
- [ ] `components/layout/` 아래 `UserLayout.tsx` 부재

### 4.5 테스트 코드

- 현재 프로젝트에 프론트엔드 단위/통합 테스트 인프라는 없음. 본 작업은 **구조적 리팩터링** 이므로 수동 QA 로 검증하며, 테스트 자동화는 별도 이니셔티브로 분리한다.
- 대신 Next 빌드 결과의 라우트 스냅샷을 `frontend/docs/` 아래 보고서로 남겨 추후 회귀 감지.

---

## 5. 후속 연결 (이월 작업과의 관계)

본 계획서의 산출물(디렉토리 구조)은 다음 후속 회차의 **선행 조건**이다.

- **1-2**: `app/(admin)/management/layout.tsx` 를 Server Component + `cookies()` + `redirect()` 로 전환. 이번 작업에서 `(admin)` 그룹을 이미 분리해 두었으므로 레이아웃 자체의 Client/Server 전환에 집중할 수 있다.
- **1-3**: `app/(public)/error.tsx`, `app/(public)/posts/[slug]/loading.tsx` 등 세그먼트별 특수 파일. 그룹별 경계가 서고 나면 자연스럽게 추가.
- **2-3**: on-demand revalidation. `(public)/category/[name]` 구조가 확정돼야 `revalidateTag("categories")` 의 범위를 명확히 기술할 수 있다.

---

## 6. 체크리스트

- [ ] 계획서 검토 및 사용자 확인
- [ ] Step 1 — `(admin)/management/` 이동 + 빌드 통과
- [ ] Step 2 — `(public)/` 3페이지 이동 + 래퍼 제거 + param 이름 변경
- [ ] Step 3 — `UserLayout.tsx` 삭제
- [ ] Step 4 — `next.config.ts` redirects 추가 + `app/boards/` 삭제
- [ ] Step 5 — `CategoryNav.tsx` 외 내부 링크 업데이트
- [ ] § 4 검증 전 항목 통과
- [ ] 변경 내용 요약 후 사용자 커밋 확인 요청 (커밋 자동 실행 금지)
