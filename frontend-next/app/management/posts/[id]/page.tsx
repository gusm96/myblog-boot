"use client";

import dynamic from "next/dynamic";
import { use } from "react";

const PostEditFormClient = dynamic(
  () => import("@/components/editor/PostEditFormClient").then((m) => m.PostEditFormClient),
  { ssr: false, loading: () => <div style={{ padding: "2rem" }}>// loading editor...</div> }
);

export default function EditPostPage({
  params,
}: {
  params: Promise<{ id: string }>;
}) {
  const { id } = use(params);
  return <PostEditFormClient postId={id} />;
}
