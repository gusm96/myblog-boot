/**
 * Query Key Factory — TanStack Query v5
 * 계층 구조: 상위 키 무효화 시 하위 키 전체 무효화
 */
export const queryKeys = {
  // ── 게시글 ──────────────────────────────────────────────────
  posts: {
    all: () => ["posts"] as const,
    lists: () => [...queryKeys.posts.all(), "list"] as const,
    list: (page: number) =>
      [...queryKeys.posts.lists(), { page: String(page) }] as const,
    details: () => [...queryKeys.posts.all(), "detail"] as const,
    detail: (id: number | string) =>
      [...queryKeys.posts.details(), id] as const,
    likes: (id: number | string) =>
      [...queryKeys.posts.detail(id), "likes"] as const,
    likeStatus: (id: number | string) =>
      [...queryKeys.posts.detail(id), "likeStatus"] as const,
  },

  // ── 태그 ────────────────────────────────────────────────────
  tags: {
    all: () => ["tags"] as const,
    /** 공개 사이드바용 (Tag[]) */
    publicList: () => [...queryKeys.tags.all(), "public"] as const,
    /** 관리자 페이지용 (Tag[]) */
    adminList: () => [...queryKeys.tags.all(), "admin"] as const,
    posts: (slug: string) =>
      [...queryKeys.tags.all(), slug, "posts"] as const,
  },

  // ── 댓글 ────────────────────────────────────────────────────
  comments: {
    all: () => ["comments"] as const,
    list: (postId: number | string) =>
      [...queryKeys.comments.all(), postId] as const,
    children: (parentId: number | string) =>
      [...queryKeys.comments.all(), "children", parentId] as const,
  },

  // ── 검색 ────────────────────────────────────────────────────
  search: {
    all: () => ["search"] as const,
    results: (type: string, contents: string, page: number) =>
      [
        ...queryKeys.search.all(),
        { type, contents, page: String(page) },
      ] as const,
  },

  // ── 관리자 ──────────────────────────────────────────────────
  admin: {
    all: () => ["admin"] as const,
    postsAll: () => [...queryKeys.admin.all(), "posts"] as const,
    posts: (page: number) =>
      [...queryKeys.admin.postsAll(), { page: String(page) }] as const,
    trashAll: () => [...queryKeys.admin.all(), "trash"] as const,
    trash: (page: number) =>
      [...queryKeys.admin.trashAll(), { page: String(page) }] as const,
    post: (id: number | string) =>
      [...queryKeys.admin.all(), "post", id] as const,
  },
};
