"use client";

import { useState } from "react";
import { useQuery, useQueryClient } from "@tanstack/react-query";
import { Button, Form, InputGroup, ListGroup, ListGroupItem, Modal } from "react-bootstrap";
import { getTagsForAdmin, createTag, renameTag, deleteTag, mergeTags } from "@/lib/postApi";
import { queryKeys } from "@/lib/queryKeys";
import { getApiErrorMessage } from "@/lib/apiError";
import type { Tag } from "@/types";

export default function TagsPage() {
  const queryClient = useQueryClient();
  const [newTagName, setNewTagName] = useState("");
  const [editingTagId, setEditingTagId] = useState<number | null>(null);
  const [editingTagName, setEditingTagName] = useState("");
  const [mergeSrcTag, setMergeSrcTag] = useState<Tag | null>(null);
  const [mergeDstId, setMergeDstId] = useState<string>("");

  const { data: tags = [], isLoading } = useQuery<Tag[]>({
    queryKey: queryKeys.tags.adminList(),
    queryFn:  getTagsForAdmin,
    staleTime: 5 * 60 * 1000,
  });

  const handleCreateTag = async () => {
    if (!newTagName.trim()) return;
    try {
      await createTag(newTagName.trim());
      alert("태그가 등록되었습니다.");
      setNewTagName("");
      invalidateTags();
    } catch (error: unknown) {
      alert(getApiErrorMessage(error, "태그 등록에 실패했습니다."));
    }
  };

  const handleRenameTag = async (tagId: number) => {
    if (!editingTagName.trim()) return;
    try {
      await renameTag(tagId, editingTagName.trim());
      alert("태그 이름이 변경되었습니다.");
      setEditingTagId(null);
      invalidateTags();
    } catch (error: unknown) {
      alert(getApiErrorMessage(error, "태그 이름 변경에 실패했습니다."));
    }
  };

  const handleDeleteTag = async (tagId: number) => {
    if (!window.confirm("정말로 삭제하시겠습니까?")) return;
    try {
      await deleteTag(tagId);
      alert("태그가 삭제되었습니다.");
      invalidateTags();
    } catch (error: unknown) {
      alert(getApiErrorMessage(error, "태그 삭제에 실패했습니다."));
    }
  };

  const handleMergeTags = async () => {
    if (!mergeSrcTag || !mergeDstId) return;
    const dstIdNum = Number(mergeDstId);
    if (mergeSrcTag.id === dstIdNum) {
      alert("동일한 태그는 병합할 수 없습니다.");
      return;
    }
    const dstTag = tags.find(t => t.id === dstIdNum);
    if (!window.confirm(`[${mergeSrcTag.name}] 태그를 [${dstTag?.name}] 태그로 병합하시겠습니까?\n병합 후 [${mergeSrcTag.name}] 태그는 삭제되며, 이 작업은 되돌릴 수 없습니다.`)) return;

    try {
      await mergeTags(mergeSrcTag.id, dstIdNum);
      alert("태그가 병합되었습니다.");
      setMergeSrcTag(null);
      setMergeDstId("");
      invalidateTags();
    } catch (error: unknown) {
      alert(getApiErrorMessage(error, "태그 병합에 실패했습니다."));
    }
  };

  const invalidateTags = () => {
    queryClient.invalidateQueries({ queryKey: queryKeys.tags.adminList() });
    queryClient.invalidateQueries({ queryKey: queryKeys.tags.publicList() });
    queryClient.invalidateQueries({ queryKey: queryKeys.posts.lists() });
  };

  if (isLoading) return <div style={{ padding: "2rem" }}>// loading...</div>;

  return (
    <div className="categories-container">
      <Form className="category-form" onSubmit={(e) => { e.preventDefault(); handleCreateTag(); }}>
        <InputGroup>
          <Form.Control
            placeholder="새로운 태그명을 입력하세요."
            value={newTagName}
            onChange={(e) => setNewTagName(e.target.value)}
          />
        </InputGroup>
        <Button type="submit">등록하기</Button>
      </Form>
      <hr />
      
      <ListGroup className="list-group list-group-container">
        {tags.map((tag) => (
          <ListGroupItem key={tag.id} className="list-items d-flex align-items-center justify-content-between">
            {editingTagId === tag.id ? (
              <div className="d-flex align-items-center gap-2 flex-grow-1 me-3">
                <Form.Control
                  size="sm"
                  value={editingTagName}
                  onChange={(e) => setEditingTagName(e.target.value)}
                  style={{ maxWidth: "250px" }}
                />
                <Button size="sm" variant="success" onClick={() => handleRenameTag(tag.id)}>확인</Button>
                <Button size="sm" variant="secondary" onClick={() => setEditingTagId(null)}>취소</Button>
              </div>
            ) : (
              <div className="category-name flex-grow-1">
                <p className="m-0">
                  <strong>{tag.name}</strong> <span className="text-muted" style={{ fontSize: "0.85rem" }}>({tag.slug})</span> - {tag.postCount}개의 글
                </p>
              </div>
            )}
            
            <div className="btn-container d-flex gap-2">
              {editingTagId !== tag.id && (
                <Button
                  size="sm"
                  variant="outline-primary"
                  onClick={() => {
                    setEditingTagId(tag.id);
                    setEditingTagName(tag.name);
                  }}
                >
                  수정
                </Button>
              )}
              <Button
                size="sm"
                variant="outline-warning"
                onClick={() => setMergeSrcTag(tag)}
              >
                병합
              </Button>
              <Button
                className="delete-btn"
                variant="danger"
                disabled={tag.postCount > 0}
                onClick={() => handleDeleteTag(tag.id)}
              >
                삭제
              </Button>
            </div>
          </ListGroupItem>
        ))}
      </ListGroup>

      {/* Merge Tag Modal */}
      <Modal show={mergeSrcTag !== null} onHide={() => setMergeSrcTag(null)} centered>
        <Modal.Header closeButton>
          <Modal.Title>태그 병합</Modal.Title>
        </Modal.Header>
        <Modal.Body>
          {mergeSrcTag && (
            <div>
              <p>
                병합할 원본 태그: <strong>{mergeSrcTag.name}</strong> (글 개수: {mergeSrcTag.postCount})
              </p>
              <Form.Group className="mb-3">
                <Form.Label>대상 태그 선택</Form.Label>
                <Form.Select
                  value={mergeDstId}
                  onChange={(e) => setMergeDstId(e.target.value)}
                >
                  <option value="">병합할 대상 태그를 선택하세요...</option>
                  {tags
                    .filter((t) => t.id !== mergeSrcTag.id)
                    .map((t) => (
                      <option key={t.id} value={t.id}>
                        {t.name} ({t.postCount})
                      </option>
                    ))}
                </Form.Select>
              </Form.Group>
            </div>
          )}
        </Modal.Body>
        <Modal.Footer>
          <Button variant="secondary" onClick={() => setMergeSrcTag(null)}>
            취소
          </Button>
          <Button variant="warning" onClick={handleMergeTags} disabled={!mergeDstId}>
            병합 실행
          </Button>
        </Modal.Footer>
      </Modal>
    </div>
  );
}
