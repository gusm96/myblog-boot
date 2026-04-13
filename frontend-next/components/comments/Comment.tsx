"use client";

import { useState } from "react";
import { useSelector } from "react-redux";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { selectIsLoggedIn } from "@/store/userSlice";
import { addComment, getChildComments } from "@/lib/postApi";
import { queryKeys } from "@/lib/queryKeys";
import { formatTimeAgo } from "@/lib/formatTimeAgo";
import type { Comment as CommentType } from "@/types";

interface CommentProps {
  postId: number;
  comment: CommentType;
}

export function Comment({ postId, comment }: CommentProps) {
  const isLoggedIn = useSelector(selectIsLoggedIn);
  const queryClient = useQueryClient();
  const [reply, setReply] = useState({ comment: "", parentId: comment.id });
  const [showReply, setShowReply] = useState(false);
  const [showReplyForm, setShowReplyForm] = useState(false);

  const { data: children = [] } = useQuery<CommentType[]>({
    queryKey: queryKeys.comments.children(comment.id),
    queryFn: () => getChildComments(comment.id),
    enabled: showReply,
    staleTime: 30 * 1000,
    gcTime: 5 * 60 * 1000,
  });

  const replyMutation = useMutation({
    mutationFn: (replyData: typeof reply) => addComment(postId, replyData),
    onSuccess: () => {
      queryClient.invalidateQueries({
        queryKey: queryKeys.comments.list(postId),
      });
      queryClient.invalidateQueries({
        queryKey: queryKeys.comments.children(comment.id),
      });
      setReply({ comment: "", parentId: comment.id });
      setShowReplyForm(false);
    },
  });

  const handleReplySubmit = (e: React.FormEvent) => {
    e.preventDefault();
    if (reply.comment.trim() === "") return;
    replyMutation.mutate(reply);
  };

  return (
    <div className="comment-inner">
      <div className="comment-header">
        <span className="writer">{comment.writer}</span>
        <span className="upload-date">{formatTimeAgo(comment.createDate)}</span>
      </div>
      <p className="comment-content">{comment.comment}</p>

      <div className="reply-container">
        {(comment.childCount ?? 0) > 0 && (
          <button
            className={`reply-list${showReply ? " active" : ""}`}
            onClick={() => setShowReply((v) => !v)}
          >
            {showReply ? "▲" : "▼"}&nbsp;답글 {comment.childCount}개
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
              <div className="comment-header">
                <span className="writer">{child.writer}</span>
                <span className="upload-date">
                  {formatTimeAgo(child.createDate)}
                </span>
              </div>
              <p className="comment-content">{child.comment}</p>
            </li>
          ))}
        </ul>
      )}

      {showReplyForm && (
        <form className="reply-form" onSubmit={handleReplySubmit}>
          <textarea
            className="reply-form__textarea"
            placeholder="답글을 입력하세요..."
            value={reply.comment}
            onChange={(e) =>
              setReply((prev) => ({ ...prev, comment: e.target.value }))
            }
          />
          <div className="reply-form__actions">
            <button
              type="button"
              className="reply-form__cancel"
              onClick={() => setShowReplyForm(false)}
            >
              취소
            </button>
            <button
              type="submit"
              className="reply-form__submit"
              disabled={replyMutation.isPending}
            >
              {replyMutation.isPending ? "작성 중..." : "작성"}
            </button>
          </div>
        </form>
      )}
    </div>
  );
}
