# Draft.js → Tiptap 교체 계획 및 완료 보고

> 작성일: 2026-03-13 | 완료일: 2026-03-13
> 작업 범위: `src/components/Boards/BoardEditor.js`, `src/components/Boards/BoardEditForm.js`

### 변경 이력

| 날짜 | 내용 |
|------|------|
| 2026-03-13 | 계획 수립, 교체 작업 완료 |
| 2026-03-13 | Context7 MCP 재검토 반영 — Underline 중복 제거, `emitUpdate: false` 명시 |

---

## 1. 교체 배경

### Draft.js 문제점

| 문제 | 상세 |
|------|------|
| 유지보수 종료 | 2022년 이후 Facebook이 공식 유지보수를 중단. 보안 패치 없음 |
| 취약점 (수정 불가) | `immutable` < 3.8.3 High 취약점 — Draft.js 내부에 고정되어 npm fix 없음 |
| Vite 비호환 | CJS 전용 패키지 6개 → Vite 마이그레이션 시 `optimizeDeps` 우회 필요 |
| 의존성 비대 | 핵심 기능에 비해 6개 패키지 필요 |

### Tiptap 선택 이유

- **MIT 라이선스** — 블로그 기능 범위의 모든 extension 무료
- **HTML 입출력 내장** — `setContent(html)` / `getHTML()` 으로 기존 백엔드 데이터 마이그레이션 없이 호환
- **Markdown 파일 업로드** — `FileReader` + `marked` 라이브러리로 `.md` 파일을 HTML로 변환 후 에디터에 로드
- **Vite ESM 완전 호환** — CRA → Vite 마이그레이션 시 추가 설정 불필요
- **React 공식 지원** — `@tiptap/react` 패키지 제공

---

## 2. 데이터 흐름 비교

### 변경 전 (Draft.js)

```
[작성] EditorState → convertToRaw → draftjsToHtml → HTML 문자열 → 백엔드
[수정] 백엔드 HTML → htmlToDraft → ContentState → EditorState → 에디터
[열람] 백엔드 HTML → html-react-parser + DOMPurify → 렌더링 (변경 없음)
```

### 변경 후 (Tiptap)

```
[작성] editor.getHTML() → HTML 문자열 → 백엔드                              (동일 결과)
[수정] 백엔드 HTML → editor.commands.setContent(html, {emitUpdate:false})  (동일 결과)
[열람] 백엔드 HTML → html-react-parser + DOMPurify                          (변경 없음)
```

백엔드 저장 형식(HTML)이 동일하므로 **기존 게시글 데이터 마이그레이션 없음**.

---

## 3. 패키지 변경 ✅ 완료

### 제거 (6개)

```bash
npm uninstall draft-js react-draft-wysiwyg draftjs-to-html html-to-draftjs @draft-js-plugins/editor @draft-js-plugins/image
```

### 추가 (4개)

```bash
npm install @tiptap/react @tiptap/starter-kit @tiptap/extension-image marked
```

| 패키지 | 역할 |
|--------|------|
| `@tiptap/react` | React `useEditor` 훅, `EditorContent` 컴포넌트 |
| `@tiptap/starter-kit` | Bold, Italic, **Underline**, Strike, Heading, List, CodeBlock, Blockquote, History 등 기본 기능 묶음 |
| `@tiptap/extension-image` | 이미지 삽입 (StarterKit 미포함) |
| `marked` | Markdown 문자열 → HTML 변환 (`.md` 파일 업로드 시 사용) |

> **참고**: `@tiptap/extension-underline`은 초기에 설치했으나, Context7 검토를 통해 StarterKit v3에 Underline이 이미 포함되어 있음을 확인하고 제거했다.

### 취약점 변화

| 단계 | 취약점 수 |
|------|----------|
| Draft.js 제거 전 | 39개 |
| Draft.js 제거 후 | **32개** (-7개) |

---

## 4. 변경 파일 ✅ 완료

| 파일 | 변경 내용 |
|------|-----------|
| `src/components/Boards/BoardEditor.js` | Draft.js → Tiptap, 직접 구현한 툴바, 이미지/Markdown 파일 업로드 |
| `src/components/Boards/BoardEditForm.js` | htmlToDraft 제거, `editor.commands.setContent(html, {emitUpdate: false})` 로 대체 |
| `src/components/Styles/Board/editor.css` | Tiptap ProseMirror 에디터 영역 스타일 (신규) |
| `package.json` | 의존성 변경 반영 |

---

## 5. 툴바 기능 목록

| 기능 | Draft.js (react-draft-wysiwyg) | Tiptap |
|------|-------------------------------|--------|
| 굵게 | ✅ | ✅ |
| 기울임 | ✅ | ✅ |
| 밑줄 | ✅ | ✅ StarterKit 내장 |
| 취소선 | ✅ | ✅ |
| 제목 H1/H2/H3 | ✅ | ✅ |
| 글머리 기호 목록 | ✅ | ✅ |
| 번호 목록 | ✅ | ✅ |
| 코드 블록 | ✅ | ✅ |
| 인용 | ✅ | ✅ |
| 구분선 | ✅ | ✅ |
| 이미지 업로드 | ✅ 콜백 방식 | ✅ `setImage` 커맨드 |
| Markdown 파일 업로드 | ❌ | ✅ `FileReader + marked` |
| 되돌리기/다시실행 | ✅ | ✅ StarterKit History 내장 |

---

## 6. Context7 MCP 재검토 결과

작업 완료 후 Context7 MCP를 통해 Tiptap v3 공식 문서 기준으로 구현을 재검토했다.

### 발견 및 수정 사항

#### 수정 1: `Underline` extension 중복 등록 제거

**원인**: StarterKit v3의 Marks에 Underline이 포함되어 있음 (`Bold, Code, Italic, Strike, Underline` 등).

```diff
- import Underline from "@tiptap/extension-underline";

  extensions: [
    StarterKit,
-   Underline,       // StarterKit v3에 이미 포함 → 중복
    Image.configure({ inline: false }),
  ]
```

적용 파일: `BoardEditor.js`, `BoardEditForm.js` 양쪽 모두 수정.
후속 조치: `@tiptap/extension-underline` 패키지 `npm uninstall`로 제거.

---

#### 수정 2: `setContent` emitUpdate 기본값 변경 대응

**원인**: Tiptap **v3에서 `setContent`의 `emitUpdate` 기본값이 `false` → `true`로 변경**됨. 명시하지 않으면 초기 데이터 로드 시 onUpdate 핸들러가 불필요하게 트리거되거나 undo 히스토리가 오염될 수 있음.

```diff
# BoardEditForm.js — 기존 게시글 HTML 로드
- editor.commands.setContent(htmlContent);
+ editor.commands.setContent(htmlContent, { emitUpdate: false });

# BoardEditor.js — Markdown 파일 업로드
- editor.commands.setContent(html);
+ editor.commands.setContent(html, { emitUpdate: false });
```

### 정상 확인 항목

| 항목 | 결과 |
|------|------|
| `useEditor` 훅 사용법 | ✅ 올바름 |
| `EditorContent` 컴포넌트 | ✅ 올바름 |
| `editor.chain().focus().toggleBold().run()` 등 체인 커맨드 | ✅ 공식 예제와 일치 |
| `editor.isActive("bold")` 등 상태 확인 API | ✅ 올바름 |
| `Image.configure({ inline: false })` | ✅ 유효한 공식 옵션 |
| `editor.chain().focus().setImage({ src })` | ✅ 공식 예제와 일치 |
| `marked.parse()` 동기 반환 (string) | ✅ async 옵션 미사용 시 string 반환 확인 |
| `onMouseDown + e.preventDefault()` 툴바 패턴 | ✅ ProseMirror 권장 패턴 |

---

## 7. 검증 체크리스트

### 글 작성 (`BoardEditor`)

- [ ] 툴바 각 버튼 동작 (굵게, 기울임, 밑줄, 취소선)
- [ ] 제목 H1/H2/H3 적용
- [ ] 목록 (글머리 기호, 번호)
- [ ] 코드 블록 입력
- [ ] 이미지 파일 업로드 → 에디터 내 이미지 삽입
- [ ] `.md` 파일 업로드 → 에디터에 포맷 변환 표시
- [ ] 작성 완료 후 저장 → 게시글 상세 페이지에서 내용 정상 표시

### 글 수정 (`BoardEditForm`)

- [ ] 기존 HTML 게시글 로드 → 에디터에 정상 표시
- [ ] 내용 수정 후 저장 → 변경 내용 반영 확인
- [ ] 삭제 / 삭제 취소 / 영구 삭제 버튼 동작 확인

### 기존 게시글 열람 (`BoardDetailV2`)

- [ ] 변경 없이 기존 HTML 게시글 정상 렌더링 확인
