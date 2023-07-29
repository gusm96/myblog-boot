import axios from "axios";
import React, { useState } from "react";
import { Form, Button } from "react-bootstrap";
import reactSessionApi from "react-session-api";
import { GUEST_LOGIN } from "../../apiConfig";

export const LoginForm = () => {
  const [formData, setFormData] = useState({
    username: "",
    password: "",
  });

  const handleChange = (e) => {
    const { name, value } = e.target;
    setFormData((prevData) => ({
      ...prevData,
      [name]: value,
    }));
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    await axios
      .post(GUEST_LOGIN, {
        username: formData.username,
        password: formData.password,
      })
      .then((res) => res.data)
      .then((data) => reactSessionApi.set("Guest", data))
      .catch((error) => {
        alert("아이디 또는 비밀번호를 확인하세요.");
        console.log(error);
      });
  };

  return (
    <Form onSubmit={handleSubmit}>
      <Form.Group controlId="username">
        <Form.Label>아이디</Form.Label>
        <Form.Control
          type="text"
          name="username"
          value={formData.username}
          onChange={handleChange}
          required
        />
      </Form.Group>

      <Form.Group controlId="password">
        <Form.Label>비밀번호</Form.Label>
        <Form.Control
          type="password"
          name="password"
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
