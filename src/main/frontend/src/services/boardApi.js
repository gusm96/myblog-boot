import axios from "axios";
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
  return axios.get(`${BOARD_GET(boardId)}`).then((res) => res.data);
};
export const getBoardForAdmin = (boardId) => {
  return axios.get(`${BOARD_FOR_ADMIN}/${boardId}`).then((res) => res.data);
};
export const getBoardList = (page) => {
  return axios.get(`${BOARD_CRUD}?${page}`).then((res) => res.data);
};

export const getCategoryOfBoardList = (categoryName, page) => {
  return axios
    .get(`${CATEGORY_OF_BOARD_LIST}?c=${categoryName}&${page}`)
    .then((res) => res.data);
};

export const getSearchedBoardList = (type, contents, page) => {
  return axios
    .get(`${BOARD_CRUD}/search?type=${type}&contents=${contents}&p=${page}`)
    .then((res) => res.data);
};

export const uploadBoard = (formData, htmlString, accessToken) => {
  return axios.post(
    `${BOARD_CRUD}`,
    {
      title: formData.title,
      content: htmlString,
      category: formData.category,
      images: formData.images,
    },
    {
      headers: {
        Authorization: getToken(accessToken),
      },
    },
    {
      withCredentials: true,
    }
  );
};
export const editBoard = (boardId, board, htmlString, accessToken) => {
  return axios
    .put(
      `${BOARD_CRUD}/${boardId}`,
      {
        title: board.title,
        content: htmlString,
        category: board.category,
      },
      {
        headers: {
          Authorization: getToken(accessToken),
        },
      }
    )
    .then((res) => res.data);
};

export const deleteBoard = (boardId, accessToken) => {
  return axios
    .delete(`${BOARD_CRUD}/${boardId}`, {
      headers: {
        Authorization: getToken(accessToken),
      },
    })
    .then((res) => res.data);
};
export const addBoardLike = (boardId, accessToken) => {
  return axios
    .post(
      `${BOARD_LIKE_CRUD(boardId)}`,
      {},
      {
        headers: {
          Authorization: getToken(accessToken),
        },
      }
    )
    .then((res) => res.data);
};

export const cancelBoardLike = (boardId, accessToken) => {
  return axios
    .delete(`${BOARD_LIKE_CRUD(boardId)}`, {
      headers: {
        Authorization: getToken(accessToken),
      },
    })
    .then((res) => res.data);
};

export const getBoardLikes = (page) => {
  return axios.get(`${BOARD_LIKE_CRUD}?${page}`).then((res) => res.data);
};

export const checkBoardLike = (page) => {
  return axios.get();
};
export const getComments = (boardId) => {
  return axios.get(`${COMMENT_CRUD}/${boardId}`).then((res) => res.data);
};
export const getChildComments = (parentId) => {
  return axios.get(`${COMMENT_CRUD}/child/${parentId}`).then((res) => res.data);
};
export const addComment = (boardId, commentData, accessToken) => {
  return axios
    .post(
      `${COMMENT_CRUD}/${boardId}`,
      {
        comment: commentData.comment,
        parentId: commentData.parentId,
      },
      {
        headers: {
          Authorization: getToken(accessToken),
        },
      }
    )
    .then((res) => res.data);
};

export const getBoardLikeStatus = (boardId, accessToken) => {
  return axios
    .get(`${BOARD_LIKE_CRUD(boardId)}`, {
      headers: {
        Authorization: getToken(accessToken),
      },
    })
    .then((res) => res.data);
};

export const uploadImageFile = (formData, accessToken) => {
  return axios
    .post(`${IMAGE_FILE_CRUD}`, formData, {
      headers: {
        "Content-Type": "multipart/form-data",
        Authorization: getToken(accessToken),
      },
    })
    .then((res) => res.data);
};

export const getDeletedBoards = (accessToken, page) => {
  return axios
    .get(`${DELETED_BOARDS}?${page}`, {
      headers: {
        Authorization: getToken(accessToken),
      },
    })
    .then((res) => res.data);
};

export const undeleteBoard = (boardId, accessToken) => {
  return axios.put(
    `${DELETED_BOARDS}/${boardId}`,
    {},
    {
      headers: {
        Authorization: getToken(accessToken),
      },
    }
  );
};

export const deletePermanently = (boardId, accessToken) => {
  return axios.delete(`${DELETED_BOARDS}/${boardId}`, {
    headers: {
      Authorization: getToken(accessToken),
    },
  });
};

const getToken = (accessToken) => {
  return `bearer ${accessToken}`;
};
