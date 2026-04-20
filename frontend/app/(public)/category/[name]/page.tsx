import { notFound } from "next/navigation";
import type { Metadata } from "next";
import { PostListInfinite } from "@/components/posts/PostListInfinite";
import { SearchBar } from "@/components/posts/SearchBar";
import { getCategoriesV2, getCategoryPostList } from "@/lib/api";

// ISR: 60초마다 갱신
export const revalidate = 60;

// 빌드 시 모든 카테고리 사전 생성
export async function generateStaticParams() {
  try {
    const categories = await getCategoriesV2();
    return categories.map((c) => ({ name: c.name }));
  } catch {
    return [];
  }
}

export async function generateMetadata({
  params,
}: {
  params: Promise<{ name: string }>;
}): Promise<Metadata> {
  const { name } = await params;
  const decoded = decodeURIComponent(name);
  const canonical = `/category/${encodeURIComponent(decoded)}`;
  return {
    title: `${decoded} 게시글`,
    description: `${decoded} 카테고리의 게시글 목록입니다.`,
    alternates: { canonical },
    openGraph: {
      type: "website",
      title: `${decoded} 게시글`,
      description: `${decoded} 카테고리의 게시글 목록입니다.`,
      url: canonical,
    },
  };
}

export default async function CategoryPage({
  params,
}: {
  params: Promise<{ name: string }>;
}) {
  const { name } = await params;
  const decoded = decodeURIComponent(name);

  let initialData;
  try {
    initialData = await getCategoryPostList(decoded, 1, 60);
  } catch {
    notFound();
  }

  return (
    <>
      <SearchBar />
      <div className="category-header">{decoded}</div>
      <PostListInfinite
        initialData={initialData}
        queryType="category"
        categoryName={decoded}
      />
    </>
  );
}
