# SEO 구현 계획서 — 점검영역 3 (3-1 ~ 3-4)

- **작성일**: 2026-04-20
- **대상 리뷰 문서**: `.claude/reviews/frontend-review-2-seo-query.md` — 점검 영역 3 (SEO 구현)
- **대상 디렉토리**: `frontend/`
- **전제 기술 스택**: Next.js 16.2.3, React 19.2.4, TypeScript 5.x, Bootstrap 5.3.8, html-react-parser 6.0.1 (설치 완료)
- **작업 범위**: 3-1 메타데이터 API / 3-2 사이트 인덱싱 파일 / 3-3 JSON-LD / 3-4 Core Web Vitals

---

## Problem (문제 정의)

### 리뷰가 지적한 공백 (총 ~20건)

| 영역 | 우선순위 | 항목 |
| --- | --- | --- |
| 3-1 | High | `posts/[slug]` canonical, modifiedTime, OG URL, 썸네일 OG 폴백, Twitter Card 누락 |
| 3-1 | Medium | `category/[name]` OG/canonical/robots 부재, `search`/`login` `robots: noindex` 누락, 로그인 메타 부재 |
| 3-1 | Low | 루트 `verification` 주석 (운영 전 활성화 필요) |
| 3-2 | High | `sitemap.ts` `lastModified: new Date()` — `PostSlug.updateDate` 활용 안 함, SITE_URL placeholder 폴백 |
| 3-2 | Medium | 사이트맵에 카테고리 URL 누락, `robots.ts` `disallow: [/search, /login]` 누락 |
| 3-3 | High | JSON-LD 0건 — Article/BlogPosting schema, BreadcrumbList, Organization 전부 부재 |
| 3-4 | High | `next/image` 0건 — 본문 `<img>` raw 렌더링으로 LCP/CLS 악화, `next/font` 0건 — Google Fonts CDN `@import` 로 폰트 로딩 (`globals.css:4`) |
| 3-4 | Medium | Bootstrap 전체 CSS 임포트 (200KB+), 본문 HTML width/height 누락 (백엔드 협의 필요 → 이번 범위 외) |
| 3-4 | Low | `PostList` 썸네일 미표시 (CTR 기회 손실) |

### 왜 중요한가

1. **블로그는 검색 유입이 생명선** — 메타·구조화 데이터가 빈약하면 Google/Naver가 "기술 문서 사이트"로 인식하지 못해 리치 결과(작성자·게시일·썸네일 표시) 미적용.
2. **공유 트래픽 손실** — `metadataBase` 는 이미 있으나 `posts/[slug]` Twitter Card·썸네일 폴백 부재로 X/카카오 공유 시 카드가 부분 깨짐 → 클릭률 하락.
3. **크롤 예산 낭비** — `search`/`login` 이 인덱스 대상에 포함되면 신규 게시글 재크롤 우선순위가 밀림. `sitemap.lastModified` 정확도가 떨어지면 변경된 글 우선 재크롤 최적화가 무력화됨.
4. **Core Web Vitals 점수** — LCP/CLS 는 모바일 SEO 랭킹 시그널. 본문 이미지가 raw `<img>` 로 들어오면 LCP/CLS 둘 다 직격.

### 해결하지 않을 경우

- Google Search Console 커버리지 리포트에서 중복 컨텐츠(canonical 없음 → `/posts/{slug}` vs `/boards/{id}` redirect shim) 경고 지속.
- Rich Results Test 통과 실패 → "Article" 리치 스니펫 미노출.
- `new Date()` lastmod 로 사이트맵 신호가 거짓말 → 크롤러가 모든 글을 동등 취급.

---

## Analyze (분석 및 선택지 검토)

### 현재 코드 실태 (리뷰 vs 실제)

리뷰는 2026-04-19 작성본이라 일부 항목은 App Router Top 1~3 커밋(`5fe9069`)에서 이미 처리됨.

| 영역 | 리뷰 지적 | 실제 코드 상태 (2026-04-20) | 이번 계획 |
| --- | --- | --- | --- |
| 루트 `metadataBase` | 미설정 | **이미 설정** (`app/layout.tsx:10`) — `NEXT_PUBLIC_SITE_URL ?? "http://localhost:3000/"` | 그대로 유지. SITE_URL 폴백만 전역 표준화 |
| 루트 OG/Twitter | 전무 | **이미 있음** (`app/layout.tsx:17-29`) — siteName=`Dev-Moya`, `/og-default.png` 기본 이미지 | 그대로 유지 |
| `posts/[slug]` canonical/twitter/modifiedTime | 누락 | **누락** (`posts/[slug]/page.tsx:33-53`) | **추가 필요** |
| `posts/[slug]` OG 썸네일 폴백 | 누락 | **누락** (`posts/[slug]/page.tsx:41`) | **추가 필요** (루트 `/og-default.png` 상속이 `metadataBase` 로는 OG images 배열엔 자동 상속 안 됨 — 명시 필요) |
| `category/[name]` OG/canonical/robots | 부재 | **부재** (`category/[name]/page.tsx:20-31`) | **추가 필요** |
| `search` `robots: noindex` | 누락 | **누락** (`search/page.tsx:4-7`) | **추가 필요** |
| `login` 메타 + `noindex` | 누락 | **메타 자체 없음** (`login/page.tsx` 최소 구조) | **추가 필요** |
| `sitemap.ts` `lastModified` | `new Date()` | **`new Date()` 그대로** (`sitemap.ts:16`) | **`new Date(updateDate)` 로 교체** |
| `sitemap.ts` 카테고리 | 없음 | **없음** | **추가 필요** |
| `robots.ts` disallow 확장 | 누락 | `"/management/"` 만 | **`/search`, `/login` 추가** |
| JSON-LD | 0건 | **0건 (grep 확인)** | `components/seo/JsonLd.tsx` 신설 |
| `next/image` | 0건 | **0건** (`PostContent.tsx:9-15` `dangerouslySetInnerHTML`) | **html-react-parser + next/image 로 교체** |
| `next/font` | 0건 | **0건** (`globals.css:4` `@import url("https://fonts.googleapis.com/css2?...")`) | **next/font/google 도입** |
| Bootstrap 전체 CSS | 200KB+ | `app/layout.tsx:4` `"bootstrap/dist/css/bootstrap.min.css"` | **현상 유지 — 트레이드오프 섹션 참조** |
| SITE_URL 폴백 불일치 | — | `layout.tsx` 는 `http://localhost:3000/`, `sitemap.ts`/`robots.ts` 는 `https://myblog.com` | **세 파일 폴백 정합성 정리** |

### 핵심 설계 결정: 5가지 트레이드오프

---

#### 결정 1 — `next/font` 도입 방식: `next/font/google` vs `next/font/local` vs 현상 유지

| 옵션 | 장점 | 단점 |
| --- | --- | --- |
| **A. `next/font/google`** | Google Fonts 를 빌드 타임에 자체 호스팅, preload 자동, `font-display: swap` 기본, Layout Shift 감소 | Google 빌드 캐시 종속 (빌드 시 네트워크 필요), 한글 Noto Sans KR 은 subsets `latin` 만 지원 (한글 글리프는 사용 시 다운로드) |
| B. `next/font/local` | 완전 자체 호스팅, 오프라인 빌드 가능 | 폰트 파일을 `public/` 또는 `app/fonts/` 에 직접 넣고 관리 — 추가 번들 크기·관리 부담 |
| C. 현상 유지 (`@import` CDN) | 변경 없음 | font-display 제어 불가 (FOIT/FOUT 깜빡임), preload 안 됨, 서드파티 요청 1개 추가 (Google Fonts), 개인정보 보호(GDPR) 회색지대 |

**선택: A (`next/font/google`)**
- 이유: 신입 포트폴리오 범위에서 "빌드 네트워크 필요" 는 Vercel/CI 표준 환경에서 문제 없음. Layout Shift 감소 효과가 가장 크고 설정 비용 최소.
- 한글 subsets 이슈: Noto Sans KR 은 `subsets: ["latin"]` 지정 시 한글 글리프는 별도 fallback 필요 — 다만 `next/font/google` 가 `unicode-range` 분리로 실제 한글 브라우저에서 자동 동작. 검증 필요 항목 (Result 섹션).

**충돌 정리 전략**: `globals.css:4` 의 `@import url("...fonts.googleapis.com/...")` 제거 + `:root` 의 `--font-sans`/`--font-mono` 선언 제거. `next/font` 의 `variable` 옵션으로 `--font-sans`/`--font-mono` 를 `<html>` 에 주입. 나머지 `globals.css` 는 CSS 변수를 그대로 사용하므로 변경 불필요.

---

#### 결정 2 — `next/image` 본문 적용 범위: 어디까지 적용할 것인가

Tiptap 업로드 이미지는 `src="/api/v1/images/..."` 꼴이고 **width/height 메타가 백엔드에 없음**.

| 옵션 | 장점 | 단점 |
| --- | --- | --- |
| **A. `PostContent` 본문 이미지에만 `next/image` (고정 비율 1200×675)** | LCP/CLS 즉시 개선, html-react-parser 활용 | 실제 이미지 비율과 다를 수 있음 → `style={{ width: "100%", height: "auto" }}` 로 완화, 단 최초 페인트엔 빈 공간 |
| B. 본문 이미지 + 백엔드에 width/height 컬럼 추가 | 완벽한 CLS=0 | **백엔드 협의 필요 → 이번 범위 외** |
| C. 현상 유지 | 변경 없음 | LCP/CLS 악화 지속 |

**선택: A**
- 이유: 리뷰 권장안 그대로. `sizes="(max-width: 768px) 100vw, 768px"` 로 반응형 처리. 본문 이미지가 `article.post-content` 의 `max-width: 768px` 내부이므로 1200 은 아트디렉션용 상한.
- **주의**: `remotePatterns` 는 `next.config.ts` 에 `localhost:8080` / `backend:8080` 이미 등록됨. 추가 설정 없이 동작.

**썸네일 이미지(`PostList`) 는 이번 범위에 포함하지 않음** — 리뷰 Low priority이며 별도 UX 결정 필요 (상단 N개만 priority 등).

---

#### 결정 3 — Bootstrap 축소: 이번 범위에 포함할 것인가

| 옵션 | 장점 | 단점 |
| --- | --- | --- |
| A. 전면 이주 (Bootstrap → Tailwind/CSS Modules) | 번들 절감 200KB+ | 큰 리팩터링, 이번 SEO 범위와 독립된 성격, 리그레션 위험 |
| B. 선택적 SCSS import (`bootstrap/scss/grid`, `bootstrap/scss/forms` 등) | 번들 절감 가능 | react-bootstrap 컴포넌트가 기대하는 클래스 전부 커버 필요 — 실질적 난이도 높음 |
| **C. 현상 유지 + 차기 회차 이월** | 변경 없음 | 지적 사항 미해결 |

**선택: C (현상 유지)**
- 이유: 이번 회차는 SEO 핵심(메타·JSON-LD·CWV 즉시 개선)에 집중. Bootstrap 교체는 UI 전반 회귀 테스트가 필요해 **리뷰의 Medium priority 이면서도 별도 단일 이슈 단위로 분리**하는 것이 건강함. 계획서에 기록만 남김.

---

#### 결정 4 — canonical URL vs `/boards/*` redirect shim 상호작용

현재 `next.config.ts` 에 `/boards/:path*` → `/` 301 redirect 가 있음 (구 URL 호환). canonical 을 `/posts/{slug}` 로 박으면:

- `/boards/123` 유입 → 301 → `/` (상세 페이지 아님) → 현재 redirect 는 게시글 단건 URL 을 유지하지 않음.
- 따라서 canonical 설정만으로 충분. 사용자가 올바른 신규 URL `/posts/{slug}` 로 공유·색인됨.

**선택: canonical 만 설정, redirect shim 은 건드리지 않음.**

---

#### 결정 5 — JSON-LD 저자/조직 정보: 하드코딩 vs 환경변수 vs 단순화

| 옵션 | 장점 | 단점 |
| --- | --- | --- |
| A. `author: { name: "Seongmo Gu" }` 하드코딩 | 단순 | 다중 작성자 확장 불가, 실명 노출 |
| **B. 환경변수 `NEXT_PUBLIC_AUTHOR_NAME` / `NEXT_PUBLIC_AUTHOR_URL`** | 배포 시 주입, 테스트/프로덕션 분리 | 환경변수 1개 추가 |
| C. `author` 생략, `publisher` 만 명시 | 최소 정보 | Article 리치 결과에서 저자 카드 미노출 |

**선택: B (환경변수)**
- 이유: 개인 블로그라도 이름/URL 을 빌드 아티팩트에 하드코딩하지 않는 편이 깔끔. 폴백은 `"Dev-Moya"` 로 두어 환경변수 없어도 동작.

---

### 신규 의존성 / 환경변수 요약

| 종류 | 이름 | 비고 |
| --- | --- | --- |
| 의존성 | 없음 | `html-react-parser@6.0.1` 이미 설치, `next/font` 는 Next.js 내장 |
| 환경변수 | `NEXT_PUBLIC_SITE_URL` | **이미 존재**, 다만 폴백 값 통일 (`http://localhost:3000` 통일, 트레일링 슬래시 제거) |
| 환경변수 | `NEXT_PUBLIC_AUTHOR_NAME` (신규) | 기본값 `"Dev-Moya"` |
| 환경변수 | `NEXT_PUBLIC_AUTHOR_URL` (신규, 선택) | 미지정 시 JSON-LD `author.url` 생략 |

---

## Action (구현 계획 및 설계)

### 목표

리뷰의 SEO 영역 High/Medium 항목을 일괄 처리하여:
1. Google Rich Results Test 에서 `posts/{slug}` 페이지가 "Article" 로 인식됨.
2. Twitter/X 공유 시 `summary_large_image` 카드 정상 노출.
3. 사이트맵의 `lastmod` 가 실제 수정일 기반 + 카테고리 URL 포함.
4. 본문 이미지 CLS 0에 근접, LCP 소폭 개선, 폰트 FOUT/FOIT 제거.

### 변경 대상 파일 목록

#### A. 메타데이터 (3-1)

1. **`frontend/app/(public)/posts/[slug]/page.tsx`** (수정)
   - `generateMetadata` 에 `alternates.canonical`, `openGraph.modifiedTime`, `openGraph.url`, OG 썸네일 폴백, `twitter: { card, title, description, images }` 추가.
   - `default export` 함수 내부에 JSON-LD 삽입 (3-3 참조).

2. **`frontend/app/(public)/category/[name]/page.tsx`** (수정)
   - `generateMetadata` 에 `alternates.canonical: '/' + encodeURIComponent(decoded)` (※ `app/` 라우팅과 실제 URL 기준 확인 필요 — `category/[name]` 이므로 canonical 은 `/category/${encodeURIComponent(decoded)}`), `openGraph: { type: "website", title, description, url }` 추가.

3. **`frontend/app/(public)/search/page.tsx`** (수정)
   - `metadata` 에 `robots: { index: false, follow: true }` 추가.

4. **`frontend/app/login/page.tsx`** (수정)
   - 페이지 상단에 `export const metadata: Metadata = { title: "로그인", robots: { index: false, follow: true } }` 추가.

5. **`frontend/app/layout.tsx`** (수정 — 폰트/Organization 추가)
   - `next/font/google` 임포트 + `<html>` className 적용. `verification` 주석은 이번 범위 외 (운영 배포 시점 결정).

#### B. 사이트맵 / robots (3-2)

6. **`frontend/app/sitemap.ts`** (수정)
   - `getCategoriesV2()` 추가 호출, `Promise.all` 로 병렬화.
   - `lastModified: new Date(post.updateDate)` 로 교체.
   - 카테고리 항목 추가 (`priority: 0.6`, `changeFrequency: "weekly"`).
   - `SITE_URL` 폴백은 `http://localhost:3000` 으로 통일 (운영 환경에서 반드시 `NEXT_PUBLIC_SITE_URL` 주입).

7. **`frontend/app/robots.ts`** (수정)
   - `disallow: ["/management/", "/search", "/login"]` 배열로 확장.
   - `SITE_URL` 폴백 동일하게 통일.

#### C. JSON-LD (3-3)

8. **`frontend/components/seo/JsonLd.tsx`** (신규) — Server Component
   - `{ data: Record<string, unknown> }` 인자, `<script type="application/ld+json">` 반환.

9. **`frontend/lib/seo.ts`** (신규) — JSON-LD 빌더 헬퍼
   - `buildArticleSchema(post, siteUrl)` — BlogPosting
   - `buildBreadcrumbSchema(post, siteUrl)` — BreadcrumbList
   - `buildOrganizationSchema(siteUrl)` — Organization
   - 이유: 페이지 컴포넌트에서 큰 객체 리터럴을 숨겨 가독성↑, 단위 테스트도 용이.

10. **`frontend/app/(public)/posts/[slug]/page.tsx`** (수정 — JSON-LD 주입)
    - `<article>` 최상단에 `<JsonLd data={buildArticleSchema(...)} />`, `<JsonLd data={buildBreadcrumbSchema(...)} />`.

11. **`frontend/app/layout.tsx`** (수정 — Organization JSON-LD)
    - `<body>` 아래 `<JsonLd data={buildOrganizationSchema(SITE_URL)} />` 1회.

#### D. Core Web Vitals (3-4)

12. **`frontend/components/posts/PostContent.tsx`** (재작성) — Server Component 유지
    - `html-react-parser` + `next/image` 조합으로 본문 `<img>` 를 `<Image>` 로 교체.
    - 기타 HTML(`<pre>`, `<code>`, `<p>` 등)은 parse 가 자동 보존.

13. **`frontend/app/layout.tsx`** (수정 — next/font 적용)
    - `Noto_Sans_KR` / `JetBrains_Mono` 를 `next/font/google` 에서 import, `variable` 옵션으로 CSS 변수 주입. `<html className={\`${noto.variable} ${mono.variable}\`}>`.

14. **`frontend/app/globals.css`** (수정 — 폰트 선언 정리)
    - 1-4 줄 (`@import url("https://fonts.googleapis.com/...")`) 제거.
    - `:root` 의 `--font-sans` / `--font-mono` 값을 `var(--font-sans)` 가 아닌 폰트 fallback 스택만 남기고, `next/font` 가 주입한 `--font-sans`/`--font-mono` 을 우선 사용하도록 변경.
    - 구체적으로: `--font-sans: var(--font-sans-next, "Noto Sans KR"), sans-serif;` 형태 또는 `next/font` 의 `variable` 이름을 `--font-sans` 로 직접 쓰면 덮어쓰기.

    → **구현 전략**: `next/font` `variable: "--font-sans"` 로 지정 → `:root` 의 `--font-sans` 선언 제거 (중복 회피). `--font-mono` 동일.

### 주요 트레이드오프 (요약)

| 항목 | 선택 | 비용 |
| --- | --- | --- |
| 폰트 | `next/font/google` | 빌드 시 네트워크 필요 (Vercel/CI OK) |
| 본문 이미지 | 고정 비율 1200×675 + height:auto | 실제 비율 다를 시 최초 페인트 빈 공간 |
| Bootstrap | 현상 유지 | 번들 감소 기회 지연 |
| JSON-LD 저자 | 환경변수 | ENV 키 1개 추가 |
| canonical | 절대 경로 하드코딩 (`/posts/{slug}`) | `metadataBase` 와 조합돼 자동 절대 URL 됨 |

### 예상 이슈 및 대응

1. **`next/font` 빌드 실패 (네트워크 차단 환경)**
   - 대응: 빌드 실패 시 빠른 폴백 = `next/font/local` 로 전환. 이번 범위에선 `google` 로 진행하되, CI 실패 관측되면 즉시 전환.

2. **Noto Sans KR 한글 글리프 미적용**
   - 검증: 빌드 후 실제 페이지에서 한글이 fallback(`sans-serif`)로 렌더링되면 문제. `next/font/google` 이 `unicode-range` 분리로 한글도 동작하는지 Lighthouse/DevTools 네트워크로 확인.

3. **`html-react-parser` + `next/image` 의 `src` 가 백엔드 업로드 상대 경로(`/api/v1/images/...`)인 경우**
   - `next/image` 는 절대 URL 또는 `remotePatterns` 매칭 필수. 현재 `remotePatterns` 에 `localhost:8080` / `backend:8080` 등록돼 있음. 본문 HTML 의 `src` 가 `/api/v1/images/...` 같은 same-origin 상대 경로면 next/image 가 same-origin으로 취급 → OK.
   - 단, 백엔드가 S3 CDN URL(`https://s3.ap-northeast-2.amazonaws.com/...`)을 내려주면 `remotePatterns` 확장 필요. 본문 이미지 1~2개로 스모크 테스트 필수.

4. **`canonical` 이 상대경로일 때 `metadataBase` 가 절대 URL 로 변환 — 검증**
   - `metadataBase: new URL(SITE_URL)` + `alternates: { canonical: "/posts/my-slug" }` → 최종 `<link rel="canonical" href="http://localhost:3000/posts/my-slug" />`. 빌드 후 실제 HTML 확인.

5. **JSON-LD `image` 필드는 절대 URL 권장**
   - `post.thumbnailUrl` 이 상대 경로(`/api/v1/...`)면 검증기에서 warning. 빌더에서 `new URL(thumbnailUrl, siteUrl).toString()` 처리.

6. **`app/layout.tsx` 가 Server Component 인지 확인** — `next/font` 는 Server 전용. 현재 layout 은 Server Component ✅.

### 구현 순서 (독립 단위로 분리 — 커밋 단위)

1. **C1. 메타데이터 보강** (3-1) — `posts/[slug]`, `category/[name]`, `search`, `login` 메타 추가.
2. **C2. 사이트맵/robots 정확도 개선** (3-2) — `sitemap.ts`, `robots.ts`.
3. **C3. JSON-LD 인프라 + 게시글 적용** (3-3) — `components/seo/JsonLd.tsx`, `lib/seo.ts`, `posts/[slug]/page.tsx`, `app/layout.tsx` Organization.
4. **C4. next/font 도입** (3-4 일부) — `app/layout.tsx`, `app/globals.css`.
5. **C5. PostContent `next/image` 전환** (3-4 일부) — `components/posts/PostContent.tsx`.

각 단계가 독립 검증 가능하도록 커밋 분리 (리뷰 시 회귀 지점 특정 용이).

---

## Result (검증 계획)

### 단계별 검증

#### C1. 메타데이터 검증
- **빌드 후 HTML 확인** — `npm run build && npm run start` 후 `/posts/my-post` curl → 다음이 모두 있는지:
  - `<link rel="canonical" href="http://localhost:3000/posts/my-post" />`
  - `<meta property="og:modified_time" content="..." />`
  - `<meta name="twitter:card" content="summary_large_image" />`
  - `/search` / `/login` → `<meta name="robots" content="noindex,follow" />`
- **카테고리 페이지**: `/category/java` (encodeURIComponent 경로) → canonical + OG 존재.

#### C2. 사이트맵/robots 검증
- `curl http://localhost:3000/sitemap.xml` — `<lastmod>` 가 실제 `updateDate` 기반인지 (`2026-04-20` 이 아닌 글마다 다른 날짜).
- 카테고리 `<url>` 항목 포함 여부.
- `curl http://localhost:3000/robots.txt` — `Disallow: /search` + `Disallow: /login` 확인.

#### C3. JSON-LD 검증
- **Google Rich Results Test**: `https://search.google.com/test/rich-results?url=...` (로컬은 ngrok 필요. 또는 HTML 문자열 직접 검사)
- **validator.schema.org**: `https://validator.schema.org/` 에 빌드 HTML 페이스트 → Article / BreadcrumbList / Organization 세 개 모두 0 에러.
- **수작업 파싱**: `curl /posts/my-post | grep -A 30 ld+json` 으로 스키마 프린트 확인.

#### C4. next/font 검증
- 빌드 후 네트워크 탭에서 `fonts.googleapis.com` 요청 **없음** (자체 호스팅). `_next/static/media/...woff2` 요청으로 바뀜.
- 한글 표시 정상 (FOUT 없이 즉시 Noto Sans KR 적용).
- Lighthouse Performance: **Ensure text remains visible during webfont load** 통과.

#### C5. `next/image` 검증
- `/posts/my-post` 본문 이미지가 `<img>` 가 아닌 `<img srcset="_next/image?url=...">` 로 치환.
- Lighthouse Performance 재측정: LCP 시간 감소, CLS < 0.1.
- 스모크: S3 URL 이미지 1건 포함 글로 테스트 (`remotePatterns` 확장 필요 여부 판정).

### 성공 기준

| 기준 | 측정 방법 | 목표 |
| --- | --- | --- |
| Rich Results 검증 | validator.schema.org | Article / Breadcrumb / Organization 0 에러 |
| 사이트맵 정확도 | curl sitemap.xml | 모든 글의 `<lastmod>` 가 고유, 카테고리 URL 포함 |
| Twitter Card | Twitter Card Validator (`https://cards-dev.twitter.com/validator` — 서비스 상태에 따라 대체: 브라우저 메타 검사) | `summary_large_image` 카드 미리보기 정상 |
| Facebook OG | Facebook Sharing Debugger | 썸네일 + 제목 + 설명 정상 |
| LCP/CLS | Lighthouse (시크릿 모드) | LCP < 2.5s, CLS < 0.1 |
| 폰트 로딩 | DevTools Network | `fonts.googleapis.com` 0 요청, woff2 `/_next/static/media/` |
| 회귀 테스트 | 빌드 + `/`, `/posts/{slug}`, `/category/{name}`, `/search`, `/login`, `/management` 수동 네비 | 레이아웃 깨짐 없음, 에러 없음 |

### 재검토 체크리스트 (CLAUDE.md §5)

- [ ] 모든 `metadataBase` 조합이 실제 절대 URL 로 변환되는지 curl 로 확인
- [ ] `globals.css` 의 `@import` 제거 후에도 `--font-sans`/`--font-mono` 참조 CSS 규칙이 정상 작동 (`body`, `.post-content`, `.code-block` 등)
- [ ] `PostContent` 재작성 후 기존 본문(코드블록, 인용, 테이블) 스타일 유지
- [ ] JSON-LD 의 `image`, `author.url`, `publisher.logo.url` 모두 절대 URL
- [ ] `sitemap.ts` 가 백엔드 미실행 시에도 크래시 없이 기본 항목(홈만) 반환 (`.catch(() => [])` 유지)
- [ ] 불필요 로그/디버그 코드 없음
- [ ] 기존 redirect shim(`/boards/*` → `/`) 과 canonical 충돌 없음
- [ ] 관리자 페이지(`/management/*`)가 사이트맵/JSON-LD 에 유출되지 않음 (redirect 안 되므로 이미 OK, 확인만)

---

## 차기 회차 이월

- **Bootstrap 축소** (3-4 Medium) — 단일 이슈로 분리, UI 회귀 테스트 필요.
- **본문 이미지 width/height 메타** (3-4 Medium) — 백엔드 `Post.content` 업로드 파이프라인에 width/height 저장 추가 — **백엔드 계획서 별도 필요**.
- **`PostList` 썸네일 표시** (3-4 Low) — CTR UX 개선, `priority` 전략 설계 필요.
- **Search Console `verification` 태그 활성화** — 운영 배포 시점에 `google-site-verification` 환경변수 주입.

---

## 참고 — 현재 코드 대비 구체 diff 스케치 (구현 세션용)

### `posts/[slug]/page.tsx` generateMetadata 변경 포인트
- `return` 객체에 `alternates: { canonical: \`/posts/\${post.slug}\` }` 추가.
- `openGraph` 에 `url`, `modifiedTime`, 그리고 `images` 삼항: `post.thumbnailUrl ? [{ url: post.thumbnailUrl, width: 1200, height: 630 }] : [{ url: "/og-default.png", width: 1200, height: 630 }]`.
- `twitter: { card: "summary_large_image", title, description, images }` 추가.

### `category/[name]/page.tsx` generateMetadata 변경 포인트
- `alternates: { canonical: \`/category/\${encodeURIComponent(decoded)}\` }`.
- `openGraph: { type: "website", title, description, url: \`/category/\${encodeURIComponent(decoded)}\` }`.

### `sitemap.ts` 변경 포인트
- `const [posts, categories] = await Promise.all([getAllSlugs().catch(() => []), getCategoriesV2().catch(() => [])])`.
- posts 엔트리: `lastModified: new Date(p.updateDate)`.
- categories 엔트리: `url: \`\${SITE_URL}/category/\${encodeURIComponent(c.name)}\``.

### `robots.ts` 변경 포인트
- `disallow: ["/management/", "/search", "/login"]`.

### `components/seo/JsonLd.tsx` (신규)
```tsx
export function JsonLd({ data }: { data: Record<string, unknown> }) {
  return (
    <script
      type="application/ld+json"
      dangerouslySetInnerHTML={{ __html: JSON.stringify(data) }}
    />
  );
}
```

### `lib/seo.ts` (신규) — 시그니처 스케치
```ts
export function buildArticleSchema(post: Post, siteUrl: string): Record<string, unknown>;
export function buildBreadcrumbSchema(post: Post, siteUrl: string): Record<string, unknown>;
export function buildOrganizationSchema(siteUrl: string): Record<string, unknown>;
```

### `PostContent.tsx` 재작성 스케치
```tsx
import parse, { type DOMNode, Element } from "html-react-parser";
import Image from "next/image";

export default function PostContent({ content }: { content: string }) {
  return (
    <div className="post-content">
      {parse(content, {
        replace: (node: DOMNode) => {
          if (node instanceof Element && node.name === "img") {
            const { src, alt = "" } = node.attribs;
            if (!src) return;
            return (
              <Image
                src={src}
                alt={alt}
                width={1200}
                height={675}
                sizes="(max-width: 768px) 100vw, 768px"
                style={{ width: "100%", height: "auto" }}
              />
            );
          }
        },
      })}
    </div>
  );
}
```

### `app/layout.tsx` next/font 삽입 스케치
```tsx
import { Noto_Sans_KR, JetBrains_Mono } from "next/font/google";

const sans = Noto_Sans_KR({
  subsets: ["latin"], weight: ["400", "500", "600", "700"],
  display: "swap", variable: "--font-sans",
});
const mono = JetBrains_Mono({
  subsets: ["latin"], weight: ["400", "500", "700"],
  display: "swap", variable: "--font-mono",
});

export default function RootLayout({ children }) {
  return (
    <html lang="ko" className={`${sans.variable} ${mono.variable}`}>
      <body>
        <Providers>{children}</Providers>
        <JsonLd data={buildOrganizationSchema(SITE_URL)} />
        <Script src="..." strategy="lazyOnload" />
      </body>
    </html>
  );
}
```

### `globals.css` 삭제 대상
- 1–4 라인 `@import url("https://fonts.googleapis.com/...")` **제거**.
- `:root` 내부 `--font-sans`, `--font-mono` 선언 **제거** (next/font 가 덮어씀).
