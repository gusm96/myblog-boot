import type { Metadata } from "next";
import { Suspense } from "react";
import LoginContent from "./LoginContent";

export const metadata: Metadata = {
  title: "로그인",
  robots: { index: false, follow: true },
};

export default function LoginPage() {
  return (
    <Suspense fallback={<div className="form-container"><div className="auth-card" /></div>}>
      <LoginContent />
    </Suspense>
  );
}
