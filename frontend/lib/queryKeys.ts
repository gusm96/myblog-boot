/**
 * Query Key Factory — TanStack Query v5
 * 계층 구조: 상위 키 무효화 시 하위 키 전체 무효화
 */
export const queryKeys = {
  // ── 게시글 ──────────────────────────────────────────────────
  posts: {
    all: (): string[] => ["posts"],
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

  // ── 카테고리 ────────────────────────────────────────────────
  categories: {
    all: (): string[] => ["categories"],
    list: () => [...queryKeys.categories.all(), "list"] as const,
    posts: (name: string) =>
      [...queryKeys.categories.all(), name, "posts"] as const,
  },

  // ── 댓글 ────────────────────────────────────────────────────
  comments: {
    all: (): string[] => ["comments"],
    list: (postId: number | string) =>
      [...queryKeys.comments.all(), postId] as const,
    children: (parentId: number | string) =>
      [...queryKeys.comments.all(), "children", parentId] as const,
  },

  // ── 검색 ────────────────────────────────────────────────────
  search: {
    all: (): string[] => ["search"],
    results: (type: string, contents: string, page: number) =>
      [
        ...queryKeys.search.all(),
        { type, contents, page: String(page) },
      ] as const,
  },

  // ── 관리자 ──────────────────────────────────────────────────
  admin: {
    all: (): string[] => ["admin"],
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
