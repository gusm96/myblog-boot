import React from "react";

import moment from "moment";
import "../Styles/Board/commentList.css";
export const CommentList = ({ comments }) => {
  return (
    <div className="comment-list-container">
      <ul className="comment-list">
        {comments.map((comment) => (
          <li key={comment.id} className="comment-item">
            <div className="comment-header">
              <span className="writer">{comment.writer}</span>
              <span className="upload-date">
                {moment(comment.upload_date).format("YYYY-MM-DD")}
              </span>
            </div>
            <p className="comment-content">{comment.comment}</p>
            <div className="reply-container">
              <span className="reply-list">답글보기</span>
              <span className="reply-btn">답글</span>
            </div>
          </li>
        ))}
      </ul>
    </div>
  );
};
