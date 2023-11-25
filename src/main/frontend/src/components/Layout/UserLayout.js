import React from "react";
import { Outlet } from "react-router-dom";
import { Col, Container, Nav, Navbar, Row } from "react-bootstrap";
import { CategoryNav } from "../Navbar/CategoryNav";
import "../Styles/css/header.css";
import { useDispatch, useSelector } from "react-redux";
import { selectIsLoggedIn } from "../../redux/userSlice";
import { userLogout } from "../../redux/authAction";
import { logout } from "../../services/authApi";
export const UserLayout = () => {
  const isLoggedIn = useSelector(selectIsLoggedIn);
  const dispatch = useDispatch();
  const handleLogout = () => {
    if (window.confirm("정말 로그아웃 하시겠습니까?")) {
      dispatch(userLogout());
      logout();
    }
  };
  return (
    <div>
      <div id="header">
        <Navbar collapseOnSelect expand="lg" variant="light" className="inner">
          <Container>
            <Navbar.Brand href="/">
              <h1>MyBlog</h1>
            </Navbar.Brand>
            <Nav className="ml-auto">
              {isLoggedIn ? (
                <Nav.Link href="/" onClick={handleLogout}>
                  Logout
                </Nav.Link>
              ) : (
                <Nav.Link href="/login">Login</Nav.Link>
              )}
            </Nav>
          </Container>
        </Navbar>
        <Navbar expand="lg" className="inner">
          <Container className="inner">
            <Navbar.Toggle aria-controls="basic-navbar-nav" />
            <Navbar.Collapse id="basic-navbar-nav">
              <Nav className="me-auto">
                <Nav.Link href="/">홈</Nav.Link>
                <Nav.Link href="#guestbook">방명록</Nav.Link>
              </Nav>
            </Navbar.Collapse>
          </Container>
        </Navbar>
      </div>
      <main
        className="inner"
        style={{
          marginTop: "100px",
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
