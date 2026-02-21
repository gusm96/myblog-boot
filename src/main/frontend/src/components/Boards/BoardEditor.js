import { ContentState, EditorState, convertToRaw } from "draft-js";
import React, { useState } from "react";
import { useSelector } from "react-redux";
import SyntaxHighlighter from "react-syntax-highlighter";
import { solarizedlight } from "react-syntax-highlighter/dist/esm/styles/prism";
import { selectAccessToken } from "../../redux/userSlice";
import { uploadBoard, uploadImageFile } from "../../services/boardApi";
import draftjsToHtml from "draftjs-to-html";
import "react-draft-wysiwyg/dist/react-draft-wysiwyg.css";
import { Button, Form, InputGroup } from "react-bootstrap";
import { CategoryForm } from "../Category/CategoryForm";
import { Editor } from "react-draft-wysiwyg"; // WYSIWYG 에디터 컴포넌트 가져오기

export const BoardEditor = () => {
  const [editorState, setEditorState] = useState(
    EditorState.createWithContent(ContentState.createFromText(""))
  );
  const [selectedLanguage, setSelectedLanguage] = useState("java");
  const [formData, setFormData] = useState({
    title: "",
    category: "",
    content: "",
    images: [],
  });
  const [htmlString, setHtmlString] = useState("");
  const accessToken = useSelector(selectAccessToken);

  const handleChange = (e) => {
    const { name, value } = e.target;
    setFormData((prevData) => ({
      ...prevData,
      [name]: value,
    }));
  };

  const uploadImageCallBack = (file) => {
    return new Promise((resolve, reject) => {
      const formData = new FormData();
      formData.append("image", file);
      uploadImageFile(formData, accessToken)
        .then((data) => {
          setFormData((prevState) => ({
            ...prevState,
            images: [...prevState.images, data],
          }));
          const imagePath = data.filePath;
          resolve({ data: { link: imagePath } });
        })
        .catch((error) => {
          console.error("이미지 업로드 중 오류 발생:", error);
          reject("이미지 업로드 중 오류 발생");
        });
    });
  };

  const updateTextDescription = (state) => {
    setEditorState(state);
    const html = draftjsToHtml(convertToRaw(state.getCurrentContent())); // 현재 상태의 editorState 사용
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

  const handleLanguageChange = (event) => {
    setSelectedLanguage(event.target.value);
  };

  const blockStyleFn = (contentBlock) => {
    const type = contentBlock.getType();
    if (type === "code-block") {
      return "code-block-style";
    }
    return null;
  };

  const getCodeBlock = (content) => {
    const code = content.getPlainText();
    return (
      <SyntaxHighlighter language={selectedLanguage} style={solarizedlight}>
        {code}
      </SyntaxHighlighter>
    );
  };

  return (
    <div className="code-block-editor">
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
          <CategoryForm
            formData={formData}
            onChange={handleChange}
            accessToken={accessToken}
          />
        </InputGroup>

        <select value={selectedLanguage} onChange={handleLanguageChange}>
          <option value="javascript">JavaScript</option>
          <option value="java">Java</option>
          <option value="python">Python</option>
        </select>

        <Editor
          placeholder="게시글을 작성해주세요"
          editorState={editorState}
          onEditorStateChange={updateTextDescription}
          toolbar={{
            image: {
              uploadEnabled: true,
              uploadCallback: uploadImageCallBack,
              previewImage: true,
              inputAccept: "image/gif,image/jpeg,image/jpg,image/png,image/svg",
              alt: { present: false, mandatory: false },
              defaultSize: {
                height: "auto",
                width: "auto",
              },
            },
          }}
          localization={{ locale: "ko" }}
          editorStyle={{
            height: "500px",
            width: "100%",
            border: "3px solid lightgray",
            padding: "20px",
          }}
          blockStyleFn={blockStyleFn}
        />

        {editorState
          .getCurrentContent()
          .getBlockMap()
          .map((block) => {
            if (block.getType() === "code-block") {
              return getCodeBlock(block);
            }
            return null;
          })}

        <Button type="submit">작성하기</Button>
      </Form>
    </div>
  );
};

export default BoardEditor;
