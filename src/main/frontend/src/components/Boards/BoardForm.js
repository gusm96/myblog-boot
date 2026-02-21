import { EditorState, convertToRaw } from "draft-js";
import React, { useEffect, useState } from "react";
import { Editor } from "react-draft-wysiwyg";
import draftjsToHtml from "draftjs-to-html";
import "react-draft-wysiwyg/dist/react-draft-wysiwyg.css";
import {
  Button,
  Form,
  InputGroup,
  Modal,
  ModalBody,
  ModalHeader,
  ModalTitle,
} from "react-bootstrap";
import { useSelector } from "react-redux";
import { selectAccessToken } from "../../redux/userSlice";
import { uploadBoard, uploadImageFile } from "../../services/boardApi";
import { addNewCategory, getCategories } from "../../services/categoryApi";

export const BoardForm = () => {
  const [showModal, setShowModal] = useState(false);
  const [newCategory, setNewCategory] = useState("");
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
    const html = draftjsToHtml(convertToRaw(state.getCurrentContent()));
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
      // formData를 생성해 에디터로 부터 File을 formData에 담는다.
      const formData = new FormData();
      formData.append("image", file);
      // ajax 비동기 통신으로 서버에 이미지 전송
      uploadImageFile(formData, accessToken)
        .then((data) => {
          // 서버로 부터 정상적으로 저장된 후 반환된 파일 데이터를 images 배열에 저장합니다.
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

  const handleNewCategory = (event) => {
    event.preventDefault();
    addNewCategory(newCategory, accessToken)
      .then((res) => {
        if (res.status === 200) {
          alert("카테고리 추가 완료");
          const newCategoryObject = {
            id: categories.length + 1,
            name: newCategory,
          };
          setCategories([...categories, newCategoryObject]);

          setShowModal(false);
          setNewCategory("");
        }
      })
      .catch((error) => {
        console.log(error);
      });
  };

  const handleCloseModal = () => {
    setShowModal(false);
  };
  return (
    <div>
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
          <button
            type="button"
            onClick={() => {
              setShowModal(true);
            }}
          >
            새로운 카테고리
          </button>
        </InputGroup>
        <Editor
          placeholder="게시글을 작성해주세요"
          name="content"
          value={formData.content}
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
        />
        <Button type="submit">작성하기</Button>
      </Form>
      <Modal
        show={showModal}
        onHide={handleCloseModal}
        style={{
          display: "flex",
          alignItems: "center",
          justifyContent: "center",
          position: "absolute",
          top: "0",
          left: "0",
        }}
      >
        <ModalHeader closeButton>
          <ModalTitle>새로운 카테고리</ModalTitle>
        </ModalHeader>
        <ModalBody>
          <Form>
            <InputGroup>
              <InputGroup.Text id="inputGroup-sizing-default">
                카테고리 이름{" "}
              </InputGroup.Text>
              <Form.Control
                type="text"
                id="newCategory"
                value={newCategory}
                onChange={(e) => setNewCategory(e.target.value)}
                required
              ></Form.Control>
              <Button type="submit" onClick={handleNewCategory}>
                추가
              </Button>
            </InputGroup>
          </Form>
        </ModalBody>
      </Modal>
    </div>
  );
};
