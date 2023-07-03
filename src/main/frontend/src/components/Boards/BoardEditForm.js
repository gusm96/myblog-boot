import axios from "axios";
import React, { useEffect, useState } from "react";
import { useParams } from "react-router-dom";

export const BoardEditForm = () => {
  const [boardId] = useParams();
  const [board, setBoard] = useState(null);
  useEffect(() => {
    axios
      .get(`http://localhost:8080/api/v1/board/${boardId}`)
      .then((res) => res.data)
      .then((data) => setBoard(data))
      .catch((error) => console.log(error));
  }, []);
  return <div>BoardEditForm</div>;
};
