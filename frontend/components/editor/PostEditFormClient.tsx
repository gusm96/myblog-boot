"use client";

import { useEffect, useState } from "react";
import { useRouter } from "next/navigation";
import { useEditor, EditorContent } from "@tiptap/react";
import StarterKit from "@tiptap/starter-kit";
import Image from "@tiptap/extension-image";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { Form } from "react-bootstrap";
import { EditorToolbar } from "./EditorToolbar";
import {
  getPostForAdmin,
  editPost,
  deletePost,
  undeletePost,
  deletePermanently,
  getCategoriesForAdmin,
} from "@/lib/postApi";
import { queryKeys } from "@/lib/queryKeys";

interface Props {
  postId: string;
}

export function PostEditFormClient({ postId }: Props) {
  const router = useRouter();
  const queryClient = useQueryClient();
  const [post, setPost] = useState({ title: "", category: "", deleteDate: "" });

  const editor = useEditor({
    immediatelyRender: false,
    extensions: [StarterKit, Image.configure({ inline: false })],
  });

  const { data: postData } = useQuery({
    queryKey: queryKeys.admin.post(postId),
    queryFn:  () => getPostForAdmin(postId),
    enabled:  !!postId,
    staleTime: 5 * 60 * 1000,
  });

  const { data: categories = [] } = useQuery({
    queryKey: queryKeys.categories.list(),
    queryFn:  getCategoriesForAdmin,
    staleTime: 30 * 60 * 1000,
  });

  useEffect(() => {
    if (postData && categories.length > 0) {
      const matchedCat = categories.find((c) => c.name === postData.categoryName);
      setPost({
        title:      postData.title,
        category:   matchedCat ? String(matchedCat.id) : "",
        deleteDate: postData.deleteDate ?? "",
      });
    }
  }, [postData, categories]);

  useEffect(() => {
    if (editor && postData?.content) {
      editor.commands.setContent(postData.content, { emitUpdate: false } as Parameters<typeof editor.commands.setContent>[1]);
    }
  }, [editor, postData?.content]);

  const handleChange = (e: React.ChangeEvent<HTMLElement>) => {
    const target = e.target as HTMLInputElement | HTMLSelectElement;
    const { name, value } = target;
    setPost((prev) => ({ ...prev, [name]: value }));
  };

  const editMutation = useMutation({
    mutationFn: ({ p, html }: { p: typeof post; html: string }) =>
      editPost(postId, p, html),
    onSuccess: (data) => {
      alert("게시글이 수정되었습니다.");
      queryClient.invalidateQueries({ queryKey: queryKeys.posts.lists() });
      queryClient.invalidateQueries({ queryKey: queryKeys.posts.details() });
      queryClient.invalidateQueries({ queryKey: queryKeys.admin.postsAll() });
      router.push(`/management/posts/${data}`);
    },
  });

  const deleteMutation = useMutation({
    mutationFn: () => deletePost(postId),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: queryKeys.posts.lists() });
      queryClient.invalidateQueries({ queryKey: queryKeys.admin.postsAll() });
      router.back();
    },
  });

  const undeleteMutation = useMutation({
    mutationFn: () => undeletePost(postId),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: queryKeys.admin.trashAll() });
      queryClient.invalidateQueries({ queryKey: queryKeys.admin.postsAll() });
      router.push("/management/temporary-storage");
    },
  });

  const deletePermanentlyMutation = useMutation({
    mutationFn: () => deletePermanently(postId),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: queryKeys.admin.trashAll() });
      router.push("/management/temporary-storage");
    },
  });

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    if (!editor) return;
    editMutation.mutate({ p: post, html: editor.getHTML() });
  };

  const handleDelete = (e: React.MouseEvent) => {
    e.preventDefault();
    if (window.confirm("정말로 삭제하시겠습니까?")) deleteMutation.mutate();
  };

  const handleUndelete = (e: React.MouseEvent) => {
    e.preventDefault();
    if (window.confirm("삭제를 취소하시겠습니까?")) undeleteMutation.mutate();
  };

  const handleDeletePermanently = (e: React.MouseEvent) => {
    e.preventDefault();
    if (window.confirm("영구 삭제 시 복구할 수 없습니다.\n정말로 삭제하시겠습니까?")) {
      deletePermanentlyMutation.mutate();
    }
  };

  const isDeleted = post.deleteDate && post.deleteDate !== "";

  return (
    <Form onSubmit={handleSubmit}>
      <div className="editor-page">

        <div className="editor-page__header">
          <span className="editor-page__title">edit post</span>
          <span className={`editor-status-badge ${isDeleted ? "editor-status-badge--deleted" : "editor-status-badge--live"}`}>
            <span className="editor-status-badge__dot" />
            {isDeleted ? "삭제됨" : "게시 중"}
          </span>
        </div>

        <div className="editor-meta">
          <span className="editor-meta__label">title</span>
          <Form.Control
            className="editor-meta__title-input"
            placeholder="제목을 입력하세요"
            name="title"
            value={post.title}
            onChange={handleChange}
            required
          />
        </div>

        <div className="editor-category-row">
          <span className="editor-meta__label">category</span>
          <Form.Select
            className="editor-meta__category-select editor-meta__category-select--standalone"
            name="category"
            value={post.category}
            onChange={handleChange}
          >
            <option value="">카테고리 선택</option>
            {categories.map((c) => (
              <option key={c.id} value={c.id}>{c.name}</option>
            ))}
          </Form.Select>
        </div>

        <div className="tiptap-wrapper">
          <EditorToolbar editor={editor} />
          <EditorContent editor={editor} />
        </div>

        <div className="editor-actions">
          <div className="editor-actions__left">
            <button type="button" className="btn btn-secondary" onClick={() => router.back()}>
              <i className="fa-solid fa-arrow-left" style={{ marginRight: 6 }} />
              뒤로
            </button>
          </div>
          <div className="editor-actions__right">
            <button type="submit" className="btn btn-primary" disabled={editMutation.isPending}>
              <i className="fa-solid fa-floppy-disk" style={{ marginRight: 6 }} />
              {editMutation.isPending ? "저장 중..." : "수정 저장"}
            </button>
            {!isDeleted ? (
              <button
                type="button"
                className="btn btn-danger"
                onClick={handleDelete}
                disabled={deleteMutation.isPending}
              >
                <i className="fa-solid fa-trash" style={{ marginRight: 6 }} />
                삭제
              </button>
            ) : (
              <>
                <button
                  type="button"
                  className="btn btn-warning"
                  onClick={handleUndelete}
                  disabled={undeleteMutation.isPending}
                >
                  <i className="fa-solid fa-rotate-left" style={{ marginRight: 6 }} />
                  삭제 취소
                </button>
                <button
                  type="button"
                  className="btn btn-danger"
                  onClick={handleDeletePermanently}
                  disabled={deletePermanentlyMutation.isPending}
                >
                  <i className="fa-solid fa-bomb" style={{ marginRight: 6 }} />
                  영구삭제
                </button>
              </>
            )}
          </div>
        </div>

      </div>
    </Form>
  );
}
