import axios from "axios";
import React, { useEffect, useState } from "react";
import { Button, Col, Container, Form, Row } from "react-bootstrap";
import { useCookies } from "react-cookie";
import { LoginConfirmation } from "./LoginConfirmation";

export const AdminLoginForm = () => {
  const [username, setUsername] = useState("");
  const [password, setPassword] = useState("");
  const [cookies, setCookie] = useCookies(["token"]);

  if (LoginConfirmation) {
    alert("잘못된 접근입니다.");
    window.location.href = "/management";
  }

  const handleUsernameChange = (e) => {
    setUsername(e.target.value);
  };

  const handlePasswordChange = (e) => {
    setPassword(e.target.value);
  };

  const handleSubmit = async (e) => {
    console.log(`username = ${username}`);
    console.log(`password = ${password}`);
    e.preventDefault();
    // 서버에 로그인 요청 보내기
    try {
      const response = await axios.post(
        "http://localhost:8080/api/v1/login/admin",
        {
          username,
          password,
        }
      );
      if (response.status === 200) {
        const token = response.data;
        setCookie("token", token, { path: "/" });
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
        <Form.Label>Username</Form.Label>
        <Form.Control
          type="text"
          placeholder="Enter username"
          value={username}
          onChange={handleUsernameChange}
        />
      </Form.Group>

      <Form.Group controlId="formPassword">
        <Form.Label>Password</Form.Label>
        <Form.Control
          type="password"
          placeholder="Enter password"
          value={password}
          onChange={handlePasswordChange}
        />
      </Form.Group>

      <Button variant="primary" type="submit">
        Login
      </Button>
    </Form>
  );
};
