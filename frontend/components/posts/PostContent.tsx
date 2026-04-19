/**
 * 게시글 본문 렌더러 — Server Component
 * 백엔드에서 온 신뢰된 HTML을 그대로 렌더링 (Tiptap 에디터 출력)
 */
interface PostContentProps {
  content: string;
}

export default function PostContent({ content }: PostContentProps) {
  return (
    <div
      className="post-content"
      dangerouslySetInnerHTML={{ __html: content }}
    />
  );
}
