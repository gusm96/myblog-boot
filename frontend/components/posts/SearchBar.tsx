"use client";

import { useEffect, useState } from "react";
import { Button, Form, InputGroup } from "react-bootstrap";
import { useRouter } from "next/navigation";

interface SearchBarProps {
  type?: string | null;
  contents?: string | null;
}

export function SearchBar({ type, contents }: SearchBarProps) {
  const [formData, setFormData] = useState({ type: "TITLE", contents: "" });
  const router = useRouter();

  useEffect(() => {
    if (type && contents) {
      setFormData({ type, contents });
    }
  }, [type, contents]);

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    router.push(`/search?type=${formData.type}&contents=${formData.contents}`);
  };

  const handleChange = (e: React.ChangeEvent<HTMLElement>) => {
    const target = e.target as HTMLInputElement | HTMLSelectElement;
    const { name, value } = target;
    setFormData((prev) => ({ ...prev, [name]: value }));
  };

  return (
    <div className="search-bar-wrapper">
      <Form onSubmit={handleSubmit}>
        <InputGroup className="search-input-group">
          <Form.Select
            name="type"
            value={formData.type}
            onChange={handleChange}
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
            onChange={handleChange}
            aria-label="검색어 입력"
          />
          <Button type="submit" variant="primary" className="search-btn">
            검색
          </Button>
        </InputGroup>
      </Form>
    </div>
  );
}
