/**
 * Query Key Factory — TanStack Query v5
 * 계층 구조: 상위 키 무효화 시 하위 키 전체 무효화
 */
export const queryKeys = {
  // ── 게시글 ──────────────────────────────────────────────────
  boards: {
    all:        ()         => ['boards'],
    lists:      ()         => [...queryKeys.boards.all(),    'list'],
    list:       (page)     => [...queryKeys.boards.lists(),  { page: String(page) }],
    details:    ()         => [...queryKeys.boards.all(),    'detail'],
    detail:     (id)       => [...queryKeys.boards.details(), id],
    likes:      (id)       => [...queryKeys.boards.detail(id), 'likes'],
    likeStatus: (id)       => [...queryKeys.boards.detail(id), 'likeStatus'],
  },

  // ── 카테고리 ────────────────────────────────────────────────
  categories: {
    all:    ()             => ['categories'],
    list:   ()             => [...queryKeys.categories.all(), 'list'],
    boards: (name, page)   => [...queryKeys.categories.all(), name, { page: String(page) }],
  },

  // ── 댓글 ────────────────────────────────────────────────────
  comments: {
    all:      ()           => ['comments'],
    list:     (boardId)    => [...queryKeys.comments.all(), boardId],
    children: (parentId)   => [...queryKeys.comments.all(), 'children', parentId],
  },

  // ── 검색 ────────────────────────────────────────────────────
  search: {
    all:     ()                          => ['search'],
    results: (type, contents, page)      => [...queryKeys.search.all(), { type, contents, page: String(page) }],
  },

  // ── 관리자 ──────────────────────────────────────────────────
  admin: {
    all:        ()     => ['admin'],
    boardsAll:  ()     => [...queryKeys.admin.all(), 'boards'],
    boards:     (page) => [...queryKeys.admin.boardsAll(), { page: String(page) }],
    trashAll:   ()     => [...queryKeys.admin.all(), 'trash'],
    trash:      (page) => [...queryKeys.admin.trashAll(), { page: String(page) }],
    board:      (id)   => [...queryKeys.admin.all(), 'board', id],
  },
};
