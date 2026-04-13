// ── 게시글 ──────────────────────────────────────────────────────

/** GET /api/v1/posts/{identifier} — 상세 */
export interface Post {
  id: number;
  title: string;
  content: string;
  views: number;
  likes: number;
  createDate: string;
  updateDate?: string;
  categoryName: string;
  slug: string;
  metaDescription?: string;
  thumbnailUrl?: string;
}

/** GET /api/v1/posts?p= — 목록 아이템 */
export interface PostSummary {
  id: number;
  title: string;
  content: string;       // HTML — 목록에서 미리보기용 (태그 제거 후 표시)
  createDate: string;
  updateDate?: string;
  slug: string;
  thumbnailUrl?: string;
}

/** GET /api/v1/posts/slugs — SSG slug 목록 */
export interface PostSlug {
  slug: string;
  updateDate: string;
}

export interface PostListResponse {
  list: PostSummary[];
  totalPage: number;
}

// ── 카테고리 ──────────────────────────────────────────────────

/** /api/v1/categories — 관리자용 */
export interface Category {
  id: number;
  categoryName: string;
}

/** /api/v2/categories — 일반 사용자용 (게시글 있는 카테고리 + 게시글 수) */
export interface CategoryV2 {
  id: number;
  name: string;
  postsCount: number;
}

// ── 댓글 ──────────────────────────────────────────────────────

export interface Comment {
  id: number;
  comment: string;
  parentId: number | null;
  writer: string;          // 백엔드 응답 필드 (회원명 or 게스트 닉네임)
  createDate: string;
  childCount?: number;
}

export interface CommentRequest {
  comment: string;
  parentId?: number | null;
  nickname?: string | null;
  password?: string | null;
}

// ── 좋아요 ────────────────────────────────────────────────────

export interface LikeStatus {
  liked: boolean;
  likeCount: number;
}

// ── 인증 ──────────────────────────────────────────────────────

export interface LoginRequest {
  username: string;
  password: string;
}

export interface TokenResponse {
  accessToken: string;
}

// ── 공통 API 응답 ─────────────────────────────────────────────

export interface ApiError {
  status: number;
  code: string;
  message: string;
}
