import React, { useState } from "react";
import { Button, Modal } from "react-bootstrap";
import { useCookies } from "react-cookie";

export const Logout = () => {
  const [isLoggedOut, setLoggedOut] = useState(false);
  const [cookies, setCookies, removeCookies] = useCookies(["token"]);
  const handleOnClick = (e) => {
    e.preventDefault();
    try {
      removeCookies("token");
      window.location.href = "/";
      alert("로그아웃 되었습니다.");
    } catch (error) {
      alert("로그아웃을 실패하였습니다.");
      console.log(error);
    }
  };
  return (
    <div
      className="modal show"
      style={{ display: "block", position: "initial" }}
    >
      <Modal.Dialog>
        <Modal.Header closeButton>
          <Modal.Title>로그아웃</Modal.Title>
        </Modal.Header>

        <Modal.Body>
          <p>정말로 로그아웃 하시겠습니까?</p>
        </Modal.Body>

        <Modal.Footer>
          <Button variant="secondary">취소</Button>
          <Button variant="primary" onClick={handleOnClick}>
            로그아웃
          </Button>
        </Modal.Footer>
      </Modal.Dialog>
    </div>
  );
};
