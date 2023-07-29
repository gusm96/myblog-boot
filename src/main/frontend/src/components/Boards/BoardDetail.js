import React, { useEffect, useState } from "react";
import axios from "axios";
import { useParams } from "react-router-dom";
import moment from "moment";
import { BOARD_CUD } from "../../apiConfig";
const BoardDetail = () => {
  const { boardId } = useParams();
  const [board, setBoard] = useState("");
  useEffect(() => {
    axios
      .get(`${BOARD_CUD}/${boardId}`)
      .then((response) => response.data)
      .then((data) => setBoard(data))
      .catch((error) => console.log(error));
  }, []);
  const uploadDate = moment(board.uploadDate).format("YYYY-MM-DD");
  return (
    <div>
      <h1>{board.title}</h1>
      <div>{board.content}</div>
      <p>{uploadDate}</p>
    </div>
  );
};

export default BoardDetail;
