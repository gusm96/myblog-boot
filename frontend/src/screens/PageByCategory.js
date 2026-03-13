import React, { useEffect, useState } from "react";
import { Container } from "react-bootstrap";
import { useParams, useSearchParams } from "react-router-dom";
import { SearchBar } from "../components/SearchBar";
import BoardList from "../components/Boards/BoardList";
import { PageButton } from "../components/Boards/PageButton";
import { getCategoryOfBoardList } from "../services/boardApi";

export const PageByCategory = () => {
  const [page] = useSearchParams("p");
  const { categoryName } = useParams();
  const [boards, setBoards] = useState([]);
  const [pageCount, setPageCount] = useState("");

  useEffect(() => {
    getCategoryOfBoardList(categoryName, page)
      .then((data) => {
        setBoards(data.list);
        setPageCount(data.totalPage);
      })
      .catch((error) => console.log(error));
  }, [categoryName, page]);

  return (
    <Container>
      <SearchBar />
      <BoardList boards={boards} path="/boards" />
      <PageButton pageCount={pageCount} path={`${categoryName}?`} />
    </Container>
  );
};
