import { useEffect, useState } from "react";
import "../Styles/Board/commentList.css";
import { Comment } from "./Comment";
import { getComments } from "../../services/boardApi";

export const CommentList = ({ boardId }) => {
  const [comments, setComments] = useState([]);
  useEffect(() => {
    const fetchData = async () => {
      const commentsData = await getComments(boardId);
      setComments(commentsData);
    };
    fetchData();
  }, [boardId]);
  return (
    <div className="comment-list-container">
      <ul className="comment-list">
        {comments.map((comment) => (
          <li key={comment.id} className="comment-item">
            <Comment boardId={boardId} comment={comment} />
          </li>
        ))}
      </ul>
    </div>
  );
};
