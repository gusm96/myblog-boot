"use client";

import { useSearchParams, useRouter } from "next/navigation";
import { useQuery, keepPreviousData } from "@tanstack/react-query";
import { getPostList } from "@/lib/postApi";
import { AdminPostList } from "@/components/management/AdminPostList";
import { queryKeys } from "@/lib/queryKeys";
import { Button } from "react-bootstrap";

export default function ManagementPage() {
  const searchParams = useSearchParams();
  const router = useRouter();
  const page = Number(searchParams.get("p")) || 1;

  const { data, isPending, isPlaceholderData } = useQuery({
    queryKey: queryKeys.admin.posts(page),
    queryFn:  () => getPostList(page),
    staleTime: 3 * 60 * 1000,
    gcTime:    10 * 60 * 1000,
    placeholderData: keepPreviousData,
  });

  if (isPending) return <div style={{ padding: "2rem" }}>// loading...</div>;

  const totalPage = data?.totalPage ?? 0;

  return (
    <div style={{ opacity: isPlaceholderData ? 0.6 : 1 }}>
      <AdminPostList posts={data?.list ?? []} basePath="/management/posts" />

      {totalPage > 1 && (
        <div className="page-buttons-container">
          {Array.from({ length: totalPage }, (_, i) => i + 1).map((p) => (
            <Button
              key={p}
              variant={p === page ? "primary" : "outline-secondary"}
              size="sm"
              onClick={() => router.push(`/management?p=${p}`)}
            >
              {p}
            </Button>
          ))}
        </div>
      )}
    </div>
  );
}
