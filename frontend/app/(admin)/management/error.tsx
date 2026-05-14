"use client";

import { useEffect } from "react";
import { useRouter } from "next/navigation";

export default function ManagementError({
  error,
  reset,
}: {
  error: Error & { digest?: string };
  reset: () => void;
}) {
  const router = useRouter();

  useEffect(() => {
    console.error("[ManagementError]", error);
  }, [error]);

  return (
    <div role="alert" className="container py-5 text-center">
      <h2 className="mb-3">요청을 처리할 수 없습니다</h2>
      <p className="text-muted mb-4">
        세션이 만료되었거나 일시적 오류가 발생했을 수 있어요.
      </p>
      <div className="d-flex justify-content-center gap-2">
        <button className="btn btn-outline-secondary" onClick={reset}>
          다시 시도
        </button>
        <button
          className="btn btn-primary"
          onClick={() => router.push("/login?from=/management")}
        >
          로그인 페이지로
        </button>
      </div>
    </div>
  );
}
