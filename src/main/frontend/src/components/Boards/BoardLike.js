import React, { useEffect, useState } from "react";
import { useSelector } from "react-redux";
import { selectAccessToken, selectIsLoggedIn } from "../../redux/userSlice";
import {
  addBoardLike,
  cancelBoardLike,
  getBoardLikeStatus,
} from "../../services/boardApi";
import { useQuery } from "@tanstack/react-query";
import axios from "axios";

export const BoardLike = ({ boardId }) => {
  const isLoggedIn = useSelector(selectIsLoggedIn);
  const accessToken = useSelector(selectAccessToken);
  const [likeCount, setLikeCount] = useState("");
  const [isLiked, setIsLiked] = useState(false);
  const boardLikes = useQuery({
    queryKey: ["boardLikes", boardId],
    queryFn: () =>
      axios
        .get(`http://localhost:8080/api/v1/boards/${boardId}/likes`)
        .then((res) => res.data),
    staleTime: 3 * 1000,
  });

  useEffect(() => {
    if (!boardLikes.isLoading) setLikeCount(boardLikes.data);
    if (isLoggedIn) {
      getBoardLikeStatus(boardId, accessToken)
        .then((data) => setIsLiked(data))
        .catch((error) => console.log(error.message));
    }
  }, [boardLikes, isLoggedIn, boardId, accessToken]);

  const handleBoardLike = (e) => {
    e.preventDefault();
    if (!isLoggedIn) {
      alert("로그인이 필요한 서비스입니다."); // 로그인하지 않은 경우 경고 메시지 표시
      return; // 이벤트 핸들러 종료
    }
    addBoardLike(boardId, accessToken)
      .then((data) => {
        setLikeCount(data);
        setIsLiked(true);
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
      setLikeCount(data);
      setIsLiked(false);
    });
  };

  return (
    <div>
      {isLiked ? (
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
      <span className="board-like-count">{likeCount}</span>
    </div>
  );
};
