import "../Styles/Board/commentList.css";
import { Comment } from "./Comment";
import { useCommentsQuery } from "../../hooks/useQueries";

export const CommentList = ({ boardId }) => {
  const comments = useCommentsQuery(boardId);

  if (comments.isLoading) {
    return (
      <section className="comment-section">
        <p className="comment-state-msg">// loading comments...</p>
      </section>
    );
  }
  if (comments.error) {
    return (
      <section className="comment-section">
        <p className="comment-state-msg comment-state-msg--error">
          // error: {comments.error.message}
        </p>
      </section>
    );
  }

  const count = comments.data.length;

  return (
    <section className="comment-section">
      <h3 className="comment-section-title">
        {count} {count === 1 ? "comment" : "comments"}
      </h3>
      <ul className="comment-list">
        {comments.data.map((comment) => (
          <li key={comment.id} className="comment-item">
            <Comment boardId={boardId} comment={comment} />
          </li>
        ))}
      </ul>
    </section>
  );
};
