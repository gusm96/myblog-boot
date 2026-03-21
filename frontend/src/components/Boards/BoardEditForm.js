import React, { useEffect, useState } from "react";
import { useParams } from "react-router";
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
import { Button, Form, InputGroup } from "react-bootstrap";
import { getCategories } from "../../services/categoryApi";
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

export const BoardEditForm = () => {
  const { boardId } = useParams();
  const [board, setBoard] = useState({
    title: "",
    category: "",
    deleteDate: "",
  });
  const [categories, setCategories] = useState([]);
  const [htmlContent, setHtmlContent] = useState("");

  const editor = useEditor({
    extensions: [
      StarterKit,
      Image.configure({ inline: false }),
    ],
  });

  // 데이터 패치 (API 1회 호출)
  useEffect(() => {
    getBoardForAdmin(boardId).then((data) => {
      setBoard({
        title: data.title,
        category: data.category,
        deleteDate: data.deleteDate,
      });
      setHtmlContent(data.content);
    });
    getCategories().then((data) => setCategories(data));
  }, [boardId]);

  // editor와 htmlContent가 모두 준비됐을 때 내용 로드 (emitUpdate: false — undo 히스토리 오염 방지)
  useEffect(() => {
    if (editor && htmlContent) {
      editor.commands.setContent(htmlContent, { emitUpdate: false });
    }
  }, [editor, htmlContent]);

  const handleSubmit = (e) => {
    e.preventDefault();
    if (!editor) return;
    const htmlString = editor.getHTML();
    editBoard(boardId, board, htmlString)
      .then((data) => {
        alert("게시글이 수정 되었습니다");
        window.location.href = `/management/boards/${data}`;
      })
      .catch((error) => {
        console.log(error);
      });
  };

  const handleChange = (e) => {
    const { name, value } = e.target;
    setBoard((prev) => ({ ...prev, [name]: value }));
  };

  const handleDelete = (e) => {
    e.preventDefault();
    if (window.confirm("정말로 삭제하시겠습니까?")) {
      deleteBoard(boardId)
        .then(() => window.history.go(-1))
        .catch((error) => console.log(error));
    }
  };

  const handleUndelete = (e) => {
    e.preventDefault();
    if (window.confirm("삭제를 취소하시겠습니까?")) {
      undeleteBoard(boardId)
        .then((res) => {
          if (res.status === 200) {
            window.location.href = "/management/temporary-storage";
          }
        })
        .catch((error) => console.log(error));
    }
  };

  const handleDeletePermanently = (e) => {
    e.preventDefault();
    if (
      window.confirm(
        "영구 삭제시 게시글을 복구할 수 없습니다.\n정말로 삭제하시겠습니까?"
      )
    ) {
      deletePermanently(boardId)
        .then((res) => {
          if (res.status === 200) {
            window.location.href = "/management/temporary-storage";
          }
        })
        .catch((error) => console.log(error));
    }
  };

  return (
    <Form onSubmit={handleSubmit}>
      <InputGroup className="mb-2">
        <InputGroup.Text>제목</InputGroup.Text>
        <Form.Control
          placeholder="제목을 입력하세요."
          name="title"
          value={board.title}
          onChange={handleChange}
        />
        <Form.Select
          name="category"
          value={board.category}
          onChange={handleChange}
        >
          <option>카테고리</option>
          {categories.map((c) => (
            <option key={c.id} value={c.id}>
              {c.name}
            </option>
          ))}
        </Form.Select>
      </InputGroup>

      <div className="tiptap-wrapper mb-2">
        <EditorToolbar editor={editor} />
        <EditorContent editor={editor} />
      </div>

      <div className="d-flex gap-2">
        <Button type="submit">수정</Button>
        {board.deleteDate === null || board.deleteDate === "" ? (
          <Button type="button" variant="danger" onClick={handleDelete}>
            삭제
          </Button>
        ) : (
          <>
            <Button type="button" variant="warning" onClick={handleUndelete}>
              삭제 취소
            </Button>
            <Button
              type="button"
              variant="danger"
              onClick={handleDeletePermanently}
            >
              영구삭제
            </Button>
          </>
        )}
      </div>
    </Form>
  );
};
