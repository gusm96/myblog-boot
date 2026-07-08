"use client";

import { useQuery } from "@tanstack/react-query";
import { ListGroup, ListGroupItem } from "react-bootstrap";
import Link from "next/link";
import { usePathname, useParams } from "next/navigation";
import { queryKeys } from "@/lib/queryKeys";
import { clientGetPublicTags } from "@/lib/postApi";
import type { Tag } from "@/types";

export function TagNav() {
  const pathname = usePathname();
  const params = useParams();
  const tagSlug = params?.slug as string | undefined;

  const { isPending, isError, data } = useQuery<Tag[]>({
    queryKey: queryKeys.tags.publicList(),
    queryFn: clientGetPublicTags,
    staleTime: 30 * 60 * 1000,
    gcTime: 60 * 60 * 1000,
  });

  const isActive = (slug: string | null) => {
    if (slug === null) {
      return pathname === "/" && !tagSlug;
    }
    return tagSlug === slug;
  };

  if (isPending) {
    return (
      <div className="sidebar-section">
        <span className="sidebar-label">// tags</span>
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
        <span className="sidebar-label">// tags</span>
      </div>
    );
  }

  const totalCount = data.reduce((sum, t) => sum + (t.postCount || 0), 0);

  return (
    <div className="sidebar-section">
      <span className="sidebar-label">// tags</span>
      <ListGroup id="tag-list-group">
        <ListGroupItem className="category-list-item">
          <Link
            className={`category-link${isActive(null) ? " active-category" : ""}`}
            href="/"
          >
            <span>전체보기</span>
            <span className="category-count">{totalCount}</span>
          </Link>
        </ListGroupItem>
        {data.map((t) => (
          <ListGroupItem key={t.id} className="category-list-item">
            <Link
              className={`category-link${isActive(t.slug) ? " active-category" : ""}`}
              href={`/tag/${t.slug}`}
            >
              <span>{t.name}</span>
              <span className="category-count">{t.postCount}</span>
            </Link>
          </ListGroupItem>
        ))}
      </ListGroup>
    </div>
  );
}
