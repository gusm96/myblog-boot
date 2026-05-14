import { cookies } from "next/headers";
import { redirect } from "next/navigation";
import { Container, Row, Col } from "react-bootstrap";
import { Header } from "@/components/layout/Header";
import { AdminNavBar } from "@/components/management/AdminNavBar";
import { RoleGate } from "@/components/management/RoleGate";

export default async function ManagementLayout({
  children,
}: {
  children: React.ReactNode;
}) {
  // 1차 게이트 — HTTPS 운영 환경에서만 쿠키 존재 확인
  // HTTP 개발 환경에서는 Secure 쿠키가 전송되지 않으므로 생략
  if (process.env.NODE_ENV === "production") {
    const token = (await cookies()).get("refresh_token");
    if (!token) redirect("/login?from=/management");
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
              {/* 2차 게이트 — role 검증 (Client, 본문 영역만 한정) */}
              <RoleGate>{children}</RoleGate>
            </Col>
          </Row>
        </Container>
      </main>
    </div>
  );
}
