import axios from "axios";
import React, { useState } from "react";
import { Button, Form } from "react-bootstrap";
import { useCookies } from "react-cookie";
import { LoginConfirmation } from "./LoginConfirmation";
import { ADMIN_LOGIN } from "../../apiConfig";
export const AdminLoginForm = () => {
  const [formData, setFormData] = useState({
    username: "",
    password: "",
  });
  const [cookies, setCookie] = useCookies(["token"]);
  const isLoggedIn = LoginConfirmation();
  if (isLoggedIn) {
    alert("로그인 된 상태");
    window.location.href = "/management";
  }

  const handleChange = (e) => {
    const { name, value } = e.target;
    setFormData((prevData) => ({
      ...prevData,
      [name]: value,
    }));
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    // 서버에 로그인 요청 보내기
    try {
      const response = await axios.post(`${ADMIN_LOGIN}`, {
        username: formData.username,
        password: formData.password,
      });
      if (response.status === 200) {
        const token = response.data;
        // Access Token은 private 변수에 저장
        setCookie("refresh_token", token.refreshToken, { path: "/" });
        window.location.href = "/management";
        // 리다이렉트
      } else {
        // 실패시 ..
        alert("아이디 또는 비밀번호가 일치하지 않습니다.");
      }
    } catch (error) {
      alert("아이디 또는 비밀번호가 일치하지 않습니다.");
    }
  };
  return (
    <Form onSubmit={handleSubmit}>
      <Form.Group controlId="formUsername">
        <Form.Label>아이디</Form.Label>
        <Form.Control
          type="text"
          name="username"
          placeholder="아이디를 입력하세요."
          value={formData.username}
          onChange={handleChange}
          required
        />
      </Form.Group>

      <Form.Group controlId="formPassword">
        <Form.Label>비밀번호</Form.Label>
        <Form.Control
          type="password"
          name="password"
          placeholder="비밀번호를 입력하세요."
          value={formData.password}
          onChange={handleChange}
          required
        />
      </Form.Group>

      <Button variant="primary" type="submit">
        로그인
      </Button>
    </Form>
  );
};
