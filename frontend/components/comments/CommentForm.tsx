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
    mutationFn: (data: typeof commentData) =>
      addComment(postId, { ...data, parentId }),
    onSuccess: () => {
      queryClient.invalidateQueries({
        queryKey: queryKeys.comments.list(postId),
      });
      if (parentId) {
        queryClient.invalidateQueries({
          queryKey: queryKeys.comments.children(parentId),
        });
      }
      setCommentData({ comment: "", nickname: "", password: "" });
      onSuccess?.();
    },
  });

  const handleChange = (
    e: React.ChangeEvent<HTMLInputElement | HTMLTextAreaElement>
  ) => {
    const { name, value } = e.target;
    setCommentData((prev) => ({ ...prev, [name]: value }));
  };

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    const trimmed = commentData.comment.trim();
    if (trimmed.length < 2) {
      alert("댓글은 2글자 이상 입력하세요.");
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

  const isReply = parentId !== null;

  return (
    <form
      className={isReply ? "reply-form" : "comment-form"}
      onSubmit={handleSubmit}
    >
      {!isReply && (
        <div className="comment-form__label">
          {isLoggedIn ? "// write a comment" : "// leave a comment"}
        </div>
      )}
      {!isLoggedIn && (
        <div className={isReply ? "reply-form__meta" : "comment-form__meta"}>
          <input
            className={isReply ? "reply-form__input" : "comment-form__input"}
            type="text"
            name="nickname"
            placeholder="닉네임"
            value={commentData.nickname}
            onChange={handleChange}
            maxLength={6}
          />
          <input
            className={isReply ? "reply-form__input" : "comment-form__input"}
            type="password"
            name="password"
            placeholder="비밀번호 (수정/삭제에 사용)"
            value={commentData.password}
            onChange={handleChange}
            maxLength={4}
          />
        </div>
      )}
      <textarea
        className={isReply ? "reply-form__textarea" : "comment-form__textarea"}
        name="comment"
        placeholder={isReply ? "답글을 입력하세요..." : "댓글을 입력하세요..."}
        value={commentData.comment}
        onChange={handleChange}
        maxLength={500}
      />
      <div className={isReply ? "reply-form__actions" : "comment-form__actions"}>
        {isReply && onSuccess && (
          <button
            type="button"
            className="reply-form__cancel"
            onClick={onSuccess}
          >
            취소
          </button>
        )}
        <button
          type="submit"
          className={isReply ? "reply-form__submit" : "comment-form__submit"}
          disabled={addCommentMutation.isPending}
        >
          {addCommentMutation.isPending ? "작성 중..." : "작성"}
        </button>
      </div>
    </form>
  );
}
