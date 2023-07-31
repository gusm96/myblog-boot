import React from "react";
import NavBarElements from "../Navbar/NavBarElements";
import { Outlet } from "react-router-dom";
import { Col, Container, Row } from "react-bootstrap";
import { CategoryNav } from "../Navbar/CategoryNav";

export const UserLayout = () => {
  return (
    <div>
      <header>
        <NavBarElements />
      </header>
      <main>
        <Container>
          <Row>
            <Col xs="2"></Col>
            <Col xs="8">
              <Outlet />
            </Col>
            <Col xs="2">
              <CategoryNav />
            </Col>
          </Row>
        </Container>
      </main>
    </div>
  );
};
