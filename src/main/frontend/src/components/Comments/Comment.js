import axios from "axios";
import React, { useEffect, useState } from "react";
import {
  Button,
  Col,
  Container,
  Modal,
  ModalBody,
  ModalDialog,
  ModalFooter,
  ModalHeader,
  ModalTitle,
  Row,
} from "react-bootstrap";
import reactSessionApi from "react-session-api";
import { COMMENT_CUD } from "../../apiConfig";
import { LoginForm } from "../Guest/LoginForm";
import { FormContainer } from "../Styles/Container/FormContainer";

export const Comment = () => {
  const [sessionData, setSesstionData] = useState(null);
  const [isLoggedIn, setIsLoggedIn] = useState(false);
  const [isLoginModal, setIsLoginModal] = useState(false);
  const [commentData, setCommentData] = useState("");
  useEffect(() => {
    const guest = reactSessionApi.get("guest");
    setSesstionData(guest);
    if (sessionData !== null) {
      setIsLoggedIn(true);
    }
  }, []);
  const handleChange = (e) => {
    setCommentData(e.target.value);
  };
  const handleCommentBoxClick = (e) => {
    e.preventDefault();
    if (!isLoggedIn) {
      console.log("true");
      setIsLoginModal(true);
    }
  };

  const handleOnSubmit = (e) => {
    e.preventDefault();
    // textarea의 값이 0 or null or " "이 아닌지 확인
    if (commentData !== null || commentData.trim() === "") {
      alert("댓글의 내용을 입력하세요.");
    } else {
      axios.post(`${COMMENT_CUD}`, {
        writer: sessionData,
        comment: commentData.comment,
        commentType: "GUEST",
      });
    }
  };

  const handleCloseLoginModal = () => {
    setIsLoginModal(false);
  };
  return (
    <div
      onSubmit={handleOnSubmit}
      style={{
        display: "flex",
      }}
    >
      <textarea
        onClick={handleCommentBoxClick}
        name="comment"
        placeholder="댓글을 입력하세요."
        value={commentData}
        onChange={handleChange}
        style={{
          width: "70%",
          margin: "0, 0",
        }}
      ></textarea>
      <Button
        variant="primary"
        type="submit"
        style={{
          width: "30%",
          margin: "0, 0",
        }}
      >
        댓글작성
      </Button>
      <Modal
        show={isLoginModal}
        onHide={handleCloseLoginModal}
        style={{
          display: "flex",
          alignItems: "center",
          justifyContent: "center",
          position: "absolute",
          top: "0",
          left: "0",
        }}
      >
        <ModalHeader closeButton>
          <ModalTitle>로그인</ModalTitle>
        </ModalHeader>
        <ModalBody>
          <LoginForm />
        </ModalBody>
        <ModalFooter>회원가입</ModalFooter>
      </Modal>
    </div>
  );
};
