# P1 버그 수정 내역

> 수정일: 2026-02-21
> 대상: 즉시 수정이 필요한 치명적 버그 7건

---

## BUG-001: BoardEditForm.js 에디터 상태 버그

**파일**: `src/main/frontend/src/components/Boards/BoardEditForm.js`
**라인**: 74

### 원인
`updateTextDescription(newState)` 함수에서 `setEditorState(newState)`로 상태를 갱신한 직후,
HTML 변환 시 갱신 **이전의** `editorState`를 사용하고 있었음.
React의 `useState`는 상태 갱신이 비동기적으로 반영되므로, 갱신 직후 `editorState`를 읽으면
항상 **이전 값**을 반환함 → 게시글 수정 시 마지막 입력 내용이 저장되지 않는 버그.

### 수정 내용

```diff
  const updateTextDescription = (newState) => {
    setEditorState(newState);
-   const html = draftToHtml(convertToRaw(editorState.getCurrentContent()));
+   const html = draftToHtml(convertToRaw(newState.getCurrentContent()));
    setHtmlString(html);
  };
```

---

## BUG-002: BoardForm.js 에디터 상태 버그

**파일**: `src/main/frontend/src/components/Boards/BoardForm.js`
**라인**: 49

### 원인
BUG-001과 동일한 패턴. 함수 파라미터 `state`를 받아 상태를 갱신하지만,
HTML 변환 시 갱신 전의 `editorState`를 사용함 → 게시글 작성 시 마지막 입력 내용이 누락.

### 수정 내용

```diff
  const updateTextDescription = (state) => {
    setEditorState(state);
-   const html = draftjsToHtml(convertToRaw(editorState.getCurrentContent()));
+   const html = draftjsToHtml(convertToRaw(state.getCurrentContent()));
    setHtmlString(html);
  };
```

---

## BUG-003: CategoryModal.js props 구조 분해 오류

**파일**: `src/main/frontend/src/components/Category/CategoryModal.js`
**라인**: 14

### 원인
React 컴포넌트 함수의 파라미터는 항상 단일 `props` 객체임.
`(accessToken)` 으로 받으면 `accessToken` 변수에 `{ accessToken: "..." }` 형태의 **객체 전체**가 담기고,
이를 그대로 `addNewCategory(newCategory, accessToken)` 에 전달하면 문자열이 아닌 객체가 넘어가
Authorization 헤더가 올바르게 구성되지 않음 → 카테고리 추가 기능 401 에러 또는 오동작.

### 수정 내용

```diff
- export const CategoryModal = (accessToken) => {
+ export const CategoryModal = ({ accessToken }) => {
```

---

## BUG-004: categoryApi.js 함수명 오타 및 axios 구문 오류

**파일**: `src/main/frontend/src/services/categoryApi.js`
**연관 파일**: `src/main/frontend/src/components/Category/CategoryList.js`

### 원인 1 - 함수명 오타
`getCategoiresForAdmin` → `getCategoriesForAdmin`
오타로 인해 코드 검색 및 유지보수 시 혼란 유발.

### 원인 2 - axios.post 4번째 인자 문제
`axios.post(url, data, config)` 는 인자를 3개까지만 받음.
`withCredentials: true` 를 4번째 인자로 별도 객체로 전달하면 **완전히 무시됨** → 인증 쿠키가 전송되지 않을 수 있음.

### 수정 내용

**categoryApi.js**
```diff
- export const getCategoiresForAdmin = (accessToken) => {
+ export const getCategoriesForAdmin = (accessToken) => {

  export const addNewCategory = (categoryName, accessToken) => {
    return axios.post(
      `${CATEGORY_CRUD}`,
      { categoryName: categoryName },
      {
        headers: { Authorization: getToken(accessToken) },
+       withCredentials: true,
      },
-     { withCredentials: true }   // ← 4번째 인자: axios에서 무시됨
    );
  };
```

**CategoryList.js** (import 참조 수정)
```diff
  import {
    addNewCategory,
    deleteCategory,
-   getCategoiresForAdmin,
+   getCategoriesForAdmin,
  } from "../../services/categoryApi";

  useEffect(() => {
-   getCategoiresForAdmin(accessToken).then(...)
+   getCategoriesForAdmin(accessToken).then(...)
  }, [accessToken]);
```

---

## BUG-005: BoardDetailResDto.java 필드명 오타

**파일**: `src/main/java/com/moya/myblogboot/dto/board/BoardDetailResDto.java`
**라인**: 19, 32

### 원인
`creatDate` 오타로 인해 Jackson이 API 응답 JSON에 `creatDate` 필드로 직렬화.
프론트엔드(`BoardDetailV2.js:34`)는 `board.data.createDate`로 접근하므로 항상 `undefined` 반환 → 작성일자가 표시되지 않음.

### 수정 내용

```diff
  // 필드 선언
- private LocalDateTime creatDate;
+ private LocalDateTime createDate;

  // 빌더 생성자 내부
- this.creatDate = boardForRedis.getCreateDate();
+ this.createDate = boardForRedis.getCreateDate();
```

> Lombok `@Getter` 가 생성하는 메서드명도 `getCreatDate()` → `getCreateDate()` 로 변경됨.
> 프론트엔드의 `board.data.createDate` 참조가 이제 정상적으로 값을 받게 됨.

---

## BUG-006: BoardForm.js toolbar props 케이스 오류

**파일**: `src/main/frontend/src/components/Boards/BoardForm.js`
**라인**: 153~162

### 원인
`react-draft-wysiwyg`의 `toolbar.image` 옵션은 camelCase props를 요구하나,
소문자로 잘못 작성되어 이미지 업로드 기능이 정상 동작하지 않음.

### 수정 내용

```diff
  toolbar={{
    image: {
-     uploadenabled: true,
+     uploadEnabled: true,
      uploadCallback: uploadImageCallBack,
-     previewimage: true,
+     previewImage: true,
-     inputaccept: "image/gif,image/jpeg,image/jpg,image/png,image/svg",
+     inputAccept: "image/gif,image/jpeg,image/jpg,image/png,image/svg",
      alt: { present: false, mandatory: false },
-     defaultsize: { height: "auto", width: "auto" },
+     defaultSize: { height: "auto", width: "auto" },
    },
  }}
```

---

## BUG-007: HTML 파싱 XSS 취약점 (DOMPurify 적용)

**파일**: `src/main/frontend/src/components/Boards/BoardDetailV2.js`
**라인**: 27

### 원인
DB에서 조회한 HTML 콘텐츠를 `html-react-parser`로 바로 파싱하여 렌더링.
게시글 내용에 악의적인 `<script>` 태그나 이벤트 핸들러가 포함된 경우 그대로 실행될 수 있음 → XSS(Cross-Site Scripting) 취약점.

### 조치 사항

**1. DOMPurify 패키지 설치**
```bash
npm install dompurify
```

**2. 코드 수정**
```diff
  import Parser from "html-react-parser";
+ import DOMPurify from "dompurify";

- <div>{Parser(board.data.content)}</div>
+ <div>{Parser(DOMPurify.sanitize(board.data.content))}</div>
```

### DOMPurify 동작 방식
- `DOMPurify.sanitize(html)` : 위험한 태그(`<script>`, `<iframe>` 등) 및 이벤트 핸들러(`onclick`, `onerror` 등)를 제거
- 안전한 HTML 태그(`<p>`, `<strong>`, `<img>` 등)는 그대로 유지
- `html-react-parser`와 함께 사용하면 렌더링 단계 전에 HTML을 정화하여 XSS 차단

---

## 수정 파일 목록 요약

| 파일 | 수정 내용 |
|------|---------|
| `src/components/Boards/BoardEditForm.js` | BUG-001: `newState.getCurrentContent()` 로 수정 |
| `src/components/Boards/BoardForm.js` | BUG-002: `state.getCurrentContent()` 로 수정<br>BUG-006: toolbar props camelCase 수정 |
| `src/components/Category/CategoryModal.js` | BUG-003: `({ accessToken })` 구조 분해 수정 |
| `src/services/categoryApi.js` | BUG-004: 함수명 오타 수정, axios config 병합 |
| `src/components/Category/CategoryList.js` | BUG-004: import 함수명 동기화 |
| `src/main/java/.../BoardDetailResDto.java` | BUG-005: `creatDate` → `createDate` |
| `src/components/Boards/BoardDetailV2.js` | BUG-007: DOMPurify.sanitize() 적용 |
| `package.json` | BUG-007: dompurify 패키지 추가 |
