import React, { useState } from "react";
import { Button, Modal, ModalBody, ModalFooter, ModalHeader, ModalTitle } from "react-bootstrap";
import { useSelector } from "react-redux";
import { selectIsLoggedIn } from "../../redux/userSlice";
import { addComment } from "../../services/boardApi";
import { useMutation, useQueryClient } from "@tanstack/react-query";
import { queryKeys } from "../../services/queryKeys";
import "./commentForm.css";

export const CommentForm = ({ boardId }) => {
  const isLoggedIn  = useSelector(selectIsLoggedIn);
  const queryClient = useQueryClient();
  const [isLoggedInModal, setIsLoggedInModal] = useState(false);
  const [commentData, setCommentData] = useState({ comment: "", parentId: "" });

  const addCommentMutation = useMutation({
    mutationFn: (data) => addComment(boardId, data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: queryKeys.comments.list(boardId) });
      setCommentData({ comment: "", parentId: "" });
    },
  });

  const handleChange = (e) => {
    const { name, value } = e.target;
    setCommentData((prev) => ({ ...prev, [name]: value }));
  };

  const handleCommentBoxClick = (e) => {
    e.preventDefault();
    if (!isLoggedIn) setIsLoggedInModal(true);
  };

  const handleOnSubmit = (e) => {
    e.preventDefault();
    if (commentData.comment.trim() === "") {
      alert("댓글의 내용을 입력하세요.");
      return;
    }
    addCommentMutation.mutate(commentData);
  };

  return (
    <>
      <form className="comment-form" onSubmit={handleOnSubmit}>
        <textarea
          className="comment-form__textarea"
          onClick={handleCommentBoxClick}
          name="comment"
          placeholder="댓글을 입력하세요..."
          value={commentData.comment}
          onChange={handleChange}
        />
        <Button
          variant="primary"
          type="submit"
          className="comment-form__submit"
          disabled={addCommentMutation.isPending}
        >
          {addCommentMutation.isPending ? "작성 중..." : "작성"}
        </Button>
      </form>

      <Modal show={isLoggedInModal} onHide={() => setIsLoggedInModal(false)} centered>
        <ModalHeader closeButton>
          <ModalTitle>로그인 필요</ModalTitle>
        </ModalHeader>
        <ModalBody>
          <p style={{ color: "var(--text-secondary)", marginBottom: 0 }}>
            댓글을 작성하려면 로그인이 필요합니다.
          </p>
        </ModalBody>
        <ModalFooter>
          <Button variant="primary" href="/login">로그인</Button>
          <Button variant="secondary" onClick={() => setIsLoggedInModal(false)}>닫기</Button>
        </ModalFooter>
      </Modal>
    </>
  );
};
