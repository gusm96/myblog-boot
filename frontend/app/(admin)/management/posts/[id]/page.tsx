import { PostEditFormLazy } from "@/components/editor/PostEditorDynamic";

export default async function EditPostPage({
  params,
}: {
  params: Promise<{ id: string }>;
}) {
  const { id } = await params;
  return <PostEditFormLazy postId={id} />;
}
