import React from "react";
import { Navbar, Container, Nav, Button } from "react-bootstrap";
import { useDispatch, useSelector } from "react-redux";
import { useNavigate } from "react-router-dom";
import { selectIsLoggedIn } from "../../redux/userSlice";
import { userLogout } from "../../redux/authAction";
const NavBarElements = () => {
  const navigate = useNavigate();
  const dispatch = useDispatch();
  const isLoggedIn = useSelector(selectIsLoggedIn);
  const handleLogout = (e) => {
    e.preventDefault();
    dispatch(userLogout());
  };

  return (
    <Navbar collapseOnSelect expand="lg" variant="dark" bg="dark">
      <Container>
        <Navbar.Brand href="/">My Blog</Navbar.Brand>
        <Nav>
          <Nav.Link href="#">Resume</Nav.Link>
          <Nav.Link href="#">Portfolio</Nav.Link>
          {isLoggedIn ? (
            <Button variant="primary" onClick={handleLogout}>
              로그아웃
            </Button>
          ) : (
            <Nav.Link href="/login">Login</Nav.Link>
          )}
        </Nav>
      </Container>
    </Navbar>
  );
};

export default NavBarElements;
