import React from "react";
import { BrowserRouter as Router, Routes, Route } from "react-router-dom";
import Home from "./screens/Home";
import NavBarElements from "./components/Navbar/NavBarElements";
import BoardDetail from "./components/Boards/BoardDetail";
import { Management } from "./screens/Management";
import { BoardForm } from "./components/Boards/BoardForm";
import { BoardEditForm } from "./components/Boards/BoardEditForm";
import { JoinForm } from "./components/Guest/JoinForm";
import { LoginForm } from "./components/Guest/LoginForm";
import { AdminLoginForm } from "./components/Admin/AdminLoginForm";
import { AdminLogout } from "./components/Admin/AdminLogout";
import { Col, Container, Row } from "react-bootstrap";
import { CategoryNav } from "./components/Navbar/CategoryNav";

export default App;

function App() {
  return (
    <Router>
      <NavBarElements />
      <Container
        style={{
          margin: "100px 0 0 0",
        }}
      >
        <Row>
          <Col></Col>
          <Col xs="8">
            <Routes>
              <Route path="/" element={<Home />} />
              <Route path="/boards" element={<Home />} />
              <Route path="/:categoryName" />
              <Route path="/board/:boardId" element={<BoardDetail />} />
              {/* 관리자 전용 Route */}
              <Route path="/login/admin" element={<AdminLoginForm />} />
              <Route path="/logout/admin" element={<AdminLogout />} />
              <Route path="/management" element={<Management />} />
              <Route path="/management/new-post" element={<BoardForm />} />
              <Route
                path="/management/boards/:boardId"
                element={<BoardEditForm />}
              />
            </Routes>
          </Col>
          <Col>
            <CategoryNav />
          </Col>
        </Row>
      </Container>
    </Router>
  );
}
