"use client";

import { useQuery } from "@tanstack/react-query";
import { ListGroup, ListGroupItem } from "react-bootstrap";
import Link from "next/link";
import { usePathname, useParams } from "next/navigation";
import { queryKeys } from "@/lib/queryKeys";
import type { CategoryV2 } from "@/types";
import apiClient from "@/lib/apiClient";

const fetchCategoriesV2 = (): Promise<CategoryV2[]> =>
  apiClient.get("/api/v2/categories").then((res) => res.data);

export function CategoryNav() {
  const pathname = usePathname();
  const params = useParams();
  const categoryName = params?.name as string | undefined;

  const { isPending, isError, data } = useQuery<CategoryV2[]>({
    queryKey: queryKeys.categories.all(),
    queryFn: fetchCategoriesV2,
    staleTime: 30 * 60 * 1000,
    gcTime: 60 * 60 * 1000,
  });

  const isActive = (name: string | null) => {
    if (name === null) {
      return pathname === "/" && !categoryName;
    }
    return categoryName === name;
  };

  if (isPending) {
    return (
      <div className="sidebar-section">
        <span className="sidebar-label">// categories</span>
        <div
          style={{
            padding: "8px 6px",
            color: "var(--text-faint)",
            fontFamily: "var(--font-mono)",
            fontSize: "0.78rem",
          }}
        >
          loading...
        </div>
      </div>
    );
  }
  if (isError || !data) {
    return (
      <div className="sidebar-section">
        <span className="sidebar-label">// categories</span>
      </div>
    );
  }

  const totalCount = data.reduce((sum, c) => sum + (c.postsCount || 0), 0);

  return (
    <div className="sidebar-section">
      <span className="sidebar-label">// categories</span>
      <ListGroup id="category-list-group">
        <ListGroupItem className="category-list-item">
          <Link
            className={`category-link${isActive(null) ? " active-category" : ""}`}
            href="/"
          >
            <span>전체보기</span>
            <span className="category-count">{totalCount}</span>
          </Link>
        </ListGroupItem>
        {data.map((c) => (
          <ListGroupItem key={c.id} className="category-list-item">
            <Link
              className={`category-link${isActive(c.name) ? " active-category" : ""}`}
              href={`/category/${c.name}`}
            >
              <span>{c.name}</span>
              <span className="category-count">{c.postsCount}</span>
            </Link>
          </ListGroupItem>
        ))}
      </ListGroup>
    </div>
  );
}
