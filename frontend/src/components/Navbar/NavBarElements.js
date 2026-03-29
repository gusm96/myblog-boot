import React from "react";
import { Navbar, Container, Nav } from "react-bootstrap";
import { Link, useNavigate } from "react-router";
import { useDispatch, useSelector } from "react-redux";
import { userLogout } from "../../redux/authAction";
import { logout } from "../../services/authApi";
import { selectIsLoggedIn } from "../../redux/userSlice";
const NavBarElements = () => {
  const isLoggedIn = useSelector(selectIsLoggedIn);
  const dispatch = useDispatch();
  const navigate = useNavigate();
  const handleLogout = () => {
    if (window.confirm("정말 로그아웃 하시겠습니까?")) {
      dispatch(userLogout());
      logout();
      navigate("/");
    }
  };

  return (
    <Navbar collapseOnSelect expand="lg" variant="dark" bg="dark">
      <Container>
        <Navbar.Brand as={Link} to="/">My Blog</Navbar.Brand>
        <Nav>
          <Nav.Link href="#">Resume</Nav.Link>
          <Nav.Link href="#">Portfolio</Nav.Link>
          {isLoggedIn ? (
            <Nav.Link onClick={handleLogout}>
              Logout
            </Nav.Link>
          ) : (
            <Nav.Link as={Link} to="/login">Login</Nav.Link>
          )}
        </Nav>
      </Container>
    </Navbar>
  );
};

export default NavBarElements;
