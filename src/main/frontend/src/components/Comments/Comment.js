import axios from "axios";
import React, { useState } from "react";
import {
  Button,
  Modal,
  ModalBody,
  ModalFooter,
  ModalHeader,
  ModalTitle,
} from "react-bootstrap";
import { useSelector } from "react-redux";
import { selectIsLoggedIn } from "../../redux/userSlice";

export const Comment = () => {
  const isLoggedIn = useSelector(selectIsLoggedIn);
  const [isLoggedInModal, setIsLoggedInModal] = useState(false);
  const [commentData, setCommentData] = useState("");

  const handleChange = (e) => {
    setCommentData(e.target.value);
  };

  const handleCommentBoxClick = (e) => {
    e.preventDefault();
    if (!isLoggedIn) {
      setIsLoggedInModal(true);
    }
  };

  const handleOnSubmit = (e) => {
    e.preventDefault();
    // textarea의 값이 0 or null or " "이 아닌지 확인
    if (commentData.trim() === "") {
      alert("댓글의 내용을 입력하세요.");
    } else {
    }
  };

  const handleCloseLoginModal = () => {
    setIsLoggedInModal(false);
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
        show={isLoggedInModal}
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
