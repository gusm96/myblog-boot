import React, { useEffect, useState } from "react";
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
import { useDispatch, useSelector } from "react-redux";
import axios from "axios";
import { REISSUING_TOKEN, TOKEN_VALIDATION } from "./apiConfig";
import { useCookies } from "react-cookie";
import {
  selectAccessToken,
  selectIsLoggedIn,
  updateAccessToken,
} from "./store/userSlice";

export default App;

function App() {
  const [cookies] = useCookies(["refresh_token"]);
  const [isExpired, setIsExpired] = useState(false);
  const isLoggedIn = useSelector(selectIsLoggedIn);
  const access_token = useSelector(selectAccessToken);
  const dispatch = useDispatch();

  useEffect(() => {
    const reissuingAccessToken = async () => {
      await axios
        .post(`${REISSUING_TOKEN}`, {
          refresh_token: cookies.refresh_token,
        })
        .then((res) => res.data)
        .then((data) => dispatch(updateAccessToken(data)))
        .catch((error) => console.log(error));
    };
    if (isLoggedIn && access_token) {
      // access_token 유효 검사
      axios
        .get(`${TOKEN_VALIDATION}`, {
          headers: {
            Authorization: `bearer ${access_token}`,
          },
        })
        .then((res) => res.data)
        .then((data) => setIsExpired(data))
        .catch((error) => {
          if (error.response.status === 401 || error.response.status === 403) {
            reissuingAccessToken();
          } else {
            console.log(error);
          }
        });
    } else if (isExpired) {
      reissuingAccessToken();
    }
  }, [access_token, dispatch, isLoggedIn, cookies.refresh_token, isExpired]);

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

          <Route path="/new-post" element={<BoardForm />} />
          <Route path="/boards/:boardId" element={<BoardEditForm />} />
        </Route>
      </Routes>
    </Router>
  );
}
