"use client";

import React, { useState } from "react";
import { useQuery } from "@tanstack/react-query";
import { Badge, Form } from "react-bootstrap";
import { queryKeys } from "@/lib/queryKeys";
import { getTagsForAdmin } from "@/lib/postApi";
import type { Tag } from "@/types";

interface TagInputProps {
  value: string[];
  onChange: (next: string[]) => void;
}

export function TagInput({ value, onChange }: TagInputProps) {
  const [inputValue, setInputValue] = useState("");
  const [error, setError] = useState<string | null>(null);

  // admin tags for auto-completion
  const { data: adminTags } = useQuery<Tag[]>({
    queryKey: queryKeys.tags.adminList(),
    queryFn: getTagsForAdmin,
  });

  const handleKeyDown = (e: React.KeyboardEvent<HTMLInputElement>) => {
    if (e.key === "Enter" || e.key === ",") {
      e.preventDefault();
      addTag();
    }
  };

  const addTag = () => {
    const rawName = inputValue.trim().replace(/,/g, "");
    if (!rawName) return;

    if (rawName.length > 40) {
      setError("태그 이름은 40자 이하여야 합니다.");
      return;
    }

    if (value.includes(rawName)) {
      setError("이미 추가된 태그입니다.");
      return;
    }

    if (value.length >= 5) {
      setError("태그는 최대 5개까지 입력할 수 있습니다.");
      return;
    }

    const nextTags = [...value, rawName];
    onChange(nextTags);
    setInputValue("");
    setError(null);
  };

  const removeTag = (indexToRemove: number) => {
    const nextTags = value.filter((_, i) => i !== indexToRemove);
    onChange(nextTags);
    setError(null);
  };

  // filter auto-complete tags based on current input and already selected tags
  const filteredSuggestions = inputValue.trim()
    ? (adminTags || []).filter(
        (t) =>
          t.name.toLowerCase().includes(inputValue.toLowerCase()) &&
          !value.includes(t.name)
      )
    : [];

  return (
    <Form.Group className="mb-3" style={{ position: "relative" }}>
      <Form.Label style={{ fontWeight: "bold" }}>태그 (1~5개)</Form.Label>
      
      {/* Selected tags */}
      <div className="d-flex flex-wrap gap-2 mb-2">
        {value.map((tag, index) => (
          <Badge
            key={`${tag}-${index}`}
            bg="info"
            className="d-flex align-items-center gap-1 py-2 px-3"
            style={{ fontSize: "0.85rem", cursor: "pointer" }}
            onClick={() => removeTag(index)}
          >
            {tag} <span style={{ fontWeight: "bold", marginLeft: "2px" }}>&times;</span>
          </Badge>
        ))}
      </div>

      <Form.Control
        type="text"
        placeholder={value.length >= 5 ? "태그는 최대 5개까지입니다" : "태그를 입력하고 Enter 혹은 쉼표를 누르세요"}
        value={inputValue}
        onChange={(e) => {
          setInputValue(e.target.value);
          setError(null);
        }}
        onKeyDown={handleKeyDown}
        disabled={value.length >= 5}
      />

      {error && <Form.Text className="text-danger d-block mt-1">{error}</Form.Text>}

      {/* Auto-complete suggestions */}
      {filteredSuggestions.length > 0 && (
        <div 
          className="border rounded mt-1 bg-white" 
          style={{ 
            maxHeight: "150px", 
            overflowY: "auto", 
            position: "absolute", 
            zIndex: 1000, 
            width: "300px",
            boxShadow: "0 4px 6px rgba(0,0,0,0.1)"
          }}
        >
          {filteredSuggestions.map((t) => (
            <div
              key={t.id}
              className="p-2 cursor-pointer suggestion-item"
              style={{ cursor: "pointer", transition: "background 0.2s" }}
              onMouseDown={() => {
                if (value.length >= 5) {
                  setError("태그는 최대 5개까지 입력할 수 있습니다.");
                  return;
                }
                onChange([...value, t.name]);
                setInputValue("");
              }}
            >
              {t.name} ({t.postCount})
            </div>
          ))}
        </div>
      )}
    </Form.Group>
  );
}
