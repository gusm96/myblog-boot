import React, { useState, useEffect } from "react";
import { Container } from "../components/Styles/Container/Container.style";
import BoardList from "../components/Boards/BoardList";
import { useSearchParams } from "react-router-dom";
import { getBoardList } from "../services/boardApi";
const Home = () => {
  const [boards, setBoards] = useState([]);
  const [pageCount, setPageCount] = useState("");
  const [page] = useSearchParams("p");
  useEffect(() => {
    getBoardList(page)
      .then((data) => {
        setBoards(data.list);
        setPageCount(data.pageCount);
      })
      .catch((error) => console.log(error));
  }, [page]);

  return (
    <Container>
      <BoardList boards={boards} pageCount={pageCount} />
    </Container>
  );
};

export default Home;
