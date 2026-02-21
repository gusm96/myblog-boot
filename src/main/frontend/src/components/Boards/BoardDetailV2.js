import React from "react";
import { useSelector } from "react-redux";
import { useParams } from "react-router-dom";
import { selectAccessToken, selectIsLoggedIn } from "../../redux/userSlice";
import Parser from "html-react-parser";
import DOMPurify from "dompurify";
import { useBoardQuery, useLikeStatusQuery } from "../../hooks/useQueries";
import { ErrorMessage } from "../ErrorMessage";
import { CommentForm } from "../Comments/CommentForm";
import { CommentList } from "../Comments/CommentList";
import moment from "moment";
import { BoardLike } from "./BoardLike";

export const BoardDetailV2 = () => {
  const { boardId } = useParams();
  const isLoggedIn = useSelector(selectIsLoggedIn);
  const accessToken = useSelector(selectAccessToken);

  const board = useBoardQuery(boardId);
  const likeStatus = useLikeStatusQuery(boardId, accessToken, isLoggedIn);

  if (board.isLoading || likeStatus.isLoading) return <div>Loading...</div>;
  if (board.error) return <ErrorMessage message={board.error.message} />;
  return (
    <div>
      <h1>{board.data.title}</h1>
      <hr></hr>
      <div>{Parser(DOMPurify.sanitize(board.data.content))}</div>
      <div className="board-info">
        <div className="board-like">
          <span>조회수 {board.data.views}</span>
          <br></br>
          <BoardLike boardId={board.data.id} likes={board.data.likes} />
        </div>
        <span>{moment(board.data.createDate).format("YYYY-MM-DD")}</span>
      </div>
      <hr></hr>
      {isLoggedIn ? (
        <CommentForm boardId={board.data.id} />
      ) : (
        <p>로그인을 하면 댓글을 작성할 수 있습니다.</p>
      )}
      <CommentList boardId={board.data.id} />
    </div>
  );
};
