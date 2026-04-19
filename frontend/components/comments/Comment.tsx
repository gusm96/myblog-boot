"use client";

import { useState } from "react";
import { useQuery, useMutation, useQueryClient } from "@tanstack/react-query";
import { useSelector } from "react-redux";
import { selectIsLoggedIn } from "@/store/userSlice";
import { getChildComments, editComment, deleteComment } from "@/lib/postApi";
import { queryKeys } from "@/lib/queryKeys";
import { formatTimeAgo } from "@/lib/formatTimeAgo";
import { CommentForm } from "./CommentForm";
import type { Comment as CommentType } from "@/types";

type ActionMode = "idle" | "edit" | "delete";

interface CommentProps {
  postId: number;
  comment: CommentType;
}

interface CommentActionFormProps {
  postId: number;
  comment: CommentType;
  isLoggedIn: boolean;
  mode: "edit" | "delete";
  onClose: () => void;
  isChild?: boolean;
  parentId?: number | null;
}

function CommentActionForm({
  postId,
  comment,
  isLoggedIn,
  mode,
  onClose,
  isChild = false,
  parentId = null,
}: CommentActionFormProps) {
  const queryClient = useQueryClient();
  const [editText, setEditText] = useState(comment.comment);
  const [password, setPassword] = useState("");
  const [error, setError] = useState("");

  const invalidateComments = () => {
    queryClient.invalidateQueries({
      queryKey: queryKeys.comments.list(postId),
    });
    if (isChild && parentId) {
      queryClient.invalidateQueries({
        queryKey: queryKeys.comments.children(parentId),
      });
    }
  };

  const editMutation = useMutation({
    mutationFn: () =>
      editComment(comment.id, {
        comment: editText.trim(),
        password: isLoggedIn ? null : password,
      }),
    onSuccess: () => {
      invalidateComments();
      onClose();
    },
    onError: (err: unknown) => {
      const msg =
        (err as { response?: { data?: { message?: string } } })?.response?.data
          ?.message ?? "수정에 실패했습니다.";
      setError(msg);
    },
  });

  const deleteMutation = useMutation({
    mutationFn: () =>
      deleteComment(comment.id, {
        password: isLoggedIn ? null : password,
      }),
    onSuccess: () => {
      invalidateComments();
      onClose();
    },
    onError: (err: unknown) => {
      const msg =
        (err as { response?: { data?: { message?: string } } })?.response?.data
          ?.message ?? "삭제에 실패했습니다.";
      setError(msg);
    },
  });

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    if (mode === "edit") {
      if (editText.trim().length < 2) {
        setError("댓글은 2글자 이상 입력하세요.");
        return;
      }
    }
    if (!isLoggedIn && password.trim() === "") {
      setError("비밀번호를 입력하세요.");
      return;
    }
    setError("");
    if (mode === "edit") {
      editMutation.mutate();
    } else {
      deleteMutation.mutate();
    }
  };

  const isPending =
    mode === "edit" ? editMutation.isPending : deleteMutation.isPending;

  return (
    <form className="comment-action-form" onSubmit={handleSubmit}>
      {mode === "edit" ? (
        <textarea
          className="comment-action-form__textarea"
          value={editText}
          onChange={(e) => setEditText(e.target.value)}
          maxLength={500}
        />
      ) : (
        <p className="comment-action-form__confirm">
          이 댓글을 삭제하시겠습니까?
        </p>
      )}
      {!isLoggedIn && (
        <input
          className="comment-action-form__password"
          type="password"
          placeholder="비밀번호"
          value={password}
          onChange={(e) => setPassword(e.target.value)}
          maxLength={4}
        />
      )}
      {error && <p className="comment-action-form__error">{error}</p>}
      <div className="comment-action-form__buttons">
        <button
          type="button"
          className="comment-action-form__cancel"
          onClick={onClose}
        >
          취소
        </button>
        <button
          type="submit"
          className={`comment-action-form__submit${mode === "delete" ? " comment-action-form__submit--danger" : ""}`}
          disabled={isPending}
        >
          {isPending
            ? mode === "edit"
              ? "수정 중..."
              : "삭제 중..."
            : mode === "edit"
              ? "수정"
              : "삭제"}
        </button>
      </div>
    </form>
  );
}

function CommentBody({
  postId,
  comment,
  isLoggedIn,
  isChild = false,
  parentId = null,
}: {
  postId: number;
  comment: CommentType;
  isLoggedIn: boolean;
  isChild?: boolean;
  parentId?: number | null;
}) {
  const [mode, setMode] = useState<ActionMode>("idle");

  return (
    <>
      <div className="comment-header">
        <div className="comment-header__left">
          <span className="writer">{comment.writer}</span>
          <span className="upload-date">
            {formatTimeAgo(comment.createDate)}
          </span>
          {comment.modificationStatus === "MODIFIED" && (
            <span className="comment-modified">(수정됨)</span>
          )}
        </div>
        {mode === "idle" && (
          <div className="comment-actions">
            <button
              className="comment-actions__btn"
              onClick={() => setMode("edit")}
            >
              수정
            </button>
            <button
              className="comment-actions__btn comment-actions__btn--danger"
              onClick={() => setMode("delete")}
            >
              삭제
            </button>
          </div>
        )}
      </div>
      {mode === "idle" ? (
        <p className="comment-content">{comment.comment}</p>
      ) : (
        <CommentActionForm
          postId={postId}
          comment={comment}
          isLoggedIn={isLoggedIn}
          mode={mode}
          onClose={() => setMode("idle")}
          isChild={isChild}
          parentId={parentId}
        />
      )}
    </>
  );
}

export function Comment({ postId, comment }: CommentProps) {
  const isLoggedIn = useSelector(selectIsLoggedIn);
  const [showReply, setShowReply] = useState(false);
  const [showReplyForm, setShowReplyForm] = useState(false);

  const { data: children = [] } = useQuery<CommentType[]>({
    queryKey: queryKeys.comments.children(comment.id),
    queryFn: () => getChildComments(comment.id),
    enabled: showReply,
    staleTime: 30 * 1000,
    gcTime: 5 * 60 * 1000,
  });

  return (
    <div className="comment-inner">
      <CommentBody
        postId={postId}
        comment={comment}
        isLoggedIn={isLoggedIn}
      />

      <div className="reply-container">
        {(comment.childCount ?? 0) > 0 && (
          <button
            className={`reply-list${showReply ? " active" : ""}`}
            onClick={() => setShowReply((v) => !v)}
          >
            {showReply ? "\u25B2" : "\u25BC"}&nbsp;답글 {comment.childCount}개
          </button>
        )}
        <button
          className={`reply-btn${showReplyForm ? " active" : ""}`}
          onClick={() => setShowReplyForm((v) => !v)}
        >
          답글 달기
        </button>
      </div>

      {showReply && (
        <ul className="comment-list reply-thread">
          {children.map((child) => (
            <li key={child.id} className="comment-item reply-item">
              <CommentBody
                postId={postId}
                comment={child}
                isLoggedIn={isLoggedIn}
                isChild
                parentId={comment.id}
              />
            </li>
          ))}
        </ul>
      )}

      {showReplyForm && (
        <CommentForm
          postId={postId}
          parentId={comment.id}
          onSuccess={() => setShowReplyForm(false)}
        />
      )}
    </div>
  );
}
