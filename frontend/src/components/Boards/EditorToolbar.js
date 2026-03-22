import React from "react";

/**
 * Shared Tiptap toolbar — icon buttons using FontAwesome
 * Used by both BoardEditor (new post) and BoardEditForm (edit post)
 */
const ToolbarBtn = ({ icon, label, action, isActive, isText }) => (
  <button
    type="button"
    title={label}
    aria-label={label}
    className={`toolbar-btn${isActive ? " toolbar-btn--active" : ""}${isText ? " toolbar-btn--text" : ""}`}
    onMouseDown={(e) => {
      e.preventDefault();
      action();
    }}
  >
    {isText ? (
      <span className="toolbar-btn__text">{icon}</span>
    ) : (
      <i className={`fa-solid ${icon}`} aria-hidden="true" />
    )}
  </button>
);

const ToolbarDivider = () => <span className="toolbar-divider" aria-hidden="true" />;

const EditorToolbar = ({ editor }) => {
  if (!editor) return null;

  return (
    <div className="tiptap-toolbar" role="toolbar" aria-label="에디터 도구">
      {/* ── 서식 ───────────────────────── */}
      <div className="toolbar-group">
        <ToolbarBtn
          icon="fa-bold" label="굵게"
          action={() => editor.chain().focus().toggleBold().run()}
          isActive={editor.isActive("bold")}
        />
        <ToolbarBtn
          icon="fa-italic" label="기울임"
          action={() => editor.chain().focus().toggleItalic().run()}
          isActive={editor.isActive("italic")}
        />
        <ToolbarBtn
          icon="fa-underline" label="밑줄"
          action={() => editor.chain().focus().toggleUnderline().run()}
          isActive={editor.isActive("underline")}
        />
        <ToolbarBtn
          icon="fa-strikethrough" label="취소선"
          action={() => editor.chain().focus().toggleStrike().run()}
          isActive={editor.isActive("strike")}
        />
      </div>

      <ToolbarDivider />

      {/* ── 제목 ───────────────────────── */}
      <div className="toolbar-group">
        {[1, 2, 3].map((level) => (
          <ToolbarBtn
            key={level}
            icon={`H${level}`} label={`제목 ${level}`}
            isText
            action={() => editor.chain().focus().toggleHeading({ level }).run()}
            isActive={editor.isActive("heading", { level })}
          />
        ))}
      </div>

      <ToolbarDivider />

      {/* ── 목록 ───────────────────────── */}
      <div className="toolbar-group">
        <ToolbarBtn
          icon="fa-list-ul" label="불릿 목록"
          action={() => editor.chain().focus().toggleBulletList().run()}
          isActive={editor.isActive("bulletList")}
        />
        <ToolbarBtn
          icon="fa-list-ol" label="번호 목록"
          action={() => editor.chain().focus().toggleOrderedList().run()}
          isActive={editor.isActive("orderedList")}
        />
      </div>

      <ToolbarDivider />

      {/* ── 블록 ───────────────────────── */}
      <div className="toolbar-group">
        <ToolbarBtn
          icon="fa-code" label="코드 블록"
          action={() => editor.chain().focus().toggleCodeBlock().run()}
          isActive={editor.isActive("codeBlock")}
        />
        <ToolbarBtn
          icon="fa-quote-left" label="인용"
          action={() => editor.chain().focus().toggleBlockquote().run()}
          isActive={editor.isActive("blockquote")}
        />
        <ToolbarBtn
          icon="fa-minus" label="구분선"
          action={() => editor.chain().focus().setHorizontalRule().run()}
        />
      </div>

      <ToolbarDivider />

      {/* ── 실행취소 / 다시실행 ──────────── */}
      <div className="toolbar-group">
        <ToolbarBtn
          icon="fa-rotate-left" label="실행취소"
          action={() => editor.chain().focus().undo().run()}
        />
        <ToolbarBtn
          icon="fa-rotate-right" label="다시실행"
          action={() => editor.chain().focus().redo().run()}
        />
      </div>
    </div>
  );
};

export default EditorToolbar;
