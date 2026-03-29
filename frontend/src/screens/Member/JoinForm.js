import React from "react";
import { useState, useEffect } from "react";
import { Button, Form } from "react-bootstrap";
import { join } from "../../services/authApi";
import { useNavigate } from "react-router";
import "../../components/Styles/css/form-container.css";

export const JoinForm = () => {
  const navigate = useNavigate();
  const [formData, setFormData] = useState({
    username: "",
    password1: "",
    password2: "",
    nickname: "",
  });
  const [passwordMatch, setPasswordMatch] = useState(true);

  const isFormFilled = () => Object.values(formData).every((f) => f !== "");

  useEffect(() => {
    if (formData.password1 !== "" && formData.password2 !== "") {
      setPasswordMatch(formData.password1 === formData.password2);
    } else {
      setPasswordMatch(true);
    }
  }, [formData.password1, formData.password2]);

  const handleOnChange = (e) => {
    const { name, value } = e.target;
    setFormData((prev) => ({ ...prev, [name]: value }));
  };

  const handleSubmit = (e) => {
    e.preventDefault();
    if (formData.password1 !== formData.password2) {
      setPasswordMatch(false);
      return;
    }
    join(formData)
      .then(() => {
        if (window.confirm("바로 로그인 하시겠습니까?")) {
          navigate("/login");
        } else {
          navigate("/");
        }
      })
      .catch((error) => {
        alert(error.response?.data?.message || "회원가입에 실패했습니다.");
      });
  };

  return (
    <div className="form-container">
      <div className="auth-card">
        <div className="auth-card-header">
          <div className="auth-title">register</div>
          <div className="auth-subtitle">새 계정을 만드세요</div>
        </div>

        <Form onSubmit={handleSubmit}>
          <Form.Group className="mb-3">
            <Form.Control
              type="text"
              name="username"
              value={formData.username}
              onChange={handleOnChange}
              placeholder="아이디"
              autoComplete="username"
              required
            />
          </Form.Group>

          <Form.Group className="mb-3">
            <Form.Control
              type="password"
              name="password1"
              placeholder="비밀번호"
              value={formData.password1}
              onChange={handleOnChange}
              autoComplete="new-password"
              required
            />
          </Form.Group>

          <Form.Group className="mb-3">
            <Form.Control
              type="password"
              name="password2"
              placeholder="비밀번호 확인"
              value={formData.password2}
              onChange={handleOnChange}
              autoComplete="new-password"
              required
            />
            {!passwordMatch && formData.password1 !== "" && formData.password2 !== "" && (
              <Form.Text className="text-danger">
                비밀번호가 일치하지 않습니다.
              </Form.Text>
            )}
          </Form.Group>

          <Form.Group className="mb-4">
            <Form.Control
              type="text"
              name="nickname"
              placeholder="닉네임"
              value={formData.nickname}
              onChange={handleOnChange}
              required
            />
          </Form.Group>

          <div className="auth-actions">
            <Button
              variant="primary"
              type="submit"
              disabled={!passwordMatch || !isFormFilled()}
            >
              가입하기
            </Button>
            <Button variant="secondary" onClick={() => navigate("/login")}>
              로그인으로
            </Button>
          </div>
        </Form>
      </div>
    </div>
  );
};
