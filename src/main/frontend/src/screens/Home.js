import React, { useState, useEffect } from "react";
import { Container } from "../components/Styles/Container/Container.style";
import { Header, MainHeader } from "../components/Styles/Header/Header.style";
import axios from "axios";
import BoardList from "../components/Boards/BoardList";
import { useSearchParams } from "react-router-dom";
import { BOARD_LIST } from "../apiConfig";
const Home = () => {
  const [boards, setBoards] = useState([]);
  const [pageCount, setPageCount] = useState("");
  const [page] = useSearchParams("p");
  useEffect(() => {
    axios
      .get(`${BOARD_LIST}?${page}`)
      .then((res) => res.data)
      .then((data) => {
        setBoards(data.list);
        setPageCount(data.pageCount);
      })
      .catch((error) => console.log(error));
  }, [page]);

  return (
    <Container>
      <Header>
        <MainHeader>
          <BoardList boards={boards} pageCount={pageCount} />
        </MainHeader>
      </Header>
    </Container>
  );
};

export default Home;
