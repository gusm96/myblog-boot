import React from "react";
import { BrowserRouter as Router, Routes, Route } from "react-router-dom";
import Home from "./screens/Home";
import NavBarElements from "./components/Navbar/NavBarElements";
import BoardDetail from "./components/Boards/BoardDetail";
import { Management } from "./screens/Management";
import { Login } from "./components/Login";

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
        <Route path="/management" element={<Management />} />
      </Routes>
    </Router>
  );
}
