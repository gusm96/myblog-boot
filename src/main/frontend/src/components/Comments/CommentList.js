import "../Styles/Board/commentList.css";
import { Comment } from "./Comment";
import { useCommentsQuery } from "../Queries/quries";

export const CommentList = ({ boardId }) => {
  const comments = useCommentsQuery(boardId);
  if (comments.isLoading) return <div>Loading...</div>;
  if (comments.error) return <div>Error : {comments.error.message} </div>;
  return (
    <div className="comment-list-container">
      <ul className="comment-list">
        {comments.data.map((comment) => (
          <li key={comment.id} className="comment-item">
            <Comment boardId={boardId} comment={comment} />
          </li>
        ))}
      </ul>
    </div>
  );
};
