import axios from "axios";
import React, { useEffect, useState } from "react";
import BoardList from "../components/Boards/BoardList";

export const Management = () => {
  const [boards, setBoards] = useState([]);
  useEffect(() => {
    axios
      .get("http://localhost:8080/api/v1/boards")
      .then((res) => res.data)
      .then((data) => setBoards(data))
      .catch((error) => console.log(error));
  }, []);
  return (
    <div>
      <BoardList boards={boards} />
    </div>
  );
};
