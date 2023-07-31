import React from "react";
import { Col, Container, Row } from "react-bootstrap";
import { Outlet } from "react-router-dom";
import { AdminNavBar } from "../Navbar/AdminNavBar";

export const AdminLayout = () => {
  return (
    <div>
      <header>
        <h1>관리자 모드</h1>
        <hr></hr>
      </header>
      <main>
        <Container>
          <Row>
            <Col xs="3">
              <AdminNavBar />
            </Col>
            <Col xs="9">
              <Outlet />
            </Col>
          </Row>
        </Container>
      </main>
    </div>
  );
};
