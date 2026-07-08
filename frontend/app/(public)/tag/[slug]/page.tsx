import { notFound } from "next/navigation";
import type { Metadata } from "next";
import { dehydrate, HydrationBoundary } from "@tanstack/react-query";
import { getServerQueryClient } from "@/lib/queryClientServer";
import { getPublicTags, getTagPostList, getTagBySlug } from "@/lib/api";
import { queryKeys } from "@/lib/queryKeys";
import { PostListInfinite } from "@/components/posts/PostListInfinite";
import { SearchBar } from "@/components/posts/SearchBar";

export const revalidate = 60;

export async function generateStaticParams() {
  try {
    const tags = await getPublicTags();
    return tags.map((t) => ({ slug: t.slug }));
  } catch {
    return [];
  }
}

export async function generateMetadata({
  params,
}: {
  params: Promise<{ slug: string }>;
}): Promise<Metadata> {
  const { slug } = await params;
  const decodedSlug = decodeURIComponent(slug);
  
  let tagName = decodedSlug;
  try {
    const tag = await getTagBySlug(decodedSlug);
    tagName = tag.name;
  } catch {
    // fallback
  }

  const canonical = `/tag/${encodeURIComponent(decodedSlug)}`;
  return {
    title: `${tagName} 게시글`,
    description: `${tagName} 태그의 게시글 목록입니다.`,
    alternates: { canonical },
    openGraph: {
      type: "website",
      title: `${tagName} 게시글`,
      description: `${tagName} 태그의 게시글 목록입니다.`,
      url: canonical,
    },
  };
}

export default async function TagPage({
  params,
}: {
  params: Promise<{ slug: string }>;
}) {
  const { slug } = await params;
  const decodedSlug = decodeURIComponent(slug);

  const qc = getServerQueryClient();

  let tagExists = true;
  await Promise.all([
    qc.prefetchInfiniteQuery({
      queryKey: queryKeys.tags.posts(decodedSlug),
      queryFn: () => getTagPostList(decodedSlug, 1, 60),
      initialPageParam: 1,
    }).catch(() => { tagExists = false; }),
    qc.prefetchQuery({
      queryKey: queryKeys.tags.publicList(),
      queryFn: () => getPublicTags(),
    }).catch(() => {}),
  ]);

  if (!tagExists) notFound();

  // Get tag details for displaying display name
  let tagName = decodedSlug;
  try {
    const tag = await getTagBySlug(decodedSlug);
    tagName = tag.name;
  } catch {
    // fallback
  }

  return (
    <HydrationBoundary state={dehydrate(qc)}>
      <SearchBar />
      <div className="category-header">{tagName}</div>
      <PostListInfinite queryType="tag" tagSlug={decodedSlug} />
    </HydrationBoundary>
  );
}
