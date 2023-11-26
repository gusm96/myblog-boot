import { useSelector } from "react-redux";
import { selectAccessToken, selectIsLoggedIn } from "../../redux/userSlice";
import { getRoleFromToken } from "../../services/authApi";
import { useState } from "react";
import { Col, Container, Row } from "react-bootstrap";
import { AdminNavBar } from "../Navbar/AdminNavBar";
import { Navigate, Outlet, useLocation } from "react-router-dom";
import { useEffect } from "react";
import { Header } from "./Header";

export const ProtectedRoute = () => {
  const [role, setRole] = useState(null);
  const isLoggedIn = useSelector(selectIsLoggedIn);
  const accessToken = useSelector(selectAccessToken);
  const headerTitle = "admin";
  const location = useLocation();
  useEffect(() => {
    if (isLoggedIn) {
      getRoleFromToken(accessToken)
        .then((data) => setRole(data))
        .catch((error) => {
          if (error.response.data === "토큰이 만료되었습니다.") {
          }
        });
    }
  }, [accessToken, isLoggedIn]);

  if (!isLoggedIn) {
    return <Navigate to="/login" state={{ from: location }} />;
  } else if (role === "ROLE_NORMAL") {
    alert("접근 권한이 없습니다.");
    return <Navigate to="/" />;
  } else if (role === "ROLE_ADMIN") {
    return (
      <div>
        <Header headerTitle={headerTitle} isLoggedIn={isLoggedIn} />
        <main
          className="inner"
          style={{
            marginTop: "100px",
          }}
        >
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
  }
  return null;
};
