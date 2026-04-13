import { notFound } from "next/navigation";
import type { Metadata } from "next";
import { UserLayout } from "@/components/layout/UserLayout";
import { PostListInfinite } from "@/components/boards/PostListInfinite";
import { SearchBar } from "@/components/boards/SearchBar";
import { getCategoriesV2, getCategoryPostList } from "@/lib/api";

// ISR: 60초마다 갱신
export const revalidate = 60;

// 빌드 시 모든 카테고리 사전 생성
export async function generateStaticParams() {
  try {
    const categories = await getCategoriesV2();
    return categories.map((c) => ({ categoryName: c.name }));
  } catch {
    return [];
  }
}

export async function generateMetadata({
  params,
}: {
  params: Promise<{ categoryName: string }>;
}): Promise<Metadata> {
  const { categoryName } = await params;
  const decoded = decodeURIComponent(categoryName);
  return {
    title: `${decoded} 게시글`,
    description: `${decoded} 카테고리의 게시글 목록입니다.`,
  };
}

export default async function CategoryPage({
  params,
}: {
  params: Promise<{ categoryName: string }>;
}) {
  const { categoryName } = await params;
  const decoded = decodeURIComponent(categoryName);

  let initialData;
  try {
    initialData = await getCategoryPostList(decoded, 1, 60);
  } catch {
    notFound();
  }

  return (
    <UserLayout>
      <SearchBar />
      <div className="category-header">{decoded}</div>
      <PostListInfinite
        initialData={initialData}
        queryType="category"
        categoryName={decoded}
      />
    </UserLayout>
  );
}
