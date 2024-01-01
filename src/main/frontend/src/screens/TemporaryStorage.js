import React, { useEffect, useState } from "react";
import { useSelector } from "react-redux";
import { selectAccessToken } from "../redux/userSlice";
import { getDeletedBoards } from "../services/boardApi";
import BoardList from "../components/Boards/BoardList";
import { useSearchParams } from "react-router-dom";
import { PageButton } from "../components/Boards/PageButton";

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
      <h3>휴지통</h3>
      <p
        style={{
          fontSize: "0.75em",
          color: "#999",
        }}
      >
        삭제일로 부터 15일 이후 자동 영구 삭제됩니다.
      </p>
      <hr></hr>
      <BoardList boards={boards} path={"/management/boards"} />
      <PageButton
        pageCount={pageCount}
        path={`/management/temporary-storage?`}
      />
    </div>
  );
};
