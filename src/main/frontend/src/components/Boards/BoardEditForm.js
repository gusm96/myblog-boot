import React from "react";
import { useEffect } from "react";
import { useState } from "react";
import { useParams } from "react-router-dom";
import "react-draft-wysiwyg/dist/react-draft-wysiwyg.css";
import {
  deleteBoard,
  editBoard,
  getBoard,
  undeleteBoard,
} from "../../services/boardApi";
import { Button, Form, InputGroup } from "react-bootstrap";
import { useSelector } from "react-redux";
import { selectAccessToken } from "../../redux/userSlice";
import { ContentState, EditorState, convertToRaw } from "draft-js";
import { Editor } from "react-draft-wysiwyg";
import draftToHtml from "draftjs-to-html";
import htmlToDraft from "html-to-draftjs";
import { getCategories } from "../../services/categoryApi";

export const BoardEditForm = () => {
  const accessToken = useSelector(selectAccessToken);
  const { boardId } = useParams();
  const [board, setBoard] = useState({
    title: "",
    category: "",
    deleteDate: "",
  });

  const [categories, setCategories] = useState([]);
  const [editorState, setEditorState] = useState(EditorState.createEmpty());
  const [htmlString, setHtmlString] = useState("");
  useEffect(() => {
    getBoard(boardId).then((data) => {
      const blocksFromHTML = htmlToDraft(data.content);
      const { contentBlocks, entityMap } = blocksFromHTML;
      const contentState = ContentState.createFromBlockArray(
        contentBlocks,
        entityMap
      );
      const newEditorState = EditorState.createWithContent(contentState);
      setBoard({
        title: data.title,
        category: data.category,
        deleteDate: data.deleteDate,
      });
      setEditorState(newEditorState);
    });
    getCategories().then((data) => setCategories(data));
  }, [boardId]);

  const handleSubmit = (e) => {
    e.preventDefault();
    editBoard(boardId, board, htmlString, accessToken)
      .then((data) => {
        alert("게시글이 수정 되었습니다");
        window.location.href = `/management/boards/${data}`;
      })
      .catch((error) => console.log(error));
  };

  const handleChange = (e) => {
    const { name, value } = e.target;
    setBoard((prevData) => ({
      ...prevData,
      [name]: value,
    }));
  };

  const updateTextDescription = (newState) => {
    setEditorState(newState);
    const html = draftToHtml(convertToRaw(editorState.getCurrentContent()));
    setHtmlString(html);
  };

  const uploadCallback = (e) => {
    e.preventDefault();
  };
  const handleDelete = (e) => {
    e.preventDefault();
    if (window.confirm("정말로 삭제하시겠습니까?")) {
      deleteBoard(boardId, accessToken)
        .then((data) => (window.location.href = "/management"))
        .catch((error) => console.log(error));
    } else {
      return;
    }
  };
  const handleUndelete = (e) => {
    e.preventDefault();
    if (window.confirm("삭제를 취소하시겠습니까?")) {
      undeleteBoard(boardId, accessToken)
        .then((data) => alert(data))
        .catch((error) => console.log(error));
    } else {
      return;
    }
  };
  return (
    <Form onSubmit={handleSubmit}>
      <InputGroup>
        <InputGroup.Text id="inputGroup-sizing-default">제목</InputGroup.Text>
        <Form.Control
          aria-label="Default"
          aria-describedby="inputGroup-sizing-default"
          placeholder="제목을 입력하세요."
          name="title"
          value={board.title}
          onChange={handleChange}
        />
        <Form.Select
          aria-label="Default select example"
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
      <Editor
        placeholder="게시글을 작성해주세요"
        name="content"
        value={board.content}
        editorState={editorState}
        onEditorStateChange={updateTextDescription}
        toolbar={{
          image: { uploadCallback: uploadCallback },
        }}
        localization={{ locale: "ko" }}
        editorStyle={{
          height: "500px",
          width: "100%",
          border: "3px solid lightgray",
          padding: "20px",
        }}
      />
      <Button type="submit">수정</Button>
      {board.deleteDate === null || "" ? (
        <Button type="button" onClick={handleDelete}>
          삭제
        </Button>
      ) : (
        <Button type="button" onClick={handleUndelete}>
          삭제 취소
        </Button>
      )}
    </Form>
  );
};
