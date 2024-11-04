import React from "react";
import { Outlet } from "react-router-dom";
import { Col, Container, Row } from "react-bootstrap";
import { useSelector } from "react-redux";
import { selectIsLoggedIn } from "../../redux/userSlice";
import { Header } from "./Header";
import { CategoryNavV2 } from "../Navbar/CategoryNavV2";
import VisitorCount from "../VisitorCount";
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
              <CategoryNavV2 />
              <VisitorCount />
            </Col>
          </Row>
        </Container>
      </main>
    </div>
  );
};
