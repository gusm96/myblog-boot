import React from "react";
import { Container, Nav, Navbar } from "react-bootstrap";
import { useDispatch } from "react-redux";
import { userLogout } from "../../redux/authAction";
import { logout } from "../../services/authApi";
import "../Styles/css/header.css";

export const Header = ({ headerTitle, isLoggedIn }) => {
  const dispatch = useDispatch();
  const handleLogout = () => {
    if (window.confirm("정말 로그아웃 하시겠습니까?")) {
      dispatch(userLogout());
      logout();
      window.location.reload();
    }
    return;
  };
  return (
    <div id="header">
      <Navbar collapseOnSelect expand="lg" variant="light" className="inner">
        <Container>
          {headerTitle === "admin" ? (
            <Navbar.Brand href="/management">
              <h1>{"MyBlog 관리자모드"}</h1>
            </Navbar.Brand>
          ) : (
            <Navbar.Brand href="/">
              <h1>{headerTitle}</h1>
            </Navbar.Brand>
          )}

          <Nav className="ml-auto">
            {isLoggedIn ? (
              <Nav.Link onClick={handleLogout}>Logout</Nav.Link>
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
              <Nav.Link className="nav-link" href="/">
                홈
              </Nav.Link>
              <Nav.Link className="nav-link" href="#guestbook">
                방명록
              </Nav.Link>
            </Nav>
          </Navbar.Collapse>
        </Container>
      </Navbar>
    </div>
  );
};
