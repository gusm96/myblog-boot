import React, { useState, useEffect } from "react";
import { Button, Form, InputGroup, Modal } from "react-bootstrap";
import { addNewCategory, getCategories } from "../../services/categoryApi";

export const CategoryForm = ({ formData, onChange, accessToken }) => {
  const [newCategory, setNewCategory] = useState("");
  const [categories, setCategories] = useState([]);
  const [showModal, setShowModal] = useState(false);

  useEffect(() => {
    getCategories()
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
    onChange(event);
  };

  const handleCloseModal = () => {
    setShowModal(false);
  };

  return (
    <div>
      <Form.Select
        aria-label="Default select example"
        name="category"
        value={formData.category} // 'cateory'를 'category'로 수정
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
      <Modal show={showModal} onHide={handleCloseModal}>
        <Modal.Header closeButton>
          <Modal.Title>새로운 카테고리</Modal.Title>
        </Modal.Header>
        <Modal.Body>
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
              />
              <Button type="button" onClick={handleNewCategory}>
                {" "}
                추가{" "}
              </Button>{" "}
              {/* type="button"으로 수정 */}
            </InputGroup>
          </Form>
        </Modal.Body>
      </Modal>
    </div>
  );
};
