import axios from "axios";
import React, { useEffect, useState } from "react";
import BoardList from "../components/Boards/BoardList";
import { useSearchParams } from "react-router-dom";
import { getBoardList } from "../services/boardApi";

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
    <div>
      <BoardList boards={boards} pageCount={pageCount} adminMode={true} />
    </div>
  );
};
