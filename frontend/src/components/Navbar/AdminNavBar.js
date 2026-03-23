import React from "react";
import { ListGroup, ListGroupItem } from "react-bootstrap";
import { Link, useLocation } from "react-router";
import "../Styles/css/listGroup.css";

const NAV_ITEMS = [
  { label: "전체 게시글", to: "/management", icon: "fa-list" },
  { label: "게시글 작성", to: "/management/new-post", icon: "fa-pen-to-square" },
  { label: "카테고리 관리", to: "/management/categories", icon: "fa-folder-open" },
  { label: "휴지통", to: "/management/temporary-storage", icon: "fa-trash-can" },
];

export const AdminNavBar = () => {
  const { pathname } = useLocation();

  const isActive = (to) => {
    if (to === "/management") return pathname === "/management";
    return pathname.startsWith(to);
  };

  return (
    <div className="sidebar-section">
      <span className="sidebar-label">// admin</span>
      <ListGroup id="category-list-group">
        {NAV_ITEMS.map(({ label, to, icon }) => (
          <ListGroupItem key={to} className="category-list-item">
            <Link
              className={`category-link admin-nav-link${isActive(to) ? " active-category" : ""}`}
              to={to}
            >
              <span className="admin-nav-link__label">
                <i className={`fa-solid ${icon} admin-nav-link__icon`} aria-hidden="true" />
                {label}
              </span>
            </Link>
          </ListGroupItem>
        ))}
      </ListGroup>
    </div>
  );
};
