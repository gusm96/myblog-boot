"use client";

import { useEffect, useState } from "react";
import { useSelector, useDispatch } from "react-redux";
import { useRouter } from "next/navigation";
import { Container, Row, Col, Spinner } from "react-bootstrap";
import { selectIsLoggedIn } from "@/store/userSlice";
import { userLogout } from "@/store/authActions";
import { getRoleFromToken } from "@/lib/authApi";
import type { AppDispatch } from "@/store";
import { AdminNavBar } from "@/components/management/AdminNavBar";
import { Header } from "@/components/layout/Header";

export default function ManagementLayout({
  children,
}: {
  children: React.ReactNode;
}) {
  const [role, setRole] = useState<string | null>(null);
  const isLoggedIn = useSelector(selectIsLoggedIn);
  const dispatch = useDispatch<AppDispatch>();
  const router = useRouter();

  useEffect(() => {
    if (!isLoggedIn) {
      router.replace("/login?from=/management");
      return;
    }

    getRoleFromToken()
      .then((data: string) => setRole(data))
      .catch((error: { response?: { status?: number } }) => {
        if (error.response?.status === 401) {
          dispatch(userLogout());
          router.replace("/login?from=/management");
        }
      });
  }, [isLoggedIn, dispatch, router]);

  if (!isLoggedIn || role === null) {
    return (
      <div style={{ display: "flex", justifyContent: "center", alignItems: "center", height: "100vh" }}>
        <Spinner animation="border" role="status">
          <span className="visually-hidden">Loading...</span>
        </Spinner>
      </div>
    );
  }

  if (role !== "ROLE_ADMIN") {
    router.replace("/");
    return null;
  }

  return (
    <div>
      <Header />
      <main className="layout-main">
        <Container>
          <Row>
            <Col xs={12} md={3} className="order-2 order-md-1">
              <div className="sidebar-sticky">
                <AdminNavBar />
              </div>
            </Col>
            <Col xs={12} md={9} className="order-1 order-md-2">
              {children}
            </Col>
          </Row>
        </Container>
      </main>
    </div>
  );
}
