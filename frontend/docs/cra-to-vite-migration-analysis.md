# CRA → Vite 마이그레이션 분석 보고서

> 작성일: 2026-03-13
> 대상 프로젝트: `myblog-boot/frontend/` (CRA 5.0.1 / react-scripts 5.0.1)

---

## 목차

1. [현재 상황 요약](#1-현재-상황-요약)
2. [이점](#2-이점)
3. [단점 및 마이그레이션 비용](#3-단점-및-마이그레이션-비용)
4. [이 프로젝트에서 추가로 확인해야 할 호환성 이슈](#4-이-프로젝트에서-추가로-확인해야-할-호환성-이슈)
5. [예상 작업 목록](#5-예상-작업-목록)
6. [종합 판단](#6-종합-판단)

---

## 1. 현재 상황 요약

| 항목 | 현재 (CRA) | 목표 (Vite) |
|------|------------|-------------|
| 빌드 도구 | Webpack 5 (react-scripts 내장) | Vite 6.x (esbuild + Rollup) |
| 개발 서버 | webpack-dev-server | Vite Dev Server (네이티브 ESM) |
| 번들러 | Webpack | Rollup (production) |
| 트랜스파일러 | Babel | esbuild (개발), Rollup (빌드) |
| 설정 파일 | 없음 (내장 고정) | `vite.config.js` (완전 제어) |
| 취약점 (react-scripts 체인) | **~30개** | **0개** (react-scripts 제거) |
| 전체 취약점 | 39개 | ~9개 (Draft.js·prismjs 잔여) |

---

## 2. 이점

### 2.1 개발 생산성 향상

#### 개발 서버 시작 속도
CRA는 전체 앱을 번들링한 후 서버가 준비된다. Vite는 브라우저의 네이티브 ESM을 활용해 번들링 없이 즉시 서버를 시작한다.

| 지표 | CRA (Webpack) | Vite |
|------|--------------|------|
| 콜드 스타트 | 10–30초 | **1–3초** |
| HMR 반영 속도 | 1–5초 | **< 100ms** |
| 프로덕션 빌드 | 30–60초 | **10–20초** |

> 이 프로젝트는 소스 파일 53개, 컴포넌트 ~38개 규모다. CRA 대비 HMR 체감 차이가 즉시 느껴지는 수준이다.

#### HMR 정확도
Vite의 HMR은 변경된 모듈만 교체하므로 React 상태가 보존된다. `BoardEditor.js`(Draft.js 에디터)처럼 입력 상태가 복잡한 컴포넌트를 수정할 때 전체 새로고침 없이 변경사항이 반영된다.

---

### 2.2 보안 취약점 근본 해결

현재 `npm audit` 결과 39개 취약점 중 약 30개가 `react-scripts` 의존성 체인에서 발생한다. 이 취약점들은 `npm audit fix`로도 해결 불가능하다(강제 적용 시 react-scripts가 0.0.0으로 다운그레이드되어 빌드 불가).

```
react-scripts 내부 취약점 체인 (현재 해결 불가):
  - nth-check (high)        ← svgo ← @svgr/webpack ← react-scripts
  - webpack-dev-server (moderate) ← react-scripts
  - serialize-javascript (high)   ← workbox-webpack-plugin ← react-scripts
  - postcss (moderate)            ← resolve-url-loader ← react-scripts
  - @tootallnate/once (high)      ← jest ← react-scripts
  - underscore (high)             ← bfj ← react-scripts
```

Vite로 전환하면 `react-scripts`가 완전히 제거되며 위 체인이 모두 사라진다. 잔여 취약점은 Draft.js(immutable), prismjs 관련 ~9개만 남는다.

---

### 2.3 ESLint 버전 제약 해제

CRA는 ESLint **8.56.0**을 내부에 고정한다. 이 제약 때문에 현재 `@tanstack/eslint-plugin-query`를 `5.52.3`으로 버전 고정해야 했다 (`^` 사용 불가).

Vite 전환 후 ESLint를 직접 설치·관리할 수 있으므로:
- `@tanstack/eslint-plugin-query` 고정 해제 → `^5.91.4` 자유롭게 사용
- ESLint 9.x 도입 가능 (flat config 방식)
- 원하는 ESLint 플러그인을 버전 제약 없이 추가 가능

---

### 2.4 설정 완전 제어

CRA는 빌드 설정이 `react-scripts` 내부에 완전히 숨겨져 있어 `eject` 없이는 커스터마이징이 불가능하다. Vite는 `vite.config.js` 하나로 모든 설정을 제어한다.

```js
// vite.config.js — 예시: 현재 setProxy.js를 대체
import { defineConfig } from 'vite';
import react from '@vitejs/plugin-react';

export default defineConfig({
  plugins: [react()],
  server: {
    proxy: {
      '/api': {
        target: 'http://localhost:8080',
        changeOrigin: true,
      },
    },
  },
});
```

현재 `src/setProxy.js` + `http-proxy-middleware` 패키지가 필요 없어진다.

---

### 2.5 번들 크기 최적화

Rollup 기반의 Vite 프로덕션 빌드는 Webpack 대비 tree-shaking이 더 세밀하게 동작한다.

이 프로젝트에서 특히 효과적인 패키지:
- `react-bootstrap`: 사용한 컴포넌트만 포함
- `react-syntax-highlighter`: 특정 언어 하이라이터만 포함
- `@tanstack/react-query`: 미사용 API 제외

---

### 2.6 최신 생태계 지원

| 항목 | CRA (현재) | Vite |
|------|-----------|------|
| React 19 지원 | 불확실 (유지보수 중단) | ✅ |
| TypeScript 점진적 도입 | 별도 설정 복잡 | ✅ 즉시 지원 |
| SWC 트랜스파일러 | ❌ | `@vitejs/plugin-react-swc` 선택 가능 |
| CSS Modules | 지원 | ✅ 네이티브 지원 |
| 환경변수 타입 안전성 | ❌ | ✅ `vite-env.d.ts`로 타입 정의 가능 |

---

### 2.7 `npm install` DEPRECATED 경고 제거

현재 `npm install` 시마다 출력되는 아래 경고가 사라진다:
```
npm warn deprecated react-scripts@5.0.1
```

---

## 3. 단점 및 마이그레이션 비용

### 3.1 테스트 환경 재설정 필요

CRA의 `react-scripts test`는 Jest + jsdom을 내장 제공한다. Vite는 테스트 환경을 포함하지 않아 별도 설정이 필요하다.

**선택지 A: Vitest 도입 (권장)**
```bash
npm install -D vitest @vitest/ui jsdom @testing-library/react
```
```js
// vite.config.js 추가
test: {
  environment: 'jsdom',
  setupFiles: './src/setupTests.js',
}
```

**선택지 B: Jest 유지**
```bash
npm install -D jest jest-environment-jsdom babel-jest @babel/preset-react
```
Jest + Babel 설정 파일(`jest.config.js`, `babel.config.js`)을 별도 작성해야 한다. CRA에서 내장으로 제공하던 것들을 수동으로 구성하는 작업량이 적지 않다.

현재 프로젝트는 `setupTests.js`만 존재하고 실제 테스트 파일이 없어 **선택지 A(Vitest)의 마이그레이션 비용이 매우 낮다**.

---

### 3.2 환경변수 prefix 변경

CRA: `REACT_APP_*` → Vite: `VITE_*`

현재 `src/apiConfig.js`에서 `process.env.REACT_APP_*` 사용 여부를 확인해야 한다. `.env` 파일이 있다면 prefix를 일괄 변경해야 하며, 코드 내 `process.env.REACT_APP_XXX`를 `import.meta.env.VITE_XXX`로 교체해야 한다.

> 현재 `apiConfig.js`를 보면 환경변수보다 하드코딩된 상수를 주로 사용하므로 영향 범위가 제한적일 수 있다.

---

### 3.3 `index.html` 위치 변경

CRA: `public/index.html` (빌드 시 자동 처리)
Vite: 프로젝트 **루트**의 `index.html` (직접 `<script type="module">` 태그 추가)

```html
<!-- Vite용 루트 index.html에 추가 필요 -->
<script type="module" src="/src/index.js"></script>
```

`public/` 디렉토리는 그대로 유지되며 정적 파일 서빙에 사용된다.

---

### 3.4 CommonJS → ESM 전환

현재 `src/setProxy.js`가 CommonJS 문법을 사용한다:

```js
// src/setProxy.js (현재 — CommonJS)
const { createProxyMiddleware } = require('http-proxy-middleware');
module.exports = function (app) { ... };
```

Vite 전환 시 이 파일은 `vite.config.js`의 `server.proxy`로 완전히 대체되므로 삭제해도 된다. `http-proxy-middleware` 패키지도 제거 가능하다.

---

### 3.5 `browserslist` 설정 방식 차이

현재 `package.json`의 `browserslist` 블록은 Vite에서 `build.target` 옵션으로 대체된다. Vite 기본값(`es2015`)은 현재 CRA 설정과 비슷하나 명시적으로 확인이 필요하다.

---

## 4. 이 프로젝트에서 추가로 확인해야 할 호환성 이슈

### 4.1 Draft.js 관련 패키지 — CJS 모듈 충돌 가능성 (높음)

이 프로젝트에서 가장 주의가 필요한 부분이다. 아래 패키지들이 모두 CommonJS 전용으로 배포된다:

| 패키지 | Vite ESM 호환 | 조치 |
|--------|-------------|------|
| `draft-js` | ⚠️ CJS 전용 | `optimizeDeps.include` 필요 |
| `react-draft-wysiwyg` | ⚠️ CJS 전용 | `optimizeDeps.include` 필요 |
| `draftjs-to-html` | ⚠️ CJS 전용 | `optimizeDeps.include` 필요 |
| `html-to-draftjs` | ⚠️ CJS 전용 | `optimizeDeps.include` 필요 |

`vite.config.js`에 아래 설정 없이는 런타임에 모듈 로드 오류가 발생할 수 있다:

```js
export default defineConfig({
  optimizeDeps: {
    include: [
      'draft-js',
      'react-draft-wysiwyg',
      'draftjs-to-html',
      'html-to-draftjs',
    ],
  },
});
```

> Draft.js가 유지보수 종료 상태이므로, Vite 마이그레이션과 Draft.js 교체를 동시에 진행하면 이 문제를 아예 우회할 수 있다.

---

### 4.2 `redux-persist` — ESM 호환

`redux-persist`는 일반적으로 Vite와 호환되나, 현재 `store.js`에서 `redux-persist/es/persistReducer` (ES 경로)를 직접 임포트하고 있어 문제 없다.

---

### 4.3 `@draft-js-plugins/editor`, `@draft-js-plugins/image`

Draft.js 플러그인 패키지들도 CJS 전용이므로 `optimizeDeps.include`에 함께 추가해야 한다.

---

### 4.4 `react-draft-wysiwyg` CSS 임포트

`BoardEditor.js`에서 `import "react-draft-wysiwyg/dist/react-draft-wysiwyg.css"` 방식을 사용하는데, Vite는 이 방식을 지원하므로 변경 없이 동작한다.

---

### 4.5 `styled-components` — Vite 플러그인

`styled-components` 6.x는 Vite와 기본 호환되지만, SSR 또는 Babel 플러그인 기능(클래스명 자동생성 등)이 필요한 경우 `babel-plugin-styled-components`를 Vite 플러그인에 포함해야 한다.

```js
// 필요 시 babel 플러그인 추가
plugins: [
  react({
    babel: {
      plugins: ['babel-plugin-styled-components'],
    },
  }),
],
```

---

## 5. 예상 작업 목록

### 필수 작업 (마이그레이션 완료를 위한 최소 범위)

| # | 작업 | 난이도 | 비고 |
|---|------|--------|------|
| 1 | `react-scripts` 제거, `vite` + `@vitejs/plugin-react` 설치 | 낮음 | |
| 2 | `package.json` scripts 교체 | 낮음 | `react-scripts` → `vite` |
| 3 | 루트 `index.html` 생성 (기존 `public/index.html` 이동 + script 태그 추가) | 낮음 | |
| 4 | `vite.config.js` 생성 (proxy 설정 포함) | 낮음 | `setProxy.js` 대체 |
| 5 | `src/setProxy.js` 삭제, `http-proxy-middleware` 제거 | 낮음 | |
| 6 | `optimizeDeps` 설정 (Draft.js 계열 CJS 패키지) | 중간 | 누락 시 런타임 오류 |
| 7 | 환경변수 prefix 확인 및 변경 (`REACT_APP_` → `VITE_`) | 낮음 | 현 프로젝트는 최소 영향 예상 |
| 8 | `setupTests.js` → Vitest 설정으로 전환 | 낮음 | 현재 테스트 파일 없음 |

### 선택 작업 (품질 향상)

| # | 작업 | 비고 |
|---|------|------|
| 9 | ESLint 직접 설치 및 설정 (`eslint.config.js`) | ESLint 9 flat config 방식 권장 |
| 10 | `@tanstack/eslint-plugin-query` 버전 고정 해제 | ESLint 제약 해소 후 가능 |
| 11 | `vite-tsconfig-paths` 등 경로 alias 설정 | TypeScript 도입 예정 시 |

---

## 6. 종합 판단

### 이점 vs 단점 요약

| 구분 | 내용 |
|------|------|
| **이점** (큰 것) | 취약점 ~30개 즉시 해소, 개발 서버 10배 빠름, ESLint 제약 해제 |
| **이점** (부수적) | 설정 완전 제어, 번들 최적화, 최신 생태계, DEPRECATED 경고 제거 |
| **단점** (큰 것) | Draft.js 계열 CJS 호환 이슈 (optimizeDeps 설정으로 해결 가능) |
| **단점** (부수적) | index.html 이동, proxy 재설정, 환경변수 prefix 확인 |

### 권고

**마이그레이션을 진행하되, Draft.js 교체와 함께 계획하는 것을 권장한다.**

- **단독 진행 시**: Draft.js의 CJS 모듈 이슈를 `optimizeDeps` 설정으로 우선 우회하고, Vite 전환을 완료한다. 이후 Draft.js를 Tiptap 또는 Quill로 교체한다.
- **동시 진행 시**: Draft.js → Tiptap 교체와 Vite 마이그레이션을 같은 PR에서 처리하면 CJS 이슈를 원천 제거할 수 있다. 단, 작업 범위가 커져 리뷰 부담이 증가한다.

### 마이그레이션 난이도

```
전체 난이도: ★★☆☆☆ (낮음)

이유:
- 소스 코드 변경은 최소 (index.html, vite.config.js 신규, setProxy.js 삭제)
- 현재 테스트 파일이 없어 테스트 환경 마이그레이션 부담 없음
- 환경변수 하드코딩이 많아 prefix 변경 영향 최소
- Draft.js 이슈는 설정 한 줄로 우회 가능

주의:
- Draft.js optimizeDeps 누락 시 에디터(글 작성 화면)가 런타임 오류 발생
- 마이그레이션 후 반드시 글 작성, 수정, 이미지 업로드 기능 수동 검증 필요
```
