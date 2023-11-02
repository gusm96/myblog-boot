import React from "react";
import { Button, Col, Container, Form, Row } from "react-bootstrap";

export const MemberJoinForm = () => {
  return (
    <div>
      <Container className="panel">
        <Form>
          <Form.Group
            as={Row}
            className="mb-3"
            controlId="formPlaintextPassword"
          >
            <Col sm>
              <Form.Control type="text" placeholder="아이디"></Form.Control>
            </Col>
          </Form.Group>
          <Form.Group
            as={Row}
            className="mb-3"
            controlId="formPlaintextPassword"
          >
            <Col sm>
              <Form.Control
                type="password"
                placeholder="비밀번호"
              ></Form.Control>
            </Col>
          </Form.Group>
          <Form.Group
            as={Row}
            className="mb-3"
            controlId="formPlaintextPassword"
          >
            <Col sm>
              <Form.Control type="text" placeholder="닉네임"></Form.Control>
            </Col>
          </Form.Group>
          <Button variant="primary" type="submit">
            가입하기
          </Button>
        </Form>
      </Container>
    </div>
  );
};
