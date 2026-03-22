import React from "react";
import { useSelector } from "react-redux";
import { useParams } from "react-router";
import { selectIsLoggedIn } from "../../redux/userSlice";
import Parser from "html-react-parser";
import DOMPurify from "dompurify";
import { useBoardQuery, useLikeStatusQuery } from "../../hooks/useQueries";
import { ErrorMessage } from "../ErrorMessage";
import { CommentForm } from "../Comments/CommentForm";
import { CommentList } from "../Comments/CommentList";
import dayjs from "dayjs";
import { BoardLike } from "./BoardLike";
import "../Styles/Board/boardDetail.css";

export const BoardDetailV2 = () => {
  const { boardId } = useParams();
  const isLoggedIn = useSelector(selectIsLoggedIn);

  const board = useBoardQuery(boardId);
  const likeStatus = useLikeStatusQuery(boardId, isLoggedIn);

  if (board.isLoading || likeStatus.isLoading) {
    return (
      <div className="loading-center">
        <span style={{ fontFamily: "var(--font-mono)", fontSize: "0.85rem" }}>loading...</span>
      </div>
    );
  }
  if (board.error) return <ErrorMessage message={board.error.message} />;

  return (
    <article>
      <h1 style={{ fontSize: "1.5rem", fontWeight: 700, marginBottom: "4px", color: "var(--text-primary)" }}>
        {board.data.title}
      </h1>

      <div className="board-info">
        <div className="board-info-left">
          <span>조회수 {board.data.views}</span>
          <span>{dayjs(board.data.createDate).format("YYYY-MM-DD")}</span>
        </div>
        <div className="board-info-right">
          <BoardLike boardId={board.data.id} likes={board.data.likes} />
        </div>
      </div>

      <div className="board-content">
        {Parser(DOMPurify.sanitize(board.data.content))}
      </div>

      <hr />

      {isLoggedIn ? (
        <CommentForm boardId={board.data.id} />
      ) : (
        <p style={{ color: "var(--text-muted)", fontFamily: "var(--font-mono)", fontSize: "0.82rem", padding: "12px 0" }}>
          // 댓글을 작성하려면 로그인이 필요합니다.
        </p>
      )}

      <CommentList boardId={board.data.id} />
    </article>
  );
};
