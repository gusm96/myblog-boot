// 기본 API URL
export const BASE_URL = "http://localhost:8080";

// API 별 URL

// 카테고리 등록/수정/삭제 HTTP Method GET POST DELETE
export const CATEGORY_CUD = `${BASE_URL}/api/v1/category`;
// 카테고리 리스트
export const CATEGORY_LIST = `${BASE_URL}/api/v1/categories`;

// 게시글 등록/수정/삭제  HTTP Method GET POST DELETE
export const BOARD_CUD = `${BASE_URL}/api/v1/board`;
// 게시글 리스트
export const BOARD_LIST = `${BASE_URL}/api/v1/boards`;

// 게시글 좋아요 CRRUD
export const BOARD_LIKE_CRUD = `${BASE_URL}/api/v1/board-like`;
// 게시글 좋아요 확인
export const BOARD_LIKE_CHECK = `${BASE_URL}/api/v1/board-like/check`;

// 댓글 등록/수정/삭제 HTTP Method GET POST DELETE
export const COMMENT_CUD = `${BASE_URL}/api/v1/comment`;
// 댓글 리스트
export const COMMENT_LIST = `${BASE_URL}/api/v1/comments`;

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