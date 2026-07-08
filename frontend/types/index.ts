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
  tags: TagSummary[];
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
  tags?: TagSummary[];
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

// ── 태그 ──────────────────────────────────────────────────────

export interface TagSummary {
  name: string;
  slug: string;
}

export interface Tag {
  id: number;
  name: string;
  slug: string;
  postCount: number;
}

// ── 댓글 ──────────────────────────────────────────────────────

export interface Comment {
  id: number;
  comment: string;
  parentId: number | null;
  writer: string;          // 백엔드 응답 필드 (회원명 or 게스트 닉네임)
  isAdmin: boolean;
  createDate: string;
  modificationStatus: "NOT_MODIFIED" | "MODIFIED";
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
  tokenType: string;
  expiresIn: number;
}

// ── 공통 API 응답 ─────────────────────────────────────────────

export interface ApiError {
  status: number;
  code: string;
  message: string;
}
