import React, { useState } from "react";
import { BrowserRouter as Router, Routes, Route } from "react-router-dom";
import Home from "./screens/Home";
import BoardDetail from "./components/Boards/BoardDetail";
import { Management } from "./screens/Management";
import { BoardForm } from "./components/Boards/BoardForm";
import { BoardEditForm } from "./components/Boards/BoardEditForm";
import { AdminLoginForm } from "./components/Admin/AdminLoginForm";
import { AdminLogout } from "./components/Admin/AdminLogout";
import { UserLayout } from "./components/Layout/UserLayout";
import { AdminLayout } from "./components/Layout/AdminLayout";
import { LoginConfirm } from "./components/LoginConfirm";

export default App;

function App() {
  const { accessToken, setAccessToken } = useState("");
  const { isLoggedIn, setIsLoggedIn } = useState(false);

  const updateAccessToken = (token) => {
    setAccessToken(token);
  };
  if (accessToken !== null || accessToken !== "") {
    const isExpired = LoginConfirm(accessToken);
    if (!isExpired) {
      setIsLoggedIn(false);
    }
  }

  return (
    <Router>
      <Routes>
        {/* 일반 사용자 전용 페이지 */}
        <Route element={<UserLayout />}>
          <Route path="/" element={<Home />} />
          <Route path="/boards" element={<Home />} />
          <Route path="/:categoryName" />
          <Route path="/board/:boardId" element={<BoardDetail />} />
        </Route>

        {/* 관리자 전용 페이지 */}
        <Route element={<AdminLayout />}>
          <Route path="/login/admin" element={<AdminLoginForm />} />
          <Route path="/logout/admin" element={<AdminLogout />} />
          <Route path="/management" element={<Management />} />
          <Route path="/management/new-post" element={<BoardForm />} />
          <Route
            path="/management/boards/:boardId"
            element={<BoardEditForm />}
          />
        </Route>
      </Routes>
    </Router>
  );
}
