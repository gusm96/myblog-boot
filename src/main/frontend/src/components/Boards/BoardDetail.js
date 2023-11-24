import React, { useEffect, useState } from "react";
import { useParams } from "react-router-dom";
import moment from "moment";
import { Comment } from "../Comments/Comment";
import { useSelector } from "react-redux";
import { selectAccessToken, selectIsLoggedIn } from "../../redux/userSlice";
import { getBoard } from "../../services/boardApi";
import Parser from "html-react-parser";
import { CommentList } from "../Comments/CommentList";
const BoardDetail = () => {
  const isLoggedIn = useSelector(selectIsLoggedIn);
  const accessToken = useSelector(selectAccessToken);
  const { boardId } = useParams();
  const [board, setBoard] = useState({
    title: "",
    content: "",
    uploadDate: "",
    likes: "",
  });
  useEffect(() => {
    getBoard(boardId).then((data) => {
      setBoard({
        title: data.title,
        content: data.content,
        uploadDate: data.uploadDate,
        likes: data.likes,
      });
    });
  }, [boardId]);
  const uploadDateFormat = moment(board.uploadDate).format("YYYY-MM-DD");
  return (
    <div>
      <h1>{board.title}</h1>
      <div>{Parser(board.content)}</div>
      <p>{uploadDateFormat}</p>
      <p>{board.likes}</p>
      <hr></hr>
      {isLoggedIn ? (
        <Comment boardId={boardId} accessToken={accessToken} />
      ) : (
        <p>로그인을 하면 댓글을 작성할 수 있습니다.</p>
      )}
      <CommentList boardId={boardId} />
    </div>
  );
};

export default BoardDetail;
