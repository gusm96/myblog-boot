"use client";

import { useSelector } from "react-redux";
import Link from "next/link";
import { selectIsLoggedIn } from "@/store/userSlice";
import { CommentForm } from "./CommentForm";
import { CommentList } from "./CommentList";

interface CommentSectionProps {
  postId: number;
}

export function CommentSection({ postId }: CommentSectionProps) {
  const isLoggedIn = useSelector(selectIsLoggedIn);

  return (
    <>
      {isLoggedIn ? (
        <CommentForm postId={postId} />
      ) : (
        <p
          style={{
            color: "var(--text-muted)",
            fontFamily: "var(--font-mono)",
            fontSize: "0.82rem",
            padding: "12px 0",
          }}
        >
          // 댓글을 작성하려면{" "}
          <Link href="/login">로그인</Link>이 필요합니다.
        </p>
      )}
      <CommentList postId={postId} />
    </>
  );
}
