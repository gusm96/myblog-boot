import React, { useState, useEffect } from "react";
import { Button, Form, InputGroup, Modal } from "react-bootstrap";
import { addNewCategory, getCategories } from "../../services/categoryApi";

export const CategoryForm = ({ formData, onChange }) => {
  const [newCategory, setNewCategory] = useState("");
  const [categories, setCategories] = useState([]);
  const [showModal, setShowModal] = useState(false);

  useEffect(() => {
    getCategories()
      .then((data) => setCategories(data))
      .catch(() => {});
  }, []);

  const handleNewCategory = (e) => {
    e.preventDefault();
    addNewCategory(newCategory)
      .then((res) => {
        if (res.status === 200) {
          alert("카테고리 추가 완료");
          setCategories((prev) => [...prev, { id: prev.length + 1, name: newCategory }]);
          setShowModal(false);
          setNewCategory("");
        }
      })
      .catch(() => {});
  };

  return (
    <div>
      <Form.Select
        name="category"
        value={formData.category}
        onChange={onChange}
      >
        <option value="">카테고리 선택</option>
        {categories.map((c) => (
          <option key={c.id} value={c.id}>{c.name}</option>
        ))}
      </Form.Select>

      <button type="button" onClick={() => setShowModal(true)}>
        + 카테고리 추가
      </button>

      <Modal show={showModal} onHide={() => setShowModal(false)} centered>
        <Modal.Header closeButton>
          <Modal.Title>새 카테고리 추가</Modal.Title>
        </Modal.Header>
        <Modal.Body>
          <Form onSubmit={handleNewCategory}>
            <InputGroup>
              <InputGroup.Text>이름</InputGroup.Text>
              <Form.Control
                type="text"
                value={newCategory}
                onChange={(e) => setNewCategory(e.target.value)}
                placeholder="카테고리 이름을 입력하세요"
                required
                autoFocus
              />
              <Button type="submit" variant="primary">추가</Button>
            </InputGroup>
          </Form>
        </Modal.Body>
      </Modal>
    </div>
  );
};
