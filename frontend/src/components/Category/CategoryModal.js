import React from "react";
import { useState } from "react";
import { addNewCategory } from "../../services/categoryApi";
import {
  Button,
  Form,
  InputGroup,
  Modal,
  ModalBody,
  ModalHeader,
  ModalTitle,
} from "react-bootstrap";

export const CategoryModal = ({ accessToken }) => {
  const [newCategory, setNewCategory] = useState("");
  const [categories, setCategories] = useState([]);
  const [showModal, setShowModal] = useState(false);

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
  // 모달 클로즈 핸들러
  const handleCloseModal = () => {
    setShowModal(false);
  };
  return (
    <div>
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
