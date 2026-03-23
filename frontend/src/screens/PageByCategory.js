import React, { useCallback } from "react";
import { Container } from "react-bootstrap";
import { useParams } from "react-router";
import { SearchBar } from "../components/SearchBar";
import BoardList from "../components/Boards/BoardList";
import { getCategoryOfBoardList } from "../services/boardApi";
import { useInfiniteQuery } from "@tanstack/react-query";
import { queryKeys } from "../services/queryKeys";
import { useInfiniteScroll } from "../hooks/useInfiniteScroll";

export const PageByCategory = () => {
  const { categoryName } = useParams();

  const {
    data,
    isPending,
    fetchNextPage,
    hasNextPage,
    isFetchingNextPage,
  } = useInfiniteQuery({
    queryKey: queryKeys.categories.boards(categoryName),
    queryFn: ({ pageParam }) => getCategoryOfBoardList(categoryName, pageParam),
    initialPageParam: 1,
    getNextPageParam: (lastPage, allPages) => {
      const nextPage = allPages.length + 1;
      return nextPage <= lastPage.totalPage ? nextPage : undefined;
    },
    enabled: !!categoryName,
  });

  const handleLoadMore = useCallback(() => {
    if (hasNextPage && !isFetchingNextPage) fetchNextPage();
  }, [hasNextPage, isFetchingNextPage, fetchNextPage]);

  const triggerRef = useInfiniteScroll(
    handleLoadMore,
    hasNextPage && !isFetchingNextPage
  );

  const boards = data?.pages.flatMap((page) => page.list) ?? [];

  if (isPending) return <div>Loading...</div>;

  return (
    <Container>
      <SearchBar />
      <BoardList boards={boards} path="/boards" />
      <div ref={triggerRef} />
      {isFetchingNextPage && (
        <div style={{ textAlign: "center", padding: "1rem", color: "var(--text-muted)" }}>
          불러오는 중...
        </div>
      )}
    </Container>
  );
};
