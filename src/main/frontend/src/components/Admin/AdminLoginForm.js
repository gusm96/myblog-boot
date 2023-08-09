import axios from "axios";
import React, { useState } from "react";
import { Button, Form } from "react-bootstrap";
import { useCookies } from "react-cookie";
import { ADMIN_LOGIN } from "../../apiConfig";
import { useDispatch } from "react-redux";
import { login } from "../../store/userSlice";
import { useNavigate } from "react-router-dom";
export const AdminLoginForm = () => {
  const dispatch = useDispatch();
  const [formData, setFormData] = useState({
    username: "",
    password: "",
  });
  const [cookies, setCookie] = useCookies(["refresh_token"]);
  const navigate = useNavigate();
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
    await axios
      .post(`${ADMIN_LOGIN}`, {
        username: formData.username,
        password: formData.password,
      })
      .then((res) => res.data)
      .then((data) => {
        // Refresh Token은 Cookie에 저장.
        setCookie("refresh_token", data.refresh_token, { path: "/" });
        dispatch(
          login({
            accessToken: data.access_token,
            userType: "ADMIN",
          })
        );
        navigate("/management");
      })
      .catch((error) => {
        alert(error.response.data.message);
        console.log(error);
      });
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
