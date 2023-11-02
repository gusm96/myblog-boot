import React from "react";
import { Nav } from "react-bootstrap";

export const AdminNavBar = () => {
  return (
    <Nav>
      <Nav.Link href={"/management/new-post"}>게시글 작성</Nav.Link>
      <Nav.Link></Nav.Link>
      <Nav.Link></Nav.Link>
    </Nav>
  );
};
