import React from "react";
import { Navbar, Container, Nav } from "react-bootstrap";
const NavBarElements = () => {
  return (
    //#454545
    <Navbar collapseOnSelect expand="lg" variant="dark" bg="dark">
      <Container>
        <Navbar.Brand href="/">My Blog</Navbar.Brand>
        <Nav>
          <Nav.Link href="#">Resume</Nav.Link>
          <Nav.Link href="#">Portfolio</Nav.Link>
          <Nav.Link href="#"></Nav.Link>
        </Nav>
      </Container>
    </Navbar>
  );
};

export default NavBarElements;
