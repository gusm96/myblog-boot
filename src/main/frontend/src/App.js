import { BrowserRouter as Router, Routes, Route } from "react-router-dom";
import Home from "./screens/Home";
import BoardDetail from "./components/Boards/BoardDetail";
import { Management } from "./screens/Management";
import { BoardForm } from "./components/Boards/BoardForm";
import { BoardEditForm } from "./components/Boards/BoardEditForm";
import { UserLayout } from "./components/Layout/UserLayout";
import { ProtectedRoute } from "./components/Layout/ProtectedRoute";
import { LoginForm } from "./screens/Member/LoginForm";
import { useDispatch, useSelector } from "react-redux";
import { selectAccessToken, selectIsLoggedIn } from "./redux/userSlice";
import { useEffect } from "react";
import { reissuingAccessToken, validateAccessToken } from "./services/authApi";
import { updateUserAccessToken, userLogout } from "./redux/authAction";
import { JoinForm } from "./screens/Member/JoinForm";
import { NotFound } from "./screens/error/NotFound";
import { NavigateBack } from "./screens/error/NavigateBack";

export default App;

function App() {
  const isLoggedIn = useSelector(selectIsLoggedIn);
  const access_token = useSelector(selectAccessToken);
  const dispatch = useDispatch();
  useEffect(() => {
    // 로그인 상태면 access Token 주기적으로 검증.
    if (access_token !== null) {
      validateAccessToken(access_token).catch((error) => {
        if (error.response.status === 401) {
          reissuingAccessToken()
            .then((data) => {
              dispatch(updateUserAccessToken(data));
            })
            .catch((error) => {
              if (error.response.status === 401) {
                alert("토큰이 만료되어 로그아웃 합니다.");
                dispatch(userLogout());
              }
            });
        }
      });
    }
  }, [access_token, dispatch]);

  return (
    <Router>
      <Routes>
        {/* 일반 사용자 전용 페이지 */}
        <Route path="/" element={<UserLayout />}>
          <Route index element={<Home />} />
          <Route path="boards" element={<Home />} />
          <Route path=":categoryName" element={<Home />} />
          <Route path="boards/:boardId" element={<BoardDetail />} />
          <Route
            path="login"
            element={isLoggedIn ? <NavigateBack /> : <LoginForm />}
          />
          <Route path="join" element={<JoinForm />} />
          <Route path="*" element={<NotFound />} />
        </Route>

        {/* 관리자 전용 페이지 */}
        <Route path="/management" element={<ProtectedRoute />}>
          <Route index element={<Management />} />
          <Route path="new-post" element={<BoardForm />} />
          <Route path="boards/:boardId" element={<BoardEditForm />} />
          <Route path="*" element={<NotFound />} />
        </Route>
      </Routes>
    </Router>
  );
}
