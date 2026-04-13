import { Container, Row, Col } from "react-bootstrap";
import { Header } from "@/components/layout/Header";
import { CategoryNav } from "@/components/layout/CategoryNav";
import { VisitorCount } from "@/components/layout/VisitorCount";

export default function PublicLayout({
  children,
}: {
  children: React.ReactNode;
}) {
  return (
    <div>
      <Header />
      <main className="layout-main">
        <Container>
          <Row>
            {/* Main content */}
            <Col xs={12} md={9} className="order-1">
              {children}
            </Col>
            {/* Sidebar */}
            <Col xs={12} md={3} className="order-2">
              <div className="sidebar-sticky">
                <CategoryNav />
                <VisitorCount />
              </div>
            </Col>
          </Row>
        </Container>
      </main>
    </div>
  );
}
