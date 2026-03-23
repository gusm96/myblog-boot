import React from "react";
import { Outlet } from "react-router";
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
      <Header
        headerTitle={"Moya's Research Institute"}
        isLoggedIn={isLoggedIn}
      />
      <main className="layout-main">
        <Container>
          <Row>
            {/* Main content — full width on mobile, 9 cols on desktop */}
            <Col xs={12} md={9} className="order-1">
              <Outlet />
            </Col>
            {/* Sidebar — below content on mobile, 3 cols sticky on desktop */}
            <Col xs={12} md={3} className="order-2">
              <div className="sidebar-sticky">
                <CategoryNavV2 />
                <VisitorCount />
              </div>
            </Col>
          </Row>
        </Container>
      </main>
    </div>
  );
};
