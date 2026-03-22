# 프론트엔드 2차 점검 보고서

> 작성일: 2026-03-22
> 점검 범위: `frontend/src/` 전체 (50개+ JS 파일)
> 도구: context7 MCP (dayjs, react-syntax-highlighter 레퍼런스 활용)

---

## 요약

| 심각도 | 건수 | 주요 내용 |
|--------|------|---------|
| Critical | 4 | Dead code, 로직 버그, 오타, 빈 catch |
| Warning | 3 | console.log 다수, window.location, XSS 필터 누락 |
| Info | 2 | role 로딩 UX, axios 설정 중복 |

---

## [Critical] 즉시 수정 필요

### 1. `BoardForm.js` — Dead Code + 미설치 패키지 import

**파일**: `src/components/Boards/BoardForm.js`

**문제**:
- `draft-js`, `react-draft-wysiwyg`, `draftjs-to-html` import하지만 `package.json`에 없음
- 컴포넌트를 import하는 파일이 프로젝트 전체에 **단 한 군데도 없음**
- Tiptap 기반 `BoardEditor.js`가 이미 동일 역할 수행 중

**조치**: 파일 삭제

---

### 2. `BoardList.js:15` — `truncateText` 함수 반환값 버그

**파일**: `src/components/Boards/BoardList.js`

**문제**:
```js
// 현재 (버그)
const truncateText = (text, maxLength) => {
  text.replace(/<[^>]*>/g, "");   // ❌ 반환값을 버림, text는 변경되지 않음
  if (text.length <= maxLength) { // HTML 태그 포함된 원본 길이로 비교
    return text;
  } else {
    return text.slice(0, maxLength) + "...";
  }
};
```

**영향**: 게시글 미리보기에서 HTML 태그가 포함된 채로 길이가 계산되어
실제보다 짧게 잘리거나, 태그 중간에서 잘려 깨진 HTML이 노출될 수 있음

**수정안**:
```js
const truncateText = (text, maxLength) => {
  const plainText = text.replace(/<[^>]*>/g, ""); // ✅ 반환값 사용
  if (plainText.length <= maxLength) {
    return plainText;
  }
  return plainText.slice(0, maxLength) + "...";
};
```

---

### 3. `PageButton.js:23` — `propTypes` 오타

**파일**: `src/components/Boards/PageButton.js`

**문제**:
```js
// 현재 (버그)
PageButton.prototype = {       // ❌ Object.prototype을 덮어쓰는 심각한 오타
  pageCount: PropTypes.number,
};
```

**영향**: PropTypes 타입 검사가 전혀 동작하지 않음.
`PageButton.prototype`은 JavaScript 프로토타입 체인을 가리키므로
잘못된 값을 넘겨도 경고가 발생하지 않음.

**수정안**:
```js
PageButton.propTypes = {       // ✅ 정상
  pageCount: PropTypes.number,
};
```

---

### 4. `ProtectedRoute.js:20` — 토큰 만료 빈 catch 블록

**파일**: `src/components/Layout/ProtectedRoute.js`

**문제**:
```js
.catch((error) => {
  if (error.response.data === "토큰이 만료되었습니다.") {
    // 아무 처리 없음
  }
});
```

**영향**: 토큰 만료 시 사용자에게 아무 피드백 없이 화면이 멈춤.
`role`이 `null`인 채로 유지되어 페이지가 빈 화면으로 남음.

**수정안**:
```js
.catch((error) => {
  if (error.response?.data === "토큰이 만료되었습니다.") {
    dispatch(logout()); // 또는 navigate("/login")
  }
});
```

---

## [Warning] 수정 권고

### 5. `console.log` 25개 잔존

**대상 파일 및 라인**:

| 파일 | 라인 |
|------|------|
| `components/Boards/BoardDetail.js` | 67 |
| `components/Boards/BoardEditForm.js` | 106, 120, 133, 150 |
| `components/Boards/BoardEditor.js` | 89, 117 |
| `components/Boards/BoardLike.js` | 31, 46 |
| `components/Category/CategoryForm.js` | 13, 32 |
| `components/Category/CategoryList.js` | 38, 51 |
| `components/Category/CategoryModal.js` | 36 |
| `components/Comments/Comment.js` | 33, 43 |
| `components/Comments/CommentForm.js` | 47 |
| `components/Navbar/CategoryNav.js` | 11 |
| `screens/Management.js` | 18 |
| `screens/PageByCategory.js` | 21 |
| `screens/SearchPage.js` | 23 |
| `screens/TemporaryStorage.js` | 17 |
| `services/authApi.js` | 38 |

> **예외**: `ErrorBoundary.js`와 `VisitorCount.js`의 `console.error`는 에러 추적 목적이므로 유지.

---

### 6. `window.location.href` — React Router `navigate()` 전환 권고 (5곳)

`window.location.href`를 사용하면 브라우저가 전체 새로고침을 수행해
React 상태(Redux, React Query 캐시)가 모두 초기화됨.

**전환 대상**:

| 파일 | 라인 | 현재 | 수정안 |
|------|------|------|--------|
| `screens/Member/JoinForm.js` | 51 | `window.location.href = "/login"` | `navigate("/login")` |
| `screens/Member/JoinForm.js` | 53 | `window.location.href = "/"` | `navigate("/")` |
| `components/Boards/BoardEditForm.js` | 103 | `window.location.href = \`/management/boards/${data}\`` | `navigate(\`/management/boards/${data}\`)` |
| `components/Boards/BoardEditForm.js` | 130, 147 | `window.location.href = "/management/temporary-storage"` | `navigate("/management/temporary-storage")` |
| `components/Boards/BoardEditor.js` | 114 | `window.location.href = \`/management/boards/${res.data}\`` | `navigate(\`/management/boards/${res.data}\`)` |

> **제외**: `Comment.js`, `CommentForm.js`, `CategoryList.js`, `Header.js`의
> `window.location.reload()`는 서버 데이터 강제 갱신 목적으로 현재 구조에서는 유지.
> (RTK Query invalidation 도입 시 제거 가능)

---

### 7. `BoardDetail.js:85` — DOMPurify 누락 (XSS)

**파일**: `src/components/Boards/BoardDetail.js`

**문제**:
```js
// BoardDetailV2.js (적용됨 ✅)
<div>{Parser(DOMPurify.sanitize(board.data.content))}</div>

// BoardDetail.js (누락 ❌)
<div>{Parser(board.content)}</div>
```

**영향**: 악성 스크립트가 포함된 게시글 콘텐츠가 그대로 실행될 수 있음.

**수정안**:
```js
import DOMPurify from "dompurify";
// ...
<div>{Parser(DOMPurify.sanitize(board.content))}</div>
```

---

## [Info] 개선 권고

### 8. `ProtectedRoute.js` — role 로딩 중 빈 화면

**파일**: `src/components/Layout/ProtectedRoute.js:56`

**문제**: `role`이 `null`인 동안 `return null`로 빈 화면 노출.

**수정안**:
```js
if (role === null) return <div>Loading...</div>; // 또는 Spinner
```

---

### 9. `VisitorCount.js:10` — axios 전역 설정 중복

**파일**: `src/components/VisitorCount.js`

**문제**:
```js
axios.defaults.withCredentials = true; // 컴포넌트 렌더링마다 실행
```

`apiClient.js`에서 이미 `withCredentials: true`로 설정됨.
해당 컴포넌트에서 직접 `axios`를 사용하고 있다면 `apiClient`로 교체하는 것이 바람직함.

---

## 작업 계획

### Phase 1 — Critical 수정 (4건)
- [ ] `BoardForm.js` 삭제
- [ ] `BoardList.js` `truncateText` 반환값 버그 수정
- [ ] `PageButton.js` `prototype` → `propTypes` 오타 수정
- [ ] `ProtectedRoute.js` 빈 catch + role null 처리

### Phase 2 — Warning 수정 (3건)
- [ ] `console.log` 25개 제거 (15개 파일)
- [ ] `window.location.href` → `navigate()` 전환 (5곳)
- [ ] `BoardDetail.js` DOMPurify 추가

### Phase 3 — Info 개선 (선택)
- [ ] `ProtectedRoute.js` 로딩 스피너 추가
- [ ] `VisitorCount.js` axios 전역 설정 제거 및 apiClient 활용

---

## 라이브러리 버전 현황 (점검일 기준)

| 패키지 | 현재 버전 | 상태 |
|--------|----------|------|
| react | 18.2.0 | ✅ |
| react-router | 7.13.1 | ✅ |
| @reduxjs/toolkit | 2.11.2 | ✅ |
| @tanstack/react-query | 5.90.21 | ✅ |
| axios | 1.13.6 | ✅ |
| dayjs | 1.11.x | ✅ (moment 대체 완료) |
| dompurify | 3.3.3 | ✅ |
| vite | 6.4.1 | ✅ |
| 취약점 | **0개** | ✅ |
