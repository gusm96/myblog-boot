import axios from "axios";
import React, { useState } from "react";
import { Form, Button } from "react-bootstrap";
import { GUEST_LOGIN } from "../../apiConfig";
import { FormContainer } from "../Styles/Container/FormContainer";
import { useCookie } from "react-cookie";

export const LoginForm = () => {
  const [formData, setFormData] = useState({
    username: "",
    password: "",
  });
  const [cookie, setCookie] = useCookie(["token"]);

  const handleChange = (e) => {
    const { name, value } = e.target;
    setFormData((prevData) => ({
      ...prevData,
      [name]: value,
    }));
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    try {
      const response = await axios.post(`${GUEST_LOGIN}`, {
        username: formData.username,
        password: formData.password,
      });
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
    <FormContainer>
      <Form
        onSubmit={handleSubmit}
        style={{
          width: "500px",
          padding: "20px",
        }}
      >
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

        <Button
          variant="primary"
          type="submit"
          style={{
            width: "100%",
            marginTop: "30px",
          }}
        >
          로그인
        </Button>
      </Form>
    </FormContainer>
  );
};
