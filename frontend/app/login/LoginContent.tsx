"use client";

import { useState } from "react";
import { useDispatch } from "react-redux";
import { useRouter, useSearchParams } from "next/navigation";
import { Button, Form, InputGroup } from "react-bootstrap";
import { login } from "@/lib/authApi";
import { userLogin } from "@/store/authActions";
import type { AppDispatch } from "@/store";

export default function LoginContent() {
  const [formData, setFormData] = useState({ username: "", password: "" });
  const dispatch = useDispatch<AppDispatch>();
  const router = useRouter();
  const searchParams = useSearchParams();
  const from = searchParams.get("from") ?? "/";

  const handleChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    const { name, value } = e.target;
    setFormData((prev) => ({ ...prev, [name]: value }));
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    try {
      const data = await login(formData);
      dispatch(userLogin(data));
      router.push(from);
    } catch (error: unknown) {
      const err = error as { response?: { data?: { message?: string } } };
      alert(err.response?.data?.message || "로그인에 실패했습니다.");
    }
  };

  return (
    <div className="form-container">
      <div className="auth-card">
        <div className="auth-card-header">
          <div className="auth-title">login</div>
          <div className="auth-subtitle">Moya&apos;s Research Institute</div>
        </div>

        <Form onSubmit={handleSubmit}>
          <Form.Group className="mb-3">
            <InputGroup>
              <InputGroup.Text>ID</InputGroup.Text>
              <Form.Control
                type="text"
                placeholder="아이디를 입력하세요"
                name="username"
                value={formData.username}
                onChange={handleChange}
                required
                autoComplete="username"
              />
            </InputGroup>
          </Form.Group>

          <Form.Group className="mb-4">
            <InputGroup>
              <InputGroup.Text>PW</InputGroup.Text>
              <Form.Control
                type="password"
                placeholder="비밀번호를 입력하세요"
                name="password"
                value={formData.password}
                onChange={handleChange}
                required
                autoComplete="current-password"
              />
            </InputGroup>
          </Form.Group>

          <div className="auth-actions">
            <Button variant="primary" type="submit">
              로그인
            </Button>
          </div>
        </Form>
      </div>
    </div>
  );
}
