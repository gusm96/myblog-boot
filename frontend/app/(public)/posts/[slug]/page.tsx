import type { Metadata } from "next";
import { notFound } from "next/navigation";
import dayjs from "dayjs";
import { getPostBySlug, getAllSlugs } from "@/lib/api";
import PostContent from "@/components/posts/PostContent";
import { PostLike } from "@/components/posts/PostLike";
import { CommentSection } from "@/components/comments/CommentSection";

// ISR: 60초마다 캐시 갱신
export const revalidate = 60;

// 빌드 시 전체 slug 사전 생성 (SSG)
export async function generateStaticParams() {
  try {
    const posts = await getAllSlugs();
    return posts.map((post) => ({ slug: post.slug }));
  } catch {
    // 백엔드 미실행 환경(CI 등)에서는 빈 배열 반환 — 런타임 SSR로 fallback
    return [];
  }
}

// SEO 메타데이터 동적 생성
export async function generateMetadata({
  params,
}: {
  params: Promise<{ slug: string }>;
}): Promise<Metadata> {
  const { slug } = await params;

  try {
    const post = await getPostBySlug(slug);
    return {
      title: post.title,
      description: post.metaDescription,
      openGraph: {
        title: post.title,
        description: post.metaDescription ?? "",
        type: "article",
        publishedTime: post.createDate,
        ...(post.thumbnailUrl ? { images: [{ url: post.thumbnailUrl }] } : {}),
      },
      robots: {
        index: true,
        follow: true,
        googleBot: {
          index: true,
          follow: true,
          "max-image-preview": "large",
          "max-snippet": -1,
        },
      },
    };
  } catch {
    return { title: "게시글을 찾을 수 없습니다" };
  }
}

// 게시글 상세 — Server Component (SSG + ISR)
export default async function PostPage({
  params,
}: {
  params: Promise<{ slug: string }>;
}) {
  const { slug } = await params;

  let post;
  try {
    post = await getPostBySlug(slug);
  } catch {
    notFound();
  }

  return (
    <article>
      <h1
        style={{
          fontSize: "1.5rem",
          fontWeight: 700,
          marginBottom: "4px",
          color: "var(--text-primary)",
        }}
      >
        {post.title}
      </h1>

      <div className="post-info">
        <div className="post-info-left">
          <span>조회수 {post.views}</span>
          <span>{dayjs(post.createDate).format("YYYY-MM-DD")}</span>
        </div>
        <div className="post-info-right">
          <PostLike postId={post.id} initialLikes={post.likes} />
        </div>
      </div>

      {/* 본문 — Server Component (HTML 렌더링) */}
      <PostContent content={post.content} />

      <hr />

      {/* 댓글 섹션 — Client Component */}
      <CommentSection postId={post.id} />
    </article>
  );
}
