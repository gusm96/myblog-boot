import { Suspense } from "react";
import { SearchContent } from "@/components/posts/SearchContent";

export const metadata = {
  title: "검색",
  description: "게시글 검색 결과",
};

export default function SearchPage() {
  return (
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
  );
}
