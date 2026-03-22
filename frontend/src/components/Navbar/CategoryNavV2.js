import { useQuery } from "@tanstack/react-query";
import React from "react";
import { getCategoriesV2 } from "../../services/categoryApi";
import { ListGroup, ListGroupItem } from "react-bootstrap";
import { Link, useParams, useLocation } from "react-router";
import "../Styles/css/listGroup.css";

export const CategoryNavV2 = () => {
  const location = useLocation();
  const { categoryName } = useParams();

  const { isPending, isError, data } = useQuery({
    queryKey: ["categories"],
    queryFn: getCategoriesV2,
    staleTime: 600000,
  });

  const isActive = (name) => {
    if (name === null) {
      return location.pathname === "/" && !categoryName;
    }
    return categoryName === name;
  };

  if (isPending) {
    return (
      <div className="sidebar-section">
        <span className="sidebar-label">// categories</span>
        <div style={{ padding: "8px 6px", color: "var(--text-faint)", fontFamily: "var(--font-mono)", fontSize: "0.78rem" }}>
          loading...
        </div>
      </div>
    );
  }
  if (isError) {
    return (
      <div className="sidebar-section">
        <span className="sidebar-label">// categories</span>
      </div>
    );
  }

  return (
    <div className="sidebar-section">
      <span className="sidebar-label">// categories</span>
      <ListGroup id="category-list-group">
        <ListGroupItem className="category-list-item">
          <Link
            className={`category-link${isActive(null) ? " active-category" : ""}`}
            to="/"
          >
            <span>전체보기</span>
            <span className="category-count">
              {data.reduce((sum, c) => sum + (c.boardsCount || 0), 0)}
            </span>
          </Link>
        </ListGroupItem>
        {data.map((c) => (
          <ListGroupItem key={c.id} className="category-list-item">
            <Link
              className={`category-link${isActive(c.name) ? " active-category" : ""}`}
              to={`/${c.name}`}
            >
              <span>{c.name}</span>
              <span className="category-count">{c.boardsCount}</span>
            </Link>
          </ListGroupItem>
        ))}
      </ListGroup>
    </div>
  );
};
