"use client";

import dynamic from "next/dynamic";

const loading = () => <div style={{ padding: "2rem" }}>// loading editor...</div>;

// Tiptap은 window 의존 → SSR 비활성화
export const PostEditorLazy = dynamic(
  () => import("./PostEditorClient").then((m) => m.PostEditorClient),
  { ssr: false, loading },
);

export const PostEditFormLazy = dynamic(
  () => import("./PostEditFormClient").then((m) => m.PostEditFormClient),
  { ssr: false, loading },
);
