import React from "react";
import { useState } from "react";
import { useCookies } from "react-cookie";
import { login } from "../../services/authApi";
import { Button, Form } from "react-bootstrap";
import { userLogin } from "../../redux/authAction";
import { useDispatch } from "react-redux";
import Redirect from "../../components/Redirect";

export const LoginForm = () => {
  // 1. if 현재 로그인 상태인가 ?
  //  경우 1. Token 존재 및 유효
  // 경우 2. Token 존재, 만료 but refresh Token 유효
  // 방법 1. Cookie 에 Token이 있다면 Server에 유휴성 검사 요청.
  const [formData, setFormData] = useState({
    username: "",
    password: "",
  });
  const [cookies, setCookies] = useCookies(["refresh_token"]);

  const dispatch = useDispatch();

  const handleChange = (e) => {
    const { name, value } = e.target;
    setFormData((prevData) => ({
      ...prevData,
      [name]: value,
    }));
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    await login(formData)
      .then((data) => {
        setCookies("refresh_token", data.refresh_token, { path: "/" });
        dispatch(userLogin(data.access_token));
      })
      .catch((error) => console.log(error));
  };

  return (
    <Form onSubmit={handleSubmit}>
      {/* 아이디 */}
      <Form.Group className="mb-3" controlId="formUsername">
        <Form.Label>아이디</Form.Label>
        <Form.Control
          type="text"
          placeholder="아이디를 입력하세요."
          name="username"
          value={formData.username}
          onChange={handleChange}
          required
        ></Form.Control>
      </Form.Group>
      {/* 비밀번호 */}
      <Form.Group className="mb-3" controlId="formPassword">
        <Form.Label>비밀번호</Form.Label>
        <Form.Control
          type="password"
          placeholder="비밀번호를 입력하세요."
          name="password"
          value={formData.password}
          onChange={handleChange}
          required
        ></Form.Control>
      </Form.Group>
      {/* 로그인 버튼 */}
      <Button variant="primary" type="submit">
        로그인
      </Button>
    </Form>
  );
};
