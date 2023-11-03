import { useDispatch, useSelector } from "react-redux";
import { selectAccessToken, selectIsLoggedIn } from "../../redux/userSlice";
import { getRoleFromToken } from "../../services/authApi";
import { useState } from "react";
import { Col, Container, Row } from "react-bootstrap";
import { AdminNavBar } from "../Navbar/AdminNavBar";
import { Navigate, Outlet, useLocation } from "react-router-dom";
import { useCookies } from "react-cookie";

export const ProtectedRoute = () => {
  const [role, setRole] = useState(null);
  const isLoggedIn = useSelector(selectIsLoggedIn);
  const accessToken = useSelector(selectAccessToken);
  const [cookie, setCookie, removeCookie] = useCookies(["refresh_token"]);

  const dispatch = useDispatch();
  const location = useLocation();
  if (isLoggedIn) {
    getRoleFromToken(accessToken)
      .then((data) => setRole(data))
      .catch((error) => {
        if (error.response.data === "토큰이 만료되었습니다.") {
        }
      });
  }
  if (!isLoggedIn) {
    return <Navigate to="/login" state={{ from: location }} />;
  } else if (role === "NORMAL") {
    alert("접근 권한이 없습니다.");
    return <Navigate to="/" />;
  }

  return (
    <div>
      <header>
        <h1>관리자 모드</h1>
        <hr></hr>
      </header>
      <main>
        <Container>
          <Row>
            <Col xs="3">
              <AdminNavBar />
            </Col>
            <Col xs="9">
              <Outlet />
            </Col>
          </Row>
        </Container>
      </main>
    </div>
  );
};
