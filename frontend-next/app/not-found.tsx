import Link from "next/link";
import { Container } from "react-bootstrap";

export default function NotFound() {
  return (
    <Container className="text-center" style={{ paddingTop: "80px" }}>
      <p
        style={{
          fontFamily: "var(--font-mono)",
          fontSize: "0.8rem",
          color: "var(--text-faint)",
          marginBottom: "12px",
        }}
      >
        // 404
      </p>
      <h1
        style={{
          fontSize: "1.5rem",
          fontWeight: 700,
          color: "var(--text-primary)",
          marginBottom: "8px",
        }}
      >
        Page Not Found
      </h1>
      <p style={{ color: "var(--text-muted)", marginBottom: "24px" }}>
        요청하신 페이지를 찾을 수 없습니다.
      </p>
      <Link
        href="/"
        style={{
          fontFamily: "var(--font-mono)",
          fontSize: "0.875rem",
          color: "var(--accent)",
        }}
      >
        ← 홈으로 돌아가기
      </Link>
    </Container>
  );
}
