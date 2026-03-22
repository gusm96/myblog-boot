import React from "react";
import BoardList from "../components/Boards/BoardList";
import { useSearchParams } from "react-router";
import { getBoardList } from "../services/boardApi";
import { PageButton } from "../components/Boards/PageButton";
import { Container } from "react-bootstrap";
import { useQuery, keepPreviousData } from "@tanstack/react-query";
import { queryKeys } from "../services/queryKeys";

export const Management = () => {
  const [page] = useSearchParams("p");

  const { data, isPending, isPlaceholderData } = useQuery({
    queryKey: queryKeys.admin.boards(page.toString()),
    queryFn:  () => getBoardList(page),
    staleTime: 3  * 60 * 1000,
    gcTime:    10 * 60 * 1000,
    placeholderData: keepPreviousData,
  });

  if (isPending) return <div>Loading...</div>;

  return (
    <Container style={{ opacity: isPlaceholderData ? 0.6 : 1 }}>
      <BoardList boards={data?.list ?? []} path="/management/boards" />
      <PageButton pageCount={data?.totalPage ?? 0} path="/management/boards?" />
    </Container>
  );
};
