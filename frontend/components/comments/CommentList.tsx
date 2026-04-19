"use client";

import { useQuery } from "@tanstack/react-query";
import { queryKeys } from "@/lib/queryKeys";
import { getComments } from "@/lib/postApi";
import { Comment } from "./Comment";
import type { Comment as CommentType } from "@/types";

interface CommentListProps {
  postId: number;
}

export function CommentList({ postId }: CommentListProps) {
  const { isLoading, error, data } = useQuery<CommentType[]>({
    queryKey: queryKeys.comments.list(postId),
    queryFn: () => getComments(postId),
    staleTime: 30 * 1000,
    gcTime: 5 * 60 * 1000,
  });

  if (isLoading) {
    return (
      <section className="comment-section">
        <p className="comment-state-msg">// loading comments...</p>
      </section>
    );
  }
  if (error) {
    return (
      <section className="comment-section">
        <p className="comment-state-msg comment-state-msg--error">
          // error: {(error as Error).message}
        </p>
      </section>
    );
  }

  const count = data?.length ?? 0;

  return (
    <section className="comment-section">
      <h3 className="comment-section-title">
        {count} {count === 1 ? "comment" : "comments"}
      </h3>
      <ul className="comment-list">
        {data?.map((comment) => (
          <li key={comment.id} className="comment-item">
            <Comment postId={postId} comment={comment} />
          </li>
        ))}
      </ul>
    </section>
  );
}
