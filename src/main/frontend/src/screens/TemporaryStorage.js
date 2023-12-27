import React, { useEffect, useState } from "react";
import { useSelector } from "react-redux";
import { selectAccessToken } from "../redux/userSlice";
import { getDeletedBoards } from "../services/boardApi";
import BoardList from "../components/Boards/BoardList";
import { useSearchParams } from "react-router-dom";

export const TemporaryStorage = () => {
  const [boards, setBoards] = useState([]);
  const [pageCount, setPageCount] = useState("");
  const [page] = useSearchParams("p");
  const accessToken = useSelector(selectAccessToken);
  useEffect(() => {
    getDeletedBoards(accessToken, page)
      .then((data) => {
        setBoards(data.list);
        setPageCount(data.totalPage);
      })
      .catch((error) => console.log(error));
  }, [accessToken, page]);
  return (
    <div>
      <BoardList boards={boards} pageCount={pageCount} adminMode={true} />
    </div>
  );
};
