import { useSelector } from "react-redux";
import Redirect from "../Redirect";
import { selectAccessToken, selectIsLoggedIn } from "../../redux/userSlice";
import { getRoleFromToken } from "../../services/authApi";
import { useState } from "react";
import { Col, Container, Row } from "react-bootstrap";
import { AdminNavBar } from "../Navbar/AdminNavBar";
import { Outlet } from "react-router-dom";

export const ProtectedRoute = () => {
  const [role, setRole] = useState(null);
  const isLoggedIn = useSelector(selectIsLoggedIn);
  const accessToken = useSelector(selectAccessToken);
  if (isLoggedIn) {
    getRoleFromToken(accessToken).then((data) => setRole(data));
  }
  if (!isLoggedIn || role === "NORMAL") {
    return <Redirect to="/login" />;
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
