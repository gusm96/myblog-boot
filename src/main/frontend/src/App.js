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
import {
  selectAccessToken,
  selectIsLoggedIn,
  updateAccessToken,
} from "./redux/userSlice";
import { useEffect } from "react";
import { reissuingAccessToken, validateAccessToken } from "./services/authApi";
import { useCookies } from "react-cookie";
import { userLogout } from "./redux/authAction";
import { JoinForm } from "./screens/Member/JoinForm";

export default App;

function App() {
  const isLoggedIn = useSelector(selectIsLoggedIn);
  const access_token = useSelector(selectAccessToken);
  const [cookies, setCookie, removeCookie] = useCookies(["refresh_token_key"]);
  const dispatch = useDispatch();
  useEffect(() => {
    // 로그인 상태면 access Token 주기적으로 검증.
    if (isLoggedIn && access_token !== null) {
      validateAccessToken(access_token).catch((error) => {
        if (error.response.status === 401) {
          reissuingAccessToken()
            .then((data) => {
              dispatch(updateAccessToken(data));
            })
            .catch((error) => {
              if (error.response.status === 401) {
                // refresh Token 만료
                removeCookie("refresh_token_key");
                dispatch(userLogout());
              }
            });
        }
      });
    }
  }, [isLoggedIn, access_token, cookies, dispatch, removeCookie]);

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
          <Route path="/join" element={<JoinForm />} />
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
