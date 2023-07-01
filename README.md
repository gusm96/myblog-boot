# Project - MyBlog 

-----

### 목차

1. 프로젝트 개요
2. 프로젝트 구조 ( ERD / Router )
3. 주요 기능 소개
4. 배운점

-----

### Router

|                페이지 기능                |            주소             |
| :---------------------------------------: | :-------------------------: |
|                 홈 페이지                 |             "/"             |
|       카테고리별 게시글 목록 페이지       | "/category/{category_name}" |
|          게시글 상세보기 페이지           |        "/{board_id}"        |
|           관리자 로그인 페이지            |       "/login/admin"        |
|          관리자 전용 페이지 - 홈          |        "/management"        |
|  관리자 전용 페이지 - 게시글 작성 페이지  |   "/management/new-post"    |
|  관리자 전용 페이지 - 게시글 관리 페이지  |  "/management/{board_id}"   |
| 관리자 전용 페이지 - 카테고리 관리 페이지 |   /management/categories    |

### API

|              기능              |                         주소                         |
| :----------------------------: |:--------------------------------------------------:|
|       모든 게시글 리스트       |                GET "/api/v1/boards"                |
|    카테고리별 게시글 리스트    |         GET "/api/v1/boards/{category_id}"         |
| (선택한) 게시글 정보 가져오기  |           GET "/api/v1/board/{board_id}"           |
|    게시글 작성 (권한 필요)     |          POST "/api/v1/management/board"           |
|    게시글 수정 (권한 필요)     |     PUT "/api/v1/management/board/{board_id}"     |
|    게시글 삭제 (권한 필요)     |    DELETE "/api/v1/management/board/{board_id}"    |
|        카테고리 리스트         |              GET "/api/v1/categories"              |
|   카테고리 작성 (권한 필요)    |         POST "/api/v1/management/category"         |
|   카테고리 수정 (권한 필요)    |  PUT "/api/v1/management/category/{category_id}"   |
|   카테고리 삭제 (권한 필요)    | DELETE "/api/v1/management/category/{category_id}" |
|           댓글 작성            |                POST "/api/v1/reply"                |
|           댓글 수정            |           PUT "/api/1/reply/{reply_id}"           |
|           댓글 삭제            |         DELETE "/api/v1/reply/{reply_id}"          |
| 관리자 로그인 정보 (권한 필요) |              GET "/api/v1/admin-info"              |
|       관리자 로그인 요청       |             POST "/api/v1/login/admin"             |

------

