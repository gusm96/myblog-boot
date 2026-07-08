"use client";

import { useCallback } from "react";
import { useInfiniteQuery } from "@tanstack/react-query";
import { PostList } from "./PostList";
import { useInfiniteScroll } from "@/hooks/useInfiniteScroll";
import { queryKeys } from "@/lib/queryKeys";
import { clientGetPostList, clientGetTagPostList } from "@/lib/postApi";
import type { PostListResponse } from "@/types";

type QueryType = "posts" | "tag";

interface PostListInfiniteProps {
  queryType: QueryType;
  tagSlug?: string;
}

export function PostListInfinite({ queryType, tagSlug }: PostListInfiniteProps) {
  const queryKey =
    queryType === "tag" && tagSlug
      ? queryKeys.tags.posts(tagSlug)
      : queryKeys.posts.lists();

  const fetcher = (page: number): Promise<PostListResponse> =>
    queryType === "tag" && tagSlug
      ? clientGetTagPostList(tagSlug, page)
      : clientGetPostList(page);

  const { data, fetchNextPage, hasNextPage, isFetchingNextPage } =
    useInfiniteQuery<PostListResponse>({
      queryKey,
      queryFn: ({ pageParam }) => fetcher(pageParam as number),
      initialPageParam: 1,
      getNextPageParam: (lastPage, allPages) => {
        const nextPage = allPages.length + 1;
        return nextPage <= lastPage.totalPage ? nextPage : undefined;
      },
      staleTime: 1000 * 60,
    });

  const handleLoadMore = useCallback(() => {
    if (hasNextPage && !isFetchingNextPage) fetchNextPage();
  }, [hasNextPage, isFetchingNextPage, fetchNextPage]);

  const triggerRef = useInfiniteScroll(
    handleLoadMore,
    hasNextPage && !isFetchingNextPage
  );

  const posts = data?.pages.flatMap((page) => page.list) ?? [];

  return (
    <>
      <PostList posts={posts} />
      <div ref={triggerRef} />
      {isFetchingNextPage && (
        <div
          style={{
            textAlign: "center",
            padding: "1rem",
            color: "var(--text-muted)",
            fontFamily: "var(--font-mono)",
            fontSize: "0.82rem",
          }}
        >
          // loading...
        </div>
      )}
    </>
  );
}
