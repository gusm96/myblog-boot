import { Container, Row, Col } from "react-bootstrap";
import { Header } from "./Header";
import { CategoryNav } from "./CategoryNav";
import { VisitorCount } from "./VisitorCount";

/**
 * 일반 사용자 공통 레이아웃 (Header + 사이드바)
 * route group (public) 바깥의 페이지(홈, 카테고리, 검색)에서 직접 사용
 */
export function UserLayout({ children }: { children: React.ReactNode }) {
  return (
    <div>
      <Header />
      <main className="layout-main">
        <Container>
          <Row>
            <Col xs={12} md={9} className="order-1">
              {children}
            </Col>
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
