import axios from "axios";
import React, { useEffect, useState } from "react";
import {
  Button,
  Modal,
  ModalBody,
  ModalFooter,
  ModalHeader,
  ModalTitle,
} from "react-bootstrap";
import { COMMENT_CUD } from "../../apiConfig";

export const Comment = () => {
  const [sessionData, setSessionData] = useState(null);
  const [isLoggedIn, setIsLoggedIn] = useState({
    status: false,
    loginType: "",
  });
  const [isLoginModal, setIsLoginModal] = useState(false);
  const [commentData, setCommentData] = useState("");

  useEffect(() => {
    const admin = true;
    if (admin) {
      setIsLoggedIn({
        status: true,
        loginType: "ADMIN",
      });
    } else {
      alert("게스트 오류");
      // if (guest !== null) {
      //   setIsLoggedIn({
      //     status: true,
      //     loginType: "GUEST",
      //   });
      // }
    }
  }, []);

  const handleChange = (e) => {
    setCommentData(e.target.value);
  };

  const handleCommentBoxClick = (e) => {
    e.preventDefault();
    if (!isLoggedIn.status) {
      setIsLoginModal(true);
    }
  };

  const handleOnSubmit = (e) => {
    e.preventDefault();
    // textarea의 값이 0 or null or " "이 아닌지 확인
    if (commentData.trim() === "") {
      alert("댓글의 내용을 입력하세요.");
    } else {
      if (isLoggedIn.loginType === "GUEST") {
        axios.post(`${COMMENT_CUD}`, {
          writer: sessionData,
          comment: commentData,
          commentType: isLoggedIn.loginType,
        });
      } else {
        axios.post(`${COMMENT_CUD}`, {
          writer: "admin",
          comment: commentData,
          commentType: isLoggedIn.loginType,
        });
      }
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
        <ModalBody></ModalBody>
        <ModalFooter>회원가입</ModalFooter>
      </Modal>
    </div>
  );
};
