import React, { useEffect, useState } from "react";
import axios from "axios";
import { useParams } from "react-router-dom";
import { Container } from "../Styles/Container/Container.style";
import { Header, MainHeader } from "../Styles/Header/Header.style";
import moment from "moment";
const BoardDetail = () => {
  const { boardId } = useParams();
  const [board, setBoard] = useState("");
  useEffect(() => {
    axios
      .get("http://localhost:8080/api/v1/board/" + boardId)
      .then((response) => response.data)
      .then((data) => setBoard(data))
      .catch((error) => console.log(error));
  }, []);
  const uploadDate = moment(board.uploadDate).format("YYYY-MM-DD");
  return (
    <Container>
      <Header>
        <MainHeader>
          <div>
            <h1>{board.title}</h1>
            <p>{board.content}</p>
            <p>{uploadDate}</p>
          </div>
        </MainHeader>
      </Header>
    </Container>
  );
};

export default BoardDetail;
