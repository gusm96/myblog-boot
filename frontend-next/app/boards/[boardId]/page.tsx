/**
 * 구 URL 리다이렉트: /boards/:boardId → /posts/:slug
 * 백엔드 `/api/v1/posts/{boardId}` 는 숫자 ID도 처리함
 */
import { redirect, notFound } from "next/navigation";
import { getPostBySlug } from "@/lib/api";

export default async function PostsRedirectPage({
  params,
}: {
  params: Promise<{ boardId: string }>;
}) {
  const { boardId } = await params;

  let slug: string;
  try {
    const post = await getPostBySlug(boardId, 3600);
    slug = post.slug;
  } catch {
    notFound();
  }

  redirect(`/posts/${slug!}`);
}
