import React from "react";
import { ListGroup, ListGroupItem } from "react-bootstrap";
import { Link } from "react-router-dom";
export const AdminNavBar = () => {
  return (
    <ListGroup id="category-list-group">
      <ListGroupItem className="category-list-item">
        <Link className="category-link" to="/management">
          전체 게시글
        </Link>
      </ListGroupItem>
      <ListGroupItem className="category-list-item">
        <Link className="category-link" to="/management/new-post">
          게시글 작성
        </Link>
      </ListGroupItem>
      <ListGroupItem className="category-list-item">
        <Link className="category-link" to="/management/categories">
          카테고리 관리
        </Link>
      </ListGroupItem>
    </ListGroup>
  );
};
