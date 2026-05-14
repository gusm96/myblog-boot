# Phase 5: 정리 및 배포 계획

## 작업 목표 및 배경

Next.js 마이그레이션 Phase 5 — 운영 환경 배포를 위한 Docker/Nginx 설정 전환 및 구 URL 리다이렉트 처리.

## 구현 접근 방식

### 1. 구 URL 리다이렉트 (`/boards/:boardId` → `/posts/:slug`)

**문제**: 기존 `boardId(숫자)` → `slug(문자열)` 매핑이 정적으로 불가능  
**해결**: `app/boards/[boardId]/page.tsx` Server Component
- 백엔드 `GET /api/v1/posts/{boardId}` — 숫자 ID도 처리 가능
- 응답의 `slug` 필드로 `redirect('/posts/{slug}')` 수행
- 빌드 시 `generateStaticParams`로 모든 boardId 사전 생성 (optional)

### 2. Dockerfile 전환

```
현재: Vite → dist → Nginx 정적 서빙
신규: Next.js build → standalone → Node.js 서버 실행
```
- `node:18-alpine` 멀티스테이지 빌드
- `NEXT_PUBLIC_API_URL` ARG 주입
- standalone 출력물 (`/app/.next/standalone`, `/app/public`, `/app/.next/static`)

### 3. docker-compose.yaml 업데이트

- `frontend` 서비스 `build.context: ./frontend` → `./frontend-next`
- `VITE_API_URL` → `NEXT_PUBLIC_API_URL`
- 포트: 기존 `80:80` (Nginx) → `3000:3000` (Node.js) + Nginx 리버스 프록시(별도)
  - 또는 Nginx 컨테이너를 별도로 두고 `/`는 Next.js 3000으로 프록시
- 단순화: `frontend-next`가 `output: standalone`이므로 Nginx 없이 직접 3000 포트 노출도 가능

### 4. next.config.ts 운영 도메인 설정

- `images.remotePatterns`에 운영 서버 호스트 추가
- 운영 도메인 확정 후 `CORS_ALLOWED_ORIGINS`와 동기화 필요

## 변경 대상 파일

| 파일 | 작업 |
|------|------|
| `frontend-next/Dockerfile` | 신규 — Next.js standalone 빌드 |
| `frontend-next/.env.example` | 수정 — 운영 환경 변수 가이드 |
| `docker-compose.yaml` | 수정 — frontend 서비스 → frontend-next |
| `frontend-next/next.config.ts` | 수정 — 운영 도메인 이미지 허용 |
| `frontend-next/app/boards/[boardId]/page.tsx` | 신규 — 구 URL → 신 URL 301 리다이렉트 |
| `frontend-next/app/layout.tsx` | 수정 — 서치 콘솔 인증 메타태그 placeholder |

## 예상 이슈 및 대응

| 이슈 | 대응 |
|------|------|
| `NEXT_PUBLIC_*` 변수 빌드 시 주입 필요 | `ARG`로 빌드 타임 주입 + `ENV` 설정 |
| Nginx 없이 직접 포트 노출 시 `/api` 프록시 | backend 서비스 직접 연결 (Next.js → backend:8080) |
| boardId → slug 조회 실패 (게시글 없음) | `notFound()` 처리 |
