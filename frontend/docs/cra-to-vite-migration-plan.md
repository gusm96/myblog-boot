# CRA → Vite 마이그레이션 계획서 (4단계)

> 작성일: 2026-03-20
> 대상 브랜치: `develop`
> 참고: context7 `/websites/vite_dev` (Vite 공식 문서), 기존 분석 보고서 `cra-to-vite-migration-analysis.md`

---

## 1. 3단계 완료 상태 확인

| 패키지 | 버전 | 상태 |
|--------|------|------|
| `react-router` | ^7.13.1 | ✅ (react-router-dom 제거됨) |
| `@reduxjs/toolkit` | ^2.11.2 | ✅ |
| `react-redux` | ^9.2.0 | ✅ |
| `redux` | ^5.0.1 | ✅ |
| `react-cookie` | ^8.0.1 | ✅ |
| 취약점 | 30개 | react-scripts 체인 포함 |

**3단계 이상 없음. 4단계 진행.**

---

## 2. 목표

| 항목 | 현재 (CRA) | 목표 (Vite) |
|------|------------|-------------|
| 빌드 도구 | react-scripts 5.0.1 (Webpack 5) | vite 6.x (esbuild + Rollup) |
| 개발 서버 시작 | 10~30초 | 1~3초 |
| HMR | 1~5초 | < 100ms |
| 프로덕션 빌드 출력 | `build/` | `dist/` |
| 환경변수 prefix | `REACT_APP_*` | `VITE_*` |
| 프록시 설정 | `src/setProxy.js` + http-proxy-middleware | `vite.config.js` server.proxy |
| 테스트 프레임워크 | Jest (react-scripts 내장) | Vitest |
| ESLint | react-app (CRA 내장 고정) | 직접 설치·관리 |
| 취약점 | 30개 | ~0개 (react-scripts 체인 제거) |

---

## 3. 현재 코드 분석 결과

### 3.1 Draft.js → Tiptap 교체 완료 — CJS 호환 이슈 없음 ✅

기존 분석 보고서(2026-03-13)의 Draft.js CJS 모듈 충돌 이슈는 이미 해소됨.
현재 설치된 에디터 패키지: `@tiptap/react`, `@tiptap/starter-kit`, `@tiptap/extension-image` — 모두 ESM 지원.

### 3.2 환경변수 — 변경 필요 1곳 ⚠️

```js
// src/apiConfig.js — 현재
export const BASE_URL = process.env.REACT_APP_API_URL || "http://localhost:8080";

// Vite 전환 후
export const BASE_URL = import.meta.env.VITE_API_URL || "http://localhost:8080";
```

`.env` 파일 없음 — 환경변수 파일 변경 불필요.

### 3.3 프록시 설정 — setProxy.js 삭제 대상 ⚠️

```js
// src/setProxy.js — CRA webpack-dev-server 전용 (삭제 예정)
const { createProxyMiddleware } = require('http-proxy-middleware');
module.exports = function (app) {
  app.use('/api', createProxyMiddleware({
    target: 'http://localhost:8080',
    changeOrigin: true,
  }));
};
```

→ `vite.config.js`의 `server.proxy`로 대체:

```js
// context7 Vite 공식 문서 기반
server: {
  proxy: {
    '/api': {
      target: 'http://localhost:8080',
      changeOrigin: true,
    },
  },
},
```

### 3.4 index.html — 루트 이동 필요 ⚠️

Vite는 `index.html`을 프로젝트 **루트**에서 찾는다 (context7 문서 확인).
`public/index.html` → 루트 `index.html`로 이동 후 두 가지 변경 필요:

| 변경 항목 | 내용 |
|----------|------|
| `%PUBLIC_URL%` 제거 | Vite는 자동 처리 — 단순 절대경로 `/`로 대체 |
| 엔트리 script 태그 추가 | `<script type="module" src="/src/index.js"></script>` |

### 3.5 Dockerfile — 빌드 경로 + 환경변수 이름 변경 ⚠️

```dockerfile
# 현재
ARG REACT_APP_API_URL
ENV REACT_APP_API_URL=$REACT_APP_API_URL
COPY --from=build /app/build /usr/share/nginx/html

# Vite 전환 후
ARG VITE_API_URL
ENV VITE_API_URL=$VITE_API_URL
COPY --from=build /app/dist /usr/share/nginx/html
```

### 3.6 테스트 환경 — setupTests.js만 존재, 실제 테스트 파일 없음 ✅

`setupTests.js`에 `@testing-library/jest-dom` import만 있음.
실제 테스트 파일이 없어 Vitest 전환 비용 최소.

### 3.7 ESLint — CRA 내장 고정 해제 ⚠️

현재 `package.json`에 `eslintConfig` 블록이 `react-app` preset을 사용 중.
CRA 제거 후 `eslint-plugin-react-app`도 함께 사라지므로 별도 설정 필요.
`@tanstack/eslint-plugin-query` 5.52.3 버전 고정도 이 단계에서 해제 가능.

---

## 4. Breaking Changes 체크리스트

| # | 항목 | 현재 영향 | 조치 |
|---|------|----------|------|
| V-1 | `index.html` 위치 변경 (public → 루트) | 필수 변경 | 이동 + %PUBLIC_URL% 제거 + script 태그 추가 |
| V-2 | 환경변수 prefix `REACT_APP_` → `VITE_` | `apiConfig.js` 1곳 | `import.meta.env.VITE_API_URL`로 교체 |
| V-3 | `process.env` → `import.meta.env` | `apiConfig.js` 1곳 | V-2와 동시 처리 |
| V-4 | 프록시 설정 파일 방식 변경 | `setProxy.js` 삭제 | `vite.config.js` server.proxy로 대체 |
| V-5 | 빌드 출력 `build/` → `dist/` | Dockerfile | 경로 수정 |
| V-6 | Dockerfile ARG/ENV 이름 변경 | Dockerfile | `REACT_APP_API_URL` → `VITE_API_URL` |
| V-7 | Jest → Vitest | setupTests.js | vitest globals 설정 추가 |
| V-8 | ESLint 설정 CRA 의존 제거 | package.json eslintConfig | 별도 설정 파일 작성 |
| V-9 | `browserslist` 제거 | package.json | vite.config.js `build.target`으로 이전 |
| V-10 | CJS 모듈 Draft.js 충돌 | **해당 없음** (Tiptap 교체 완료) | 없음 |
| V-11 | `redux-persist/es/` 경로 import | **호환** (ESM 경로 사용 중) | 없음 |
| V-12 | `@stomp/stompjs` ESM 호환 | **호환** (v7 ESM 지원) | 없음 |

---

## 5. 실행 계획 (순서)

### Step 1 — 패키지 교체

```bash
# react-scripts 및 관련 패키지 제거
npm uninstall react-scripts http-proxy-middleware

# Vite + React 플러그인 설치 (context7 문서 기반)
npm install -D vite @vitejs/plugin-react

# Vitest + jsdom 설치 (테스트 환경)
npm install -D vitest jsdom
```

### Step 2 — vite.config.js 생성

```js
// vite.config.js (프로젝트 루트)
import { defineConfig } from 'vite';
import react from '@vitejs/plugin-react';

export default defineConfig({
  plugins: [react()],
  server: {
    port: 3000,
    proxy: {
      '/api': {
        target: 'http://localhost:8080',
        changeOrigin: true,
      },
    },
  },
  build: {
    outDir: 'dist',
    target: 'es2015',
  },
  test: {
    environment: 'jsdom',
    setupFiles: './src/setupTests.js',
    globals: true,
  },
});
```

### Step 3 — index.html 루트 이동

`public/index.html` → 루트 `index.html` 이동 후 수정:

```diff
-  <link rel="icon" href="%PUBLIC_URL%/favicon.ico" />
+  <link rel="icon" href="/favicon.ico" />

-  <link rel="apple-touch-icon" href="%PUBLIC_URL%/logo192.png" />
+  <link rel="apple-touch-icon" href="/logo192.png" />

-  <link rel="manifest" href="%PUBLIC_URL%/manifest.json" />
+  <link rel="manifest" href="/manifest.json" />

   <div id="root"></div>
+  <script type="module" src="/src/index.js"></script>
```

### Step 4 — package.json scripts 교체

```diff
 "scripts": {
-  "start": "react-scripts start",
-  "build": "react-scripts build",
-  "test": "react-scripts test",
-  "eject": "react-scripts eject"
+  "start": "vite",
+  "build": "vite build",
+  "preview": "vite preview",
+  "test": "vitest"
 },
```

`eslintConfig` 블록 및 `browserslist` 블록 제거.

### Step 5 — ESLint 설정 분리

`package.json`에서 `eslintConfig` 제거 후 `eslint.config.js` 작성 (ESLint 9 flat config):

```js
// eslint.config.js
import js from '@eslint/js';
import reactPlugin from 'eslint-plugin-react';
import reactHooksPlugin from 'eslint-plugin-react-hooks';

export default [
  js.configs.recommended,
  {
    plugins: {
      react: reactPlugin,
      'react-hooks': reactHooksPlugin,
    },
    rules: {
      ...reactPlugin.configs.recommended.rules,
      ...reactHooksPlugin.configs.recommended.rules,
    },
    settings: {
      react: { version: 'detect' },
    },
  },
];
```

필요 패키지 설치:
```bash
npm install -D eslint @eslint/js eslint-plugin-react eslint-plugin-react-hooks
```

`@tanstack/eslint-plugin-query` 버전 고정 해제 (`5.52.3` → `^latest`):
```bash
npm install -D @tanstack/eslint-plugin-query@latest
```

### Step 6 — apiConfig.js 환경변수 변경

```diff
-export const BASE_URL = process.env.REACT_APP_API_URL || "http://localhost:8080";
+export const BASE_URL = import.meta.env.VITE_API_URL || "http://localhost:8080";
```

### Step 7 — setProxy.js 삭제

```bash
rm src/setProxy.js
```

### Step 8 — Dockerfile 수정

```diff
-ARG REACT_APP_API_URL
-ENV REACT_APP_API_URL=$REACT_APP_API_URL
+ARG VITE_API_URL
+ENV VITE_API_URL=$VITE_API_URL

-COPY --from=build /app/build /usr/share/nginx/html
+COPY --from=build /app/dist /usr/share/nginx/html
```

### Step 9 — 동작 검증

```bash
npm start        # Vite 개발 서버 기동 확인 (http://localhost:3000)
npm run build    # dist/ 생성 확인, 빌드 에러 없음
npm test         # Vitest 실행 (현재 테스트 없음 → 0 passed)
```

체크포인트:
- [ ] `npm start` → 개발 서버 3000포트 기동, 브라우저 정상 렌더링
- [ ] 콘솔에 React Router / Redux 관련 에러 없음
- [ ] `/api` 프록시 동작 — 백엔드 연결 정상 (게시글 목록 로드)
- [ ] `npm run build` → `dist/` 생성, 빌드 에러 없음
- [ ] `npm run preview` → 프로덕션 빌드 서빙 정상
- [ ] `npm audit` — 취약점 대폭 감소 확인 (목표: react-scripts 30개 제거)
- [ ] `import.meta.env.VITE_API_URL` 환경변수 정상 인식

---

## 6. 변경 파일 목록

| 파일 | 변경 내용 |
|------|----------|
| `package.json` | scripts 교체, eslintConfig·browserslist 제거, devDependencies 추가 |
| `vite.config.js` | **신규 생성** — plugins, proxy, build, test 설정 |
| `index.html` (루트) | **신규** — `public/index.html` 이동 + %PUBLIC_URL% 제거 + script 태그 추가 |
| `src/apiConfig.js` | `process.env.REACT_APP_API_URL` → `import.meta.env.VITE_API_URL` |
| `src/setProxy.js` | **삭제** |
| `eslint.config.js` | **신규 생성** — ESLint 9 flat config |
| `Dockerfile` | ARG/ENV 이름 + 빌드 경로 수정 |
| `public/index.html` | **삭제** (루트로 이동) |

**총 수정 파일: 5개 수정 + 2개 신규 + 2개 삭제**

---

## 7. 취약점 해결 기대치

| 단계 | 취약점 수 |
|------|---------|
| 1단계 완료 후 | 32개 |
| 2단계 완료 후 | 30개 |
| 3단계 완료 후 | 30개 |
| **4단계 완료 후** | **~0개** (react-scripts 체인 30개 제거) |

> 잔여 취약점은 `@testing-library` 등 devDependency에 포함될 수 있으나 프로덕션 무영향.

---

## 8. 롤백 계획

```bash
git checkout -- package.json src/apiConfig.js Dockerfile
git checkout -- public/index.html  # 원래 위치 복구
rm vite.config.js index.html eslint.config.js
npm ci
```

---

## 9. 기존 분석 보고서 대비 변경 사항

| 항목 | 분석 보고서 (2026-03-13) | 현재 실제 상태 |
|------|------------------------|--------------|
| Draft.js CJS 이슈 | 주요 위험 요소 | **해소됨** — Tiptap으로 교체 완료 |
| optimizeDeps 설정 필요 여부 | Draft.js 계열 4개 패키지 | **불필요** |
| `@draft-js-plugins` 처리 | optimizeDeps.include 필요 | **해당 없음** |
| 환경변수 파일 변경 | .env prefix 일괄 변경 | `.env` 파일 없음 — 코드 1곳만 변경 |
