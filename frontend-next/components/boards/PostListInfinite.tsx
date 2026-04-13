"use client";

import { useCallback } from "react";
import { useInfiniteQuery } from "@tanstack/react-query";
import { PostList } from "./PostList";
import { useInfiniteScroll } from "@/hooks/useInfiniteScroll";
import { queryKeys } from "@/lib/queryKeys";
import { getPostList, getCategoryPostList } from "@/lib/postApi";
import type { PostListResponse } from "@/types";

type QueryType = "posts" | "category";

interface PostListInfiniteProps {
  /** 서버에서 사전 패치한 1페이지 데이터 */
  initialData: PostListResponse;
  /** 쿼리 유형: 'posts' = 전체, 'category' = 카테고리별 */
  queryType: QueryType;
  /** queryType === 'category' 일 때 필요 */
  categoryName?: string;
}

/**
 * 무한 스크롤 게시글 목록 — Client Component
 * 서버 → 클라이언트로 함수를 전달할 수 없으므로 queryType으로 fetcher 결정
 */
export function PostListInfinite({
  initialData,
  queryType,
  categoryName,
}: PostListInfiniteProps) {
  const queryKey =
    queryType === "category" && categoryName
      ? queryKeys.categories.posts(categoryName)
      : queryKeys.posts.lists();

  const fetcher = (page: number): Promise<PostListResponse> =>
    queryType === "category" && categoryName
      ? getCategoryPostList(categoryName, page)
      : getPostList(page);

  const { data, fetchNextPage, hasNextPage, isFetchingNextPage } =
    useInfiniteQuery<PostListResponse>({
      queryKey,
      queryFn: ({ pageParam }) => fetcher(pageParam as number),
      initialPageParam: 1,
      getNextPageParam: (lastPage, allPages) => {
        const nextPage = allPages.length + 1;
        return nextPage <= lastPage.totalPage ? nextPage : undefined;
      },
      initialData: {
        pages: [initialData],
        pageParams: [1],
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
