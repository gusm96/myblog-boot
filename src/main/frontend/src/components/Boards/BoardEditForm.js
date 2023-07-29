import axios from "axios";
import React, { useEffect, useState } from "react";
import { useParams } from "react-router-dom";
import { BOARD_CUD } from "../../apiConfig";

export const BoardEditForm = () => {
  const [boardId] = useParams();
  const [board, setBoard] = useState(null);
  useEffect(() => {
    axios
      .get(`${BOARD_CUD}/${boardId}`)
      .then((res) => res.data)
      .then((data) => setBoard(data))
      .catch((error) => console.log(error));
  }, []);
  return <div>BoardEditForm</div>;
};
