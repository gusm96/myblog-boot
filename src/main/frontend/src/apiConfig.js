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

// 댓글 등록/수정/삭제 HTTP Method GET POST DELETE
export const COMMENT_CUD = `${BASE_URL}/api/v1/comment`;
// 댓글 리스트
export const COMMENT_LIST = `${BASE_URL}/api/v1/comments`;

// 게스트 등록
export const GUEST_REG = `${BASE_URL}/api/v1/guest`;
// 게스트 로그인
export const GUEST_LOGIN = `${BASE_URL}/api/v1/login/guest`;

// 관리자 로그인
export const ADMIN_LOGIN = `${BASE_URL}/api/v1/login/admin`;
// 관리자 로그아웃
export const ADMIN_LOGOUT = `${BASE_URL}/api/v1/logout/admin`;

// 토큰 유효 검증
export const TOKEN_VALIDATION = `${BASE_URL}/api/v1/token-validation`;
