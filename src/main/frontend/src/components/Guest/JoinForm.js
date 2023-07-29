import axios from "axios";
import React from "react";
import { useState } from "react";
import { Button, Form } from "react-bootstrap";
import { GUEST_REG } from "../../apiConfig";
export const JoinForm = () => {
  const [formData, setFormData] = useState({
    username: "",
    password: "",
    password2: "",
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
    if (formData.password === formData.password2) {
      await axios
        .post(GUEST_REG, {
          username: formData.username,
          password: formData.password,
        })
        .then((res) => res.data)
        .then((data) => {
          alert(data);
          window.location.href = "/login/guest";
        })
        .catch((error) => {
          alert("등록에 실패하였습니다.");
          console.log(error);
        });
    } else {
      alert("비밀번호를 확인하세요.");
    }
  };
  return (
    <Form onSubmit={handleSubmit}>
      <Form.Group controlId="username">
        <Form.Label>아이디 또는 닉네임</Form.Label>
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

      <Form.Group controlId="password2">
        <Form.Label>비밀번호 확인</Form.Label>
        <Form.Control
          type="password"
          name="password2"
          value={formData.password2}
          onChange={handleChange}
          required
        />
      </Form.Group>

      <Button variant="primary" type="submit">
        가입하기
      </Button>
    </Form>
  );
};
