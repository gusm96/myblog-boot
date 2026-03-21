import React, { useCallback, useState } from "react";
import { useEditor, EditorContent } from "@tiptap/react";
import StarterKit from "@tiptap/starter-kit";
import Image from "@tiptap/extension-image";
import { marked } from "marked";
import { uploadBoard, uploadImageFile } from "../../services/boardApi";
import { Button, Form, InputGroup } from "react-bootstrap";
import { CategoryForm } from "../Category/CategoryForm";
import "../Styles/Board/editor.css";

const EditorToolbar = ({ editor }) => {
  if (!editor) return null;

  const btn = (label, action, isActive = false) => (
    <Button
      key={label}
      size="sm"
      variant={isActive ? "secondary" : "outline-secondary"}
      onMouseDown={(e) => {
        e.preventDefault();
        action();
      }}
    >
      {label}
    </Button>
  );

  return (
    <div className="tiptap-toolbar">
      {btn("B", () => editor.chain().focus().toggleBold().run(), editor.isActive("bold"))}
      {btn("I", () => editor.chain().focus().toggleItalic().run(), editor.isActive("italic"))}
      {btn("U", () => editor.chain().focus().toggleUnderline().run(), editor.isActive("underline"))}
      {btn("S", () => editor.chain().focus().toggleStrike().run(), editor.isActive("strike"))}
      <span className="border-start mx-1" />
      {[1, 2, 3].map((level) =>
        btn(
          `H${level}`,
          () => editor.chain().focus().toggleHeading({ level }).run(),
          editor.isActive("heading", { level })
        )
      )}
      <span className="border-start mx-1" />
      {btn("• 목록", () => editor.chain().focus().toggleBulletList().run(), editor.isActive("bulletList"))}
      {btn("1. 목록", () => editor.chain().focus().toggleOrderedList().run(), editor.isActive("orderedList"))}
      <span className="border-start mx-1" />
      {btn("코드블록", () => editor.chain().focus().toggleCodeBlock().run(), editor.isActive("codeBlock"))}
      {btn("인용", () => editor.chain().focus().toggleBlockquote().run(), editor.isActive("blockquote"))}
      {btn("구분선", () => editor.chain().focus().setHorizontalRule().run())}
    </div>
  );
};

export const BoardEditor = () => {
  const [formData, setFormData] = useState({
    title: "",
    category: "",
    images: [],
  });

  const editor = useEditor({
    extensions: [
      StarterKit,
      Image.configure({ inline: false }),
    ],
    editorProps: {
      attributes: {
        "data-placeholder": "게시글을 작성해주세요",
      },
    },
  });

  const handleChange = (e) => {
    const { name, value } = e.target;
    setFormData((prev) => ({ ...prev, [name]: value }));
  };

  const handleImageUpload = useCallback(
    (e) => {
      const file = e.target.files[0];
      if (!file || !editor) return;
      const fd = new FormData();
      fd.append("image", file);
      uploadImageFile(fd)
        .then((data) => {
          editor.chain().focus().setImage({ src: data.filePath }).run();
          setFormData((prev) => ({ ...prev, images: [...prev.images, data] }));
          e.target.value = "";
        })
        .catch((error) => console.error("이미지 업로드 중 오류 발생:", error));
    },
    [editor]
  );

  const handleMarkdownUpload = (e) => {
    const file = e.target.files[0];
    if (!file || !editor) return;
    const reader = new FileReader();
    reader.onload = (event) => {
      const html = marked.parse(event.target.result);
      editor.commands.setContent(html, { emitUpdate: false });
      e.target.value = "";
    };
    reader.readAsText(file);
  };

  const handleSubmit = (e) => {
    e.preventDefault();
    if (!editor) return;
    const htmlString = editor.getHTML();
    uploadBoard(formData, htmlString)
      .then((res) => {
        if (res.status === 200) {
          alert("게시글을 등록하였습니다.");
          window.location.href = `/management/boards/${res.data}`;
        }
      })
      .catch((error) => console.log(error));
  };

  return (
    <Form onSubmit={handleSubmit}>
      <InputGroup className="mb-2">
        <InputGroup.Text>제목</InputGroup.Text>
        <Form.Control
          placeholder="제목을 입력하세요."
          name="title"
          value={formData.title}
          onChange={handleChange}
        />
        <CategoryForm
          formData={formData}
          onChange={handleChange}
        />
      </InputGroup>

      <div className="tiptap-wrapper mb-2">
        <EditorToolbar editor={editor} />
        <EditorContent editor={editor} />
      </div>

      <div className="d-flex gap-4 mb-3">
        <div>
          <Form.Label className="mb-1 small fw-semibold">이미지 업로드</Form.Label>
          <Form.Control
            type="file"
            accept="image/*"
            onChange={handleImageUpload}
            size="sm"
          />
        </div>
        <div>
          <Form.Label className="mb-1 small fw-semibold">Markdown 파일 업로드</Form.Label>
          <Form.Control
            type="file"
            accept=".md"
            onChange={handleMarkdownUpload}
            size="sm"
          />
        </div>
      </div>

      <Button type="submit">작성하기</Button>
    </Form>
  );
};

export default BoardEditor;
