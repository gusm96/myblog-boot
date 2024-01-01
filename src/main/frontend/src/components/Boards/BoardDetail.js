import React, { useEffect, useState } from "react";
import { useParams } from "react-router-dom";
import moment from "moment";
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
import { CommentForm } from "../Comments/CommentForm";
const BoardDetail = () => {
  const isLoggedIn = useSelector(selectIsLoggedIn);
  const accessToken = useSelector(selectAccessToken);
  const { boardId } = useParams();
  const [board, setBoard] = useState({
    title: "",
    content: "",
    uploadDate: "",
    views: "",
    likes: "",
  });
  const [isBoardLiked, setIsBoardLiked] = useState(false);
  useEffect(() => {
    const fetchData = async () => {
      try {
        const boardData = await getBoard(boardId);
        setBoard({
          title: boardData.title,
          content: boardData.content,
          uploadDate: boardData.uploadDate,
          views: boardData.views,
          likes: boardData.likes,
        });
        if (isLoggedIn) {
          const likeStatusData = await getBoardLikeStatus(boardId, accessToken);
          setIsBoardLiked(likeStatusData);
        }
      } catch (error) {
        console.error(error);
      }
    };
    fetchData();
  }, [isLoggedIn, boardId, accessToken]);
  const uploadDateFormat = moment(board.uploadDate).format("YYYY-MM-DD");
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
  return (
    <div>
      <h1>{board.title}</h1>
      <hr></hr>
      <div>{Parser(board.content)}</div>
      <div className="board-info">
        <div className="board-like">
          <span>조회수 {board.views}</span>
          <br></br>
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
        <CommentForm boardId={boardId} accessToken={accessToken} />
      ) : (
        <p>로그인을 하면 댓글을 작성할 수 있습니다.</p>
      )}
      <CommentList boardId={boardId} />
    </div>
  );
};

export default BoardDetail;
