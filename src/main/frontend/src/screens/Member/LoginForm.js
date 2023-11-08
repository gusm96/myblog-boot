import React from "react";
import { useState } from "react";
import { login } from "../../services/authApi";
import { Button, Form, Nav } from "react-bootstrap";
import { userLogin } from "../../redux/authAction";
import { useDispatch } from "react-redux";
import { useLocation, useNavigate } from "react-router-dom";

export const LoginForm = () => {
  const location = useLocation();
  const from = location.state?.from || "/";
  const navigate = useNavigate();
  const [formData, setFormData] = useState({
    username: "",
    password: "",
  });

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
        dispatch(userLogin(data));
        navigate(from);
      })
      .catch((error) => {
        alert(error.response.data);
      });
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
      <hr></hr>
      <Nav.Link href="/join">회원가입</Nav.Link>
    </Form>
  );
};
