import { useQuery } from "@tanstack/react-query";
import { getBoard, getBoardLikeStatus, getComments } from "../services/boardApi";
import { queryKeys } from "../services/queryKeys";

const STALE = {
  board:    5  * 60 * 1000,   // 5분
  comments: 30 * 1000,        // 30초
  likes:    60 * 1000,        // 1분
};

const GC = {
  board:    15 * 60 * 1000,   // 15분
  comments:  5 * 60 * 1000,   // 5분
  likes:     5 * 60 * 1000,   // 5분
};

export const useBoardQuery = (boardId) =>
  useQuery({
    queryKey: queryKeys.boards.detail(boardId),
    queryFn:  () => getBoard(boardId),
    staleTime: STALE.board,
    gcTime:    GC.board,
    enabled:   !!boardId,
  });

export const useCommentsQuery = (boardId) =>
  useQuery({
    queryKey: queryKeys.comments.list(boardId),
    queryFn:  () => getComments(boardId),
    staleTime: STALE.comments,
    gcTime:    GC.comments,
    enabled:   !!boardId,
  });

export const useLikeStatusQuery = (boardId, isLoggedIn) =>
  useQuery({
    queryKey: queryKeys.boards.likeStatus(boardId),
    queryFn:  () => getBoardLikeStatus(boardId),
    staleTime: STALE.likes,
    gcTime:    GC.likes,
    enabled:   !!boardId && isLoggedIn,
  });
