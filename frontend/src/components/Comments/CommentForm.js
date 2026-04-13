import React, { useState } from "react";
import { useSelector } from "react-redux";
import { selectIsLoggedIn } from "../../redux/userSlice";
import { addComment } from "../../services/boardApi";
import { useMutation, useQueryClient } from "@tanstack/react-query";
import { queryKeys } from "../../services/queryKeys";
import "./commentForm.css";

export const CommentForm = ({ boardId, parentId = null, onSuccess }) => {
  const isLoggedIn  = useSelector(selectIsLoggedIn);
  const queryClient = useQueryClient();
  const [commentData, setCommentData] = useState({
    comment: "", nickname: "", password: "",
  });

  const addCommentMutation = useMutation({
    mutationFn: (data) => addComment(boardId, data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: queryKeys.comments.list(boardId) });
      setCommentData({ comment: "", nickname: "", password: "" });
      onSuccess?.();
    },
  });

  const handleChange = (e) => {
    const { name, value } = e.target;
    setCommentData((prev) => ({ ...prev, [name]: value }));
  };

  const handleOnSubmit = (e) => {
    e.preventDefault();
    if (commentData.comment.trim() === "") {
      alert("댓글 내용을 입력하세요.");
      return;
    }
    if (!isLoggedIn) {
      if (commentData.nickname.trim() === "") {
        alert("닉네임을 입력하세요.");
        return;
      }
      if (commentData.password.trim() === "") {
        alert("비밀번호를 입력하세요.");
        return;
      }
    }
    addCommentMutation.mutate({ ...commentData, parentId });
  };

  return (
    <form className="comment-form" onSubmit={handleOnSubmit}>
      {!isLoggedIn && (
        <div className="comment-form__meta">
          <input
            className="comment-form__input"
            type="text"
            name="nickname"
            placeholder="닉네임"
            value={commentData.nickname}
            onChange={handleChange}
            maxLength={20}
          />
          <input
            className="comment-form__input"
            type="password"
            name="password"
            placeholder="비밀번호 (수정·삭제에 사용)"
            value={commentData.password}
            onChange={handleChange}
            maxLength={20}
          />
        </div>
      )}
      <div style={{ display: "flex", gap: "10px", alignItems: "flex-end" }}>
        <textarea
          className="comment-form__textarea"
          name="comment"
          placeholder="댓글을 입력하세요..."
          value={commentData.comment}
          onChange={handleChange}
        />
        <button
          type="submit"
          className="comment-form__submit"
          disabled={addCommentMutation.isPending}
        >
          {addCommentMutation.isPending ? "작성 중..." : "작성"}
        </button>
      </div>
    </form>
  );
};
