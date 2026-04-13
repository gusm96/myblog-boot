/**
 * 게시글/댓글/좋아요 클라이언트 사이드 API (axios + 인터셉터)
 * 'use client' 컴포넌트 + TanStack Query에서 사용
 */
import axios from "axios";
import apiClient from "./apiClient";
import type { Comment, CommentRequest } from "@/types";

const BASE_URL = process.env.NEXT_PUBLIC_API_URL ?? "http://localhost:8080";

// ── 좋아요 ────────────────────────────────────────────────────

export const addPostLike = (postId: number) =>
  axios
    .post(`${BASE_URL}/api/v2/likes/${postId}`, {}, { withCredentials: true })
    .then((res) => res.data as number);

export const cancelPostLike = (postId: number) =>
  axios
    .delete(`${BASE_URL}/api/v2/likes/${postId}`, { withCredentials: true })
    .then((res) => res.data as number);

export const getPostLikeStatus = (postId: number) =>
  axios
    .get(`${BASE_URL}/api/v2/likes/${postId}`, { withCredentials: true })
    .then((res) => res.data as boolean);

export const getPostLikeCount = (postId: number) =>
  apiClient
    .get(`/api/v1/posts/${postId}/likes`)
    .then((res) => res.data as number);

// ── 게시글 목록 (클라이언트 사이드) ──────────────────────────

import type { PostListResponse } from "@/types";

export const getPostList = (page = 1): Promise<PostListResponse> =>
  apiClient.get(`/api/v1/posts?p=${page}`).then((res) => res.data);

export const getCategoryPostList = (
  categoryName: string,
  page = 1
): Promise<PostListResponse> =>
  apiClient
    .get(`/api/v1/posts/category?c=${encodeURIComponent(categoryName)}&p=${page}`)
    .then((res) => res.data);

export const getSearchedPostList = (
  type: string,
  contents: string,
  page: number | string
): Promise<PostListResponse> =>
  apiClient
    .get(`/api/v1/posts/search?type=${type}&contents=${contents}&p=${page}`)
    .then((res) => res.data);

// ── 댓글 ─────────────────────────────────────────────────────

export const getComments = (postId: number): Promise<Comment[]> =>
  apiClient.get(`/api/v1/comments/${postId}`).then((res) => res.data);

export const getChildComments = (parentId: number): Promise<Comment[]> =>
  apiClient
    .get(`/api/v1/comments/child/${parentId}`)
    .then((res) => res.data);

export const addComment = (
  postId: number,
  commentData: CommentRequest
) =>
  apiClient.post(`/api/v1/comments/${postId}`, {
    comment:  commentData.comment,
    parentId: commentData.parentId ?? null,
    nickname: commentData.nickname ?? null,
    password: commentData.password ?? null,
  });

export const editComment = (
  commentId: number,
  reqDto: { comment: string; password?: string | null }
) =>
  apiClient.put(`/api/v1/comments/${commentId}`, {
    comment:  reqDto.comment,
    password: reqDto.password ?? null,
  });

export const deleteComment = (
  commentId: number,
  reqDto?: { password?: string | null }
) =>
  apiClient.delete(`/api/v1/comments/${commentId}`, {
    data: { password: reqDto?.password ?? null },
  });

// ── 관리자: 게시글 ────────────────────────────────────────────

/** GET /api/v1/management/posts/{postId} 응답 (PostDetailResDto 기반) */
export interface PostForAdmin {
  id: number;
  title: string;
  content: string;
  categoryName: string;
  deleteDate?: string;
  updateDate?: string;
  metaDescription?: string;
  thumbnailUrl?: string;
  slug?: string;
}

export const getPostForAdmin = (postId: number | string): Promise<PostForAdmin> =>
  apiClient.get(`/api/v1/management/posts/${postId}`).then((res) => res.data);

export const uploadPost = (
  formData: { title: string; category: string; images: unknown[] },
  htmlString: string
) =>
  apiClient.post(
    `/api/v1/posts`,
    {
      title:    formData.title,
      content:  htmlString,
      category: Number(formData.category),
      images:   formData.images,
    },
    { withCredentials: true }
  );

export const editPost = (
  postId: number | string,
  post: { title: string; category: string },
  htmlString: string
): Promise<number> =>
  apiClient
    .put(`/api/v1/posts/${postId}`, {
      title:    post.title,
      content:  htmlString,
      category: Number(post.category),
    })
    .then((res) => res.data);

export const deletePost = (postId: number | string): Promise<unknown> =>
  apiClient.delete(`/api/v1/posts/${postId}`).then((res) => res.data);

export const getDeletedPosts = (page: number | string = 1): Promise<PostListResponse> =>
  apiClient.get(`/api/v1/deleted-posts?p=${page}`).then((res) => res.data);

export const undeletePost = (postId: number | string) =>
  apiClient.put(`/api/v1/deleted-posts/${postId}`, {});

export const deletePermanently = (postId: number | string) =>
  apiClient.delete(`/api/v1/deleted-posts/${postId}`);

export const uploadImageFile = (formData: FormData): Promise<{ filePath: string }> =>
  apiClient
    .post(`/api/v1/images`, formData, {
      headers: { "Content-Type": "multipart/form-data" },
    })
    .then((res) => res.data);

// ── 관리자: 카테고리 ──────────────────────────────────────────

export interface CategoryForAdmin {
  id: number;
  name: string;
  postsCount: number;
}

export const getCategoriesForAdmin = (): Promise<CategoryForAdmin[]> =>
  apiClient.get(`/api/v1/categories-management`).then((res) => res.data);

export const addNewCategory = (categoryName: string) =>
  apiClient.post(`/api/v1/categories`, { categoryName }, { withCredentials: true });

export const deleteCategoryById = (categoryId: number) =>
  apiClient.delete(`/api/v1/categories/${categoryId}`);
