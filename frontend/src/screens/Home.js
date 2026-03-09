import React from "react";
import { Container } from "../components/Styles/Container/Container.style";
import BoardList from "../components/Boards/BoardList";
import { useSearchParams } from "react-router-dom";
import { getBoardList } from "../services/boardApi";
import { SearchBar } from "../components/SearchBar";
import { PageButton } from "../components/Boards/PageButton";
import "../components/Styles/css/fonts.css";
import { useQuery } from "@tanstack/react-query";
import { ErrorMessage } from "../components/ErrorMessage";
const Home = () => {
  const [page] = useSearchParams("p");
  const { data, isPending, error } = useQuery({
    queryKey: ["boardList", page],
    queryFn: () => getBoardList(page),
    staleTime: 5 * 1000,
    gcTime: 5 * 1000,
  });

  if (isPending) return <div>Loding...</div>;

  if (error) return <ErrorMessage message={error.message} />;

  return (
    <Container>
      <SearchBar />
      <BoardList boards={data.list} path="/boards" />
      <PageButton pageCount={data.totalPage} path="/boards?" />
    </Container>
  );
};

export default Home;
