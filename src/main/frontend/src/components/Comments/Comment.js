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
import { addComment } from "../../services/boardApi";

export const Comment = ({ boardId, accessToken }) => {
  const isLoggedIn = useSelector(selectIsLoggedIn);
  const [isLoggedInModal, setIsLoggedInModal] = useState(false);
  const [commentData, setCommentData] = useState({
    comment: "",
    parentId: "",
  });

  const handleChange = (e) => {
    const { name, value } = e.target;
    setCommentData((prevData) => ({
      ...prevData,
      [name]: value,
    }));
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
    if (commentData.comment.trim() === "") {
      alert("댓글의 내용을 입력하세요.");
    } else {
      addComment(boardId, commentData, accessToken)
        .then((data) => console.log(data))
        .catch((error) => console.log(error));
    }
  };

  const handleCloseLoginModal = () => {
    setIsLoggedInModal(false);
  };
  return (
    <form
      onSubmit={handleOnSubmit}
      style={{
        display: "flex",
      }}
    >
      <textarea
        onClick={handleCommentBoxClick}
        name="comment"
        placeholder="댓글을 입력하세요."
        value={commentData.comment}
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
    </form>
  );
};
