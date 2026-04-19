"use client";

import Link from "next/link";
import dayjs from "dayjs";
import type { PostSummary } from "@/types";

interface AdminPostListProps {
  posts: PostSummary[];
  basePath?: string;
}

export function AdminPostList({ posts, basePath = "/management/posts" }: AdminPostListProps) {
  if (posts.length === 0) {
    return <p className="admin-empty">// 게시글이 존재하지 않습니다.</p>;
  }

  return (
    <div className="post-list">
      {posts.map((post) => (
        <div key={post.id} className="mb-3 post-item">
          <Link href={`${basePath}/${post.id}`} style={{ textDecoration: "none" }}>
            <span className="post-title">{post.title}</span>
            <p className="post-content">
              {post.content.replace(/<[^>]*>/g, "").slice(0, 200)}
              {post.content.length > 200 ? "..." : ""}
            </p>
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
