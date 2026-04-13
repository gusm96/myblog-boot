import { Suspense } from "react";
import LoginContent from "./LoginContent";

export default function LoginPage() {
  return (
    <Suspense fallback={<div className="form-container"><div className="auth-card" /></div>}>
      <LoginContent />
    </Suspense>
  );
}
