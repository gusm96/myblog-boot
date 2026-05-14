"use client";

import { useSearchParams } from "next/navigation";
import { useQuery, keepPreviousData } from "@tanstack/react-query";
import { SearchBar } from "./SearchBar";
import { PostList } from "./PostList";
import { PageButton } from "./PageButton";
import { getSearchedPostList } from "@/lib/postApi";
import { queryKeys } from "@/lib/queryKeys";

export function SearchContent() {
  const searchParams = useSearchParams();
  const type = searchParams.get("type");
  const contents = searchParams.get("contents");
  const page = Number(searchParams.get("p") ?? 1);

  const { data, isPlaceholderData } = useQuery({
    queryKey: queryKeys.search.results(type ?? "", contents ?? "", page),
    queryFn: () => getSearchedPostList(type!, contents!, page),
    enabled: !!(type && contents),
    placeholderData: keepPreviousData,
  });

  return (
    <div style={{ opacity: isPlaceholderData ? 0.6 : 1 }}>
      <SearchBar type={type} contents={contents} />
      <PostList posts={data?.list ?? []} />
      {(data?.totalPage ?? 0) > 1 && (
        <PageButton
          pageCount={data!.totalPage}
          path={`search?type=${type}&contents=${contents}&`}
          currentPage={page}
        />
      )}
    </div>
  );
}
