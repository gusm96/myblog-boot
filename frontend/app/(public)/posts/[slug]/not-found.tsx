import Link from "next/link";
import { Container } from "react-bootstrap";
import { getPostList } from "@/lib/api";

export default async function PostNotFound() {
  let recentPosts: { id: number; title: string; slug: string }[] = [];
  try {
    const res = await getPostList(1);
    recentPosts = res.list.slice(0, 5);
  } catch {
    // 백엔드 장애 시 최근 글 없이 안내 문구만 노출
  }

  return (
    <Container style={{ paddingTop: "80px" }}>
      <p
        style={{
          fontFamily: "var(--font-mono)",
          fontSize: "0.8rem",
          color: "var(--text-faint)",
          marginBottom: "12px",
        }}
      >
        // 404 · post
      </p>
      <h1
        style={{
          fontSize: "1.5rem",
          fontWeight: 700,
          color: "var(--text-primary)",
          marginBottom: "8px",
        }}
      >
        해당 게시글을 찾을 수 없습니다
      </h1>
      <p style={{ color: "var(--text-muted)", marginBottom: "24px" }}>
        삭제되었거나 URL이 변경되었을 수 있습니다.
      </p>

      {recentPosts.length > 0 && (
        <div style={{ marginBottom: "32px" }}>
          <p
            style={{
              fontFamily: "var(--font-mono)",
              fontSize: "0.8rem",
              color: "var(--text-faint)",
              marginBottom: "8px",
            }}
          >
            // 최근 게시글
          </p>
          <ul style={{ listStyle: "none", padding: 0, margin: 0 }}>
            {recentPosts.map((post) => (
              <li key={post.id} style={{ marginBottom: "8px" }}>
                <Link
                  href={`/posts/${post.slug}`}
                  style={{ color: "var(--accent)", fontSize: "0.95rem" }}
                >
                  {post.title}
                </Link>
              </li>
            ))}
          </ul>
        </div>
      )}

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
