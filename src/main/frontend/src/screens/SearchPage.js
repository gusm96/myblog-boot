import React, { useEffect, useState } from "react";
import { useLocation } from "react-router-dom";
import { getSearchedBoardList } from "../services/boardApi";
import { Container } from "../components/Styles/Container/Container.style";
import { SearchBar } from "../components/SearchBar";
import BoardList from "../components/Boards/BoardList";
import { PageButton } from "../components/Boards/PageButton";

export const SearchPage = () => {
  const [boards, setBoards] = useState([]);
  const [pageCount, setPageCount] = useState("");
  const location = useLocation();
  const params = new URLSearchParams(location.search);
  const type = params.get("type");
  const contents = params.get("contents");
  const page = params.get("p") || 1;
  useEffect(() => {
    getSearchedBoardList(type, contents, page)
      .then((data) => {
        setBoards(data.list);
        setPageCount(data.totalPage);
      })
      .catch((error) => console.log(error));
  }, [type, contents, page]);
  return (
    <Container>
      <SearchBar type={type} contents={contents} />
      <BoardList boards={boards} path={"/boards"} />
      <PageButton
        pageCount={pageCount}
        path={`search?type=${type}&contents=${contents}&`}
      />
    </Container>
  );
};
