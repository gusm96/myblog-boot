import React, { useEffect, useState } from "react";
import { Button, Col, Form, InputGroup, Row } from "react-bootstrap";
export const SearchBar = ({ type, contents }) => {
  const [formData, setFormData] = useState({
    type: "TITLE",
    contents: "",
  });
  useEffect(() => {
    if (type && contents) {
      setFormData({
        type: type,
        contents: contents,
      });
    }
  }, [type, contents]);
  const handleOnSubmit = (e) => {
    e.preventDefault();
    // 리다이렉트
    window.location.href = `/search?type=${formData.type}&contents=${formData.contents}`;
  };
  const handleOnChage = (e) => {
    const { name, value } = e.target;
    setFormData((prevState) => ({
      ...prevState,
      [name]: value,
    }));
  };

  return (
    <Row
      style={{
        marginBottom: "30px",
      }}
    >
      <Form>
        <InputGroup>
          <Col md={2}>
            <Form.Select
              aria-label="Default select example"
              name="type"
              value={formData.type}
              onChange={handleOnChage}
            >
              <option value="TITLE">제목</option>
              <option value="CONTENT">내용</option>
            </Form.Select>
          </Col>
          <Col md={8}>
            <Form.Control
              aria-label="Default"
              aria-describedby="inputGroup-sizing-default"
              placeholder="검색할 내용을 입력하세요."
              name="contents"
              value={formData.contents}
              onChange={handleOnChage}
            />
          </Col>
          <Col md={2}>
            <Button type="submit" onClick={handleOnSubmit}>
              검색
            </Button>
          </Col>
        </InputGroup>
      </Form>
    </Row>
  );
};
