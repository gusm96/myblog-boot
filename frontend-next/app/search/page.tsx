import { Suspense } from "react";
import { UserLayout } from "@/components/layout/UserLayout";
import { SearchContent } from "@/components/boards/SearchContent";

export const metadata = {
  title: "검색",
  description: "게시글 검색 결과",
};

export default function SearchPage() {
  return (
    <UserLayout>
      {/* useSearchParams는 반드시 Suspense 경계 내부에 위치해야 함 */}
      <Suspense
        fallback={
          <div
            style={{
              padding: "60px 0",
              textAlign: "center",
              fontFamily: "var(--font-mono)",
              fontSize: "0.82rem",
              color: "var(--text-muted)",
            }}
          >
            // searching...
          </div>
        }
      >
        <SearchContent />
      </Suspense>
    </UserLayout>
  );
}
