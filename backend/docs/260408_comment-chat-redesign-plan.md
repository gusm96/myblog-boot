# 댓글 시스템 재설계 및 채팅 기능 설계

> 작성일: 2026-04-08  
> 최종 수정: 2026-04-08

---

## 배경 및 목표

| 문제 | 해결 방향 |
|---|---|
| 댓글 작성에 회원가입 필요 → 참여 장벽 높음 | 비회원 댓글 (닉네임 + 비밀번호) |
| ROLE_NORMAL 회원 개념 불필요 | Member 엔티티 삭제 |
| 어드민 계정 외부 노출 위험 (가입 API, 비밀번호 찾기 등) | Admin 전용 엔티티 + DB 직접 생성 |
| 대댓글 작성 시 매번 정보 재입력 불편 | localStorage 자동 저장 |
| 채팅 기능 부재 | 소셜 로그인 기반 별도 구현 (Phase 2) |

---

## 전체 구조 변경 요약

```
Before                          After
──────────────────────────────────────────────────────
Member (ROLE_NORMAL, ROLE_ADMIN)   Admin (어드민 전용)
Comment → Member FK                Comment (nickname, discriminator, password 내장)
댓글 작성 = JWT 필요               댓글 작성 = 인증 불필요
회원가입 API 공개                  가입 API 없음 (DB 직접 생성)
비밀번호 찾기 API                  없음
```

---

## 1단계: Admin 엔티티 (Member 대체)

### 1-1. 설계 방향

- 블로그 어드민은 **1명** — 다수 계정 불필요
- 계정 생성은 **DB 직접 INSERT** — 외부 API 없음
- 비밀번호 찾기, 아이디 찾기 API 없음 — 분실 시 DB 직접 수정
- 공격 표면 최소화: 로그인 API 하나만 외부에 노출

### 1-2. Admin 엔티티

```java
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Admin {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String username;

    @Column(nullable = false)
    private String password; // BCrypt 해시

    @Builder
    public Admin(String username, String password) {
        this.username = username;
        this.password = password;
    }
}
```

Member 엔티티 대비 제거된 것: `nickname`, `role`, `boardLikes`, `BaseTimeEntity`

### 1-3. DB 직접 생성 방법

```sql
-- BCrypt 해시 생성 후 INSERT (해시는 별도 툴로 생성)
-- https://bcrypt-generator.com 또는 Spring Shell 등으로 생성
INSERT INTO admin (username, password)
VALUES ('admin', '$2a$10$...');
```

> 배포 후 이 SQL 파일은 서버에서 삭제할 것.  
> `application-prod.yaml`의 `ddl-auto: validate`로 인해 스키마 자동 생성 안 됨.  
> `src/main/resources/sql/` 에 DDL 스크립트 별도 관리 권장.

---

### 1-4. 외부 노출 API 제거 목록

| API | 현재 | 변경 후 |
|---|---|---|
| `POST /api/v1/join` | 공개 | **삭제** |
| `POST /api/v1/login` | 공개 | 유지 (어드민 로그인) |
| `GET /api/v1/logout` | 공개 | 유지 |
| `GET /api/v1/reissuing-token` | 공개 | 유지 |
| `GET /api/v1/token-role` | 공개 | 유지 |
| `GET /api/v1/token-validation` | 공개 | 유지 |
| `POST /api/v1/password-strength-check` | 공개 | **삭제** |

---

### 1-5. 삭제 대상 파일 (Member 관련)

| 파일 | 이유 |
|---|---|
| `domain/member/Member.java` | Admin으로 대체 |
| `domain/member/Role.java` | Admin에 역할 개념 불필요 |
| `domain/member/MemberJoinReqDto.java` | 가입 API 제거 |
| `domain/member/MemberBoardLike.java` | BoardLike 정리로 불필요 |
| `repository/MemberRepository.java` | AdminRepository로 대체 |

### 1-6. 변경 대상 파일 (Member → Admin)

| 파일 | 변경 내용 |
|---|---|
| `service/AuthService.java` | `memberJoin`, `retrieve(Long memberId)` 제거 |
| `service/implementation/AuthServiceImpl.java` | Member → Admin, join 관련 메서드 제거 |
| `controller/AuthController.java` | join, passwordStrengthCheck 엔드포인트 제거 |
| `configuration/WebSecurityConfig.java` | ROLE_NORMAL 규칙 전부 제거, join 경로 제거 |
| `configuration/JwtFilter.java` | Member → Admin 참조 변경 |
| `utils/JwtUtil.java` | Member → Admin 참조 변경 |

---

## 2단계: 댓글 시스템 재설계

### 2-1. 사용자 식별 방식

**첫 댓글 작성 흐름:**
```
사용자 입력: nickname("김철수") + password("1234")
     ↓
서버: discriminator 랜덤 생성 (1000~9999)
     + password BCrypt 해시 저장
     + 응답에 { nickname: "김철수", discriminator: "4236" } 반환
     ↓
브라우저: localStorage에 { nickname, discriminator, password } 저장
```

**대댓글 / 이후 댓글 작성 흐름:**
```
브라우저: localStorage에서 자동 완성
사용자: 댓글 내용만 입력 후 등록
     ↓
서버: nickname + discriminator + password(BCrypt 검증) 확인
```

**식별자 설계:**
- `nickname + discriminator` 조합으로 동명이인 구분 (예: `김철수#4236`)
- discriminator는 게시글 범위 내에서 중복 방지 (같은 글에 `김철수#4236`이 두 명이면 재생성)
- 브라우저가 기억하므로 사용자가 매번 재입력할 필요 없음

---

### 2-2. Comment 엔티티 변경

**현재:**
```java
@ManyToOne(fetch = LAZY)
@JoinColumn(name = "member_id")
private Member member;
```

**변경 후:**
```java
@Column(nullable = false, length = 10)
private String nickname;        // 작성자 닉네임

@Column(nullable = false, length = 4)
private String discriminator;   // 4자리 식별 번호

@Column(nullable = false)
private String password;        // BCrypt 해시 (수정/삭제 검증용)

@Column(nullable = false)
private Boolean isAdmin;        // 어드민 작성 여부
```

**화면 표시:**
- 일반 방문자: `김철수#4236`
- 어드민: `[관리자]` (discriminator, password 불필요)

---

### 2-3. DTO 변경

**CommentReqDto (댓글 작성):**
```java
public class CommentReqDto {

    @NotBlank
    @Size(min = 1, max = 10)
    private String nickname;

    @NotBlank
    @Size(min = 4, max = 20)
    private String password;      // 평문 → 서버에서 BCrypt 해시

    @NotBlank
    @Size(min = 2, max = 500)
    private String comment;

    private Long parentId;        // 대댓글인 경우
}
```

**CommentUpdateReqDto (댓글 수정):**
```java
public class CommentUpdateReqDto {

    @NotBlank
    private String password;      // 작성 시 입력한 비밀번호

    @NotBlank
    @Size(min = 2, max = 500)
    private String comment;
}
```

**CommentDeleteReqDto (댓글 삭제):**
```java
public class CommentDeleteReqDto {

    @NotBlank
    private String password;
}
```

**CommentResDto (응답):**
```java
public class CommentResDto {
    private Long id;
    private String writer;              // "김철수#4236" 또는 "[관리자]"
    private Boolean isAdmin;
    private String comment;
    private LocalDateTime writeDate;
    private ModificationStatus modificationStatus;
    private Long childCount;
    // password 절대 포함하지 않음
}
```

**첫 댓글 작성 성공 응답 (CommentWriteResDto):**
```java
public class CommentWriteResDto {
    private String nickname;
    private String discriminator;   // 브라우저 localStorage 저장용
}
```

---

### 2-4. 댓글 수정/삭제 인증

| 작성자 | 수정/삭제 방식 |
|---|---|
| 일반 방문자 | 요청 바디의 password → BCrypt 검증 |
| 어드민 | JWT 인증 (비밀번호 검증 스킵) |

```java
// CommentServiceImpl.update()
public void update(Long commentId, CommentUpdateReqDto dto, boolean isAdmin) {
    Comment comment = retrieve(commentId);
    if (!isAdmin) {
        if (!passwordEncoder.matches(dto.getPassword(), comment.getPassword())) {
            throw new UnauthorizedAccessException(ErrorCode.COMMENT_ACCESS_DENIED);
        }
    }
    comment.updateComment(dto.getComment());
}
```

---

### 2-5. API 변경

| 메서드 | 엔드포인트 | 인증 | 요청 바디 |
|---|---|---|---|
| GET | `/api/v1/comments/{boardId}` | 불필요 | - |
| GET | `/api/v1/comments/child/{parentId}` | 불필요 | - |
| POST | `/api/v1/comments/{boardId}` | 불필요 | CommentReqDto |
| PUT | `/api/v1/comments/{commentId}` | 불필요 | CommentUpdateReqDto |
| DELETE | `/api/v1/comments/{commentId}` | 불필요 | CommentDeleteReqDto |

> 어드민이 댓글을 수정/삭제할 때는 JWT 헤더 포함 시 비밀번호 검증 스킵.  
> JWT 없으면 일반 방문자로 처리.

---

### 2-6. DB 마이그레이션

```sql
-- comment 테이블
ALTER TABLE comment DROP FOREIGN KEY FK_comment_member;
ALTER TABLE comment DROP COLUMN member_id;

ALTER TABLE comment ADD COLUMN nickname     VARCHAR(10)  NOT NULL;
ALTER TABLE comment ADD COLUMN discriminator CHAR(4)     NOT NULL;
ALTER TABLE comment ADD COLUMN password     VARCHAR(255) NOT NULL;
ALTER TABLE comment ADD COLUMN is_admin     TINYINT(1)   NOT NULL DEFAULT 0;

-- admin 테이블 생성
CREATE TABLE admin (
    id       BIGINT       NOT NULL AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(50)  NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL
);
```

---

## 3단계: BoardLike 정리

`BoardLike` 엔티티가 `Member` FK에 종속되어 있어 함께 정리 필요.

### 옵션 A — HMAC 쿠키 기반 전환 (권장)

방문자 조회수 중복 방지(`VisitorHmacService`)와 동일한 패턴 재활용.

```
좋아요 클릭 시:
  - 쿠키에서 좋아요한 게시글 ID 목록 확인 (HMAC 서명)
  - 포함되어 있으면 → 이미 좋아요 처리
  - 없으면 → likes 카운트 증가 + 쿠키에 게시글 ID 추가
```

- `BoardLike` 테이블 삭제
- `Member.boardLikes` 컬렉션 삭제
- `BoardLikeController`, `BoardLikeServiceImpl` 로직 변경

### 옵션 B — 좋아요 기능 제거

개인 블로그 특성상 좋아요 기능의 필요성이 낮다면 제거.

---

## 4단계: 채팅 시스템 (Phase 2 — 추후 구현)

### 기술 스택

| 항목 | 선택 |
|---|---|
| 실시간 통신 | Spring WebSocket + STOMP |
| 인증 | OAuth2 소셜 로그인 (Google / Kakao) |
| 메시지 브로커 | In-memory SimpleBroker (단일 서버) |
| 메시지 저장 | MySQL + Redis (온라인 유저 목록) |

### ChatUser 엔티티 (신규 — Admin과 독립)

```java
@Entity
public class ChatUser {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String providerId;      // "google_1234567"

    @Enumerated(EnumType.STRING)
    private OAuthProvider provider; // GOOGLE, KAKAO

    private String nickname;
    private String profileImage;
    private LocalDateTime lastLoginAt;
}
```

댓글 시스템(비회원)과 채팅 시스템(소셜 로그인)은 **완전히 독립된 인증 체계**로 분리.

---

## 구현 순서 (Phase 1)

```
Step 1. Admin 엔티티 생성, Member 엔티티 삭제
Step 2. AuthService / AuthController 정리 (join, passwordCheck 제거)
Step 3. WebSecurityConfig 정리 (ROLE_NORMAL 제거)
Step 4. Comment 엔티티 변경 (Member FK → nickname/discriminator/password)
Step 5. CommentReqDto / CommentUpdateReqDto / CommentDeleteReqDto / CommentResDto 변경
Step 6. CommentServiceImpl 변경 (password 검증 로직)
Step 7. CommentController 변경 (Principal 제거)
Step 8. BoardLike 처리 (옵션 A or B 결정 후 진행)
Step 9. DB 마이그레이션 스크립트 작성
Step 10. 테스트 코드 수정
```

---

## 보안 체크리스트

| 항목 | 처리 방식 |
|---|---|
| 어드민 계정 생성 | DB 직접 INSERT (API 없음) |
| 어드민 비밀번호 분실 | DB 직접 UPDATE (API 없음) |
| 댓글 비밀번호 저장 | BCrypt 해시 (평문 저장 금지) |
| 댓글 응답에 password 포함 | 절대 불가 |
| 어드민 JWT 탈취 시 댓글 악용 | isAdmin 댓글은 별도 표시 — 방문자가 어드민 사칭 불가 |
| 스팸 댓글 | 어드민 삭제 권한 유지, 추후 rate limiting 추가 |
