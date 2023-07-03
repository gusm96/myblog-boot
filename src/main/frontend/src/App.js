import React from "react";
import { BrowserRouter as Router, Routes, Route } from "react-router-dom";
import Home from "./screens/Home";
import NavBarElements from "./components/Navbar/NavBarElements";
import BoardDetail from "./components/Boards/BoardDetail";
import { Management } from "./screens/Management";
import { Login } from "./components/User/Login";
import { BoardForm } from "./components/Boards/BoardForm";
import { BoardEditForm } from "./components/Boards/BoardEditForm";
import { Logout } from "./components/User/Logout";

export default App;

function App() {
  return (
    <Router>
      <NavBarElements />
      <Routes>
        <Route path="/" element={<Home />} />
        <Route path="/category/:categoryName" />
        <Route path="/:boardId" element={<BoardDetail />} />
        <Route path="/login/admin" element={<Login />} />
        <Route path="/logout" element={<Logout />} />
        {/* 관리자 전용 Route */}
        <Route path="/management" element={<Management />} />
        <Route path="/management/new-post" element={<BoardForm />} />
        <Route path="/management/boards/:boardId" element={<BoardEditForm />} />
      </Routes>
    </Router>
  );
}
