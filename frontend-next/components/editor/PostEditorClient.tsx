"use client";

import { useCallback, useState } from "react";
import { useRouter } from "next/navigation";
import { useQueryClient } from "@tanstack/react-query";
import { useEditor, EditorContent } from "@tiptap/react";
import StarterKit from "@tiptap/starter-kit";
import Image from "@tiptap/extension-image";
import { marked } from "marked";
import { Form } from "react-bootstrap";
import { EditorToolbar } from "./EditorToolbar";
import { CategorySelectInline } from "./CategorySelectInline";
import { uploadPost, uploadImageFile } from "@/lib/postApi";
import { queryKeys } from "@/lib/queryKeys";

export function PostEditorClient() {
  const router = useRouter();
  const queryClient = useQueryClient();
  const [formData, setFormData] = useState({ title: "", category: "", images: [] as { filePath: string }[] });

  const editor = useEditor({
    immediatelyRender: false,
    extensions: [
      StarterKit,
      Image.configure({ inline: false }),
    ],
    editorProps: {
      attributes: { "data-placeholder": "게시글을 작성해주세요..." },
    },
  });

  const handleChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    const { name, value } = e.target;
    setFormData((prev) => ({ ...prev, [name]: value }));
  };

  const handleImageUpload = useCallback((e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0];
    if (!file || !editor) return;
    const fd = new FormData();
    fd.append("image", file);
    uploadImageFile(fd)
      .then((data) => {
        editor.chain().focus().setImage({ src: data.filePath }).run();
        setFormData((prev) => ({ ...prev, images: [...prev.images, data] }));
        e.target.value = "";
      })
      .catch(() => {});
  }, [editor]);

  const handleMarkdownUpload = (e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0];
    if (!file || !editor) return;
    const reader = new FileReader();
    reader.onload = (event) => {
      const content = marked.parse(event.target?.result as string);
      if (typeof content === "string") {
        editor.commands.setContent(content, { emitUpdate: false } as Parameters<typeof editor.commands.setContent>[1]);
      }
      e.target.value = "";
    };
    reader.readAsText(file);
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!editor) return;
    try {
      const res = await uploadPost(formData, editor.getHTML());
      if (res.status === 200) {
        queryClient.invalidateQueries({ queryKey: queryKeys.posts.lists() });
        queryClient.invalidateQueries({ queryKey: queryKeys.categories.all() });
        alert("게시글을 등록하였습니다.");
        router.push(`/management/posts/${res.data}`);
      }
    } catch {
      alert("게시글 등록에 실패했습니다.");
    }
  };

  return (
    <Form onSubmit={handleSubmit}>
      <div className="editor-page">

        <div className="editor-page__header">
          <span className="editor-page__title">new post</span>
        </div>

        <div className="editor-meta">
          <span className="editor-meta__label">title</span>
          <Form.Control
            className="editor-meta__title-input"
            placeholder="제목을 입력하세요"
            name="title"
            value={formData.title}
            onChange={handleChange}
            required
          />
        </div>

        <div className="editor-category-row">
          <span className="editor-meta__label">category</span>
          <CategorySelectInline
            value={formData.category}
            onChange={(val) => setFormData((prev) => ({ ...prev, category: val }))}
          />
        </div>

        <div className="tiptap-wrapper">
          <EditorToolbar editor={editor} />
          <EditorContent editor={editor} />
        </div>

        <div className="editor-uploads">
          <div className="editor-upload-item">
            <div className="editor-upload-label">
              <i className="fa-solid fa-image" />
              이미지 업로드
            </div>
            <Form.Control type="file" accept="image/*" onChange={handleImageUpload} />
          </div>
          <div className="editor-upload-item">
            <div className="editor-upload-label">
              <i className="fa-solid fa-file-lines" />
              Markdown 파일 가져오기
            </div>
            <Form.Control type="file" accept=".md" onChange={handleMarkdownUpload} />
          </div>
        </div>

        <div className="editor-actions">
          <div className="editor-actions__left">
            <button type="button" className="btn btn-secondary" onClick={() => router.back()}>
              <i className="fa-solid fa-arrow-left" style={{ marginRight: 6 }} />
              취소
            </button>
          </div>
          <div className="editor-actions__right">
            <button type="submit" className="btn btn-primary">
              <i className="fa-solid fa-floppy-disk" style={{ marginRight: 6 }} />
              작성하기
            </button>
          </div>
        </div>

      </div>
    </Form>
  );
}
