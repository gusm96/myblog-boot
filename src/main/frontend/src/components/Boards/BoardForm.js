import axios from "axios";
import React, { useEffect, useState } from "react";

const BoardForm = () => {
  const [categories, setCategories] = useState([]);
  useEffect(() => {
    axios
      .get("/api/v1/categories")
      .then((response) => response.data)
      .then((data) => setCategories(data))
      .catch((error) => console.log(error));
  }, []);

  return <div></div>;
};

export default BoardForm;
