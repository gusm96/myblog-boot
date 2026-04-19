"use client";

import { useState } from "react";
import { useQuery, useQueryClient } from "@tanstack/react-query";
import { Form, Button, Modal, InputGroup } from "react-bootstrap";
import { getCategoriesForAdmin, addNewCategory } from "@/lib/postApi";
import { queryKeys } from "@/lib/queryKeys";

interface CategorySelectInlineProps {
  value: string;
  onChange: (value: string) => void;
  showAddButton?: boolean;
}

export function CategorySelectInline({ value, onChange, showAddButton = true }: CategorySelectInlineProps) {
  const queryClient = useQueryClient();
  const [showModal, setShowModal] = useState(false);
  const [newCategory, setNewCategory] = useState("");

  const { data: categories = [] } = useQuery({
    queryKey: queryKeys.categories.list(),
    queryFn:  getCategoriesForAdmin,
    staleTime: 30 * 60 * 1000,
  });

  const handleAddCategory = async (e: React.FormEvent) => {
    e.preventDefault();
    try {
      await addNewCategory(newCategory);
      alert("카테고리 추가 완료");
      queryClient.invalidateQueries({ queryKey: queryKeys.categories.list() });
      setShowModal(false);
      setNewCategory("");
    } catch (error: unknown) {
      const err = error as { response?: { data?: { message?: string } } };
      alert(err.response?.data?.message || "카테고리 추가에 실패했습니다.");
    }
  };

  return (
    <>
      <div style={{ display: "flex", flex: 1, alignItems: "stretch" }}>
        <Form.Select
          name="category"
          value={value}
          onChange={(e) => onChange(e.target.value)}
          className="editor-meta__category-select editor-meta__category-select--standalone"
        >
          <option value="">카테고리 선택</option>
          {categories.map((c) => (
            <option key={c.id} value={c.id}>{c.name}</option>
          ))}
        </Form.Select>
        {showAddButton && (
          <button
            type="button"
            className="editor-meta__add-cat-btn"
            onClick={() => setShowModal(true)}
          >
            +
          </button>
        )}
      </div>

      <Modal show={showModal} onHide={() => setShowModal(false)} centered>
        <Modal.Header closeButton>
          <Modal.Title>새 카테고리 추가</Modal.Title>
        </Modal.Header>
        <Modal.Body>
          <Form onSubmit={handleAddCategory}>
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
    </>
  );
}
