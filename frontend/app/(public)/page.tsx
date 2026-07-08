import { dehydrate, HydrationBoundary } from "@tanstack/react-query";
import { getServerQueryClient } from "@/lib/queryClientServer";
import { getPostList, getPublicTags } from "@/lib/api";
import { queryKeys } from "@/lib/queryKeys";
import { PostListInfinite } from "@/components/posts/PostListInfinite";
import { SearchBar } from "@/components/posts/SearchBar";

export const revalidate = 60;

export default async function HomePage() {
  const qc = getServerQueryClient();

  await Promise.all([
    qc.prefetchInfiniteQuery({
      queryKey: queryKeys.posts.lists(),
      queryFn: () => getPostList(1, 60),
      initialPageParam: 1,
    }).catch(() => {}),
    qc.prefetchQuery({
      queryKey: queryKeys.tags.publicList(),
      queryFn: () => getPublicTags(),
    }).catch(() => {}),
  ]);

  return (
    <HydrationBoundary state={dehydrate(qc)}>
      <SearchBar />
      <PostListInfinite queryType="posts" />
    </HydrationBoundary>
  );
}
