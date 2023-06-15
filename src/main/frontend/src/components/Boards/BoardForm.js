import axios from "axios";
import React, { useEffect, useState } from "react";
//import Editor from "@toast-ui/editor"; // 추후 추가
const BoardForm = () => {
  const [categories, setCategories] = useState([]);
  useEffect(() => {
    axios
      .get("/api/v1/categories")
      .then((response) => response.data)
      .then((data) => setCategories(data))
      .catch((error) => console.log(error));
  }, []);

  return (
    <div>
      <form method="post">
        <select id="category" name="category">
          <option>선택하세요.</option>
          {categories.map((category) => (
            <option value={category.id}>{category.name}</option>
          ))}
        </select>
        <input
          id="title"
          name="title"
          type="text"
          placeholder="제목을 입력하세요."
          required
        />
        <input
          id="content"
          name="content"
          type="text"
          placeholder="내용을 입력하세요."
          required
        />
        <button type="submit">업로드</button>
      </form>
    </div>
  );
};

export default BoardForm;
