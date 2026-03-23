import { useQuery } from "@tanstack/react-query";
import { getBoard, getBoardLikeStatus, getComments } from "../services/boardApi";
import { queryKeys } from "../services/queryKeys";

// staleTime만 개별 설정 — gcTime은 전역(24h) 사용
const STALE = {
  board:    5  * 60 * 1000,   // 5분
  comments: 30 * 1000,        // 30초
  likes:    60 * 1000,        // 1분
};

export const useBoardQuery = (boardId) =>
  useQuery({
    queryKey:  queryKeys.boards.detail(boardId),
    queryFn:   () => getBoard(boardId),
    staleTime: STALE.board,
    enabled:   !!boardId,
  });

export const useCommentsQuery = (boardId) =>
  useQuery({
    queryKey:  queryKeys.comments.list(boardId),
    queryFn:   () => getComments(boardId),
    staleTime: STALE.comments,
    enabled:   !!boardId,
  });

export const useLikeStatusQuery = (boardId, isLoggedIn) =>
  useQuery({
    queryKey:  queryKeys.boards.likeStatus(boardId),
    queryFn:   () => getBoardLikeStatus(boardId),
    staleTime: STALE.likes,
    enabled:   !!boardId && isLoggedIn,
  });
