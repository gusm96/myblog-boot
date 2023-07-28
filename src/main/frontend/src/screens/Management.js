import axios from "axios";
import React, { useEffect, useState } from "react";
import BoardList from "../components/Boards/BoardList";
import { useSearchParams } from "react-router-dom";

export const Management = () => {
  const [boards, setBoards] = useState([]);
  const [pageCount, setPageCount] = useState("");
  const [page] = useSearchParams("p");
  useEffect(() => {
    axios
      .get(`http://localhost:8080/api/v1/boards?${page}`)
      .then((res) => res.data)
      .then((data) => {
        setBoards(data.list);
        setPageCount(data.pageCount);
      })
      .catch((error) => console.log(error));
  }, [page]);
  return (
    <div>
      <BoardList boards={boards} pageCount />
    </div>
  );
};
