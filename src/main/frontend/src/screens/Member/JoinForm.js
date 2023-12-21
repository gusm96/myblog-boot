import React from "react";
import { useState } from "react";
import { Button, Col, Container, Form, Row } from "react-bootstrap";
import { join } from "../../services/authApi";
import { useEffect } from "react";

export const JoinForm = () => {
  const [formData, setFormData] = useState({
    username: "",
    password1: "",
    password2: "",
    nickname: "",
  });
  const [passwordMatch, setPasswordMatch] = useState(false);

  const isFormFilled = () => {
    return Object.values(formData).every((field) => field !== "");
  };

  // 비밀번호 일치 여부 상태 변수
  useEffect(() => {
    if (formData.password1 !== "" && formData.password2 !== "") {
      setPasswordMatch(confirmPassword(formData.password1, formData.password2));
    } else {
      setPasswordMatch(true);
    }
  }, [formData.password1, formData.password2]);
  const handleOnChange = (e) => {
    const { name, value } = e.target;
    setFormData((prevData) => ({
      ...prevData,
      [name]: value,
    }));
  };

  const confirmPassword = (password1, password2) => {
    return password1 === password2;
  };
  const handleSubmit = (e) => {
    e.preventDefault();
    // 1. 비밀번호와 비밀번호 확인이 동일한지 확인.
    if (!confirmPassword(formData.password1, formData.password2)) {
      setPasswordMatch(false); // 비밀번호 불일치 상태로 설정
      return;
    }
    setPasswordMatch(true); // 비밀번호 일치 상태로 설정

    join(formData)
      .then(() => {
        if (window.confirm("바로 로그인 하시겠습니까?")) {
          window.location.href = "/login";
        } else {
          window.location.href = "/";
        }
      })
      .catch((error) => {
        alert(error.response.data);
      });
  };
  return (
    <div>
      <Container className="panel">
        <Form onSubmit={handleSubmit}>
          <Form.Group
            as={Row}
            className="mb-3"
            controlId="formPlaintextUsername"
          >
            <Col sm>
              <Form.Control
                type="text"
                name="username"
                value={formData.username}
                onChange={handleOnChange}
                placeholder="아이디"
              ></Form.Control>
            </Col>
          </Form.Group>
          <Form.Group
            as={Row}
            className="mb-3"
            controlId="formPlaintextPassword1"
          >
            <Col sm>
              <Form.Control
                type="password"
                name="password1"
                placeholder="비밀번호"
                value={formData.password1}
                onChange={handleOnChange}
                required
              ></Form.Control>
            </Col>
          </Form.Group>
          <Form.Group
            as={Row}
            className="mb-3"
            controlId="formPlaintextPassword2"
          >
            <Col sm>
              <Form.Control
                type="password"
                name="password2"
                placeholder="비밀번호 확인"
                value={formData.password2}
                onChange={handleOnChange}
                required
              ></Form.Control>
              {!passwordMatch &&
                formData.password1 !== "" &&
                formData.password2 !== "" && (
                  <p className="text-danger">비밀번호가 일치하지 않습니다.</p>
                )}
            </Col>
          </Form.Group>
          <Form.Group
            as={Row}
            className="mb-3"
            controlId="formPlaintextNickname"
          >
            <Col sm>
              <Form.Control
                type="text"
                name="nickname"
                placeholder="닉네임"
                value={formData.nickname}
                onChange={handleOnChange}
                required
              ></Form.Control>
            </Col>
          </Form.Group>
          <Button
            variant="primary"
            type="submit"
            disabled={!passwordMatch || !isFormFilled()}
          >
            가입하기
          </Button>
        </Form>
      </Container>
    </div>
  );
};
