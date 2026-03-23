# RTK 2.x 마이그레이션 계획서

> 작성일: 2026-03-14
> 대상 브랜치: `develop`
> 참고: context7 `/reduxjs/redux-toolkit` (v2.11.0 문서), 코드베이스 직접 분석

---

## 1. 목표 버전

| 패키지 | 현재 | 목표 | 비고 |
|--------|------|------|------|
| `@reduxjs/toolkit` | ^1.9.5 | ^2.11.0 | RTK 최신 stable |
| `react-redux` | ^8.1.2 | ^9.x | RTK 2.x 공식 동반 버전 |
| `redux` | ^4.2.1 | ^5.x | RTK 2.x peer dependency |
| `react-cookie` | ^4.1.1 | ^8.x | `cookie` 취약점 해결 (2개) |

`redux-persist ^6.0.0`, `redux-logger ^3.0.6` — 버전 변경 없음 (호환 유지)

---

## 2. 현재 코드 분석 결과

### 2.1 `src/redux/userSlice.js` — 영향 없음 ✅

```js
// extraReducers 사용 없음 — reducers만 사용
const userSlice = createSlice({
  name: "user",
  initialState: { isLoggedIn: false, accessToken: null },
  reducers: { login, logout, updateAccessToken },
});
```

RTK 2.x 최대 breaking change인 `extraReducers` 객체 문법 제거가 해당 없음.
**codemod 실행 불필요.**

### 2.2 `src/redux/authAction.js` — 영향 없음 ✅

```js
// createAsyncThunk 미사용 — 순수 thunk 패턴
export const userLogin = (accessToken) => (dispatch) => dispatch(login({ accessToken }));
```

`createAsyncThunk` 타입 변경(RTK 2.x)과 무관.

### 2.3 `src/redux/store.js` — 수정 필요 ⚠️

**문제 1 — `ignoreActions` 오타 + redux-persist 액션 누락:**

```js
// 현재 — 두 가지 문제
serializableCheck: {
  ignoreActions: ["persist/PERSIST"],   // ❌ 오타: 올바른 키는 ignoredActions
                                        // ❌ 누락: REHYDRATE, PAUSE, PURGE, REGISTER 미등록
}
```

context7 RTK 공식 문서에 따르면, redux-persist 연동 시 6개 액션 상수
(`FLUSH, REHYDRATE, PAUSE, PERSIST, PURGE, REGISTER`)를 모두 `ignoredActions`에 등록해야 한다.
`ignoreActions`는 RTK에서 인식하지 않는 키이며, 직렬화 검사가 사실상 무력화되어 있었을 가능성이 있다.

**문제 2 — redux-logger 프로덕션 포함:**

```js
// 현재 — NODE_ENV 무관하게 항상 포함
.concat(logger)
```

---

### 2.4 `src/index.js` — react-cookie API 확인 필요 ⚠️

```js
import { CookiesProvider } from "react-cookie";
// ...
<CookiesProvider> ... </CookiesProvider>
```

react-cookie 8.x에서 `CookiesProvider`는 기존 4.x API와 동일하게 동작하나,
내부 `universal-cookie` v7 의존성 업그레이드로 쿠키 파싱 동작이 일부 변경됨.
현 프로젝트에서 `useCookies` 훅은 **사용되지 않음** (index.js CookiesProvider만 존재) — 변경 최소화.

---

## 3. Breaking Changes 체크리스트

### RTK 2.x (context7 문서 기반)

| # | Breaking Change | 현재 코드 영향 | 조치 |
|---|----------------|--------------|------|
| B-1 | `extraReducers` 객체 문법 제거 | 없음 (미사용) | 없음 |
| B-2 | `createReducer` 객체 문법 제거 | 없음 (미사용) | 없음 |
| B-3 | `action.type` 반드시 string 강제 | 없음 (createSlice 사용) | 없음 |
| B-4 | Middleware `action`/`next` 타입 `unknown`으로 변경 | JS 프로젝트 — 런타임 무영향 | 없음 |
| B-5 | `getDefaultMiddleware` 단독 import 제거 | 이미 콜백 방식 사용 | 없음 |

### react-redux 9.x

| # | Breaking Change | 현재 코드 영향 | 조치 |
|---|----------------|--------------|------|
| R-1 | React 16/17 지원 제거 (18+ 필수) | React 18.2.0 사용 중 | 없음 |
| R-2 | `connect()` strict mode 동작 변경 | `connect()` 미사용 | 없음 |
| R-3 | `batch` API 변경 | `batch` 미사용 | 없음 |

### redux 5.x

| # | Breaking Change | 현재 코드 영향 | 조치 |
|---|----------------|--------------|------|
| X-1 | `action.type` string 강제 (runtime 에러) | createSlice 사용 — 이미 string | 없음 |
| X-2 | CJS/ESM 모듈 구조 변경 | 번들러(webpack) 자동 처리 | 없음 |

### react-cookie 4.x → 8.x

| # | Breaking Change | 현재 코드 영향 | 조치 |
|---|----------------|--------------|------|
| C-1 | `universal-cookie` v4 → v7 의존성 | `CookiesProvider`만 사용 | API 동일 — import 유지 |
| C-2 | `useCookies` 2번째 인자 옵션 구조 변경 | `useCookies` 미사용 | 없음 |
| C-3 | `CookiesProvider` `defaultSetOptions` prop 추가 | 기존 prop 없이 사용 — 무영향 | 없음 |

---

## 4. 실행 계획 (순서)

### Step 1 — codemod 실행 (extraReducers 변환)

```bash
npx @reduxjs/rtk-codemods createSliceBuilder ./src/**/*.js
```

> 현재 코드에서 `extraReducers` 객체 문법이 없으므로 변환 대상 0건 예상.
> 실행 후 diff를 확인하여 의도치 않은 변경이 없는지 검증.

### Step 2 — 패키지 업그레이드

```bash
npm install \
  @reduxjs/toolkit@^2.11.0 \
  react-redux@^9.0.0 \
  redux@^5.0.0 \
  react-cookie@^8.0.0
```

### Step 3 — `store.js` 수정 (2곳)

**3-A. `serializableCheck` 보완 (context7 RTK 공식 문서 기반):**

```js
// Before — 두 가지 문제 존재
serializableCheck: {
  ignoreActions: ["persist/PERSIST"],   // ❌ 오타: ignoreActions → ignoredActions
                                        // ❌ 누락: redux-persist 6개 액션 중 1개만 등록
}

// After — redux-persist 공식 권장 패턴
import {
  FLUSH, REHYDRATE, PAUSE, PERSIST, PURGE, REGISTER,
} from "redux-persist";

serializableCheck: {
  ignoredActions: [FLUSH, REHYDRATE, PAUSE, PERSIST, PURGE, REGISTER],
}
```

> `ignoreActions`는 RTK 1.x부터 이미 무효 키(오타)였으나 내부적으로 무시되어 경고가 발생했을 수 있음.
> RTK 2.x에서는 `ignoredActions`만 유효하며, redux-persist의 6개 액션 상수를 모두 등록하는 것이 공식 권장 패턴.

**3-B. redux-logger 개발 환경 한정:**

```js
// Before
.concat(logger)

// After
...(process.env.NODE_ENV === "development" ? [logger] : [])
```

### Step 4 — 동작 검증

```bash
npm start        # 개발 서버 정상 기동 확인
npm run build    # 빌드 에러 없음 확인
```

체크포인트:
- [x] ✅ 프로덕션 빌드 성공 (`npm run build` — 빌드 -1.23 kB 감소, 2026-03-14)
- [x] ✅ 브라우저 콘솔에 Redux 직렬화 경고 없음 — `persist/PERSIST`, `persist/REHYDRATE` 모두 경고 없이 처리 (Playwright 확인, 2026-03-14)
- [x] ✅ 로그인/로그아웃 정상 동작 — login 액션 dispatch 후 Logout 버튼 전환, logout 후 `isLoggedIn: false` / `accessToken: null` (Playwright 확인, 2026-03-14)
- [x] ✅ 토큰 재발급(updateAccessToken) 정상 동작 — Redux store 직접 dispatch 후 즉시 반영 확인 (Playwright 확인, 2026-03-14)
- [x] ✅ redux-persist hydration 정상 — 새로고침 후 로그인 상태(Logout 버튼) 유지, REHYDRATE 액션 직렬화 경고 없음 (Playwright 확인, 2026-03-14)
- [x] ✅ 프로덕션 빌드 시 redux-logger 콘솔 출력 없음 — `NODE_ENV=production` 빌드 serve(3001) 에서 logger 출력 0건 확인 (Playwright 확인, 2026-03-14)
- [x] ✅ react-cookie CookiesProvider 정상 렌더링 — React fiber 탐색으로 `CookiesProvider → Provider → PersistGate → QueryClientProvider` 전체 Provider 트리 마운트 확인 (Playwright 확인, 2026-03-14)

---

## 5. 변경 파일 목록

| 파일 | 변경 내용 |
|------|----------|
| `package.json` | 4개 패키지 버전 업 |
| `src/redux/store.js` | `ignoredActions` 키 수정 + `persist/REHYDRATE` 추가, logger dev-only |
| `src/index.js` | 변경 없음 (CookiesProvider API 동일) |
| `src/redux/userSlice.js` | 변경 없음 |
| `src/redux/authAction.js` | 변경 없음 |

**총 수정 파일: 2개** (`package.json`, `store.js`)

---

## 6. 롤백 계획

작업 전 현재 `package-lock.json`을 git stash 또는 별도 브랜치로 보존.
문제 발생 시:

```bash
git checkout -- package.json package-lock.json src/redux/store.js
npm ci
```

---

## 7. 취약점 해결 기대치

| 현재 | 작업 후 | 감소 |
|------|--------|------|
| 32개 | ~30개 | -2개 (`react-cookie` 내 `cookie` 패키지 취약점) |

> react-scripts 체인 ~23개는 CRA→Vite 전환(4단계) 전까지 해결 불가.

---

## 8. 후속 작업

이 마이그레이션 완료 후 다음 단계:

- **3단계**: React Router v7 마이그레이션 (`react-router-dom` → `react-router@7`)
- **4단계**: CRA → Vite 마이그레이션 (react-scripts 취약점 근본 해결)
