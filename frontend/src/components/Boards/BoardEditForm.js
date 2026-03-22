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
import { getCategories } from "../../services/categoryApi";
import { Form } from "react-bootstrap";
import EditorToolbar from "./EditorToolbar";
import "../Styles/Board/editor.css";
import "../Styles/Board/editorPage.css";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { queryKeys } from "../../services/queryKeys";

export const BoardEditForm = () => {
  const navigate    = useNavigate();
  const { boardId } = useParams();
  const queryClient = useQueryClient();

  const [board, setBoard] = useState({ title: "", category: "", deleteDate: "" });

  const editor = useEditor({
    extensions: [StarterKit, Image.configure({ inline: false })],
  });

  // 게시글 데이터 조회
  const { data: boardData } = useQuery({
    queryKey: queryKeys.admin.board(boardId),
    queryFn:  () => getBoardForAdmin(boardId),
    enabled:  !!boardId,
    staleTime:  5 * 60 * 1000,
    gcTime:    15 * 60 * 1000,
  });

  // 카테고리 목록 조회
  const { data: categories = [] } = useQuery({
    queryKey: queryKeys.categories.list(),
    queryFn:  getCategories,
    staleTime: 30 * 60 * 1000,
    gcTime:    60 * 60 * 1000,
  });

  // 게시글 데이터 → 폼 상태 동기화
  useEffect(() => {
    if (boardData) {
      setBoard({ title: boardData.title, category: boardData.category, deleteDate: boardData.deleteDate });
    }
  }, [boardData]);

  // 에디터 콘텐츠 초기화
  useEffect(() => {
    if (editor && boardData?.content) {
      editor.commands.setContent(boardData.content, { emitUpdate: false });
    }
  }, [editor, boardData?.content]);

  const handleChange = (e) => {
    const { name, value } = e.target;
    setBoard((prev) => ({ ...prev, [name]: value }));
  };

  // ── Mutations ─────────────────────────────────────────────────
  const editMutation = useMutation({
    mutationFn: ({ board: b, html }) => editBoard(boardId, b, html),
    onSuccess: (data) => {
      alert("게시글이 수정되었습니다.");
      queryClient.invalidateQueries({ queryKey: queryKeys.boards.details() });
      queryClient.invalidateQueries({ queryKey: queryKeys.admin.boardsAll() });
      navigate(`/management/boards/${data}`);
    },
  });

  const deleteMutation = useMutation({
    mutationFn: () => deleteBoard(boardId),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: queryKeys.boards.lists() });
      queryClient.invalidateQueries({ queryKey: queryKeys.admin.boardsAll() });
      navigate(-1);
    },
  });

  const undeleteMutation = useMutation({
    mutationFn: () => undeleteBoard(boardId),
    onSuccess: (res) => {
      if (res.status === 200) {
        queryClient.invalidateQueries({ queryKey: queryKeys.admin.trashAll() });
        queryClient.invalidateQueries({ queryKey: queryKeys.admin.boardsAll() });
        navigate("/management/temporary-storage");
      }
    },
  });

  const deletePermanentlyMutation = useMutation({
    mutationFn: () => deletePermanently(boardId),
    onSuccess: (res) => {
      if (res.status === 200) {
        queryClient.invalidateQueries({ queryKey: queryKeys.admin.trashAll() });
        navigate("/management/temporary-storage");
      }
    },
  });

  // ── Handlers ──────────────────────────────────────────────────
  const handleSubmit = (e) => {
    e.preventDefault();
    if (!editor) return;
    editMutation.mutate({ board, html: editor.getHTML() });
  };

  const handleDelete = (e) => {
    e.preventDefault();
    if (window.confirm("정말로 삭제하시겠습니까?")) {
      deleteMutation.mutate();
    }
  };

  const handleUndelete = (e) => {
    e.preventDefault();
    if (window.confirm("삭제를 취소하시겠습니까?")) {
      undeleteMutation.mutate();
    }
  };

  const handleDeletePermanently = (e) => {
    e.preventDefault();
    if (window.confirm("영구 삭제 시 복구할 수 없습니다.\n정말로 삭제하시겠습니까?")) {
      deletePermanentlyMutation.mutate();
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
            <button type="button" className="btn btn-secondary" onClick={() => navigate(-1)}>
              <i className="fa-solid fa-arrow-left" style={{ marginRight: 6 }} />
              뒤로
            </button>
          </div>
          <div className="editor-actions__right">
            <button type="submit" className="btn btn-primary" disabled={editMutation.isPending}>
              <i className="fa-solid fa-floppy-disk" style={{ marginRight: 6 }} />
              {editMutation.isPending ? "저장 중..." : "수정 저장"}
            </button>
            {!isDeleted ? (
              <button
                type="button"
                className="btn btn-danger"
                onClick={handleDelete}
                disabled={deleteMutation.isPending}
              >
                <i className="fa-solid fa-trash" style={{ marginRight: 6 }} />
                삭제
              </button>
            ) : (
              <>
                <button
                  type="button"
                  className="btn btn-warning"
                  onClick={handleUndelete}
                  disabled={undeleteMutation.isPending}
                >
                  <i className="fa-solid fa-rotate-left" style={{ marginRight: 6 }} />
                  삭제 취소
                </button>
                <button
                  type="button"
                  className="btn btn-danger"
                  onClick={handleDeletePermanently}
                  disabled={deletePermanentlyMutation.isPending}
                >
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
