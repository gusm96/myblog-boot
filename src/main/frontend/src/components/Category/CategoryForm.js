import React from "react";
import {
  Button,
  Form,
  InputGroup,
  Modal,
  ModalBody,
  ModalHeader,
  ModalTitle,
} from "react-bootstrap";
import { addNewCategory, getCategoriesV2 } from "../../services/categoryApi";
import { useState } from "react";
import { useEffect } from "react";

export const CategoryForm = ({ formData, onChange, accessToken }) => {
  const [newCategory, setNewCategory] = useState("");
  const [categories, setCategories] = useState([]);
  const [showModal, setShowModal] = useState(false);

  useEffect(() => {
    getCategoriesV2()
      .then((data) => setCategories(data))
      .catch((error) => console.log(error));
  }, []);

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
  const handleSelectChange = (event) => {
    onChange(event); // 선택한 카테고리를 Editor로 전달
  };

  // 모달 클로즈 핸들러
  const handleCloseModal = () => {
    setShowModal(false);
  };
  return (
    <div>
      <Form.Select
        aria-label="Default select example"
        name="category"
        value={formData.cateory}
        onChange={handleSelectChange}
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
