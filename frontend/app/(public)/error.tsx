"use client";

import { useEffect } from "react";

export default function PublicError({
  error,
  reset,
}: {
  error: Error & { digest?: string };
  reset: () => void;
}) {
  useEffect(() => {
    console.error("[PublicError]", error);
  }, [error]);

  return (
    <div role="alert" className="container py-5 text-center">
      <h2 className="mb-3">잠시 불러올 수 없습니다</h2>
      <p className="text-muted mb-4">
        네트워크 또는 서버 오류가 발생했어요. 잠시 후 다시 시도해 주세요.
      </p>
      <button className="btn btn-primary" onClick={reset}>
        다시 시도
      </button>
      {error.digest && (
        <p className="text-muted small mt-3">오류 코드: {error.digest}</p>
      )}
    </div>
  );
}
