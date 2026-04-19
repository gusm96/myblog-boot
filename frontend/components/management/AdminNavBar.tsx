"use client";

import Link from "next/link";
import { usePathname } from "next/navigation";
import { useDispatch } from "react-redux";
import { useRouter } from "next/navigation";
import { logout } from "@/lib/authApi";
import { userLogout } from "@/store/authActions";
import type { AppDispatch } from "@/store";

const navItems = [
  { href: "/management", label: "게시글 목록", icon: "fa-list" },
  { href: "/management/new-post", label: "새 게시글", icon: "fa-pen-to-square" },
  { href: "/management/categories", label: "카테고리 관리", icon: "fa-folder" },
  { href: "/management/temporary-storage", label: "휴지통", icon: "fa-trash" },
];

export function AdminNavBar() {
  const pathname = usePathname();
  const dispatch = useDispatch<AppDispatch>();
  const router = useRouter();

  const handleLogout = async () => {
    try {
      await logout();
    } finally {
      dispatch(userLogout());
      router.push("/");
    }
  };

  return (
    <nav className="admin-nav">
      <ul className="admin-nav__list">
        {navItems.map((item) => {
          const isActive =
            item.href === "/management"
              ? pathname === "/management"
              : pathname.startsWith(item.href);
          return (
            <li key={item.href} className="admin-nav__item">
              <Link
                href={item.href}
                className={`admin-nav__link${isActive ? " active" : ""}`}
              >
                <i className={`fa-solid ${item.icon}`} />
                {item.label}
              </Link>
            </li>
          );
        })}
      </ul>
      <button className="admin-nav__logout" onClick={handleLogout}>
        <i className="fa-solid fa-right-from-bracket" />
        로그아웃
      </button>
    </nav>
  );
}
