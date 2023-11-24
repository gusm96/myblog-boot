import React from "react";
import { useState } from "react";
import { useEffect } from "react";
import { getComments } from "../../services/boardApi";
import moment from "moment";

export const CommentList = ({ boardId }) => {
  const [comments, setComments] = useState([]);
  useEffect(() => {
    getComments(boardId)
      .then((data) => setComments(data))
      .catch((error) => console.log(error));
  }, [boardId]);
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
