import React, { useState } from "react";
import { Button, Form, InputGroup } from "react-bootstrap";
import { addComment, getChildComments } from "../../services/boardApi";
import { useSelector } from "react-redux";
import { selectAccessToken, selectIsLoggedIn } from "../../redux/userSlice";
import { formatTimeAgo } from "../dateFormat";

export const Comment = ({ boardId, comment }) => {
  const isLoggedIn = useSelector(selectIsLoggedIn);
  const accessToken = useSelector(selectAccessToken);
  const [reply, setReply] = useState({
    comment: "",
    parentId: comment.id,
  });
  const [showReply, setShowReply] = useState(false);
  const [showReplyForm, setShowReplyForm] = useState(false);

  const [child, setChild] = useState([]);

  const handleOnChange = (e) => {
    e.preventDefault();
    setReply((prevReply) => ({
      ...prevReply,
      comment: e.target.value,
    }));
  };

  const handleShowChild = (parentId) => {
    getChildComments(parentId)
      .then((data) => {
        setChild(data);
        setShowReply(true);
      })
      .catch((error) => console.log(error));
  };
  const handleOnSubmit = (e) => {
    e.preventDefault();
    // axios
    addComment(boardId, reply, accessToken)
      .then((data) => {
        alert(data);
        window.location.reload();
      })
      .catch((error) => console.log(error));
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
          <span
            className="reply-list"
            onClick={() => handleShowChild(comment.id)}
          >
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
            {child.map((comment) => (
              <li key={comment.id} className="comment-item">
                <div className="comment-header">
                  <span className="writer">{comment.writer}</span>
                  <span className="upload-date">
                    {formatTimeAgo(comment.write_date)}
                  </span>
                </div>
                <p className="comment-content">{comment.comment}</p>
              </li>
            ))}
          </ul>
        </div>
      ) : null}
      {showReplyForm ? (
        <div>
          <hr></hr>
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
              <Button type="submit" onClick={handleOnSubmit}>
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
