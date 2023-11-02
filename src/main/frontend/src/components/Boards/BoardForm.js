import { EditorState, convertToRaw } from "draft-js";
import React, { useEffect, useState } from "react";
import { Editor } from "react-draft-wysiwyg";
import draftjsToHtml from "draftjs-to-html";
import "react-draft-wysiwyg/dist/react-draft-wysiwyg.css";
import { Button, Form, InputGroup } from "react-bootstrap";
import axios from "axios";
import { BOARD_CUD, CATEGORY_LIST } from "../../apiConfig";
import { useSelector } from "react-redux";
import { selectAccessToken } from "../../redux/userSlice";
export const BoardForm = () => {
  const [categories, setCategories] = useState([]);
  const [editorState, setEditorState] = useState(EditorState.createEmpty());
  const [htmlString, setHtmlString] = useState("");
  const accessToken = useSelector(selectAccessToken);
  const [formData, setFormData] = useState({
    title: "",
    category: "",
    content: "",
  });

  useEffect(() => {
    axios
      .get(`${CATEGORY_LIST}`)
      .then((res) => res.data)
      .then((data) => setCategories(data))
      .catch((error) => console.log(error));
  }, []);

  const updateTextDescription = async (state) => {
    await setEditorState(state);
    const html = draftjsToHtml(convertToRaw(editorState.getCurrentContent()));
    setHtmlString(html);
  };
  const handleSubmit = async (e) => {
    e.preventDefault();
    await axios.post(`${BOARD_CUD}`, {
      headers: {
        Authonrization: `bearer ${accessToken}`,
      },
      body: {},
    });
  };
  const uploadCallback = () => {
    // 추가 예정
  };
  return (
    <Form onSubmit={handleSubmit}>
      <InputGroup>
        <InputGroup.Text id="inputGroup-sizing-default">제목</InputGroup.Text>
        <Form.Control
          aria-label="Default"
          aria-describedby="inputGroup-sizing-default"
          placeholder="제목을 입력하세요."
        />
        <Form.Select aria-label="Default select example">
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
        editorState={editorState}
        onEditorStateChange={updateTextDescription}
        toolbar={{
          image: { uploadCallback: uploadCallback },
        }}
        localization={{ locale: "ko" }}
        editorStyle={{
          height: "400px",
          width: "100%",
          border: "3px solid lightgray",
          padding: "20px",
        }}
      />
      <Button type="submit">작성하기.</Button>
    </Form>
  );
};
