"use client";

import { Button } from "react-bootstrap";
import { useRouter } from "next/navigation";

interface PageButtonProps {
  pageCount: number;
  path: string;
  currentPage?: number;
}

export function PageButton({ pageCount, path, currentPage }: PageButtonProps) {
  const router = useRouter();

  const handleClick = (page: number) => {
    router.push(`/${path}p=${page}`);
  };

  return (
    <div className="page-buttons-container">
      {Array.from({ length: pageCount }, (_, i) => i + 1).map((page) => (
        <Button
          key={page}
          onClick={() => handleClick(page)}
          className={currentPage === page ? "active" : ""}
        >
          {page}
        </Button>
      ))}
    </div>
  );
}
