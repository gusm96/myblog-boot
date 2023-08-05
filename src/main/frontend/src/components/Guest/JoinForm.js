import axios from "axios";
import React from "react";
import { useState } from "react";
import { Button, Form } from "react-bootstrap";
import { GUEST_REG } from "../../apiConfig";
import { FormContainer } from "../Styles/Container/FormContainer";
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
    <FormContainer>
      <h1>회원가입</h1>
      <Form
        onSubmit={handleSubmit}
        style={{
          border: "1px solid #ccc",
          borderRadius: "10px",
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
            placeholder="사용할 아이디 또는 닉네임을 입력하세요."
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
            placeholder="비밀번호는 4-8자 영문과 숫자를 입력하세요."
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
            placeholder="비밀번호를 확인하세요."
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
          가입하기
        </Button>
      </Form>
    </FormContainer>
  );
};
