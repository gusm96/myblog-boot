# CRA → Vite 마이그레이션 결과 보고서 (4단계)

> 작성일: 2026-03-20
> 대상 브랜치: `develop`
> 참고: context7 `/websites/vite_dev`, `/vitejs/vite` (Vite 공식 문서)

---

## 1. 작업 결과 요약

| 항목 | 이전 (CRA) | 이후 (Vite) | 상태 |
|------|------------|-------------|------|
| 빌드 도구 | react-scripts 5.0.1 (Webpack 5) | vite 6.4.1 (esbuild + Rollup) | ✅ |
| 빌드 시간 | 미측정 (CRA ~30~60초 예상) | **3.85초** | ✅ |
| 빌드 출력 경로 | `build/` | `dist/` | ✅ |
| 취약점 | **30개** | **3개** (−27개) | ✅ |
| 환경변수 prefix | `REACT_APP_*` | `VITE_*` | ✅ |
| 프록시 설정 | `src/setProxy.js` + http-proxy-middleware | `vite.config.js` server.proxy | ✅ |
| 테스트 프레임워크 | Jest (react-scripts 내장) | Vitest 4.1.0 | ✅ |
| ESLint | react-app (CRA 고정) | ESLint 10 + flat config | ✅ |
| `@tanstack/eslint-plugin-query` 버전 고정 | `5.52.3` (고정) | `^5.91.5` (자유) | ✅ |

---

## 2. 수행한 작업

### Step 1 — 패키지 교체 ✅

```bash
npm uninstall react-scripts http-proxy-middleware

npm install -D vite@^6 @vitejs/plugin-react@^4
npm install -D vitest jsdom
npm install -D eslint @eslint/js eslint-plugin-react eslint-plugin-react-hooks --legacy-peer-deps
npm install -D @tanstack/eslint-plugin-query@latest --legacy-peer-deps
```

**설치 결과:**

| 패키지 | 버전 |
|--------|------|
| `vite` | ^6.4.1 |
| `@vitejs/plugin-react` | ^4.7.0 |
| `vitest` | ^4.1.0 |
| `jsdom` | ^29.0.1 |
| `eslint` | ^10.0.3 |
| `@eslint/js` | ^10.0.1 |
| `eslint-plugin-react` | ^7.37.5 |
| `eslint-plugin-react-hooks` | ^7.0.1 |
| `@tanstack/eslint-plugin-query` | ^5.91.5 (고정 해제) |

> **Vite 버전 선택 이유**: 최초 `vite@latest`(8.0.1) 설치 시 rolldown 번들러가 `.js` 파일 JSX 파싱 오류 발생.
> context7 문서 확인 결과 Vite 8은 rolldown으로 전환하여 `optimizeDeps.esbuildOptions`가 deprecated됨.
> CRA 마이그레이션 안정성을 위해 **Vite 6(esbuild + Rollup)** 으로 변경.

---

### Step 2 — vite.config.js 생성 ✅

```js
// vite.config.js
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
  // .js 파일 JSX 처리 (CRA 마이그레이션 — 확장자 .js 유지)
  optimizeDeps: {
    esbuildOptions: {
      loader: { '.js': 'jsx' },
    },
  },
  esbuild: {
    loader: 'jsx',
    include: /src\/.*\.js$/,
    exclude: [],
  },
  test: {
    environment: 'jsdom',
    setupFiles: './src/setupTests.js',
    globals: true,
  },
});
```

**핵심 설정 설명:**
- `server.proxy`: 기존 `setProxy.js`의 `/api → localhost:8080` 프록시를 Vite 네이티브 설정으로 대체
- `optimizeDeps.esbuildOptions.loader`: 개발 서버의 dep pre-bundling에서 `.js` → JSX 파싱 활성화
- `esbuild.loader` + `include`: 빌드 단계에서 `src/` 내 `.js` 파일 JSX 처리

---

### Step 3 — index.html 루트 이동 ✅

`public/index.html` → 프로젝트 루트 `index.html`로 이동.

**변경 내용 (context7 Vite 공식 문서 기반):**

| 항목 | 변경 전 | 변경 후 |
|------|---------|---------|
| `%PUBLIC_URL%/favicon.ico` | CRA 빌드 치환자 | `/favicon.ico` |
| `%PUBLIC_URL%/logo192.png` | CRA 빌드 치환자 | `/logo192.png` |
| `%PUBLIC_URL%/manifest.json` | CRA 빌드 치환자 | `/manifest.json` |
| 엔트리 script | CRA가 자동 주입 | `<script type="module" src="/src/index.js"></script>` 수동 추가 |

> Vite는 `index.html`을 프로젝트 루트에서 찾으며, `%PUBLIC_URL%` 없이 절대 경로를 자동 처리함 (context7 확인).

---

### Step 4 — package.json 수정 ✅

**scripts 교체:**

```diff
-"start": "react-scripts start",
-"build": "react-scripts build",
-"test":  "react-scripts test",
-"eject": "react-scripts eject"
+"start":   "vite",
+"build":   "vite build",
+"preview": "vite preview",
+"test":    "vitest"
```

**제거된 블록:**
- `eslintConfig` (CRA 내장 `react-app` preset → `eslint.config.mjs`로 분리)
- `browserslist` (Vite `build.target: 'es2015'`으로 대체)

---

### Step 5 — eslint.config.mjs 생성 ✅

ESLint 9 flat config 방식으로 전환 (`package.json` eslintConfig 블록 제거).

```js
// eslint.config.mjs
import js from '@eslint/js';
import reactPlugin from 'eslint-plugin-react';
import reactHooksPlugin from 'eslint-plugin-react-hooks';
import tanstackQueryPlugin from '@tanstack/eslint-plugin-query';

export default [
  js.configs.recommended,
  {
    plugins: {
      react: reactPlugin,
      'react-hooks': reactHooksPlugin,
      '@tanstack/query': tanstackQueryPlugin,
    },
    rules: {
      ...reactPlugin.configs.recommended.rules,
      ...reactHooksPlugin.configs.recommended.rules,
      ...tanstackQueryPlugin.configs.recommended.rules,
    },
    settings: { react: { version: 'detect' } },
    languageOptions: {
      globals: { window: 'readonly', document: 'readonly', console: 'readonly', process: 'readonly' },
    },
  },
];
```

`@tanstack/eslint-plugin-query` 버전 고정(`5.52.3`) 해제 → `^5.91.5` 자유 사용 가능 (CRA ESLint 8 고정 제약 해소).

---

### Step 6 — apiConfig.js 환경변수 변경 ✅

```diff
-export const BASE_URL = process.env.REACT_APP_API_URL || "http://localhost:8080";
+export const BASE_URL = import.meta.env.VITE_API_URL || "http://localhost:8080";
```

> `.env` 파일 없음 → 파일 변경 불필요. 코드 1곳만 수정.

---

### Step 7 — setProxy.js 삭제 ✅

```bash
rm src/setProxy.js
```

`http-proxy-middleware` 패키지도 함께 제거됨 (Step 1).

---

### Step 8 — Dockerfile 수정 ✅

```diff
-ARG REACT_APP_API_URL
-ENV REACT_APP_API_URL=$REACT_APP_API_URL
+ARG VITE_API_URL
+ENV VITE_API_URL=$VITE_API_URL

-COPY --from=build /app/build /usr/share/nginx/html
+COPY --from=build /app/dist /usr/share/nginx/html
```

---

## 3. 빌드 결과

```
vite v6.4.1 building for production...
✓ 634 modules transformed.

dist/index.html                1.18 kB │ gzip:   0.58 kB
dist/assets/index-C3YY2QDB.css  235.42 kB │ gzip:  31.84 kB
dist/assets/index-BxVvEtNr.js   975.13 kB │ gzip: 318.38 kB

✓ built in 3.85s
```

| 항목 | 값 |
|------|-----|
| 빌드 시간 | **3.85초** |
| 변환 모듈 수 | 634개 |
| JS 번들 크기 (gzip) | 318.38 kB |
| CSS 크기 (gzip) | 31.84 kB |

> 번들 크기 경고(`> 500 kB`): Rollup code-splitting 미적용으로 단일 청크에 모든 의존성이 포함됨.
> 향후 `build.rollupOptions.output.manualChunks`로 vendor 청크 분리 권장 (현재 기능 영향 없음).

---

## 4. 취약점 현황

| 단계 | 취약점 수 | 변화 |
|------|---------|------|
| 1단계 완료 후 | 32개 | |
| 2단계 완료 후 | 30개 | −2 |
| 3단계 완료 후 | 30개 | 0 |
| **4단계 완료 후** | **3개** | **−27** |

**잔여 취약점 3개 (moderate) — react-scripts와 무관:**

| 패키지 | 심각도 | 경로 | 해결 방법 |
|--------|--------|------|----------|
| `prismjs` | moderate | `react-syntax-highlighter → refractor → prismjs` | `react-syntax-highlighter@16.1.1` 업그레이드 (breaking change) |

> `react-syntax-highlighter` 업그레이드는 API 변경이 동반되므로 별도 작업으로 분리.

---

## 5. 변경 파일 목록

| 파일 | 변경 유형 | 내용 |
|------|----------|------|
| `package.json` | 수정 | scripts 교체, eslintConfig·browserslist 제거, devDependencies 추가 |
| `vite.config.js` | **신규** | Vite 설정 (plugins, proxy, build, esbuild, test) |
| `index.html` (루트) | **신규** | `public/index.html` 이동 + %PUBLIC_URL% 제거 + script 태그 추가 |
| `eslint.config.mjs` | **신규** | ESLint 10 flat config |
| `src/apiConfig.js` | 수정 | `process.env.REACT_APP_API_URL` → `import.meta.env.VITE_API_URL` |
| `Dockerfile` | 수정 | ARG/ENV 이름 + 빌드 경로 (`build/` → `dist/`) |
| `src/setProxy.js` | **삭제** | vite.config.js server.proxy로 대체 |
| `public/index.html` | **삭제** (루트로 이동) | CRA 전용 위치 → Vite 루트 위치로 이전 |

---

## 6. 특이 사항

### Vite 8 → 6 다운그레이드

최초 `vite@latest(8.0.1)` 설치 시 빌드 오류 발생:

```
[builtin:vite-transform] Error: Unexpected JSX expression
Help: JSX syntax is disabled and should be enabled via the parser options
```

Vite 8은 rolldown/oxc 번들러를 사용하며, `.js` 파일에서 JSX를 처리하려면 별도 rolldown 설정이 필요함.
context7 문서 확인 결과 `optimizeDeps.esbuildOptions`가 Vite 8에서 deprecated됨.

**결정**: CRA 마이그레이션 안정성을 위해 Vite 6.4.1 사용.
Vite 6는 esbuild + Rollup 조합으로 `optimizeDeps.esbuildOptions.loader`가 정상 동작.

---

## 7. 체크포인트

- [x] ✅ `npm run build` 성공 — `dist/` 생성, 빌드 3.85초 (2026-03-20)
- [x] ✅ 취약점 30개 → 3개 — react-scripts 체인 27개 제거 (2026-03-20)
- [x] ✅ 환경변수 `import.meta.env.VITE_API_URL` 적용 (2026-03-20)
- [x] ✅ `src/setProxy.js` 삭제, `http-proxy-middleware` 제거 (2026-03-20)
- [x] ✅ Dockerfile 빌드 경로·환경변수명 수정 (2026-03-20)
- [x] ✅ ESLint 10 flat config 전환, `@tanstack/eslint-plugin-query` 버전 고정 해제 (2026-03-20)
- [x] ✅ Vite 개발 서버 기동 — **379ms** cold start (Playwright 확인, 2026-03-20)
- [x] ✅ `/` Home 정상 렌더링 — 게시글 목록, 방문자 수 표시 (Playwright 확인, 2026-03-20)
- [x] ✅ `/boards/24` 게시글 상세 — `useParams` + Vite 프록시로 백엔드 데이터 렌더링 (Playwright 확인, 2026-03-20)
- [x] ✅ `/management` → `/login` 리다이렉트 — ProtectedRoute 정상 동작 (Playwright 확인, 2026-03-20)
- [x] ✅ `/api` 프록시 동작 — `GET /api/v2/categories 200`, `GET /api/v2/visitor-count 200` (Playwright 네트워크 확인, 2026-03-20)
- [x] ✅ 브라우저 콘솔 에러 0건 (Playwright 확인, 2026-03-20)

---

## 8. 후속 권장 사항

| 우선순위 | 항목 | 내용 |
|---------|------|------|
| 중간 | `react-syntax-highlighter` 업그레이드 | `@16.1.1` 업그레이드로 잔여 취약점 3개 해소 |
| 낮음 | 번들 청크 분리 | `build.rollupOptions.output.manualChunks`로 vendor 청크 분리 (975 kB → 분리) |
| 낮음 | 파일 확장자 표준화 | `.js` → `.jsx` 변환 시 `esbuild.include` 설정 불필요 |
| 낮음 | Vite 8 재검토 | rolldown 안정화 후 (`optimizeDeps.rolldownOptions` 사용) 업그레이드 가능 |
