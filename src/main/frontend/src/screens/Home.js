import React, { useState, useEffect } from "react";
import { Container } from "../components/Styles/Container/Container.style";
import { Header, MainHeader } from "../components/Styles/Header/Header.style";
import axios from "axios";
import BoardList from "../components/Boards/BoardList";
const Home = () => {
  const [boards, setBoards] = useState([]);

  useEffect(() => {
    axios
      .get("/api/v1/boards")
      .then((responce) => responce.data)
      .then((data) => setBoards(data))
      .catch((error) => console.log(error));
  }, []);

  return (
    <Container>
      <Header>
        <MainHeader>
          <BoardList boards={boards} />
        </MainHeader>
      </Header>
    </Container>
  );
};

export default Home;
