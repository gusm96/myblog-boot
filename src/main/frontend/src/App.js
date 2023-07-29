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

export default App;

function App() {
  return (
    <Router>
      <NavBarElements />
      <Routes>
        <Route path="/" element={<Home />} />
        <Route path="/boards" element={<Home />} />
        <Route path="/:categoryName" />
        <Route path="/:boardId" element={<BoardDetail />} />
        {/* 게스트 */}
        <Route path="/login/guest" element={<LoginForm />} />
        <Route path="/join/guest" element={<JoinForm />} />
        {/* 관리자 전용 Route */}
        <Route path="/login/admin" element={<AdminLoginForm />} />
        <Route path="/logout/admin" element={<AdminLogout />} />
        <Route path="/management" element={<Management />} />
        <Route path="/management/new-post" element={<BoardForm />} />
        <Route path="/management/boards/:boardId" element={<BoardEditForm />} />
      </Routes>
    </Router>
  );
}
