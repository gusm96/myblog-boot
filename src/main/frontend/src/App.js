import { BrowserRouter as Router, Routes, Route } from "react-router-dom";
import Home from "./screens/Home";
import BoardDetail from "./components/Boards/BoardDetail";
import { Management } from "./screens/Management";
import { BoardForm } from "./components/Boards/BoardForm";
import { BoardEditForm } from "./components/Boards/BoardEditForm";
import { UserLayout } from "./components/Layout/UserLayout";
import { ProtectedRoute } from "./components/Layout/ProtectedRoute";
import { LoginForm } from "./screens/Member/LoginForm";

export default App;

function App() {
  return (
    <Router>
      <Routes>
        {/* 일반 사용자 전용 페이지 */}
        <Route element={<UserLayout />}>
          <Route path="/" element={<Home />} />
          <Route path="/boards" element={<Home />} />
          <Route path="/:categoryName" />
          <Route path="/board/:boardId" element={<BoardDetail />} />
          <Route path="/login" element={<LoginForm />} />
        </Route>

        {/* 관리자 전용 페이지 */}
        <Route element={<ProtectedRoute />}>
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
