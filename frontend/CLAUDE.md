# Frontend CLAUDE.md

## 기술 스택 및 버전

### 런타임 / 빌드 도구

| 항목 | 버전 | 비고 |
|------|------|------|
| Node.js (Docker) | 18-alpine | |
| Vite | 6.4.1 | CRA에서 마이그레이션 |
| @vitejs/plugin-react | 4.7.0 | |
| ESLint | 10.0.3 | |
| Vitest | 4.1.0 | jsdom 환경 |

### UI 프레임워크

| 항목 | 버전 | 비고 |
|------|------|------|
| React | 18.2.0 | |
| React DOM | 18.2.0 | |
| React Router | 7.13.1 | |
| Bootstrap | 5.3.8 | |
| React Bootstrap | 2.10.10 | |
| Styled Components | 6.3.11 | |

### 상태 관리

| 항목 | 버전 | 비고 |
|------|------|------|
| @reduxjs/toolkit | 2.11.2 | |
| React Redux | 9.2.0 | |
| Redux Persist | 6.0.0 | |
| @tanstack/react-query | 5.90.21 | 서버 상태 관리 |
| @tanstack/react-query-devtools | 5.91.3 | |
| @tanstack/react-query-persist-client | 5.95.0 | |
| @tanstack/query-sync-storage-persister | 5.95.0 | |

### HTTP / 통신

| 항목 | 버전 | 비고 |
|------|------|------|
| Axios | 1.13.6 | interceptor 적용 |
| @stomp/stompjs | 7.3.0 | WebSocket (댓글/채팅) |
| ws | 8.19.0 | |

### 에디터 / 콘텐츠

| 항목 | 버전 | 비고 |
|------|------|------|
| @tiptap/react | 3.20.1 | 리치 텍스트 에디터 |
| @tiptap/starter-kit | 3.20.1 | |
| @tiptap/extension-image | 3.20.1 | |
| Marked | 17.0.4 | Markdown 파싱 |
| DOMPurify | 3.3.3 | XSS 방지 |
| html-react-parser | 5.2.17 | |

### 유틸리티

| 항목 | 버전 |
|------|------|
| Day.js | 1.11.20 |
| react-cookie | 8.0.1 |
| prop-types | 15.8.1 |

### 프로덕션 서빙

- **Nginx** (alpine) — SPA 라우팅, `/api` 리버스 프록시

### Docker

- **빌드 이미지**: `node:18-alpine`
- **실행 이미지**: `nginx:alpine`
- 빌드 시 `VITE_API_URL` ARG로 API 주소 주입

---

## 빌드 & 실행 명령어

```bash
# 개발 서버 (port 3000, /api → localhost:8080 프록시)
npm start

# 프로덕션 빌드
npm run build

# 테스트
npm test

# 빌드 프리뷰
npm run preview
```

---

## Vite 설정 주의사항

- `.js` 파일에서 JSX 사용 (CRA 마이그레이션, 확장자 변경 없이 유지)
- `esbuild.loader: 'jsx'` + `optimizeDeps.esbuildOptions.loader` 설정 필수
- 빌드 타겟: `es2015`
