"use client";

import { useEffect, useState } from "react";
import { useSelector, useDispatch } from "react-redux";
import { useRouter } from "next/navigation";
import { Spinner } from "react-bootstrap";
import { selectIsLoggedIn } from "@/store/userSlice";
import { userLogout } from "@/store/authActions";
import { getRoleFromToken } from "@/lib/authApi";
import type { AppDispatch } from "@/store";

export function RoleGate({ children }: { children: React.ReactNode }) {
  const [role, setRole] = useState<string | null>(null);
  const isLoggedIn = useSelector(selectIsLoggedIn);
  const dispatch = useDispatch<AppDispatch>();
  const router = useRouter();

  useEffect(() => {
    if (!isLoggedIn) {
      router.replace("/login?from=/management");
      return;
    }
    getRoleFromToken()
      .then((data: string) => setRole(data))
      .catch((error: { response?: { status?: number } }) => {
        if (error.response?.status === 401) {
          dispatch(userLogout());
          router.replace("/login?from=/management");
        }
      });
  }, [isLoggedIn, dispatch, router]);

  if (!isLoggedIn || role === null) {
    return (
      <div style={{ display: "flex", justifyContent: "center", padding: "4rem 0" }}>
        <Spinner animation="border" role="status">
          <span className="visually-hidden">Loading...</span>
        </Spinner>
      </div>
    );
  }

  if (role !== "ROLE_ADMIN") {
    router.replace("/");
    return null;
  }

  return <>{children}</>;
}
