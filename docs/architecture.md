# 아키텍처 구조 분석

## 기술 스택

### 프론트엔드
| 항목 | 기술 |
|------|------|
| UI 라이브러리 | React 18.2.0 |
| 상태 관리 | Redux Toolkit 1.9.5 + Redux Persist |
| 서버 상태 관리 | TanStack React Query 5.17.15 |
| 라우팅 | React Router DOM 6.11.1 |
| HTTP 클라이언트 | Axios 1.3.5 |
| 에디터 | Draft.js 0.11.7, React Draft WYSIWYG 1.15.0 |
| UI 프레임워크 | React Bootstrap 2.7.4, Styled Components 6.0.0-rc.1 |
| HTML 파싱 | html-react-parser 5.0.6 |
| WebSocket | @stomp/stompjs 7.0.0 |
| 날짜 처리 | Moment.js 2.29.4 |

### 백엔드
| 항목 | 기술 |
|------|------|
| 프레임워크 | Spring Boot |
| 인증 | Spring Security + JWT |
| ORM | Spring Data JPA (Hibernate) |
| 캐시/세션 | Redis |
| 파일 저장 | AWS S3 |
| 코드 생성 | Lombok |
| 퍼시스턴스 | Jakarta Persistence |

---

## 디렉토리 구조

```
myblog-boot/
├── src/main/
│   ├── frontend/                          # React 프론트엔드
│   │   └── src/
│   │       ├── App.js                     # 라우팅 및 진입점
│   │       ├── apiConfig.js               # API 엔드포인트 상수 정의
│   │       ├── components/
│   │       │   ├── Boards/                # 게시글 관련 컴포넌트
│   │       │   │   ├── BoardDetailV2.js   # 게시글 상세
│   │       │   │   ├── BoardEditor.js     # 게시글 작성 (관리자)
│   │       │   │   ├── BoardEditForm.js   # 게시글 수정 (관리자)
│   │       │   │   ├── BoardForm.js       # 게시글 작성 폼
│   │       │   │   ├── BoardList.js       # 게시글 목록
│   │       │   │   ├── BoardLike.js       # 좋아요
│   │       │   │   └── BoardDetail.js     # 게시글 상세 (구버전)
│   │       │   ├── Category/
│   │       │   │   ├── CategoryForm.js    # 카테고리 선택 폼
│   │       │   │   ├── CategoryModal.js   # 카테고리 추가 모달
│   │       │   │   └── CategoryNavV2.js   # 사이드바 카테고리 네비
│   │       │   ├── Comments/
│   │       │   │   ├── CommentList.js     # 댓글 목록
│   │       │   │   ├── CommentForm.js     # 댓글 작성
│   │       │   │   └── Comment.js         # 댓글 단건
│   │       │   ├── Layout/
│   │       │   │   ├── UserLayout.js      # 사용자 레이아웃
│   │       │   │   └── AdminLayout.js     # 관리자 레이아웃
│   │       │   ├── Navbar/
│   │       │   │   ├── UserNavBar.js
│   │       │   │   └── AdminNavBar.js
│   │       │   ├── ErrorBoundary.js       # React 에러 경계
│   │       │   ├── ErrorMessage.js        # 에러 메시지 컴포넌트
│   │       │   └── VisitorCount.js        # 방문자 수
│   │       ├── screens/                   # 페이지 컴포넌트
│   │       │   ├── Home.js                # 홈 (게시글 목록)
│   │       │   ├── PageByCategory.js      # 카테고리별 게시글
│   │       │   ├── Management.js          # 관리자 메인
│   │       │   └── Member/
│   │       │       ├── LoginForm.js
│   │       │       └── JoinForm.js
│   │       ├── services/                  # API 호출 함수
│   │       │   ├── boardApi.js
│   │       │   ├── categoryApi.js
│   │       │   └── authApi.js
│   │       ├── hooks/                     # 커스텀 훅
│   │       │   └── useQueries.js          # React Query 훅 모음
│   │       └── redux/                     # 전역 상태
│   │           ├── store.js
│   │           ├── userSlice.js
│   │           └── authAction.js
│   │
│   └── java/com/moya/myblogboot/          # Spring Boot 백엔드
│       ├── controller/                    # REST 컨트롤러
│       ├── service/                       # 서비스 인터페이스
│       │   └── implementation/            # 서비스 구현체
│       ├── domain/                        # JPA 엔티티
│       │   ├── board/
│       │   ├── category/
│       │   ├── comment/
│       │   ├── member/
│       │   └── base/
│       ├── dto/                           # 데이터 전송 객체
│       │   └── board/
│       ├── repository/                    # 데이터 접근 계층
│       │   └── implementation/
│       ├── configuration/                 # Spring 설정
│       ├── constants/                     # 상수 정의
│       ├── exception/                     # 커스텀 예외
│       ├── scheduler/                     # 스케줄러 (정기 작업)
│       └── utils/                         # 유틸리티
│
├── Dockerfile
├── docker-compose.yaml
└── Jenkinsfile                            # CI/CD 파이프라인
```

---

## 데이터 흐름

### 게시글 조회 흐름
```
Home.js
  → useQuery (React Query)
  → boardApi.getBoardList()
  → GET /api/v1/boards?p={page}
  → BoardController.getAllBoards()
  → BoardServiceImpl.retrieveAll(page)
  → BoardRepository (DB 조회)
  → Redis 캐싱
  → BoardListResDto 반환
  → BoardList 컴포넌트 렌더링
```

### 게시글 상세 조회 (조회수 포함)
```
BoardDetailV2.js
  → useBoardQuery(boardId)
  → GET /api/v7/boards/{boardId}  (최신 버전)
  → BoardController.getBoard_v7()
  → Cookie 기반 중복 조회 제어
  → BoardServiceImpl.retrieveAndIncrementViewsDto()
  → Redis 조회수 업데이트
  → BoardDetailResDto 반환
```

### 게시글 작성 흐름
```
BoardEditor.js (관리자)
  → uploadImageFile() → AWS S3
  → POST /api/v1/boards
  → BoardController.writeBoard()
  → BoardServiceImpl.write()
    → Member 조회
    → Category 조회
    → Board 엔티티 생성 & 저장
    → Redis 캐싱
  → 새 게시글 ID 반환
  → 상세 페이지로 리다이렉트
```

---

## 인증 흐름

```
로그인 요청
  → POST /api/v1/auth/login
  → JWT accessToken, refreshToken 발급
  → accessToken: Redux store 저장 (redux-persist)
  → refreshToken: HttpOnly Cookie 저장

API 요청 시
  → Authorization: Bearer {accessToken} 헤더 첨부
  → JwtFilter에서 토큰 검증
  → 만료 시: POST /api/v1/auth/token/refresh
  → 새 accessToken 발급 → Redux store 업데이트
```

---

## 조회수 관리 진화

| 버전 | 방식 | 문제점 |
|------|------|--------|
| V4 | 매 요청마다 증가 | 새로고침 시 무한 증가 |
| V5 | Redis IP 기반 중복 확인 | 메모리 부담, 공유 IP 문제 |
| V6 | Cookie 기반 24시간 제어 | 쿠키 삭제 시 우회 가능 |
| V7 | 사용자별 조회 기록 저장 | 현재 사용 중 |

---

## 배포 구성

```
코드 push
  → Jenkins CI/CD 트리거 (Jenkinsfile)
  → Docker 이미지 빌드 (Dockerfile)
  → docker-compose 배포 (docker-compose.yaml)
    ├── myblog-boot 앱 컨테이너
    └── Redis 컨테이너
```
