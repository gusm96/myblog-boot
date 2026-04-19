"use client";

import { useSelector } from "react-redux";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { selectIsLoggedIn } from "@/store/userSlice";
import {
  addPostLike,
  cancelPostLike,
  getPostLikeCount,
  getPostLikeStatus,
} from "@/lib/postApi";
import { queryKeys } from "@/lib/queryKeys";

interface PostLikeProps {
  postId: number;
  initialLikes?: number;
}

export function PostLike({ postId, initialLikes = 0 }: PostLikeProps) {
  const isLoggedIn = useSelector(selectIsLoggedIn);
  const queryClient = useQueryClient();

  const { data: likeCount = initialLikes } = useQuery({
    queryKey: queryKeys.posts.likes(postId),
    queryFn: () => getPostLikeCount(postId),
    initialData: initialLikes,
    staleTime: 60 * 1000,
    gcTime: 5 * 60 * 1000,
  });

  const { data: isLiked = false } = useQuery({
    queryKey: queryKeys.posts.likeStatus(postId),
    queryFn: () => getPostLikeStatus(postId),
    staleTime: 5 * 60 * 1000,
    gcTime: 10 * 60 * 1000,
    enabled: !!postId && isLoggedIn,
  });

  const addLikeMutation = useMutation({
    mutationFn: () => addPostLike(postId),
    onSuccess: (newCount) => {
      queryClient.setQueryData(queryKeys.posts.likes(postId), newCount);
      queryClient.setQueryData(queryKeys.posts.likeStatus(postId), true);
    },
  });

  const cancelLikeMutation = useMutation({
    mutationFn: () => cancelPostLike(postId),
    onSuccess: (newCount) => {
      queryClient.setQueryData(queryKeys.posts.likes(postId), newCount);
      queryClient.setQueryData(queryKeys.posts.likeStatus(postId), false);
    },
  });

  const handlePostLike = (e: React.MouseEvent) => {
    e.preventDefault();
    if (!isLoggedIn) {
      alert("로그인이 필요한 서비스입니다.");
      return;
    }
    addLikeMutation.mutate();
  };

  const handlePostLikeCancel = (e: React.MouseEvent) => {
    e.preventDefault();
    if (!isLoggedIn) {
      alert("로그인이 필요한 서비스입니다.");
      return;
    }
    cancelLikeMutation.mutate();
  };

  return (
    <div>
      {isLiked ? (
        <i
          className="fa-solid fa-heart post-like-status"
          onClick={handlePostLikeCancel}
        />
      ) : (
        <i
          className="fa-regular fa-heart post-like-status"
          onClick={handlePostLike}
        />
      )}
      <span className="post-like-count">{likeCount}</span>
    </div>
  );
}
