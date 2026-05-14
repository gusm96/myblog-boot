"use client";

import { useSearchParams, useRouter } from "next/navigation";
import { useQuery, keepPreviousData } from "@tanstack/react-query";
import { getDeletedPosts } from "@/lib/postApi";
import { AdminPostList } from "@/components/management/AdminPostList";
import { queryKeys } from "@/lib/queryKeys";
import { Button } from "react-bootstrap";

export default function TemporaryStoragePage() {
  const searchParams = useSearchParams();
  const router = useRouter();
  const page = Number(searchParams.get("p")) || 1;

  const { data, isPending, isPlaceholderData } = useQuery({
    queryKey: queryKeys.admin.trash(page),
    queryFn:  () => getDeletedPosts(page),
    staleTime: 3 * 60 * 1000,
    gcTime:    10 * 60 * 1000,
    placeholderData: keepPreviousData,
  });

  if (isPending) return <div style={{ padding: "2rem" }}>// loading...</div>;

  const totalPage = data?.totalPage ?? 0;

  return (
    <div>
      <h3>휴지통</h3>
      <p style={{ fontSize: "0.75em", color: "#999" }}>
        삭제일로부터 15일 이후 자동 영구 삭제됩니다.
      </p>
      <hr />
      <div style={{ opacity: isPlaceholderData ? 0.6 : 1 }}>
        <AdminPostList posts={data?.list ?? []} basePath="/management/posts" />

        {totalPage > 1 && (
          <div className="page-buttons-container">
            {Array.from({ length: totalPage }, (_, i) => i + 1).map((p) => (
              <Button
                key={p}
                variant={p === page ? "primary" : "outline-secondary"}
                size="sm"
                onClick={() => router.push(`/management/temporary-storage?p=${p}`)}
              >
                {p}
              </Button>
            ))}
          </div>
        )}
      </div>
    </div>
  );
}
