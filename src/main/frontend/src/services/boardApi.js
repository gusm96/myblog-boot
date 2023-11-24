import axios from "axios";
import {
  BOARD_GET,
  BOARD_LIKE_CRUD,
  BOARD_LIST,
  COMMENT_CUD,
  COMMENT_LIST,
} from "../apiConfig";
export const getBoard = (boardId) => {
  return axios.get(`${BOARD_GET}/${boardId}`).then((res) => res.data);
};

export const getBoardList = (page) => {
  return axios.get(`${BOARD_LIST}?${page}`).then((res) => res.data);
};

export const addBoardLike = (page) => {
  return axios.post(`${BOARD_LIKE_CRUD}?${page}`).then((res) => res.data);
};

export const getBoardLikes = (page) => {
  return axios.get(`${BOARD_LIKE_CRUD}?${page}`).then((res) => res.data);
};

export const deleteBoardLike = (page) => {
  return axios.delete(`${BOARD_LIKE_CRUD}?${page}`).then((res) => res.data);
};

export const checkBoardLike = (page) => {
  return axios.get();
};

export const getComments = (boardId) => {
  return axios.get(COMMENT_LIST(boardId)).then((res) => res.data);
};

export const addComment = (boardId, commentData, accessToken) => {
  return axios
    .post(
      `${COMMENT_CUD}/${boardId}`,
      {
        comment: commentData.comment,
      },
      {
        headers: {
          Authorization: `bearer ${accessToken}`,
        },
      }
    )
    .then((res) => res.data);
};
