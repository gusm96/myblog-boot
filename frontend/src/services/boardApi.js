import axios from "axios";
import apiClient from "./apiClient";
import {
  BOARD_CRUD,
  BOARD_FOR_ADMIN,
  BOARD_GET,
  BOARD_LIKE_CRUD,
  CATEGORY_OF_BOARD_LIST,
  COMMENT_CRUD,
  DELETED_BOARDS,
  IMAGE_FILE_CRUD,
} from "../apiConfig";

export const getBoard = (boardId) => {
  return apiClient.get(`${BOARD_GET(boardId)}`).then((res) => res.data);
};

export const getBoardForAdmin = (boardId) => {
  return apiClient
    .get(`${BOARD_FOR_ADMIN}/${boardId}`)
    .then((res) => res.data);
};

export const getBoardList = (page = 1) => {
  return apiClient.get(`${BOARD_CRUD}?p=${page}`).then((res) => res.data);
};

export const getCategoryOfBoardList = (categoryName, page = 1) => {
  return apiClient
    .get(`${CATEGORY_OF_BOARD_LIST}?c=${categoryName}&p=${page}`)
    .then((res) => res.data);
};

export const getSearchedBoardList = (type, contents, page) => {
  return apiClient
    .get(`${BOARD_CRUD}/search?type=${type}&contents=${contents}&p=${page}`)
    .then((res) => res.data);
};

export const uploadBoard = (formData, htmlString) => {
  return apiClient.post(
    `${BOARD_CRUD}`,
    {
      title: formData.title,
      content: htmlString,
      category: formData.category,
      images: formData.images,
    },
    { withCredentials: true }
  );
};

export const editBoard = (boardId, board, htmlString) => {
  return apiClient
    .put(`${BOARD_CRUD}/${boardId}`, {
      title: board.title,
      content: htmlString,
      category: board.category,
    })
    .then((res) => res.data);
};

export const deleteBoard = (boardId) => {
  return apiClient
    .delete(`${BOARD_CRUD}/${boardId}`)
    .then((res) => res.data);
};

export const addBoardLike = (boardId) => {
  return axios
    .post(`${BOARD_LIKE_CRUD(boardId)}`, {}, { withCredentials: true })
    .then((res) => res.data);
};

export const cancelBoardLike = (boardId) => {
  return axios
    .delete(`${BOARD_LIKE_CRUD(boardId)}`, { withCredentials: true })
    .then((res) => res.data);
};

export const getBoardLikes = (page) => {
  return apiClient.get(`${BOARD_LIKE_CRUD}?p=${page}`).then((res) => res.data);
};

export const getComments = (boardId) => {
  return apiClient.get(`${COMMENT_CRUD}/${boardId}`).then((res) => res.data);
};

export const getChildComments = (parentId) => {
  return apiClient.get(`${COMMENT_CRUD}/child/${parentId}`).then((res) => res.data);
};

export const addComment = (boardId, commentData) => {
  return apiClient.post(`${COMMENT_CRUD}/${boardId}`, {
    comment:  commentData.comment,
    parentId: commentData.parentId || null,
    nickname: commentData.nickname || null,
    password: commentData.password || null,
  });
};

export const editComment = (commentId, reqDto) => {
  return apiClient.put(`${COMMENT_CRUD}/${commentId}`, {
    comment:  reqDto.comment,
    password: reqDto.password || null,
  });
};

export const deleteComment = (commentId, reqDto) => {
  return apiClient.delete(`${COMMENT_CRUD}/${commentId}`, {
    data: { password: reqDto?.password || null },
  });
};

export const getBoardLikeStatus = (boardId) => {
  return axios
    .get(`${BOARD_LIKE_CRUD(boardId)}`, { withCredentials: true })
    .then((res) => res.data);
};

export const getBoardLikeCount = (boardId) => {
  return apiClient
    .get(`${BOARD_CRUD}/${boardId}/likes`)
    .then((res) => res.data);
};

export const uploadImageFile = (formData) => {
  return apiClient
    .post(`${IMAGE_FILE_CRUD}`, formData, {
      headers: { "Content-Type": "multipart/form-data" },
    })
    .then((res) => res.data);
};

export const getDeletedBoards = (page) => {
  return apiClient
    .get(`${DELETED_BOARDS}?p=${page}`)
    .then((res) => res.data);
};

export const undeleteBoard = (boardId) => {
  return apiClient.put(`${DELETED_BOARDS}/${boardId}`, {});
};

export const deletePermanently = (boardId) => {
  return apiClient.delete(`${DELETED_BOARDS}/${boardId}`);
};
