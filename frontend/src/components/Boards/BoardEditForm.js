import React, { useEffect, useState } from "react";
import { useNavigate, useParams } from "react-router";
import { useEditor, EditorContent } from "@tiptap/react";
import StarterKit from "@tiptap/starter-kit";
import Image from "@tiptap/extension-image";
import {
  deleteBoard,
  deletePermanently,
  editBoard,
  getBoardForAdmin,
  undeleteBoard,
} from "../../services/boardApi";
import { Form } from "react-bootstrap";
import { getCategories } from "../../services/categoryApi";
import EditorToolbar from "./EditorToolbar";
import "../Styles/Board/editor.css";
import "../Styles/Board/editorPage.css";

export const BoardEditForm = () => {
  const navigate = useNavigate();
  const { boardId } = useParams();
  const [board, setBoard] = useState({ title: "", category: "", deleteDate: "" });
  const [categories, setCategories] = useState([]);
  const [htmlContent, setHtmlContent] = useState("");

  const editor = useEditor({
    extensions: [
      StarterKit,
      Image.configure({ inline: false }),
    ],
  });

  useEffect(() => {
    getBoardForAdmin(boardId).then((data) => {
      setBoard({ title: data.title, category: data.category, deleteDate: data.deleteDate });
      setHtmlContent(data.content);
    });
    getCategories().then((data) => setCategories(data));
  }, [boardId]);

  useEffect(() => {
    if (editor && htmlContent) {
      editor.commands.setContent(htmlContent, { emitUpdate: false });
    }
  }, [editor, htmlContent]);

  const handleChange = (e) => {
    const { name, value } = e.target;
    setBoard((prev) => ({ ...prev, [name]: value }));
  };

  const handleSubmit = (e) => {
    e.preventDefault();
    if (!editor) return;
    editBoard(boardId, board, editor.getHTML())
      .then((data) => {
        alert("게시글이 수정되었습니다.");
        navigate(`/management/boards/${data}`);
      })
      .catch(() => {});
  };

  const handleDelete = (e) => {
    e.preventDefault();
    if (window.confirm("정말로 삭제하시겠습니까?")) {
      deleteBoard(boardId).then(() => navigate(-1)).catch(() => {});
    }
  };

  const handleUndelete = (e) => {
    e.preventDefault();
    if (window.confirm("삭제를 취소하시겠습니까?")) {
      undeleteBoard(boardId)
        .then((res) => { if (res.status === 200) navigate("/management/temporary-storage"); })
        .catch(() => {});
    }
  };

  const handleDeletePermanently = (e) => {
    e.preventDefault();
    if (window.confirm("영구 삭제 시 복구할 수 없습니다.\n정말로 삭제하시겠습니까?")) {
      deletePermanently(boardId)
        .then((res) => { if (res.status === 200) navigate("/management/temporary-storage"); })
        .catch(() => {});
    }
  };

  const isDeleted = board.deleteDate && board.deleteDate !== "";

  return (
    <Form onSubmit={handleSubmit}>
      <div className="editor-page">

        {/* ── 헤더 ────────────────────────────────── */}
        <div className="editor-page__header">
          <span className="editor-page__title">edit post</span>
          <span className={`editor-status-badge ${isDeleted ? "editor-status-badge--deleted" : "editor-status-badge--live"}`}>
            <span className="editor-status-badge__dot" />
            {isDeleted ? "삭제됨" : "게시 중"}
          </span>
        </div>

        {/* ── 메타: 제목 ──────────────────────────── */}
        <div className="editor-meta">
          <span className="editor-meta__label">title</span>
          <Form.Control
            className="editor-meta__title-input"
            placeholder="제목을 입력하세요"
            name="title"
            value={board.title}
            onChange={handleChange}
            required
          />
        </div>

        {/* ── 메타: 카테고리 ──────────────────────── */}
        <div className="editor-category-row">
          <span className="editor-meta__label">category</span>
          <Form.Select
            className="editor-meta__category-select editor-meta__category-select--standalone"
            name="category"
            value={board.category}
            onChange={handleChange}
          >
            <option value="">카테고리 선택</option>
            {categories.map((c) => (
              <option key={c.id} value={c.id}>{c.name}</option>
            ))}
          </Form.Select>
        </div>

        {/* ── 에디터 ──────────────────────────────── */}
        <div className="tiptap-wrapper">
          <EditorToolbar editor={editor} />
          <EditorContent editor={editor} />
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
              뒤로
            </button>
          </div>
          <div className="editor-actions__right">
            <button type="submit" className="btn btn-primary">
              <i className="fa-solid fa-floppy-disk" style={{ marginRight: 6 }} />
              수정 저장
            </button>
            {!isDeleted ? (
              <button type="button" className="btn btn-danger" onClick={handleDelete}>
                <i className="fa-solid fa-trash" style={{ marginRight: 6 }} />
                삭제
              </button>
            ) : (
              <>
                <button type="button" className="btn btn-warning" onClick={handleUndelete}>
                  <i className="fa-solid fa-rotate-left" style={{ marginRight: 6 }} />
                  삭제 취소
                </button>
                <button type="button" className="btn btn-danger" onClick={handleDeletePermanently}>
                  <i className="fa-solid fa-bomb" style={{ marginRight: 6 }} />
                  영구삭제
                </button>
              </>
            )}
          </div>
        </div>

      </div>
    </Form>
  );
};
