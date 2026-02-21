import { useQuery } from "@tanstack/react-query";
import {
  getBoard,
  getBoardLikeStatus,
  getComments,
} from "../services/boardApi";

export const useBoardQuery = (boardId) =>
  useQuery({
    queryKey: ["board", boardId],
    queryFn: () => getBoard(boardId),
    staleTime: 5 * 1000,
    gcTime: 5 * 1000,
    refetchOnMount: true,
    refetchOnReconnect: true,
  });

export const useCommentsQuery = (boardId) =>
  useQuery({
    queryKey: ["comments", boardId],
    queryFn: () => getComments(boardId),
    staleTime: 5 * 1000,
    gcTime: 5 * 1000,
    refetchOnMount: true,
    refetchOnReconnect: true,
  });

export const useLikeStatusQuery = (boardId, accessToken, isLoggedIn) =>
  useQuery({
    queryKey: ["likeStatus", boardId],
    queryFn: () =>
      isLoggedIn ? getBoardLikeStatus(boardId, accessToken) : null,
    staleTime: 5 * 1000,
    gcTime: 5 * 1000,
    refetchOnMount: true,
    refetchOnReconnect: true,
    enabled: isLoggedIn,
  });
