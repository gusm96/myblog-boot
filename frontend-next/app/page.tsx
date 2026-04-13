import { UserLayout } from "@/components/layout/UserLayout";
import { PostListInfinite } from "@/components/boards/PostListInfinite";
import { SearchBar } from "@/components/boards/SearchBar";
import { getPostList } from "@/lib/api";

// ISR: 60초마다 갱신
export const revalidate = 60;

export default async function HomePage() {
  let initialData;
  try {
    initialData = await getPostList(1, 60);
  } catch {
    initialData = { list: [], totalPage: 0 };
  }

  return (
    <UserLayout>
      <SearchBar />
      <PostListInfinite initialData={initialData} queryType="posts" />
    </UserLayout>
  );
}
