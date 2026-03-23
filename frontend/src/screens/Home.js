import React, { useCallback } from "react";
import { Container } from "../components/Styles/Container/Container.style";
import BoardList from "../components/Boards/BoardList";
import { getBoardList } from "../services/boardApi";
import { SearchBar } from "../components/SearchBar";
import { useInfiniteQuery } from "@tanstack/react-query";
import { ErrorMessage } from "../components/ErrorMessage";
import { queryKeys } from "../services/queryKeys";
import { useInfiniteScroll } from "../hooks/useInfiniteScroll";
import "../components/Styles/css/fonts.css";

const Home = () => {
  const {
    data,
    isPending,
    error,
    fetchNextPage,
    hasNextPage,
    isFetchingNextPage,
  } = useInfiniteQuery({
    queryKey: queryKeys.boards.lists(),
    queryFn: ({ pageParam }) => getBoardList(pageParam),
    initialPageParam: 1,
    getNextPageParam: (lastPage, allPages) => {
      const nextPage = allPages.length + 1;
      return nextPage <= lastPage.totalPage ? nextPage : undefined;
    },
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
  if (error) return <ErrorMessage message={error.message} />;

  return (
    <Container>
      <SearchBar />
      <BoardList boards={boards} path="/boards" />
      {/* 이 div가 뷰포트에 진입하면 다음 페이지 요청 */}
      <div ref={triggerRef} />
      {isFetchingNextPage && (
        <div style={{ textAlign: "center", padding: "1rem", color: "var(--text-muted)" }}>
          불러오는 중...
        </div>
      )}
    </Container>
  );
};

export default Home;
