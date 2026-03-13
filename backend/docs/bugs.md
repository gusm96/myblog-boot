# 버그 목록 - P1 (즉시 수정 필요)

> 이 문서는 코드 실행 시 명확하게 오동작하는 버그들을 정리합니다.

---

## BUG-001: BoardEditForm.js 에디터 상태 버그

**파일**: `src/main/frontend/src/components/Boards/BoardEditForm.js`
**심각도**: 높음 (게시글 수정 기능 오동작)

### 문제
`updateTextDescription` 함수에서 `newState`를 받아 `editorState`를 갱신하지만,
HTML 변환 시 갱신 전의 `editorState`를 사용하고 있어 항상 이전 상태의 내용이 저장됨.

```javascript
// 현재 (잘못된 코드)
const updateTextDescription = (newState) => {
  setEditorState(newState);
  const html = draftToHtml(convertToRaw(editorState.getCurrentContent())); // ← 이전 상태
  setDescription(html);
};

// 수정 후
const updateTextDescription = (newState) => {
  setEditorState(newState);
  const html = draftToHtml(convertToRaw(newState.getCurrentContent())); // ← newState 사용
  setDescription(html);
};
```

---

## BUG-002: BoardForm.js 에디터 상태 버그

**파일**: `src/main/frontend/src/components/Boards/BoardForm.js`
**심각도**: 높음 (게시글 작성 기능 오동작)

### 문제
`BUG-001`과 동일한 패턴. 에디터 상태 갱신 후 변환 시 이전 상태를 사용.

```javascript
// 현재 (잘못된 코드)
const updateTextDescription = (state) => {
  setEditorState(state);
  const html = draftjsToHtml(convertToRaw(editorState.getCurrentContent())); // ← 이전 상태
  setContent(html);
};

// 수정 후
const updateTextDescription = (state) => {
  setEditorState(state);
  const html = draftjsToHtml(convertToRaw(state.getCurrentContent())); // ← state 사용
  setContent(html);
};
```

---

## BUG-003: CategoryModal.js props 구조 분해 오류

**파일**: `src/main/frontend/src/components/Category/CategoryModal.js`
**심각도**: 높음 (카테고리 추가 기능 동작 안 함)

### 문제
함수 파라미터에서 객체 구조 분해를 하지 않아 `accessToken`이 객체로 전달됨.

```javascript
// 현재 (잘못된 코드)
export const CategoryModal = (accessToken) => {
  // accessToken은 { accessToken: "..." } 형태의 객체
  addNewCategory(newCategory, accessToken); // 문자열이 아닌 객체 전달
};

// 수정 후
export const CategoryModal = ({ accessToken }) => {
  addNewCategory(newCategory, accessToken); // 정상 동작
};
```

---

## BUG-004: categoryApi.js 함수명 오타

**파일**: `src/main/frontend/src/services/categoryApi.js`
**심각도**: 낮음 (기능 동작은 하나 가독성/유지보수 문제)

### 문제
함수명에 오타 포함: `getCategoiresForAdmin` → `getCategoriesForAdmin`

```javascript
// 현재
export const getCategoiresForAdmin = async (accessToken) => { ... };

// 수정 후
export const getCategoriesForAdmin = async (accessToken) => { ... };
```

> 함수명 변경 시 이 함수를 import 하는 모든 파일도 함께 수정 필요.

---

## BUG-005: BoardDetailResDto.java 필드명 오타

**파일**: `src/main/java/com/moya/myblogboot/dto/board/BoardDetailResDto.java`
**심각도**: 낮음 (API 응답 필드명 불일치 가능성)

### 문제
`creatDate` → `createDate` 수정 필요.

```java
// 현재
private LocalDateTime creatDate;

// 수정 후
private LocalDateTime createDate;
```

> 프론트엔드에서 `creatDate`로 접근 중이라면 함께 수정 필요.
> API 응답 필드명이 바뀌므로 프론트엔드 코드 확인 후 적용.

---

## BUG-006: BoardForm.js props 케이스 오류

**파일**: `src/main/frontend/src/components/Boards/BoardForm.js`
**심각도**: 중간 (이미지 업로드 기능 오동작 가능)

### 문제
React Draft WYSIWYG 컴포넌트에 전달하는 props가 소문자로 작성됨.

```javascript
// 현재 (잘못된 코드)
toolbar={{
  image: {
    uploadenabled: true,           // ← uploadEnabled 여야 함
    previewimage: true,            // ← previewImage 여야 함
    inputaccept: 'image/*',        // ← inputAccept 여야 함
    defaultsize: { ... },          // ← defaultSize 여야 함
  }
}}

// 수정 후
toolbar={{
  image: {
    uploadEnabled: true,
    previewImage: true,
    inputAccept: 'image/*',
    defaultSize: { ... },
  }
}}
```

---

## BUG-007: HTML 파싱 XSS 취약점

**파일**: `src/main/frontend/src/components/Boards/BoardDetailV2.js`
**심각도**: 높음 (보안 취약점)

### 문제
DB에서 조회한 HTML 문자열을 `html-react-parser`로 바로 파싱하여 렌더링.
악의적인 스크립트가 삽입된 콘텐츠가 그대로 실행될 수 있음.

```javascript
// 현재 (취약한 코드)
{Parser(board.data.content)}

// 수정 후 (DOMPurify 적용)
import DOMPurify from 'dompurify';
{Parser(DOMPurify.sanitize(board.data.content))}
```

### 조치
```bash
npm install dompurify
npm install --save-dev @types/dompurify
```

---

## 수정 완료 체크리스트

- [x] BUG-001: BoardEditForm.js 에디터 상태 버그
- [x] BUG-002: BoardForm.js 에디터 상태 버그
- [x] BUG-003: CategoryModal.js props 구조 분해 오류
- [x] BUG-004: categoryApi.js 함수명 오타 + axios 구문 오류
- [x] BUG-005: BoardDetailResDto.java 필드명 오타
- [x] BUG-006: BoardForm.js props 케이스 오류
- [x] BUG-007: HTML 파싱 XSS 취약점

> 상세 수정 내역: [fix-p1-bugs.md](./fix-p1-bugs.md)
