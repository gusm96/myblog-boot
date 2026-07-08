import { QueryClient } from "@tanstack/react-query";

/** 서버 컴포넌트에서 요청마다 새 인스턴스 생성 — 싱글턴 금지 */
export function getServerQueryClient() {
  return new QueryClient({
    defaultOptions: {
      queries: {
        staleTime: 60 * 1000,
      },
    },
  });
}
