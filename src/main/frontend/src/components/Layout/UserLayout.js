import React from "react";
import { Outlet } from "react-router-dom";
import { Col, Container, Row } from "react-bootstrap";
import { CategoryNav } from "../Navbar/CategoryNav";
import { useSelector } from "react-redux";
import { selectIsLoggedIn } from "../../redux/userSlice";
import { Header } from "./Header";
export const UserLayout = () => {
  const isLoggedIn = useSelector(selectIsLoggedIn);

  return (
    <div>
      <Header headerTitle={"MyBlog"} isLoggedIn={isLoggedIn} />
      <main
        className="inner"
        style={{
          marginTop: "30px",
        }}
      >
        <Container>
          <Row>
            <Col md={9}>
              <Outlet />
            </Col>
            <Col md={3}>
              <CategoryNav />
            </Col>
          </Row>
        </Container>
      </main>
    </div>
  );
};
