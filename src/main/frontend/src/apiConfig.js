// 기본 API URL
export const BASE_URL = "http://localhost:8080";

// API 별 URL

// 카테고리 등록/조회/수정/삭제 HTTP Method GET POST PUT DELETE
export const CATEGORY_CRUD = `${BASE_URL}/api/v1/categories`;
export const CATEGORIES = `${BASE_URL}/api/v2/categories`;
// 게시글 상세
export const BOARD_GET = (boardId) => {
  return `${BASE_URL}/api/v3/boards/${boardId}`;
};

// 게시글 등록/수정/삭제  HTTP Method GET POST DELETE
export const BOARD_CRUD = `${BASE_URL}/api/v1/boards`;

export const CATEGORY_OF_BOARD_LIST = (categoryName) => {
  return `${BASE_URL}/api/v1/boards/${categoryName}`;
};
// 게시글 좋아요 CRRUD
export const BOARD_LIKE_CRUD = (boardId) => {
  return `${BASE_URL}/api/v2/likes/${boardId}`;
};

export const DELETED_BOARDS = `${BASE_URL}/api/v1/deleted-boards`;

// 이미지 파일 CRUD
export const IMAGE_FILE_CRUD = `${BASE_URL}/api/v1/images`;

// 댓글 등록/수정/삭제 HTTP Method GET POST DELETE
export const COMMENT_CRUD = `${BASE_URL}/api/v1/comments`;

// 토큰 유효 검증
export const TOKEN_VALIDATION = `${BASE_URL}/api/v1/token-validation`;
// 토큰 재발급
export const REISSUING_TOKEN = `${BASE_URL}/api/v1/reissuing-token`;
// 토큰 Role 정보 확인
export const TOKEN_ROLE = `${BASE_URL}/api/v1/token-role`;

// 멤버 로그인
export const MEMBER_LOGIN = `${BASE_URL}/api/v1/login`;
// 멤버 로그아웃
export const MEMBER_LOGOUT = `${BASE_URL}/api/v1/logout`;
// 멤버 가입
export const MEMBER_JOIN = `${BASE_URL}/api/v1/join`;
