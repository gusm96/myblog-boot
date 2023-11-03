import React from "react";
import { Navbar, Container, Nav } from "react-bootstrap";
import { useDispatch, useSelector } from "react-redux";
import { userLogout } from "../../redux/authAction";
import { useCookies } from "react-cookie";
import { logout } from "../../services/authApi";
import { selectAccessToken, selectIsLoggedIn } from "../../redux/userSlice";
const NavBarElements = () => {
  const isLoggedIn = useSelector(selectIsLoggedIn);
  const accessToken = useSelector(selectAccessToken);
  const [cookies, setCookies, removeCookies] = useCookies([
    "refresh_token_idx",
  ]);
  const dispatch = useDispatch();
  const handleLogout = () => {
    if (window.confirm("정말 로그아웃 하시겠습니까?")) {
      dispatch(userLogout());
      removeCookies("refresh_token_idx");
      logout(accessToken);
    }
  };

  return (
    <Navbar collapseOnSelect expand="lg" variant="dark" bg="dark">
      <Container>
        <Navbar.Brand href="/">My Blog</Navbar.Brand>
        <Nav>
          <Nav.Link href="#">Resume</Nav.Link>
          <Nav.Link href="#">Portfolio</Nav.Link>
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
  );
};

export default NavBarElements;
