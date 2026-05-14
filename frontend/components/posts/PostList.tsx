import Link from "next/link";
import dayjs from "dayjs";
import type { PostSummary } from "@/types";

interface PostListProps {
  posts: PostSummary[];
}

const stripHtml = (html: string, maxLen = 200) => {
  const plain = html.replace(/<[^>]*>/g, "");
  return plain.length <= maxLen ? plain : plain.slice(0, maxLen) + "...";
};

/**
 * 게시글 목록 카드 렌더러 — Server/Client 모두 사용 가능
 * /posts/:slug URL로 이동 (slug 기반)
 */
export function PostList({ posts }: PostListProps) {
  if (posts.length === 0) {
    return (
      <div className="post-list">
        <p className="empty-state">게시글이 존재하지 않습니다</p>
      </div>
    );
  }

  return (
    <div className="post-list">
      {posts.map((post) => (
        <div key={post.id} className="mb-3 post-item">
          <Link href={`/posts/${post.slug}`} style={{ textDecoration: "none" }}>
            <span className="post-title">{post.title}</span>
            <p className="post-content">{stripHtml(post.content)}</p>
            <span className="text-muted post-date">
              {dayjs(post.createDate).format("YYYY-MM-DD")}
            </span>
          </Link>
        </div>
      ))}
      <hr />
    </div>
  );
}
