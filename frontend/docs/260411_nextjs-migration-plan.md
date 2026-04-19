# Next.js App Router 마이그레이션 계획서

> 작성일: 2026-04-11  
> 참조: Context7 MCP (Next.js 공식 문서), frontend/CLAUDE.md  
> 목적: React.js SPA → Next.js App Router 전환으로 SEO 완전 지원  

---

## 1. 작업 목표 및 배경

### 1-1. 목표

구글·네이버 검색 엔진에 블로그 게시글이 노출되도록 **SSG(정적 생성) + ISR(점진적 정적 재생성)** 기반의 Next.js App Router로 전환한다.

### 1-2. 배경

| 항목 | 현재 (React SPA / CSR) | 목표 (Next.js / SSG+ISR) |
|------|----------------------|------------------------|
| 구글 노출 | 지연·불완전 (JS 큐 처리) | 완전 지원 |
| 네이버 노출 | 사실상 불가 | 완전 지원 |
| 초기 로딩(FCP) | JS 번들 다운로드 후 렌더 | 완성된 HTML 즉시 반환 |
| SEO 메타태그 | react-helmet (크롤러 무의미) | generateMetadata (서버 주입) |

### 1-3. 이미 준비된 것 (백엔드)

- **Slug 시스템**: `/api/v1/posts/{slug}` 엔드포인트 구현 완료
- **SEO 필드**: `seoTitle`, `seoDescription`, `seoKeywords` DB 저장 완료
- Next.js 전환 시 데이터는 그대로 활용 가능

---

## 2. 현재 스택 분석

### 2-1. 현재 기술 스택 (frontend/CLAUDE.md 기준)

```
React 18.2.0 + Vite 6.4.1
React Router 7.13.1          ← Next.js 파일 라우팅으로 완전 교체
Redux Toolkit 2.11.2         ← Server Component와 충돌, 구조 변경 필요
Redux Persist 6.0.0          ← Server Component와 충돌, 제거 검토
TanStack Query 5.90.21       ← Client Component에서 그대로 유지 가능
Axios 1.13.6                 ← Server Component에서 fetch API로 전환 권장
Tiptap 3.20.1                ← Client Component로 격리, 그대로 유지
Bootstrap 5.3.8              ← 그대로 유지 가능
Styled Components 6.3.11     ← Server Component 미지원, CSS Module 전환 필요
```

### 2-2. 현재 라우트 구조 (App.js)

```
/                          → Home (게시글 목록)
/boards                    → Home (동일)
/:categoryName             → PageByCategory (카테고리별 게시글)
/search                    → SearchPage (검색)
/boards/:boardId           → BoardDetail (게시글 상세)  ← slug로 변경 필요
/login                     → LoginForm (로그인)
/management                → Management (관리자 대시보드)
/management/new-post       → BoardEditor (게시글 작성)
/management/boards         → Management
/management/boards/:boardId → BoardEditForm (게시글 수정)
/management/categories     → CategoryList (카테고리 관리)
/management/temporary-storage → TemporaryStorage (임시 저장)
```

---

## 3. Next.js App Router 디렉토리 구조 설계

### 3-1. 목표 디렉토리 구조

```
frontend/
├── app/                           # Next.js App Router 루트
│   ├── layout.tsx                 # RootLayout (공통 HTML shell)
│   ├── page.tsx                   # / → 게시글 목록 (SSG)
│   ├── globals.css
│   │
│   ├── [categoryName]/            # /:categoryName → 카테고리 페이지
│   │   └── page.tsx               # SSG + generateStaticParams
│   │
│   ├── posts/                     # /posts/:slug → 게시글 상세
│   │   └── [slug]/
│   │       └── page.tsx           # SSG + ISR (revalidate: 60)
│   │
│   ├── search/                    # /search → 검색 결과
│   │   └── page.tsx               # CSR (동적 쿼리)
│   │
│   ├── login/                     # /login → 로그인
│   │   └── page.tsx               # CSR
│   │
│   └── management/                # /management → 관리자 (전체 CSR)
│       ├── layout.tsx             # 인증 Guard
│       ├── page.tsx               # 관리자 대시보드
│       ├── new-post/
│       │   └── page.tsx
│       ├── posts/
│       │   └── [id]/
│       │       └── page.tsx       # 게시글 수정
│       ├── categories/
│       │   └── page.tsx
│       └── temporary-storage/
│           └── page.tsx
│
├── components/                    # 공유 컴포넌트
│   ├── layout/
│   │   ├── Header.tsx
│   │   └── UserLayout.tsx
│   ├── boards/
│   │   ├── BoardList.tsx          # Server or Client Component
│   │   ├── BoardDetail.tsx        # Server Component
│   │   └── BoardEditor.tsx        # 'use client'
│   ├── comments/
│   │   ├── CommentList.tsx        # 'use client' (동적 데이터)
│   │   └── CommentForm.tsx        # 'use client'
│   └── ...
│
├── lib/                           # 서버 사이드 유틸리티
│   ├── api.ts                     # fetch 기반 API 클라이언트
│   └── queryKeys.ts
│
├── store/                         # Redux (Client 전용)
│   └── index.ts
│
├── providers/                     # Client Providers 래퍼
│   └── Providers.tsx              # 'use client' — Redux, TanStack Query
│
└── public/
    ├── robots.txt
    └── sitemap.xml (or app/sitemap.ts)
```

### 3-2. 라우트 URL 변경 사항

| 현재 (React Router) | Next.js App Router | 변경 이유 |
|---|---|---|
| `/boards/:boardId` | `/posts/:slug` | SEO — slug가 의미있는 URL |
| `/boards` | `/` (redirect) | 중복 라우트 제거 |
| `/management/boards/:boardId` | `/management/posts/:id` | 통일 |

> **중요**: 기존 `/boards/:boardId` URL로 유입된 링크는 `next.config.ts`의 `redirects`로 301 리다이렉트 처리

---

## 4. 렌더링 전략 페이지별 설계

```
페이지                   렌더링 방식    이유
─────────────────────────────────────────────────────────
/                       SSG            카테고리 목록 + 최신 게시글 — 변경 빈도 낮음
/[categoryName]         SSG + ISR      카테고리별 게시글 — 새 게시글 작성 시 갱신
/posts/[slug]           SSG + ISR      게시글 본문 — SEO 핵심, 수정 시 갱신
/search                 CSR            검색어 기반 동적 결과 — 사전 생성 불가
/login                  CSR            인증 상태에 의존, SEO 불필요
/management/**          CSR            관리자 전용, SEO 불필요, 인증 필요
```

### 4-1. SSG + ISR 구현 예시 (`/posts/[slug]/page.tsx`)

Context7 MCP 공식 문서 기반:

```tsx
// app/posts/[slug]/page.tsx
import type { Metadata } from 'next'

// ISR: 60초마다 캐시 무효화
export const revalidate = 60

// 빌드 시 모든 slug 사전 생성
export async function generateStaticParams() {
  const res = await fetch(`${process.env.NEXT_PUBLIC_API_URL}/api/v1/posts`)
  const posts = await res.json()
  return posts.data.map((post: { slug: string }) => ({ slug: post.slug }))
}

// SEO 메타태그 동적 생성
export async function generateMetadata(
  { params }: { params: Promise<{ slug: string }> }
): Promise<Metadata> {
  const { slug } = await params
  const res = await fetch(
    `${process.env.NEXT_PUBLIC_API_URL}/api/v1/posts/${slug}`,
    { next: { revalidate: 60 } }
  )
  const post = await res.json()

  return {
    title: post.seoTitle || post.title,
    description: post.seoDescription,
    keywords: post.seoKeywords,
    openGraph: {
      title: post.title,
      description: post.seoDescription,
      images: post.thumbnail ? [{ url: post.thumbnail }] : [],
      type: 'article',
    },
    robots: {
      index: true,
      follow: true,
      googleBot: {
        index: true,
        follow: true,
        'max-image-preview': 'large',
        'max-snippet': -1,
      },
    },
  }
}

// Server Component — 서버에서 fetch 후 완성된 HTML 반환
export default async function PostPage(
  { params }: { params: Promise<{ slug: string }> }
) {
  const { slug } = await params
  const res = await fetch(
    `${process.env.NEXT_PUBLIC_API_URL}/api/v1/posts/${slug}`,
    { next: { revalidate: 60 } }
  )
  const post = await res.json()

  return (
    <article>
      <h1>{post.title}</h1>
      {/* 게시글 본문 렌더링 */}
      <PostContent content={post.content} />
      {/* 댓글은 Client Component로 분리 */}
      <CommentSection postId={post.id} />
    </article>
  )
}
```

### 4-2. 카테고리 목록 홈 (`/page.tsx`)

```tsx
// app/page.tsx
export const revalidate = 300  // 5분

export default async function HomePage() {
  const [categoriesRes, postsRes] = await Promise.all([
    fetch(`${process.env.NEXT_PUBLIC_API_URL}/api/v1/categories`,
      { next: { revalidate: 300 } }),
    fetch(`${process.env.NEXT_PUBLIC_API_URL}/api/v1/posts?page=0&size=10`,
      { next: { revalidate: 60 } }),
  ])

  const categories = await categoriesRes.json()
  const posts = await postsRes.json()

  return <HomeView categories={categories} posts={posts} />
}
```

---

## 5. SEO 구현 상세 설계

### 5-1. Root Layout 메타데이터 (기본값)

```tsx
// app/layout.tsx
import type { Metadata } from 'next'

export const metadata: Metadata = {
  title: {
    default: 'MyBlog',
    template: '%s | MyBlog',  // 각 페이지 제목 뒤 블로그명 자동 추가
  },
  description: '개발 블로그 — 기술 지식을 공유합니다.',
  verification: {
    google: 'GOOGLE_SEARCH_CONSOLE_CODE',
    other: {
      'naver-site-verification': 'NAVER_WEBMASTER_CODE',  // 네이버 서치어드바이저
    },
  },
}
```

### 5-2. Sitemap 자동 생성

```tsx
// app/sitemap.ts
import { MetadataRoute } from 'next'

export default async function sitemap(): Promise<MetadataRoute.Sitemap> {
  const res = await fetch(`${process.env.NEXT_PUBLIC_API_URL}/api/v1/posts`)
  const posts = await res.json()

  const postEntries = posts.data.map((post: any) => ({
    url: `https://myblog.com/posts/${post.slug}`,
    lastModified: new Date(post.updatedAt),
    changeFrequency: 'weekly' as const,
    priority: 0.8,
  }))

  return [
    { url: 'https://myblog.com', lastModified: new Date(), priority: 1.0 },
    ...postEntries,
  ]
}
```

### 5-3. robots.txt

```tsx
// app/robots.ts
import { MetadataRoute } from 'next'

export default function robots(): MetadataRoute.Robots {
  return {
    rules: { userAgent: '*', allow: '/', disallow: '/management/' },
    sitemap: 'https://myblog.com/sitemap.xml',
  }
}
```

---

## 6. 상태 관리 전환 설계

### 6-1. 핵심 원칙

Next.js App Router에서 **Server Component는 Redux/TanStack Query 사용 불가**.  
따라서 상태 관리 레이어를 명확히 분리한다.

```
Server Component  →  fetch API 직접 사용 (캐싱은 Next.js가 관리)
Client Component  →  TanStack Query (서버 상태) + Redux (클라이언트 상태)
```

### 6-2. Providers 래퍼 패턴

```tsx
// providers/Providers.tsx
'use client'

import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { Provider as ReduxProvider } from 'react-redux'
import { store } from '@/store'
import { useState } from 'react'

export function Providers({ children }: { children: React.ReactNode }) {
  // Server Component 환경에서 QueryClient 싱글턴 방지
  const [queryClient] = useState(() => new QueryClient({
    defaultOptions: {
      queries: {
        staleTime: 1000 * 60 * 5,
        refetchOnWindowFocus: false,
      },
    },
  }))

  return (
    <ReduxProvider store={store}>
      <QueryClientProvider client={queryClient}>
        {children}
      </QueryClientProvider>
    </ReduxProvider>
  )
}
```

```tsx
// app/layout.tsx
import { Providers } from '@/providers/Providers'

export default function RootLayout({ children }) {
  return (
    <html lang="ko">
      <body>
        <Providers>
          {children}
        </Providers>
      </body>
    </html>
  )
}
```

### 6-3. Redux 범위 축소

현재 Redux가 담당하는 역할을 재검토하여 최소화:

| 현재 Redux 역할 | Next.js 전환 후 |
|---|---|
| 로그인 상태 (`isLoggedIn`, `accessToken`) | Redux 유지 (클라이언트 인증 상태) |
| Redux Persist (localStorage) | Next.js cookies / 서버 세션으로 이관 검토 |
| 게시글 목록 캐싱 | TanStack Query로 이관 (현재와 동일) |

### 6-4. Styled Components → CSS Module 전환

Styled Components는 Server Component에서 동작하지 않는다. 선택지:

| 방법 | 작업량 | 권장도 |
|------|--------|--------|
| **CSS Module로 전환** | 중간 | ★★★ (공식 권장) |
| Styled Components `'use client'` 격리 유지 | 낮음 | ★★ (임시방편) |
| Tailwind CSS로 교체 | 높음 | ★ (과도한 리팩토링) |

> **권장**: CSS Module 전환. 현재 Styled Components 사용 파일은  
> `Container.style.js`, `NavBarElements.js` 2개로 범위가 제한적.

---

## 7. 인증 (관리자) 구현

### 7-1. middleware.ts (Next.js Edge Middleware)

```ts
// middleware.ts
import { NextResponse } from 'next/server'
import type { NextRequest } from 'next/server'

export function middleware(request: NextRequest) {
  const token = request.cookies.get('access_token')

  if (request.nextUrl.pathname.startsWith('/management')) {
    if (!token) {
      return NextResponse.redirect(new URL('/login', request.url))
    }
  }
  return NextResponse.next()
}

export const config = {
  matcher: ['/management/:path*'],
}
```

> 현재 `ProtectedRoute` 컴포넌트를 Edge Middleware로 대체 — 서버 레벨에서 차단하므로 보안 강화.

---

## 8. 단계별 마이그레이션 계획

### Phase 0: 사전 준비 (0.5주)

- [ ] `frontend/` 디렉토리 하위에 `frontend-next/` 신규 Next.js 프로젝트 초기화
- [ ] `next.config.ts` 기본 설정 (API URL 환경변수, rewrites, redirects)
- [ ] `tsconfig.json` 경로 별칭(`@/`) 설정
- [ ] 공통 타입 파일 (`types/`) 작성
- [ ] 기존 API 클라이언트를 `fetch` 기반 `lib/api.ts`로 추출

```bash
npx create-next-app@latest frontend-next \
  --typescript --app --src-dir --import-alias "@/*"
```

### Phase 1: 공통 레이아웃 + 정적 자산 이전 (0.5주)

- [ ] `app/layout.tsx` — RootLayout, 글로벌 CSS, Providers 연결
- [ ] `components/layout/Header.tsx` 이전 (Client Component)
- [ ] `components/layout/CategoryNav.tsx` 이전
- [ ] Bootstrap import 설정
- [ ] `public/` 이미지·폰트 이전
- [ ] 404 페이지 (`app/not-found.tsx`)

### Phase 2: SEO 핵심 페이지 — 게시글 (1주) ★ 최우선

- [ ] `app/posts/[slug]/page.tsx` — SSG + ISR + generateMetadata
  - `generateStaticParams()` — 전체 slug 사전 생성
  - `generateMetadata()` — seoTitle/Description/Keywords → OG 태그
  - `PostContent` 컴포넌트 (Server Component)
  - `CommentSection` 컴포넌트 분리 (`'use client'`)
  - `BoardLike` 컴포넌트 분리 (`'use client'`)
- [ ] `app/sitemap.ts` — 자동 sitemap.xml 생성
- [ ] `app/robots.ts` — robots.txt 생성
- [ ] 구글 서치 콘솔 / 네이버 서치어드바이저 인증 메타태그 추가

### Phase 3: 목록 페이지 이전 (0.5주)

- [ ] `app/page.tsx` — 홈(게시글 목록) SSG
- [ ] `app/[categoryName]/page.tsx` — 카테고리별 목록 SSG + generateStaticParams
  - 유효하지 않은 categoryName → 404 처리
- [ ] `app/search/page.tsx` — 검색 결과 CSR (TanStack Query)
- [ ] `components/boards/BoardList.tsx` 재사용

### Phase 4: 인증 + 관리자 페이지 (1주)

- [ ] `middleware.ts` — Edge Middleware 인증 가드
- [ ] `app/login/page.tsx` — 로그인 폼 CSR
- [ ] `app/management/layout.tsx` — 관리자 레이아웃
- [ ] `app/management/page.tsx` — 대시보드
- [ ] `app/management/new-post/page.tsx` — 게시글 작성 (Tiptap 에디터)
- [ ] `app/management/posts/[id]/page.tsx` — 게시글 수정
- [ ] `app/management/categories/page.tsx` — 카테고리 관리
- [ ] `app/management/temporary-storage/page.tsx` — 임시 저장

### Phase 5: 정리 및 배포 (0.5주)

- [ ] `next.config.ts` redirects — 구 URL(`/boards/:id`) → 신 URL(`/posts/:slug`) 301 리다이렉트
- [ ] Docker 이미지 전환 (`node:18-alpine` → Next.js standalone 빌드)
- [ ] Nginx 설정 수정 (SPA fallback 제거, Next.js standalone 서버로 교체)
- [ ] 구글 서치 콘솔 sitemap 제출
- [ ] 네이버 서치어드바이저 사이트 등록
- [ ] 기존 `frontend/` 디렉토리 제거 또는 아카이브

---

## 9. 변경 대상 파일 목록

### 제거 (Next.js에서 불필요)

| 파일 | 이유 |
|------|------|
| `vite.config.js` | Next.js 자체 빌드 사용 |
| `src/App.js` | `app/` 디렉토리로 대체 |
| `src/index.js` | `app/layout.tsx`로 대체 |
| `src/apiConfig.js` | `lib/api.ts`로 대체 |
| `src/components/Layout/ProtectedRoute.js` | `middleware.ts`로 대체 |
| `src/components/Layout/UserLayout.js` | `app/layout.tsx`로 통합 |

### 재사용 (이전 후 그대로 활용)

| 파일 | 변환 방법 |
|------|-----------|
| `src/services/boardApi.js` | `lib/boardApi.ts`로 이전, fetch 기반으로 전환 |
| `src/services/queryKeys.js` | `lib/queryKeys.ts` 그대로 이전 |
| `src/redux/store.js` | `store/index.ts` 그대로 이전 |
| `src/redux/userSlice.js` | `store/userSlice.ts` 그대로 이전 |
| `src/components/Boards/BoardEditor.js` | `'use client'` 선언 추가 후 이전 |
| `src/components/Comments/*.js` | `'use client'` 선언 추가 후 이전 |
| `src/hooks/useQueries.js` | `hooks/useQueries.ts`, Client Component 전용 |
| `src/components/Boards/BoardLike.js` | `'use client'` 선언 추가 후 이전 |

### 신규 작성

| 파일 | 내용 |
|------|------|
| `app/layout.tsx` | RootLayout + 글로벌 메타데이터 |
| `app/posts/[slug]/page.tsx` | SSG + ISR + generateMetadata |
| `app/[categoryName]/page.tsx` | SSG + generateStaticParams |
| `app/sitemap.ts` | sitemap.xml 자동 생성 |
| `app/robots.ts` | robots.txt 생성 |
| `middleware.ts` | Edge 인증 가드 |
| `providers/Providers.tsx` | Redux + TanStack Query 래퍼 |
| `lib/api.ts` | fetch 기반 API 클라이언트 |
| `next.config.ts` | 리다이렉트, 환경변수, 이미지 도메인 설정 |

---

## 10. 예상 이슈 및 대응 방안

### Issue 1: `window is not defined` (Server Component 환경)

**원인**: `localStorage`, `window`, `document` 접근 코드가 Server Component에서 실행  
**대응**:
```tsx
// 해당 컴포넌트 최상단에 추가
'use client'
```
또는 동적 임포트:
```tsx
const ComponentWithWindow = dynamic(() => import('./ComponentWithWindow'), { ssr: false })
```

### Issue 2: Styled Components SSR 불지원

**원인**: Styled Components가 런타임 CSS 생성 → Server Component 렌더링 중 스타일 없음  
**대응**: CSS Module로 점진적 전환. 단기적으로는 해당 컴포넌트에 `'use client'` 추가.

### Issue 3: Redux Persist + Server Component 충돌

**원인**: `localStorage` 기반 Redux Persist는 서버에서 실행 불가  
**대응**: `Providers.tsx` 내부(`'use client'`)에서만 Redux Store 초기화 → Server Component는 Redux 접근 금지

### Issue 4: React Router 기반 네비게이션 코드

**원인**: `useNavigate`, `useParams`, `Link` (React Router) → Next.js API로 교체 필요  
**대응**:
```tsx
// Before (React Router)
import { useNavigate, useParams, Link } from 'react-router'

// After (Next.js)
import { useRouter, useParams } from 'next/navigation'
import Link from 'next/link'
```

### Issue 5: Tiptap 에디터 SSR 오류

**원인**: Tiptap은 브라우저 DOM에 의존  
**대응**: `dynamic import` + `ssr: false`
```tsx
const BoardEditor = dynamic(
  () => import('@/components/boards/BoardEditor'),
  { ssr: false }
)
```

### Issue 6: Axios interceptor (토큰 갱신)

**원인**: Server Component에서 Axios 인터셉터 동작 불가  
**대응**: Server Component는 `fetch` 직접 사용. 클라이언트 사이드(로그인/관리자 등)에서만 Axios + 인터셉터 유지.

### Issue 7: 기존 `/boards/:boardId` URL로 유입된 트래픽

**원인**: 구 URL이 외부에 공유된 경우 404 발생  
**대응**: `next.config.ts`에 영구 리다이렉트 설정
```ts
// next.config.ts
async redirects() {
  return [
    {
      source: '/boards/:boardId',
      destination: '/posts/:slug',  // slug 조회 후 리다이렉트 (API 필요)
      permanent: true,  // 301
    },
  ]
}
```
> boardId → slug 매핑이 필요하므로, 백엔드에 `GET /api/v1/posts/by-id/:boardId` 엔드포인트 추가 또는 클라이언트 리다이렉트 처리

---

## 11. Docker / 인프라 변경

### 현재

```dockerfile
# 현재: Vite 빌드 → Nginx 정적 서빙
FROM node:18-alpine AS builder
RUN npm run build

FROM nginx:alpine
COPY --from=builder /app/dist /usr/share/nginx/html
```

### Next.js 전환 후

```dockerfile
# Next.js standalone 빌드 → Node.js 서버 실행
FROM node:18-alpine AS builder
RUN npm run build

FROM node:18-alpine AS runner
WORKDIR /app
COPY --from=builder /app/.next/standalone ./
COPY --from=builder /app/.next/static ./.next/static
COPY --from=builder /app/public ./public

EXPOSE 3000
CMD ["node", "server.js"]
```

`next.config.ts`:
```ts
const nextConfig = {
  output: 'standalone',  // Docker 최적화 빌드
}
```

> Nginx는 리버스 프록시로 유지 (`proxy_pass http://nextjs:3000`)

---

## 12. 예상 작업 일정 요약

| Phase | 내용 | 예상 기간 |
|-------|------|---------|
| Phase 0 | 사전 준비, 프로젝트 초기화 | 0.5주 |
| Phase 1 | 공통 레이아웃, 정적 자산 이전 | 0.5주 |
| Phase 2 | 게시글 상세 SSG + SEO ★핵심 | 1주 |
| Phase 3 | 목록 페이지 (홈, 카테고리, 검색) | 0.5주 |
| Phase 4 | 인증 + 관리자 페이지 전체 | 1주 |
| Phase 5 | 정리, 배포, 서치 콘솔 등록 | 0.5주 |
| **합계** | | **약 4주** |

---

## 13. 작업 전 체크리스트

- [ ] 작업 계획서 작성 완료 (`260411_nextjs-migration-plan.md`) ✅
- [ ] Context7 MCP로 Next.js App Router 공식 문서 확인 ✅
- [ ] 현재 라우트 구조 및 컴포넌트 목록 파악 ✅
- [ ] 렌더링 전략 페이지별 결정 ✅
- [ ] 예상 이슈 및 대응 방안 정리 ✅
- [ ] 관련 테스트는 마이그레이션 완료 후 Vitest → Next.js 테스트 환경 전환 시 재작성
