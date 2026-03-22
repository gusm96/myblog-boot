import React, { useState } from "react";
import { Button, Form, InputGroup } from "react-bootstrap";
import { addComment, getChildComments } from "../../services/boardApi";
import { useSelector } from "react-redux";
import { selectIsLoggedIn } from "../../redux/userSlice";
import { formatTimeAgo } from "../dateFormat";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { queryKeys } from "../../services/queryKeys";

export const Comment = ({ boardId, comment }) => {
  const isLoggedIn  = useSelector(selectIsLoggedIn);
  const queryClient = useQueryClient();
  const [reply, setReply]               = useState({ comment: "", parentId: comment.id });
  const [showReply, setShowReply]       = useState(false);
  const [showReplyForm, setShowReplyForm] = useState(false);

  // 자식 댓글: 펼칠 때만 요청, 이후 캐시 재사용
  const { data: children = [] } = useQuery({
    queryKey: queryKeys.comments.children(comment.id),
    queryFn:  () => getChildComments(comment.id),
    enabled:  showReply,
    staleTime: 30 * 1000,
    gcTime:     5 * 60 * 1000,
  });

  const replyMutation = useMutation({
    mutationFn: (replyData) => addComment(boardId, replyData),
    onSuccess: (data) => {
      alert(data);
      queryClient.invalidateQueries({ queryKey: queryKeys.comments.list(boardId) });
      queryClient.invalidateQueries({ queryKey: queryKeys.comments.children(comment.id) });
      setReply({ comment: "", parentId: comment.id });
      setShowReplyForm(false);
    },
  });

  const handleOnChange = (e) => {
    e.preventDefault();
    setReply((prev) => ({ ...prev, comment: e.target.value }));
  };

  const handleOnSubmit = (e) => {
    e.preventDefault();
    replyMutation.mutate(reply);
  };

  return (
    <div>
      <div className="comment-header">
        <span className="writer">{comment.writer}</span>
        <span className="upload-date">{formatTimeAgo(comment.write_date)}</span>
      </div>
      <p className="comment-content">{comment.comment}</p>
      <div className="reply-container">
        {comment.childCount > 0 ? (
          <span className="reply-list" onClick={() => setShowReply(true)}>
            답글보기({comment.childCount})
          </span>
        ) : null}
        {isLoggedIn ? (
          <span className="reply-btn" onClick={() => setShowReplyForm(true)}>
            답글
          </span>
        ) : null}
      </div>
      {showReply ? (
        <div className="comment-list-container">
          <ul className="comment-list">
            {children.map((child) => (
              <li key={child.id} className="comment-item">
                <div className="comment-header">
                  <span className="writer">{child.writer}</span>
                  <span className="upload-date">{formatTimeAgo(child.write_date)}</span>
                </div>
                <p className="comment-content">{child.comment}</p>
              </li>
            ))}
          </ul>
        </div>
      ) : null}
      {showReplyForm ? (
        <div>
          <hr />
          <Form>
            <InputGroup>
              <InputGroup.Text>답글</InputGroup.Text>
              <Form.Control
                type="text"
                name="comment"
                value={reply.comment}
                onChange={handleOnChange}
                placeholder="댓글을 입력하세요."
              />
              <Button
                type="submit"
                onClick={handleOnSubmit}
                disabled={replyMutation.isPending}
              >
                작성
              </Button>
              <Button type="button" onClick={() => setShowReplyForm(false)}>
                취소
              </Button>
            </InputGroup>
          </Form>
        </div>
      ) : null}
    </div>
  );
};
