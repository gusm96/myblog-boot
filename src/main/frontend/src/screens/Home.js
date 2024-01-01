import React, { useState, useEffect } from "react";
import { Container } from "../components/Styles/Container/Container.style";
import BoardList from "../components/Boards/BoardList";
import { useParams, useSearchParams } from "react-router-dom";
import { getBoardList, getCategoryOfBoardList } from "../services/boardApi";
import { SearchBar } from "../components/SearchBar";
import { PageButton } from "../components/Boards/PageButton";
import "../components/Styles/css/fonts.css";
const Home = () => {
  const [boards, setBoards] = useState([]);
  const [pageCount, setPageCount] = useState("");
  const { categoryName } = useParams();
  const [page] = useSearchParams("p");

  useEffect(() => {
    if (categoryName) {
      getCategoryOfBoardList(categoryName).then((data) => {
        setBoards(data.list);
        setPageCount(data.totalPage);
      });
    } else {
      getBoardList(page)
        .then((data) => {
          setBoards(data.list);
          setPageCount(data.totalPage);
        })
        .catch((error) => console.log(error));
    }
  }, [categoryName, page]);

  return (
    <Container>
      <SearchBar />
      <BoardList boards={boards} path="/boards" />
      <PageButton pageCount={pageCount} path="/boards?" />
    </Container>
  );
};

export default Home;
