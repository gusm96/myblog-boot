# Frontend 라이브러리 버전 업그레이드 — 1단계 완료 보고서

> 작성일: 2026-03-13
> 작업 범위: `package.json` 의존성 안전 업데이트 (동일 major 범위)
> 도구: `npm outdated`, `npm audit`, context7 MCP

---

## 목차

1. [사전 재검토 결과](#1-사전-재검토-결과)
2. [1단계 업데이트 내역](#2-1단계-업데이트-내역)
3. [특이사항 — @tanstack/eslint-plugin-query 버전 고정](#3-특이사항--tanstackeslint-plugin-query-버전-고정)
4. [취약점 분석 (Before / After)](#4-취약점-분석-before--after)
5. [잔여 outdated 패키지 분류](#5-잔여-outdated-패키지-분류)
6. [후속 단계 로드맵](#6-후속-단계-로드맵)

---

## 1. 사전 재검토 결과

`npm outdated` 실행 결과, package.json에 명시된 버전과 실제 설치된 버전 사이에 **다수 불일치**가 확인되었다. 또한 총 **65개 취약점**이 검출되었다.

### 1.1 업그레이드 가능 패키지 분류

| 분류 | 기준 | 대상 수 |
|------|------|---------|
| 1단계 (안전) | 동일 major 범위 내 minor/patch 업데이트 | 13개 |
| 2단계 (코드 수정 필요) | major 버전 변경, breaking change 있음 | 4개 |
| 3단계 (라이브러리 교체) | 유지보수 종료 또는 major 교체 | 3개 |

### 1.2 사전 취약점 현황

```
65 vulnerabilities (17 low, 16 moderate, 31 high, 1 critical)
```

---

## 2. 1단계 업데이트 내역

### 2.1 업데이트 목록 (13개)

| 패키지 | package.json 이전 | package.json 이후 | 실 설치 버전 | 변경 유형 |
|--------|------------------|------------------|-------------|----------|
| `@stomp/stompjs` | ^7.0.0 | ^7.3.0 | 7.3.0 | minor |
| `@tanstack/react-query` | ^5.17.15 | ^5.90.21 | 5.90.21 | minor |
| `@tanstack/react-query-devtools` | ^5.17.18 | ^5.91.3 | 5.91.3 | minor |
| `axios` | ^1.3.5 | ^1.13.6 | 1.13.6 | minor |
| `bootstrap` | ^5.2.3 | ^5.3.8 | 5.3.8 | minor |
| `dompurify` | ^3.3.1 | ^3.3.3 | 3.3.3 | patch |
| `html-react-parser` | ^5.0.6 | ^5.2.17 | 5.2.17 | minor |
| `react-bootstrap` | ^2.7.4 | ^2.10.10 | 2.10.10 | minor |
| `react-router-dom` | ^6.11.1 | ^6.30.3 | 6.30.3 | minor (v6 내 최신) |
| `react-syntax-highlighter` | ^15.6.1 | ^15.6.6 | 15.6.6 | patch |
| `styled-components` | ^6.0.0-rc.1 | ^6.3.11 | 6.3.11 | RC → stable |
| `ws` | ^8.16.0 | ^8.19.0 | 8.19.0 | minor |
| `@tanstack/eslint-plugin-query` (dev) | ^5.17.7 | **5.52.3 (고정)** | 5.52.3 | minor (제약 있음, §3 참고) |

### 2.2 업데이트 제외 목록

아래 패키지는 1단계 범위에서 **의도적으로 제외**하였다.

| 패키지 | 현재 | 제외 이유 |
|--------|------|-----------|
| `@reduxjs/toolkit` | 1.9.7 | major 2.x — `extraReducers` 문법 변경, 2단계 대상 |
| `react-redux` | 8.1.3 | major 9.x — RTK 2.x와 함께 마이그레이션 필요 |
| `redux` | 4.2.1 | major 5.x — RTK 2.x와 함께 마이그레이션 필요 |
| `react-router-dom` | 6.30.3 | v7 — 패키지명 변경, 3단계 대상 |
| `react`, `react-dom` | 18.2.0 | major 19.x — 대규모 변경, 별도 검토 필요 |
| `@testing-library/*` | 5.x / 13.x | major 버전 — React 19 대응과 연동 필요 |
| `react-cookie` | 4.1.1 | major 8.x — 쿠키 관리 로직 변경 필요 |
| `web-vitals` | 2.1.4 | major 5.x — 별도 검토 필요 |
| `http-proxy-middleware` | 2.0.6 | major 3.x — 별도 검토 필요 |

---

## 3. 특이사항 — @tanstack/eslint-plugin-query 버전 고정

### 문제

CRA(`react-scripts 5.0.1`)가 내부적으로 **ESLint 8.56.0**을 고정 사용한다. `@tanstack/eslint-plugin-query@5.53.0` 이상은 peer dependency가 `eslint@^8.57.0 || ^9.0.0`으로 상향되어 8.56.0과 충돌한다.

### 경계 버전 조사 결과

| 버전 | eslint peer dep |
|------|----------------|
| 5.52.3 | `^8 \|\| ^9` ✅ ESLint 8.56.0 호환 |
| 5.53.0 | `^8.57.0 \|\| ^9.0.0` ❌ 8.56.0 불호환 |

### 조치

```json
// devDependencies — ^ 없이 정확한 버전 고정
"@tanstack/eslint-plugin-query": "5.52.3"
```

`^`를 붙이면 npm이 최신 5.x (5.91.4)로 resolve하여 동일 오류가 재발한다.

**근본 해결**: CRA → Vite 마이그레이션 후 ESLint 버전 제약이 해소되면 `^5.91.4`로 자유롭게 업데이트 가능하다.

---

## 4. 취약점 분석 (Before / After)

### 4.1 요약

| 시점 | critical | high | moderate | low | 합계 |
|------|---------|------|----------|-----|------|
| 1단계 업데이트 전 | 1 | 31 | 16 | 17 | **65** |
| `npm audit fix` 후 | 0 | 21 | 6 | 12 | **39** |
| **감소** | -1 | -10 | -10 | -5 | **-26** |

### 4.2 잔여 취약점 분류

#### A. react-scripts 내부 (CRA → Vite 전환 전까지 해결 불가)

| 패키지 | severity | 원인 체인 |
|--------|----------|-----------|
| `@tootallnate/once` | high | jest → jsdom → react-scripts |
| `nth-check` | high | svgo → @svgr/webpack → react-scripts |
| `postcss` (resolve-url-loader) | moderate | resolve-url-loader → react-scripts |
| `serialize-javascript` | high | workbox-webpack-plugin → react-scripts |
| `webpack-dev-server` | moderate | react-scripts 내장 |
| `underscore` | high | bfj → jsonpath → react-scripts |

이 그룹은 **`npm audit fix --force`** 를 실행하면 `react-scripts@0.0.0`으로 다운그레이드되어 빌드가 완전히 깨진다. **CRA → Vite 마이그레이션이 유일한 근본 해결책**이다.

#### B. Draft.js 의존성 (수정 불가 — 교체 필요)

| 패키지 | severity | 비고 |
|--------|----------|------|
| `immutable` < 3.8.3 | **high** | Draft.js 내부에 고정됨. npm fix 없음 |

Draft.js가 오래된 `immutable` 버전을 내부 peer dep으로 고정하고 있어 **패치 자체가 존재하지 않는다**. 이는 Draft.js를 Tiptap 또는 Quill로 교체해야 하는 또 다른 이유다.

#### C. 별도 마이그레이션으로 해결 가능

| 패키지 | severity | 해결 방법 | 예상 단계 |
|--------|----------|-----------|-----------|
| `cookie` (react-cookie 경유) | medium | `react-cookie` 4.x → 8.x 업그레이드 | 2단계 이후 |
| `prismjs` (react-syntax-highlighter 경유) | moderate | `react-syntax-highlighter` 15.x → 16.x | 2단계 이후 |

---

## 5. 잔여 outdated 패키지 분류

### 5.1 2단계 대상 — RTK 2.x 마이그레이션

| 패키지 | 현재 | 목표 | 주요 변경사항 |
|--------|------|------|--------------|
| `@reduxjs/toolkit` | 1.9.7 | 2.11.2 | `extraReducers` object syntax 제거 |
| `react-redux` | 8.1.3 | 9.2.0 | RTK 2.x 동반 업그레이드 |
| `redux` | 4.2.1 | 5.0.1 | action type 강제 string 처리 |
| `react-cookie` | 4.1.1 | 8.0.1 | 쿠키 API 변경 (cookie 취약점 해결) |

자동화 codemod 활용 가능:
```bash
npx @reduxjs/rtk-codemods createSliceBuilder ./src/**/*.js
```

### 5.2 3단계 대상 — React Router v7 마이그레이션

| 패키지 | 현재 | 목표 | 주요 변경사항 |
|--------|------|------|--------------|
| `react-router-dom` | 6.30.3 | react-router 7.13.1 | 패키지명 통합, import 경로 변경 |

- `react-router-dom` 제거 → `react-router` 설치
- `import ... from "react-router-dom"` → `import ... from "react-router"`
- Future flags 단계적 활성화로 점진적 마이그레이션 가능

### 5.3 4단계 대상 — 장기 아키텍처 개선

| 패키지 | 현재 | 목표 | 이유 |
|--------|------|------|------|
| `react-scripts` | 5.0.1 | Vite 마이그레이션 | CRA 공식 폐기, 취약점 근본 해결 |
| `draft-js` | 0.11.7 | Tiptap / Quill | 유지보수 종료, immutable 취약점 |
| `moment` | 2.29.4 | day.js | 유지보수 모드, 번들 크기 ~225KB |
| `react-syntax-highlighter` | 15.6.6 | 16.x | prismjs 취약점 해결 |
| `@tanstack/eslint-plugin-query` | 5.52.3 (고정) | 5.91.4 | Vite 전환 후 ESLint 제약 해소 |

---

## 6. 후속 단계 로드맵

```
[완료] 1단계 — 안전 minor/patch 업데이트 (13개 패키지)
              └── npm audit: 65 → 39 취약점

[다음] 2단계 — RTK 2.x + react-redux 9.x + redux 5.x + react-cookie 8.x
              └── codemod 활용, 코드 변경 범위 제한적

[이후] 3단계 — React Router v7 마이그레이션
              └── import 경로 변경, future flags 활성화

[장기] 4단계 — CRA → Vite 마이그레이션
              └── 나머지 취약점(react-scripts 체인) 근본 해결
              └── @tanstack/eslint-plugin-query 고정 해제 가능

[별도] Draft.js → Tiptap/Quill 교체 (immutable High 취약점 해결)
[별도] moment → day.js 교체 (번들 크기 ~225KB 절감)
```

---

## 부록 — 최종 package.json 의존성 현황

```json
{
  "@stomp/stompjs": "^7.3.0",
  "@tanstack/react-query": "^5.90.21",
  "@tanstack/react-query-devtools": "^5.91.3",
  "axios": "^1.13.6",
  "bootstrap": "^5.3.8",
  "dompurify": "^3.3.3",
  "html-react-parser": "^5.2.17",
  "react-bootstrap": "^2.10.10",
  "react-router-dom": "^6.30.3",
  "react-syntax-highlighter": "^15.6.6",
  "styled-components": "^6.3.11",
  "ws": "^8.19.0",
  "@tanstack/eslint-plugin-query": "5.52.3"
}
```
