import React from "react";
import { Container } from "react-bootstrap";
import { useParams, useSearchParams } from "react-router";
import { SearchBar } from "../components/SearchBar";
import BoardList from "../components/Boards/BoardList";
import { PageButton } from "../components/Boards/PageButton";
import { getCategoryOfBoardList } from "../services/boardApi";
import { useQuery, keepPreviousData } from "@tanstack/react-query";
import { queryKeys } from "../services/queryKeys";

export const PageByCategory = () => {
  const [page] = useSearchParams("p");
  const { categoryName } = useParams();

  const { data, isPending, isPlaceholderData } = useQuery({
    queryKey: queryKeys.categories.boards(categoryName, page.toString()),
    queryFn:  () => getCategoryOfBoardList(categoryName, page),
    enabled:  !!categoryName,
    placeholderData: keepPreviousData,
  });

  if (isPending) return <div>Loading...</div>;

  return (
    <Container style={{ opacity: isPlaceholderData ? 0.6 : 1 }}>
      <SearchBar />
      <BoardList boards={data?.list ?? []} path="/boards" />
      <PageButton pageCount={data?.totalPage ?? 0} path={`${categoryName}?`} />
    </Container>
  );
};
