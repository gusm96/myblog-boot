import React, { useCallback, useState } from "react";
import { useNavigate } from "react-router";
import { useEditor, EditorContent } from "@tiptap/react";
import StarterKit from "@tiptap/starter-kit";
import Image from "@tiptap/extension-image";
import { marked } from "marked";
import { uploadBoard, uploadImageFile } from "../../services/boardApi";
import { Form } from "react-bootstrap";
import { CategoryForm } from "../Category/CategoryForm";
import EditorToolbar from "./EditorToolbar";
import "../Styles/Board/editor.css";
import "../Styles/Board/editorPage.css";

const BoardEditor = () => {
  const navigate = useNavigate();
  const [formData, setFormData] = useState({ title: "", category: "", images: [] });

  const editor = useEditor({
    extensions: [
      StarterKit,
      Image.configure({ inline: false }),
    ],
    editorProps: {
      attributes: { "data-placeholder": "게시글을 작성해주세요..." },
    },
  });

  const handleChange = (e) => {
    const { name, value } = e.target;
    setFormData((prev) => ({ ...prev, [name]: value }));
  };

  const handleImageUpload = useCallback((e) => {
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
      .catch(() => {});
  }, [editor]);

  const handleMarkdownUpload = (e) => {
    const file = e.target.files[0];
    if (!file || !editor) return;
    const reader = new FileReader();
    reader.onload = (event) => {
      editor.commands.setContent(marked.parse(event.target.result), { emitUpdate: false });
      e.target.value = "";
    };
    reader.readAsText(file);
  };

  const handleSubmit = (e) => {
    e.preventDefault();
    if (!editor) return;
    uploadBoard(formData, editor.getHTML())
      .then((res) => {
        if (res.status === 200) {
          alert("게시글을 등록하였습니다.");
          navigate(`/management/boards/${res.data}`);
        }
      })
      .catch(() => {});
  };

  return (
    <Form onSubmit={handleSubmit}>
      <div className="editor-page">

        {/* ── 헤더 ────────────────────────────────── */}
        <div className="editor-page__header">
          <span className="editor-page__title">new post</span>
        </div>

        {/* ── 메타: 제목 ──────────────────────────── */}
        <div className="editor-meta">
          <span className="editor-meta__label">title</span>
          <Form.Control
            className="editor-meta__title-input"
            placeholder="제목을 입력하세요"
            name="title"
            value={formData.title}
            onChange={handleChange}
            required
          />
        </div>

        {/* ── 메타: 카테고리 ──────────────────────── */}
        <div className="editor-category-row">
          <span className="editor-meta__label">category</span>
          <CategoryForm formData={formData} onChange={handleChange} />
        </div>

        {/* ── 에디터 ──────────────────────────────── */}
        <div className="tiptap-wrapper">
          <EditorToolbar editor={editor} />
          <EditorContent editor={editor} />
        </div>

        {/* ── 파일 업로드 ─────────────────────────── */}
        <div className="editor-uploads">
          <div className="editor-upload-item">
            <div className="editor-upload-label">
              <i className="fa-solid fa-image" />
              이미지 업로드
            </div>
            <Form.Control
              type="file"
              accept="image/*"
              onChange={handleImageUpload}
            />
          </div>
          <div className="editor-upload-item">
            <div className="editor-upload-label">
              <i className="fa-solid fa-file-lines" />
              Markdown 파일 가져오기
            </div>
            <Form.Control
              type="file"
              accept=".md"
              onChange={handleMarkdownUpload}
            />
          </div>
        </div>

        {/* ── 액션 버튼 ───────────────────────────── */}
        <div className="editor-actions">
          <div className="editor-actions__left">
            <button
              type="button"
              className="btn btn-secondary"
              onClick={() => navigate(-1)}
            >
              <i className="fa-solid fa-arrow-left" style={{ marginRight: 6 }} />
              취소
            </button>
          </div>
          <div className="editor-actions__right">
            <button type="submit" className="btn btn-primary">
              <i className="fa-solid fa-floppy-disk" style={{ marginRight: 6 }} />
              작성하기
            </button>
          </div>
        </div>

      </div>
    </Form>
  );
};

export default BoardEditor;
