import React from "react";
import { BrowserRouter as Router, Routes, Route } from "react-router-dom";
import Home from "./screens/Home";
import NavBarElements from "./components/Navbar/NavBarElements";

export default App;

function App() {
  return (
    <Router>
      <NavBarElements />
      <Routes>
        <Route path="/" element={<Home />} />
        <Route path="/category/:categoryName" />
        <Route path="/:boardId" />
        <Route path="/login/admin" />
        <Route path="/management" />
      </Routes>
    </Router>
  );
}
