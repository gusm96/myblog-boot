# Frontend CLAUDE.md

## 기술 스택 및 버전

### 프레임워크 / 런타임

| 항목 | 버전 | 비고 |
|------|------|------|
| Next.js | 16.2.3 | App Router, `output: "standalone"` |
| React | 19.2.4 | |
| React DOM | 19.2.4 | |
| TypeScript | ^5 | strict 모드 |
| Node.js (Docker) | 18-alpine | |

### UI 프레임워크

| 항목 | 버전 | 비고 |
|------|------|------|
| Bootstrap | 5.3.8 | |
| React Bootstrap | 2.10.10 | |

### 상태 관리

| 항목 | 버전 | 비고 |
|------|------|------|
| @reduxjs/toolkit | 2.11.2 | 클라이언트 상태 (인증) |
| React Redux | 9.2.0 | |
| Redux Persist | 6.0.0 | `isLoggedIn`, `accessToken` 유지 |
| @tanstack/react-query | 5.99.0 | 서버 상태 관리 |
| @tanstack/react-query-devtools | 5.99.0 | dev 환경에서만 활성화 |

### HTTP / 통신

| 항목 | 버전 | 비고 |
|------|------|------|
| Axios | 1.15.0 | interceptor로 Authorization 헤더 자동 주입 |
| @stomp/stompjs | 7.3.0 | WebSocket (댓글/채팅) |

### 에디터 / 콘텐츠

| 항목 | 버전 | 비고 |
|------|------|------|
| @tiptap/react | 3.22.3 | 리치 텍스트 에디터 |
| @tiptap/starter-kit | 3.22.3 | |
| @tiptap/extension-image | 3.22.3 | |
| Marked | 18.0.0 | Markdown 파싱 |
| DOMPurify | 3.3.3 | XSS 방지 |
| html-react-parser | 6.0.1 | |

### 유틸리티

| 항목 | 버전 |
|------|------|
| Day.js | 1.11.20 |
| react-cookie | 8.1.0 |

---

## 빌드 & 실행 명령어

```bash
# 개발 서버 (port 3000)
npm run dev

# 프로덕션 빌드
npm run build

# 프로덕션 서버 실행
npm start
```

---

## 프로젝트 구조

```
frontend/
├── app/                    # Next.js App Router
│   ├── (public)/           # 공개 라우트 그룹 (SEO slug 등)
│   │   ├── layout.tsx
│   │   └── posts/[slug]/page.tsx
│   ├── [categoryName]/page.tsx
│   ├── boards/[boardId]/page.tsx
│   ├── login/              # 로그인 페이지
│   ├── management/         # 관리자 페이지 (인증 필요)
│   │   ├── layout.tsx      # 클라이언트 역할(Role) 검증
│   │   ├── categories/
│   │   ├── new-post/
│   │   ├── posts/[id]/
│   │   └── temporary-storage/
│   ├── search/
│   ├── layout.tsx          # Root Layout
│   ├── page.tsx            # 홈
│   ├── robots.ts           # SEO
│   ├── sitemap.ts          # SEO
│   └── not-found.tsx
├── components/             # 재사용 컴포넌트
│   ├── boards/
│   ├── comments/
│   ├── editor/
│   ├── layout/
│   └── management/
├── hooks/                  # 커스텀 훅
│   └── useInfiniteScroll.ts
├── lib/                    # API 클라이언트 & 유틸리티
│   ├── apiClient.ts        # Axios 인스턴스 (인터셉터)
│   ├── api.ts              # Server-side API 호출
│   ├── authApi.ts          # 인증 관련 API
│   ├── postApi.ts          # 게시글 API
│   ├── queryKeys.ts        # TanStack Query Key Factory
│   └── formatTimeAgo.ts
├── providers/              # 컨텍스트 프로바이더
│   └── Providers.tsx       # Redux + TanStack Query + PersistGate
├── store/                  # Redux store
│   ├── index.ts            # configureStore + persistor
│   ├── userSlice.ts
│   └── authActions.ts
├── types/                  # TypeScript 타입 정의
│   └── index.ts
├── proxy.ts                # Next.js middleware (관리자 인증 게이트)
├── next.config.ts
├── Dockerfile
└── tsconfig.json
```

---

## 아키텍처 핵심 사항

### 라우팅 & 인증

- **App Router** 기반, `(public)` 라우트 그룹으로 SEO slug 페이지 분리
- **관리자 인증 2단계**: `proxy.ts`(HTTPS 환경에서 `refresh_token` 쿠키 검사) → `management/layout.tsx`(클라이언트 역할 검증)
- HTTP 개발 환경에서는 Secure 쿠키가 전송되지 않으므로 proxy 검사 생략

### 상태 관리 전략

- **서버 상태**: TanStack Query v5 — `queryKeys` Factory 패턴으로 계층적 키 관리
- **클라이언트 상태**: Redux Toolkit — 인증 정보(`isLoggedIn`, `accessToken`)만 관리, `redux-persist`로 localStorage 유지
- **QueryClient 기본 옵션**: `staleTime: 10분`, `gcTime: 24시간`, `retry: 1`, `refetchOnWindowFocus: false`

### API 클라이언트

- `lib/apiClient.ts`: Axios 인스턴스, Redux store에서 `accessToken` 읽어 `Authorization` 헤더 자동 주입
- `withCredentials: true` — refresh token 쿠키 전송
- 환경변수 `NEXT_PUBLIC_API_URL` (기본값: `http://localhost:8080`)

### Path Alias

- `@/*` → 프로젝트 루트 (`tsconfig.json`의 `paths` 설정)

---

## Docker (프로덕션)

- **빌드 이미지**: `node:18-alpine`
- **실행 이미지**: `node:18-alpine` (standalone 모드)
- `output: "standalone"` — `.next/standalone` + `.next/static` + `public` 복사
- 빌드 시 `NEXT_PUBLIC_API_URL` ARG로 API 주소 주입
- `node server.js`로 실행 (Nginx 불필요)

---

## Next.js 설정 주의사항

- `output: "standalone"` 필수 — Docker 배포용
- `images.remotePatterns`: `localhost:8080` (개발) + `backend:8080` (도커 내부 통신)
- `/boards` → `/` 301 리다이렉트 설정 (구 URL 호환)
- TypeScript strict 모드, `@/*` path alias 사용
