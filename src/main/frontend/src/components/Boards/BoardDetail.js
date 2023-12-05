import React, { useEffect, useState } from "react";
import { useParams } from "react-router-dom";
import moment from "moment";
import { Comment } from "../Comments/Comment";
import { useSelector } from "react-redux";
import { selectAccessToken, selectIsLoggedIn } from "../../redux/userSlice";
import {
  addBoardLike,
  cancelBoardLike,
  getBoard,
  getBoardLikeStatus,
} from "../../services/boardApi";
import Parser from "html-react-parser";
import { CommentList } from "../Comments/CommentList";
import "../Styles/Board/boardDetail.css";
const BoardDetail = () => {
  const isLoggedIn = useSelector(selectIsLoggedIn);
  const [isBoardLiked, setIsBoardLiked] = useState(false);
  const accessToken = useSelector(selectAccessToken);
  const { boardId } = useParams();
  const [board, setBoard] = useState({
    title: "",
    content: "",
    uploadDate: "",
    likes: "",
    comments: [],
  });
  const handleBoardLike = (e) => {
    e.preventDefault();
    if (!isLoggedIn) {
      alert("로그인이 필요한 서비스입니다."); // 로그인하지 않은 경우 경고 메시지 표시
      return; // 이벤트 핸들러 종료
    }
    addBoardLike(boardId, accessToken)
      .then((data) => {
        setBoard((prevBoard) => ({ ...prevBoard, likes: data }));
        setIsBoardLiked(true);
      })
      .catch((error) => console.log(error));
  };
  const handleBoardLikeCancel = (e) => {
    e.preventDefault();
    if (!isLoggedIn) {
      alert("로그인이 필요한 서비스입니다."); // 로그인하지 않은 경우 경고 메시지 표시
      return; // 이벤트 핸들러 종료
    }
    cancelBoardLike(boardId, accessToken).then((data) => {
      setBoard((prevBoard) => ({ ...prevBoard, likes: data }));
      setIsBoardLiked(false);
    });
  };
  useEffect(() => {
    getBoard(boardId).then((data) => {
      setBoard({
        title: data.title,
        content: data.content,
        uploadDate: data.uploadDate,
        likes: data.likes,
        comments: data.comments,
      });
    });
    if (isLoggedIn) {
      getBoardLikeStatus(boardId, accessToken)
        .then((data) => setIsBoardLiked(data))
        .catch((error) => console.log(error));
    }
  }, [isLoggedIn, boardId, accessToken]);
  const uploadDateFormat = moment(board.uploadDate).format("YYYY-MM-DD");
  return (
    <div>
      <h1>{board.title}</h1>
      <hr></hr>
      <div>{Parser(board.content)}</div>
      <div className="board-info">
        <div className="board-like">
          {isBoardLiked ? (
            <i
              className="fa-solid fa-heart board-like-status"
              onClick={handleBoardLikeCancel}
            />
          ) : (
            <i
              className="fa-regular fa-heart board-like-status "
              onClick={handleBoardLike}
            />
          )}
          <span className="board-like-count">{board.likes}</span>
        </div>
        <span>{uploadDateFormat}</span>
      </div>
      <hr></hr>
      {isLoggedIn ? (
        <Comment boardId={boardId} accessToken={accessToken} />
      ) : (
        <p>로그인을 하면 댓글을 작성할 수 있습니다.</p>
      )}
      <CommentList comments={board.comments} />
    </div>
  );
};

export default BoardDetail;
