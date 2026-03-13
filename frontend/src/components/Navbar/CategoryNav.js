import React, { useEffect, useState } from "react";
import { ListGroup, ListGroupItem } from "react-bootstrap";
import { Link } from "react-router-dom";
import "../Styles/css/listGroup.css";
import { getCategoriesV2 } from "../../services/categoryApi";
export const CategoryNav = () => {
  const [categories, setCategories] = useState([]);
  useEffect(() => {
    getCategoriesV2()
      .then((data) => setCategories(data))
      .catch((error) => console.log(error));
  }, []);
  return (
    <ListGroup id="category-list-group">
      <ListGroupItem className="category-list-item">
        <Link className="category-link" to={"/"}>
          전체보기
        </Link>
      </ListGroupItem>
      {categories.map((c) => (
        <ListGroupItem key={c.id} className="category-list-item">
          <Link className="category-link" to={`/${c.name}`}>
            {c.name} ({c.boardsCount})
          </Link>
        </ListGroupItem>
      ))}
    </ListGroup>
  );
};
