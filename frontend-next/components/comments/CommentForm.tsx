"use client";

import { useState } from "react";
import { useSelector } from "react-redux";
import { useMutation, useQueryClient } from "@tanstack/react-query";
import { selectIsLoggedIn } from "@/store/userSlice";
import { addComment } from "@/lib/postApi";
import { queryKeys } from "@/lib/queryKeys";

interface CommentFormProps {
  postId: number;
  parentId?: number | null;
  onSuccess?: () => void;
}

export function CommentForm({
  postId,
  parentId = null,
  onSuccess,
}: CommentFormProps) {
  const isLoggedIn = useSelector(selectIsLoggedIn);
  const queryClient = useQueryClient();
  const [commentData, setCommentData] = useState({
    comment: "",
    nickname: "",
    password: "",
  });

  const addCommentMutation = useMutation({
    mutationFn: (data: typeof commentData) => addComment(postId, { ...data, parentId }),
    onSuccess: () => {
      queryClient.invalidateQueries({
        queryKey: queryKeys.comments.list(postId),
      });
      setCommentData({ comment: "", nickname: "", password: "" });
      onSuccess?.();
    },
  });

  const handleChange = (e: React.ChangeEvent<HTMLInputElement | HTMLTextAreaElement>) => {
    const { name, value } = e.target;
    setCommentData((prev) => ({ ...prev, [name]: value }));
  };

  const handleSubmit = (e: React.FormEvent) => {
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
    addCommentMutation.mutate(commentData);
  };

  return (
    <form className="comment-form" onSubmit={handleSubmit}>
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
}
