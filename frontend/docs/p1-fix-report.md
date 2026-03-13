# P1 버그 수정 보고서

> 작성일: 2026-03-12
> 참고 문서: `frontend-review-report.md`
> 대상 브랜치: `develop`

---

## 수정 항목 목록

| # | 심각도 | 파일 | 문제 | 상태 |
|---|--------|------|------|------|
| H-1 | High | `src/components/VisitorCount.js` | localhost 하드코딩 | ✅ 수정 완료 |
| H-2 | High | `public/index.html` | CDN + npm 패키지 이중 로드 | ✅ 수정 완료 |
| H-3 | High | `src/components/Boards/BoardList.js` | `prototype` 오타 (propTypes 무효화) | ✅ 수정 완료 |

---

## H-1: VisitorCount.js — localhost 하드코딩 제거

### 문제

```javascript
// src/components/VisitorCount.js (수정 전)
const response = await axios.get(
  "http://localhost:8080/api/v2/visitor-count"
);
```

`REACT_APP_API_URL` 환경 변수를 무시하고 localhost를 직접 사용하고 있어 **프로덕션 환경에서 API 호출이 실패**하는 버그.

### 수정

```diff
+ import { BASE_URL } from "../apiConfig";

  const response = await axios.get(
-   "http://localhost:8080/api/v2/visitor-count"
+   `${BASE_URL}/api/v2/visitor-count`
  );
```

### 효과

- `REACT_APP_API_URL` 환경 변수를 통해 API 주소가 주입되므로 개발/스테이징/프로덕션 모든 환경에서 정상 동작
- `apiConfig.js`의 `BASE_URL` fallback(`http://localhost:8080`)이 그대로 유지되므로 로컬 개발 환경에는 영향 없음

---

## H-2: public/index.html — CDN 중복 로드 제거

### 문제

`index.html`에 CDN 스크립트가 포함되어 있었으나, 동일한 라이브러리가 npm 패키지로도 번들에 포함되어 **이중 로드** 상태였다.

```html
<!-- 수정 전 — 제거된 태그들 -->
<script src="https://cdn.jsdelivr.net/npm/react/umd/react.production.min.js" crossorigin></script>
<script src="https://cdn.jsdelivr.net/npm/react-dom/umd/react-dom.production.min.js" crossorigin></script>
<script src="https://cdn.jsdelivr.net/npm/react-bootstrap@next/dist/react-bootstrap.min.js" crossorigin></script>
<script>var Alert = ReactBootstrap.Alert;</script>
<link rel="stylesheet" href="https://cdn.jsdelivr.net/npm/bootstrap@5.2.3/dist/css/bootstrap.min.css" ... />
```

| CDN 항목 | 문제 |
|---------|------|
| `react` CDN | npm 번들과 충돌 — React 인스턴스 이중화로 훅 오작동 가능 |
| `react-dom` CDN | 동일 |
| `react-bootstrap` CDN | npm 패키지와 스타일/컴포넌트 충돌 |
| `var Alert = ReactBootstrap.Alert` | CDN 전역 변수 의존 — CDN 제거 후 불필요 |
| Bootstrap CSS CDN | npm `bootstrap` 패키지와 스타일 이중 적용 |

### 수정

위 5개 항목 전체 제거. Google Fonts(Noto Sans KR, Playfair Display)와 FontAwesome Kit은 npm 패키지가 없으므로 유지.

```html
<!-- 수정 후 — 유지된 외부 리소스 -->
<link rel="preconnect" href="https://fonts.googleapis.com" />
<link rel="preconnect" href="https://fonts.gstatic.com" crossorigin />
<link href="https://fonts.googleapis.com/css2?..." rel="stylesheet" />
<script src="https://kit.fontawesome.com/9cdfdf3db8.js" crossorigin="anonymous"></script>
```

### 효과

- React 인스턴스 이중화 제거 → 훅(useState, useContext 등) 관련 잠재 오작동 방지
- Bootstrap 스타일 이중 적용 제거 → CSS 충돌 해소
- 초기 HTML 파싱 시 불필요한 CDN 스크립트 다운로드 제거 → **페이지 로드 속도 개선**
- CRA 번들(npm 패키지)이 유일한 소스가 되어 버전 일관성 보장

---

## H-3: BoardList.js — propTypes 오타 수정

### 문제

```javascript
// src/components/Boards/BoardList.js (수정 전)
BoardList.prototype = {
  boards: PropTypes.array,
  path: PropTypes.string,
};
```

`propTypes` 대신 `prototype`으로 작성되어 있어 **React prop 타입 검증이 전혀 동작하지 않는 상태**. `PropTypes`를 import하고 있음에도 런타임에서 잘못된 prop이 전달되어도 경고가 발생하지 않는다.

### 수정

```diff
- BoardList.prototype = {
+ BoardList.propTypes = {
    boards: PropTypes.array,
    path: PropTypes.string,
  };
```

### 효과

- `boards`, `path` prop 누락 또는 타입 불일치 시 개발 환경 콘솔 경고 정상 출력
- 향후 잘못된 prop 전달로 인한 런타임 에러 조기 탐지 가능

---

## 수정 전후 비교 요약

### `src/components/VisitorCount.js`

```diff
  import React, { useEffect, useState } from "react";
  import axios from "axios";
  import { Container, Card, Spinner } from "react-bootstrap";
+ import { BASE_URL } from "../apiConfig";

  ...

        const response = await axios.get(
-         "http://localhost:8080/api/v2/visitor-count"
+         `${BASE_URL}/api/v2/visitor-count`
        );
```

### `public/index.html`

```diff
- <script src="https://cdn.jsdelivr.net/npm/react/umd/react.production.min.js" crossorigin></script>
- <script src="https://cdn.jsdelivr.net/npm/react-dom/umd/react-dom.production.min.js" crossorigin></script>
- <script src="https://cdn.jsdelivr.net/npm/react-bootstrap@next/dist/react-bootstrap.min.js" crossorigin></script>
- <script>var Alert = ReactBootstrap.Alert;</script>
- <link rel="stylesheet" href="https://cdn.jsdelivr.net/npm/bootstrap@5.2.3/dist/css/bootstrap.min.css" ... />
  <script src="https://kit.fontawesome.com/9cdfdf3db8.js" crossorigin="anonymous"></script>
```

### `src/components/Boards/BoardList.js`

```diff
- BoardList.prototype = {
+ BoardList.propTypes = {
    boards: PropTypes.array,
    path: PropTypes.string,
  };
```
