import axios from "axios";
import { BOARD_CUD, BOARD_LIKE_CRUD, BOARD_LIST } from "../apiConfig";

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
