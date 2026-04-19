"use client";

import { useEffect } from "react";
import { useQueryClient } from "@tanstack/react-query";
import { queryKeys } from "@/lib/queryKeys";

const SSE_URL =
  (process.env.NEXT_PUBLIC_API_URL ?? "http://localhost:8080") +
  "/api/v1/sse/posts";

export function usePostEventSource() {
  const queryClient = useQueryClient();

  useEffect(() => {
    const es = new EventSource(SSE_URL, { withCredentials: true });

    es.addEventListener("POST_CHANGED", () => {
      queryClient.invalidateQueries({ queryKey: queryKeys.posts.all() });
      queryClient.invalidateQueries({ queryKey: queryKeys.categories.all() });
    });

    es.addEventListener("CONNECTED", () => {
      queryClient.invalidateQueries({ queryKey: queryKeys.posts.all() });
      queryClient.invalidateQueries({ queryKey: queryKeys.categories.all() });
    });

    return () => es.close();
  }, [queryClient]);
}
