import React from "react";
import { useSelector } from "react-redux";
import { selectIsLoggedIn } from "../../redux/userSlice";
import {
  addBoardLike,
  cancelBoardLike,
  getBoardLikeCount,
  getBoardLikeStatus,
} from "../../services/boardApi";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { queryKeys } from "../../services/queryKeys";

export const BoardLike = ({ boardId }) => {
  const isLoggedIn  = useSelector(selectIsLoggedIn);
  const queryClient = useQueryClient();

  const { data: likeCount = 0 } = useQuery({
    queryKey: queryKeys.boards.likes(boardId),
    queryFn:  () => getBoardLikeCount(boardId),
    staleTime: 60 * 1000,
    gcTime:     5 * 60 * 1000,
  });

  const { data: isLiked = false } = useQuery({
    queryKey: queryKeys.boards.likeStatus(boardId),
    queryFn:  () => getBoardLikeStatus(boardId),
    staleTime: 5  * 60 * 1000,
    gcTime:    10 * 60 * 1000,
    enabled:   !!boardId && isLoggedIn,
  });

  const addLikeMutation = useMutation({
    mutationFn: () => addBoardLike(boardId),
    onSuccess: (newCount) => {
      queryClient.setQueryData(queryKeys.boards.likes(boardId),      newCount);
      queryClient.setQueryData(queryKeys.boards.likeStatus(boardId), true);
    },
  });

  const cancelLikeMutation = useMutation({
    mutationFn: () => cancelBoardLike(boardId),
    onSuccess: (newCount) => {
      queryClient.setQueryData(queryKeys.boards.likes(boardId),      newCount);
      queryClient.setQueryData(queryKeys.boards.likeStatus(boardId), false);
    },
  });

  const handleBoardLike = (e) => {
    e.preventDefault();
    if (!isLoggedIn) { alert("로그인이 필요한 서비스입니다."); return; }
    addLikeMutation.mutate();
  };

  const handleBoardLikeCancel = (e) => {
    e.preventDefault();
    if (!isLoggedIn) { alert("로그인이 필요한 서비스입니다."); return; }
    cancelLikeMutation.mutate();
  };

  return (
    <div>
      {isLiked ? (
        <i className="fa-solid fa-heart board-like-status"  onClick={handleBoardLikeCancel} />
      ) : (
        <i className="fa-regular fa-heart board-like-status" onClick={handleBoardLike} />
      )}
      <span className="board-like-count">{likeCount}</span>
    </div>
  );
};
