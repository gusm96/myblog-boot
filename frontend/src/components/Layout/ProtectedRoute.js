import { useSelector, useDispatch } from "react-redux";
import { selectIsLoggedIn, logout } from "../../redux/userSlice";
import { getRoleFromToken } from "../../services/authApi";
import { useState, useEffect } from "react";
import { Col, Container, Row, Spinner } from "react-bootstrap";
import { AdminNavBar } from "../Navbar/AdminNavBar";
import { Navigate, Outlet, useLocation } from "react-router";
import { Header } from "./Header";

export const ProtectedRoute = () => {
  const [role, setRole] = useState(null);
  const isLoggedIn = useSelector(selectIsLoggedIn);
  const dispatch = useDispatch();
  const location = useLocation();

  useEffect(() => {
    if (isLoggedIn) {
      getRoleFromToken()
        .then((data) => setRole(data))
        .catch((error) => {
          if (error.response?.status === 401) {
            dispatch(logout());
          }
        });
    }
  }, [isLoggedIn, dispatch]);

  if (!isLoggedIn) {
    return <Navigate to="/login" state={{ from: location }} />;
  }

  if (role === null) {
    return (
      <div className="loading-center" style={{ height: "100vh" }}>
        <Spinner animation="border" role="status">
          <span className="visually-hidden">Loading...</span>
        </Spinner>
      </div>
    );
  }

  if (role === "ROLE_NORMAL") {
    alert("접근 권한이 없습니다.");
    return <Navigate to="/" />;
  }

  if (role === "ROLE_ADMIN") {
    return (
      <div>
        <Header headerTitle="admin" isLoggedIn={isLoggedIn} />
        <main className="layout-main">
          <Container>
            <Row>
              {/* 사이드 내비게이션 — 모바일에서 아래로 이동 */}
              <Col xs={12} md={3} className="order-2 order-md-1">
                <div className="sidebar-sticky">
                  <AdminNavBar />
                </div>
              </Col>
              {/* 메인 컨텐츠 */}
              <Col xs={12} md={9} className="order-1 order-md-2">
                <Outlet />
              </Col>
            </Row>
          </Container>
        </main>
      </div>
    );
  }

  return null;
};
