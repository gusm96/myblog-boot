"use client";

import { useState } from "react";
import { useQuery, useQueryClient } from "@tanstack/react-query";
import { Button, Form, InputGroup, ListGroup, ListGroupItem } from "react-bootstrap";
import { getCategoriesForAdmin, addNewCategory, deleteCategoryById } from "@/lib/postApi";
import { queryKeys } from "@/lib/queryKeys";

export default function CategoriesPage() {
  const queryClient = useQueryClient();
  const [newCategoryName, setNewCategoryName] = useState("");

  const { data: categories = [], isLoading } = useQuery({
    queryKey: queryKeys.categories.list(),
    queryFn:  getCategoriesForAdmin,
    staleTime: 5 * 60 * 1000,
  });

  const handleAddCategory = async () => {
    if (!newCategoryName.trim()) return;
    try {
      await addNewCategory(newCategoryName.trim());
      alert("카테고리가 등록되었습니다.");
      setNewCategoryName("");
      queryClient.invalidateQueries({ queryKey: queryKeys.categories.list() });
    } catch (error: unknown) {
      const err = error as { response?: { data?: { message?: string } } };
      alert(err.response?.data?.message || "카테고리 등록에 실패했습니다.");
    }
  };

  const handleDeleteCategory = async (categoryId: number) => {
    if (!window.confirm("정말로 삭제하시겠습니까?")) return;
    try {
      await deleteCategoryById(categoryId);
      alert("카테고리가 삭제되었습니다.");
      queryClient.invalidateQueries({ queryKey: queryKeys.categories.list() });
    } catch (error: unknown) {
      const err = error as { response?: { data?: { message?: string } } };
      alert(err.response?.data?.message || "카테고리 삭제에 실패했습니다.");
    }
  };

  if (isLoading) return <div style={{ padding: "2rem" }}>// loading...</div>;

  return (
    <div className="categories-container">
      <Form className="category-form" onSubmit={(e) => { e.preventDefault(); handleAddCategory(); }}>
        <InputGroup>
          <Form.Control
            placeholder="새로운 카테고리명을 입력하세요."
            value={newCategoryName}
            onChange={(e) => setNewCategoryName(e.target.value)}
          />
        </InputGroup>
        <Button type="submit">등록하기</Button>
      </Form>
      <hr />
      <ListGroup className="list-group list-group-container">
        {categories.map((category) => (
          <ListGroupItem key={category.id} className="list-items">
            <div className="category-name">
              <p>{category.name} ({category.postsCount})</p>
            </div>
            <div className="btn-container">
              <Button
                className="delete-btn"
                disabled={category.postsCount > 0}
                onClick={() => handleDeleteCategory(category.id)}
              >
                삭제
              </Button>
            </div>
          </ListGroupItem>
        ))}
      </ListGroup>
    </div>
  );
}
