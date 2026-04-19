# Phase 4: 인증 + 관리자 페이지 구현 계획

## 작업 목표 및 배경

Next.js 마이그레이션 Phase 4 — 로그인 페이지와 관리자(Management) 페이지 군을 App Router 방식으로 구현한다.
기존 React Router 기반 `ProtectedRoute` + `Outlet` 구조를 Next.js `middleware.ts` + `layout.tsx` + Client Component 인증 가드로 전환한다.

## 구현 접근 방식

### 인증 흐름
1. **middleware.ts (Edge Runtime)**: 쿠키 기반 사전 차단
   - 백엔드가 로그인 시 `refresh_token` HttpOnly 쿠키를 세팅한다.
   - middleware에서 `/management/**` 경로 요청 시 해당 쿠키 유무를 확인한다.
   - 쿠키 없으면 `/login?from=경로`로 redirect.
2. **management/layout.tsx (Client Component)**: Role 검증
   - Redux `isLoggedIn` + `getRoleFromToken()` 호출로 ROLE_ADMIN 확인.
   - 역할 불일치 시 `/`로 redirect.
3. **로그인 성공** → Redux dispatch(`userLogin`) → `router.push(from)`.

### Tiptap 에디터
- `@tiptap/react`, `@tiptap/starter-kit`, `@tiptap/extension-image` 를 새로 설치.
- `BoardEditorClient`, `BoardEditFormClient` 를 `'use client'` 컴포넌트로 작성.
- 각 page에서 `dynamic(() => import(...), { ssr: false })` 로 동적 임포트 → SSR 회피.

## 변경 대상 파일

| 파일 | 작업 |
|------|------|
| `frontend-next/middleware.ts` | 신규 — Edge auth guard |
| `frontend-next/app/login/page.tsx` | 신규 — 로그인 폼 |
| `frontend-next/app/management/layout.tsx` | 신규 — 관리자 레이아웃 + Role 검증 |
| `frontend-next/app/management/page.tsx` | 신규 — 게시글 목록 (관리자) |
| `frontend-next/app/management/new-post/page.tsx` | 신규 — 새 게시글 작성 |
| `frontend-next/app/management/boards/[id]/page.tsx` | 신규 — 게시글 수정 |
| `frontend-next/app/management/categories/page.tsx` | 신규 — 카테고리 관리 |
| `frontend-next/app/management/temporary-storage/page.tsx` | 신규 — 휴지통 |
| `frontend-next/components/management/AdminBoardList.tsx` | 신규 — 관리자용 게시글 목록 |
| `frontend-next/components/management/AdminNavBar.tsx` | 신규 — 관리자 사이드 내비게이션 |
| `frontend-next/components/editor/EditorToolbar.tsx` | 신규 — Tiptap 툴바 |
| `frontend-next/components/editor/BoardEditorClient.tsx` | 신규 — 새 게시글 에디터 |
| `frontend-next/components/editor/BoardEditFormClient.tsx` | 신규 — 게시글 수정 에디터 |
| `frontend-next/lib/boardApi.ts` | 추가 — 관리자 API 함수 |
| `frontend-next/app/globals.css` | 추가 — 에디터/관리자 CSS |

## 예상 이슈 및 대응

| 이슈 | 대응 |
|------|------|
| middleware.ts의 쿠키 이름 불일치 | `/api/v1/token-validation` 직접 호출 대신 쿠키 이름 확인 후 적용 |
| Tiptap SSR 오류 | `dynamic(..., { ssr: false })` 사용 |
| Redux persist가 Edge에서 동작 안 함 | middleware는 쿠키 기반으로만 체크 |
| `useRouter` redirect 루프 | isLoggedIn 체크 → layout에서만 수행, middleware 중복 방지 |
