import { EditorState, convertToRaw } from "draft-js";
import React, { useEffect, useState } from "react";
import { Editor } from "react-draft-wysiwyg";
import draftjsToHtml from "draftjs-to-html";
import "react-draft-wysiwyg/dist/react-draft-wysiwyg.css";
import { Button, Form, InputGroup } from "react-bootstrap";
import { useSelector } from "react-redux";
import { selectAccessToken } from "../../redux/userSlice";
import {
  getCategories,
  uploadBoard,
  uploadImageFile,
} from "../../services/boardApi";
export const BoardForm = () => {
  const [categories, setCategories] = useState([]);
  const [editorState, setEditorState] = useState(EditorState.createEmpty());
  const [htmlString, setHtmlString] = useState("");
  const accessToken = useSelector(selectAccessToken);
  const [formData, setFormData] = useState({
    title: "",
    category: "",
    content: "",
    images: [],
  });
  useEffect(() => {
    getCategories()
      .then((data) => setCategories(data))
      .catch((error) => console.log(error));
  }, []);
  const handleChange = (e) => {
    const { name, value } = e.target;
    setFormData((prevData) => ({
      ...prevData,
      [name]: value,
    }));
  };
  const updateTextDescription = (state) => {
    setEditorState(state);
    const html = draftjsToHtml(convertToRaw(editorState.getCurrentContent()));
    setHtmlString(html);
  };
  const handleSubmit = async (e) => {
    e.preventDefault();
    uploadBoard(formData, htmlString, accessToken)
      .then((res) => {
        if (res.status === 200) {
          alert("게시글을 등록하였습니다.");
          window.location.href = `/management/boards/${res.data}`;
        }
      })
      .catch((error) => console.log(error));
  };
  const uploadImageCallBack = (file) => {
    return new Promise((resolve, reject) => {
      const formData = new FormData();
      formData.append("image", file);

      // 'YOUR_IMAGE_UPLOAD_ENDPOINT'를 이미지 업로드를 처리하는 실제 서버 엔드포인트로 교체합니다.
      uploadImageFile(formData, accessToken)
        .then((data) => {
          setFormData((prevState) => ({
            ...prevState,
            images: [...prevState.images, data],
          }));
          // 서버가 이미지 파일 경로로 응답하는 것으로 가정합니다.
          const imagePath = data.filePath;
          resolve({ data: { link: imagePath } });
        })
        .catch((error) => {
          console.error("이미지 업로드 중 오류 발생:", error);
          reject("이미지 업로드 중 오류 발생");
        });
    });
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
          value={formData.title}
          onChange={handleChange}
        />
        <Form.Select
          aria-label="Default select example"
          name="category"
          value={formData.category}
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
        value={formData.content}
        editorState={editorState}
        onEditorStateChange={updateTextDescription}
        toolbar={{
          image: {
            uploadenabled: true,
            uploadCallback: uploadImageCallBack,
            previewimage: true,
            inputaccept: "image/gif,image/jpeg,image/jpg,image/png,image/svg",
            alt: { present: false, mandatory: false },
            defaultsize: {
              height: "auto",
              width: "auto",
            },
          },
        }}
        localization={{ locale: "ko" }}
        editorStyle={{
          height: "400px",
          width: "100%",
          border: "3px solid lightgray",
          padding: "20px",
        }}
      />
      <Button type="submit">작성하기</Button>
    </Form>
  );
};
