import React from "react";
import { useLocation } from "react-router";
import { getSearchedBoardList } from "../services/boardApi";
import { Container } from "../components/Styles/Container/Container.style";
import { SearchBar } from "../components/SearchBar";
import BoardList from "../components/Boards/BoardList";
import { PageButton } from "../components/Boards/PageButton";
import { useQuery, keepPreviousData } from "@tanstack/react-query";
import { queryKeys } from "../services/queryKeys";

export const SearchPage = () => {
  const location = useLocation();
  const params   = new URLSearchParams(location.search);
  const type     = params.get("type");
  const contents = params.get("contents");
  const page     = params.get("p") || 1;

  const { data, isPlaceholderData } = useQuery({
    queryKey: queryKeys.search.results(type, contents, page),
    queryFn:  () => getSearchedBoardList(type, contents, page),
    enabled:  !!(type && contents),
    placeholderData: keepPreviousData,
  });

  return (
    <Container style={{ opacity: isPlaceholderData ? 0.6 : 1 }}>
      <SearchBar type={type} contents={contents} />
      <BoardList boards={data?.list ?? []} path="/boards" />
      <PageButton
        pageCount={data?.totalPage ?? 0}
        path={`search?type=${type}&contents=${contents}&`}
      />
    </Container>
  );
};
