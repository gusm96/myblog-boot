import React, { useEffect, useState } from "react";
import BoardList from "../components/Boards/BoardList";
import { useSearchParams } from "react-router-dom";
import { getBoardList } from "../services/boardApi";
import { PageButton } from "../components/Boards/PageButton";
import { Container } from "react-bootstrap";

export const Management = () => {
  const [boards, setBoards] = useState([]);
  const [pageCount, setPageCount] = useState("");
  const [page] = useSearchParams("p");
  useEffect(() => {
    getBoardList(page)
      .then((data) => {
        setBoards(data.list);
        setPageCount(data.totalPage);
      })
      .catch((error) => console.log(error));
  }, [page]);
  return (
    <Container>
      <BoardList boards={boards} path={`/management/boards`} />
      <PageButton pageCount={pageCount} path={"/management/boards?"} />
    </Container>
  );
};
