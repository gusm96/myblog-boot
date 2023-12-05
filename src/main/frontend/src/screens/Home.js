import React, { useState, useEffect } from "react";
import { Container } from "../components/Styles/Container/Container.style";
import BoardList from "../components/Boards/BoardList";
import { useParams, useSearchParams } from "react-router-dom";
import { getBoardList, getCategoryOfBoardList } from "../services/boardApi";
import "../components/Styles/css/fonts.css";
const Home = () => {
  const [boards, setBoards] = useState([]);
  const { categoryName } = useParams();
  const [pageCount, setPageCount] = useState("");
  const [page] = useSearchParams("p");
  useEffect(() => {
    if (categoryName) {
      getCategoryOfBoardList(categoryName, page)
        .then((data) => {
          setBoards(data.list);
          setPageCount(data.totalPage);
        })
        .catch((error) => console.log(error));
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
      <BoardList
        boards={boards}
        pageCount={pageCount}
        categoryName={categoryName ? categoryName : null}
      />
    </Container>
  );
};

export default Home;
