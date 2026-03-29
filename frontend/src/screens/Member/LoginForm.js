import React from "react";
import { useState } from "react";
import { login } from "../../services/authApi";
import { Button, Form, InputGroup } from "react-bootstrap";
import { userLogin } from "../../redux/authAction";
import { useDispatch } from "react-redux";
import { useLocation, useNavigate } from "react-router";
import "../../components/Styles/css/form-container.css";

export const LoginForm = () => {
  const location = useLocation();
  const from = location.state?.from || "/";
  const navigate = useNavigate();
  const [formData, setFormData] = useState({ username: "", password: "" });
  const dispatch = useDispatch();

  const handleChange = (e) => {
    const { name, value } = e.target;
    setFormData((prev) => ({ ...prev, [name]: value }));
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    await login(formData)
      .then((data) => {
        dispatch(userLogin(data));
        navigate(from);
      })
      .catch((error) => {
        alert(error.response?.data?.message || "로그인에 실패했습니다.");
      });
  };

  return (
    <div className="form-container">
      <div className="auth-card">
        <div className="auth-card-header">
          <div className="auth-title">login</div>
          <div className="auth-subtitle">Moya's Research Institute</div>
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
            <Button variant="secondary" onClick={() => navigate("/join")}>
              회원가입
            </Button>
          </div>
        </Form>
      </div>
    </div>
  );
};
