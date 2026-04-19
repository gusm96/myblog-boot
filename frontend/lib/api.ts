/**
 * fetch 기반 서버 사이드 API 클라이언트
 * - Server Component에서 직접 사용
 * - Next.js fetch 캐싱 옵션 지원 (next: { revalidate, tags })
 *
 * Revalidate policy:
 *   posts    : 60s  — on-demand 도입 후 600s로 완화 예정 (revalidateTag("posts"))
 *   slugs    : 3600s — sitemap 안정성 우선 (on-demand로 /sitemap.xml 즉시 무효화)
 *   categories: 300s — 카테고리 변경은 sitemap 영향 없음
 */

const BASE_URL = process.env.NEXT_PUBLIC_API_URL ?? "http://localhost:8080";

type FetchOptions = RequestInit & {
  next?: { revalidate?: number | false; tags?: string[] };
};

async function apiFetch<T>(path: string, options?: FetchOptions): Promise<T> {
  const res = await fetch(`${BASE_URL}${path}`, {
    ...options,
    headers: {
      "Content-Type": "application/json",
      ...options?.headers,
    },
  });

  if (!res.ok) {
    throw new Error(`API error: ${res.status} ${res.statusText} (${path})`);
  }

  return res.json() as Promise<T>;
}

// ── 게시글 ──────────────────────────────────────────────────────

import type { Post, PostListResponse, PostSlug } from "@/types";

/** 게시글 목록 (페이지) */
export function getPostList(
  page = 1,
  revalidate = 60
): Promise<PostListResponse> {
  return apiFetch<PostListResponse>(`/api/v1/posts?p=${page}`, {
    next: { revalidate, tags: ["posts"] },
  });
}

/** slug로 게시글 상세 조회 */
export function getPostBySlug(
  slug: string,
  revalidate = 60
): Promise<Post> {
  return apiFetch<Post>(`/api/v1/posts/${slug}`, {
    next: { revalidate, tags: ["posts", `post:${slug}`] },
  });
}

/** 카테고리별 게시글 목록 */
export function getCategoryPostList(
  categoryName: string,
  page = 1,
  revalidate = 60
): Promise<PostListResponse> {
  return apiFetch<PostListResponse>(
    `/api/v1/posts/category?c=${encodeURIComponent(categoryName)}&p=${page}`,
    { next: { revalidate, tags: ["posts"] } }
  );
}

/** SSG용 전체 slug 목록 */
export function getAllSlugs(revalidate = 3600): Promise<PostSlug[]> {
  return apiFetch<PostSlug[]>(`/api/v1/posts/slugs`, {
    next: { revalidate, tags: ["posts", "slugs"] },
  });
}

// ── 카테고리 ─────────────────────────────────────────────────────

import type { Category, CategoryV2 } from "@/types";

/** 카테고리 목록 (게시글 있는 것만, 일반 사용자용 — V2) */
export function getCategoriesV2(revalidate = 300): Promise<CategoryV2[]> {
  return apiFetch<CategoryV2[]>(`/api/v2/categories`, {
    next: { revalidate, tags: ["categories"] },
  });
}

/** 카테고리 목록 — 관리자용 V1 */
export function getCategories(revalidate = 300): Promise<Category[]> {
  return apiFetch<Category[]>(`/api/v1/categories`, {
    next: { revalidate, tags: ["categories"] },
  });
}
