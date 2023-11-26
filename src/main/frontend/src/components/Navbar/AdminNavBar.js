import React from "react";
import { ListGroup, ListGroupItem } from "react-bootstrap";
import { Link } from "react-router-dom";
import "../Styles/css/listGroup.css";
export const AdminNavBar = () => {
  return (
    <ListGroup id="list-group">
      <ListGroupItem>
        <Link className="list-item" to="/management">
          전체 게시글
        </Link>
      </ListGroupItem>
      <ListGroupItem>
        <Link className="list-item" to="/management/new-post">
          게시글 작성
        </Link>
      </ListGroupItem>
      <ListGroupItem>
        <Link className="list-item" to="/management/category">
          카테고리 관리
        </Link>
      </ListGroupItem>
    </ListGroup>
  );
};
