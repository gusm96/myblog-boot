import { CommentForm } from "./CommentForm";
import { CommentList } from "./CommentList";

interface CommentSectionProps {
  postId: number;
}

export function CommentSection({ postId }: CommentSectionProps) {
  return (
    <>
      <CommentForm postId={postId} />
      <CommentList postId={postId} />
    </>
  );
}
