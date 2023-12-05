import React from "react";

import moment from "moment";

export const CommentList = ({ comments }) => {
  return (
    <div>
      <ul>
        {comments.map((comment) => (
          <li key={comment.id}>
            <span className="writer">{comment.writer}</span>
            <span className="comment">{comment.comment}</span>
            <span className="uploadDate">
              {moment(comment.upload_date).format("YYYY-MM-DD")}
            </span>
          </li>
        ))}
      </ul>
    </div>
  );
};
