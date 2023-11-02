import axios from "axios";
import { BOARD_LIST } from "../apiConfig";

export const getBoardList = (page) => {
  return axios.get(`${BOARD_LIST}?${page}`).then((res) => res.data);
};
