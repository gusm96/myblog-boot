# Frontend API Alignment Plan — 260409

백엔드 Member→Admin 마이그레이션 및 댓글 게스트 지원 변경에 맞춘 프론트엔드 수정 계획서.

---

## 변경 배경

| 변경 항목 | 이전 | 이후 |
|---|---|---|
| 회원가입 엔드포인트 | `POST /api/v1/join` 존재 | **삭제됨** |
| 댓글 작성 | 로그인(JWT) 필수 | 게스트 가능 (nickname + password) |
| 댓글 수정/삭제 | 미구현 | 관리자(JWT) / 게스트(password) 모두 지원 |
| 게시글 좋아요 | JWT 인증 기반 | **쿠키(HMAC) 기반 — JWT 불필요** |
| 댓글 응답 필드 | `writer` | `writer` (닉네임#식별자), `isAdmin` 추가 |

---

## Phase 1 — 회원가입 기능 제거

### 목표
백엔드에서 삭제된 `/api/v1/join` 엔드포인트를 참조하는 코드를 전부 제거한다.

### 변경 파일

**`src/apiConfig.js`**
- `MEMBER_JOIN` 상수 삭제
- `GENERATE_USER_NUMBER` 상수 삭제 (이미 사용처 없음)

**`src/services/authApi.js`**
- `join()` 함수 삭제
- `import { MEMBER_JOIN, ... }` 에서 `MEMBER_JOIN` 제거

**`src/App.js`**
- `import { JoinForm }` 제거
- `<Route path="join" element={...} />` 라우트 제거

**`src/screens/Member/JoinForm.js`**
- 파일 삭제

---

## Phase 2 — 댓글 API 레이어 수정

### 목표
`boardApi.js`의 댓글 함수들을 백엔드 신규 스팩에 맞게 정비한다.

### 백엔드 API 스팩

| Method | URL | Request Body | Auth |
|---|---|---|---|
| GET | `/api/v1/comments/{boardId}` | — | 불필요 |
| GET | `/api/v1/comments/child/{parentId}` | — | 불필요 |
| POST | `/api/v1/comments/{boardId}` | `{comment, parentId?, nickname?, password?}` | 게스트: 불필요 / 관리자: JWT |
| PUT | `/api/v1/comments/{commentId}` | `{comment, password?}` | 게스트: 불필요 / 관리자: JWT |
| DELETE | `/api/v1/comments/{commentId}` | `{password?}` | 게스트: 불필요 / 관리자: JWT |

> 관리자는 JWT를 헤더에 담아 요청하면 서버가 `isAdmin=true`로 처리.  
> 게스트는 nickname/password로 본인 인증.

### 변경 파일

**`src/services/boardApi.js`**

```js
// 수정: 게스트 필드 추가
export const addComment = (boardId, commentData) => {
  return apiClient.post(`${COMMENT_CRUD}/${boardId}`, {
    comment:   commentData.comment,
    parentId:  commentData.parentId  || null,
    nickname:  commentData.nickname  || null,
    password:  commentData.password  || null,
  });
};

// 추가: 댓글 수정
export const editComment = (commentId, reqDto) => {
  return apiClient.put(`${COMMENT_CRUD}/${commentId}`, {
    comment:  reqDto.comment,
    password: reqDto.password || null,
  });
};

// 추가: 댓글 삭제
export const deleteComment = (commentId, reqDto) => {
  return apiClient.delete(`${COMMENT_CRUD}/${commentId}`, {
    data: { password: reqDto.password || null },
  });
};
```

---

## Phase 3 — 게시글 좋아요 API 수정

### 목표
`addBoardLike` / `cancelBoardLike`가 현재 `apiClient`(JWT 인터셉터 포함)를 사용하고 있다.  
백엔드가 쿠키(HMAC) 기반으로 전환되었으므로 JWT 없이 쿠키만으로 동작해야 한다.

### 변경 파일

**`src/services/boardApi.js`**

```js
import axios from "axios";
import { BASE_URL } from "../apiConfig";

export const addBoardLike = (boardId) => {
  return axios
    .post(`${BASE_URL}/api/v2/likes/${boardId}`, {}, { withCredentials: true })
    .then((res) => res.data);
};

export const cancelBoardLike = (boardId) => {
  return axios
    .delete(`${BASE_URL}/api/v2/likes/${boardId}`, { withCredentials: true })
    .then((res) => res.data);
};

export const getBoardLikeStatus = (boardId) => {
  return axios
    .get(`${BASE_URL}/api/v2/likes/${boardId}`, { withCredentials: true })
    .then((res) => res.data);
};
```

> `apiClient` → `axios` 직접 사용. `withCredentials: true` 필수 (쿠키 전송).

---

## Phase 4 — CommentForm 재설계

### 목표
현재 로그인하지 않으면 Modal("로그인 필요")을 띄우는 구조를 제거하고,
게스트도 nickname + password를 입력해 댓글을 작성할 수 있도록 변경한다.

### 설계

| 상태 | 표시 |
|---|---|
| 관리자 로그인 (`isLoggedIn = true`) | 댓글 텍스트 입력 + 작성 버튼만 표시 |
| 비로그인 (게스트) | nickname / password / 댓글 텍스트 입력 + 작성 버튼 표시 |

### 변경 파일

**`src/components/Comments/CommentForm.js`**

- `isLoggedInModal` state 및 Modal 컴포넌트 전부 제거
- `commentData` state에 `nickname`, `password` 추가
- 비로그인 시 닉네임·비밀번호 필드 렌더링
- 제출 시 관리자: `{comment, parentId}` / 게스트: `{comment, parentId, nickname, password}` 전송

```jsx
// 핵심 렌더 로직 (예시)
{!isLoggedIn && (
  <>
    <input name="nickname" placeholder="닉네임" ... />
    <input name="password" type="password" placeholder="비밀번호 (수정·삭제에 사용)" ... />
  </>
)}
<textarea name="comment" placeholder="댓글을 입력하세요..." ... />
<button type="submit">작성</button>
```

---

## Phase 5 — Comment 컴포넌트 수정

### 목표
- 모든 사용자가 답글 작성 가능 (로그인 불필요)
- 관리자 댓글에 `[관리자]` 뱃지 표시
- 본인 댓글(게스트: localStorage 기반, 관리자: isLoggedIn 기반)에 수정/삭제 버튼 표시
- 수정/삭제 시 게스트는 비밀번호 입력 모달 표시

### 게스트 본인 댓글 식별 방법

백엔드가 댓글 작성 성공 시 `{id, writer: "닉네임#식별자"}` 형태로 응답한다.  
localStorage에 `myComments` 키로 `{ [commentId]: writer }` 형태로 저장해  
다음 방문에서도 본인 댓글을 식별한다.

```js
// 저장 (작성 성공 후)
const saved = JSON.parse(localStorage.getItem("myComments") || "{}");
saved[responseData.id] = responseData.writer;
localStorage.setItem("myComments", JSON.stringify(saved));

// 조회
const isMyComment = (commentId) => {
  const saved = JSON.parse(localStorage.getItem("myComments") || "{}");
  return commentId in saved;
};
```

### 변경 파일

**`src/components/Comments/Comment.js`**

1. **답글 버튼**: `isLoggedIn` 조건 제거 → 항상 표시
2. **답글 폼**: `CommentForm`과 동일하게 게스트 필드 추가
3. **관리자 뱃지**: `comment.isAdmin === true`이면 writer 앞에 `[관리자]` 표시
4. **수정/삭제 버튼**: `isMyComment(comment.id) || isLoggedIn` 조건으로 표시
5. **수정 흐름**:
   - 수정 버튼 클릭 → 인라인 텍스트 에디터 표시
   - 관리자: `editComment(commentId, {comment})` 직접 호출
   - 게스트: 비밀번호 입력 모달 → `editComment(commentId, {comment, password})`
6. **삭제 흐름**:
   - 관리자: `deleteComment(commentId, {})` 직접 호출
   - 게스트: 비밀번호 입력 모달 → `deleteComment(commentId, {password})`
   - 성공 후 localStorage에서 해당 댓글 항목 제거

**`src/services/queryKeys.js`**
- 댓글 목록 쿼리 키 확인 (이미 있음 — 수정 불필요)

---

## 변경 파일 요약

| 파일 | 변경 유형 | 내용 요약 |
|---|---|---|
| `src/apiConfig.js` | 수정 | `MEMBER_JOIN`, `GENERATE_USER_NUMBER` 상수 삭제 |
| `src/services/authApi.js` | 수정 | `join()` 함수 삭제 |
| `src/services/boardApi.js` | 수정 | `addComment` 게스트 필드 추가, `editComment`/`deleteComment` 추가, 좋아요 API axios 직접 사용 |
| `src/App.js` | 수정 | JoinForm import·라우트 제거 |
| `src/screens/Member/JoinForm.js` | **삭제** | 회원가입 화면 — 백엔드 엔드포인트 없음 |
| `src/components/Comments/CommentForm.js` | 수정 | 게스트 입력 폼으로 전환, Modal 제거 |
| `src/components/Comments/Comment.js` | 수정 | 답글 게스트 지원, 관리자 뱃지, 수정/삭제 기능 추가 |

---

## 구현 순서

1. Phase 1 — 회원가입 제거 (사이드 이펙트 없음, 먼저 정리)
2. Phase 2 — 댓글/좋아요 API 레이어 (컴포넌트보다 먼저)
3. Phase 3 — 좋아요 API 수정
4. Phase 4 — CommentForm 재설계
5. Phase 5 — Comment 컴포넌트 수정 (가장 복잡, 마지막)
