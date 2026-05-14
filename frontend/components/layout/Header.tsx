"use client";

import { Container, Nav, Navbar } from "react-bootstrap";
import Link from "next/link";
import { useRouter } from "next/navigation";
import { useDispatch, useSelector } from "react-redux";
import { selectIsLoggedIn } from "@/store/userSlice";
import { userLogout } from "@/store/authActions";
import { logout } from "@/lib/authApi";
import type { AppDispatch } from "@/store";

export function Header() {
  const isLoggedIn = useSelector(selectIsLoggedIn);
  const dispatch = useDispatch<AppDispatch>();
  const router = useRouter();

  const handleLogout = () => {
    if (window.confirm("정말 로그아웃 하시겠습니까?")) {
      dispatch(userLogout());
      logout();
      router.push("/");
    }
  };

  return (
    <div id="header">
      <Navbar collapseOnSelect expand="lg" variant="light" className="inner">
        <Container>
          <Navbar.Brand as={Link} href="/">
            <h1>{"Moya's Research Institute"}</h1>
          </Navbar.Brand>
          <Nav className="ml-auto">
            {isLoggedIn ? (
              <Nav.Link onClick={handleLogout}>Logout</Nav.Link>
            ) : (
              <Nav.Link as={Link} href="/login">Login</Nav.Link>
            )}
          </Nav>
        </Container>
      </Navbar>
      <Navbar expand="lg" className="inner">
        <Container className="inner">
          <Navbar.Toggle aria-controls="basic-navbar-nav" />
          <Navbar.Collapse id="basic-navbar-nav">
            <Nav className="me-auto">
              <Nav.Link as={Link} href="/" className="nav-link">
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
}
