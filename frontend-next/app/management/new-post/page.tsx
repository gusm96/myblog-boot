"use client";

import dynamic from "next/dynamic";

// Tiptap은 browser API 의존 → SSR 비활성화
const PostEditorClient = dynamic(
  () => import("@/components/editor/PostEditorClient").then((m) => m.PostEditorClient),
  { ssr: false, loading: () => <div style={{ padding: "2rem" }}>// loading editor...</div> }
);

export default function NewPostPage() {
  return <PostEditorClient />;
}
