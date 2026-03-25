import React, { useEffect, useState } from "react";
import { Button, Form, InputGroup } from "react-bootstrap";
import { useNavigate } from "react-router";
import "./Styles/css/searchBar.css";

export const SearchBar = ({ type, contents }) => {
  const [formData, setFormData] = useState({ type: "TITLE", contents: "" });
  const navigate = useNavigate();

  useEffect(() => {
    if (type && contents) {
      setFormData({ type, contents });
    }
  }, [type, contents]);

  const handleOnSubmit = (e) => {
    e.preventDefault();
    navigate(`/search?type=${formData.type}&contents=${formData.contents}`);
  };

  const handleOnChange = (e) => {
    const { name, value } = e.target;
    setFormData((prev) => ({ ...prev, [name]: value }));
  };

  return (
    <div className="search-bar-wrapper">
      <Form onSubmit={handleOnSubmit}>
        <InputGroup className="search-input-group">
          <Form.Select
            name="type"
            value={formData.type}
            onChange={handleOnChange}
            className="search-type-select"
            aria-label="검색 유형"
          >
            <option value="TITLE">제목</option>
            <option value="CONTENT">내용</option>
          </Form.Select>
          <Form.Control
            placeholder="검색어를 입력하세요..."
            name="contents"
            value={formData.contents}
            onChange={handleOnChange}
            aria-label="검색어 입력"
          />
          <Button type="submit" variant="primary" className="search-btn">
            검색
          </Button>
        </InputGroup>
      </Form>
    </div>
  );
};
