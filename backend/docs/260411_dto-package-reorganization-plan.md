# DTO 패키지 재구성 계획서

## 작업 목표 및 배경

현재 DTO 클래스들이 `domain/` 패키지(엔티티와 혼재)와 `dto/` 패키지에 분산되어 있다.
실무에서는 **엔티티(domain)와 DTO(dto)를 패키지 수준에서 분리**하는 것이 표준 관행이다.

**현재 문제점:**
- `domain/category/` 에 `Category` 엔티티와 `CategoryReqDto`, `CategoryResDto`, `CategoriesResDto`가 공존
- `domain/comment/` 에 `Comment` 엔티티와 5개 DTO가 공존
- `domain/member/` 에 엔티티 없이 `MemberLoginReqDto`만 존재 (Member → Admin 전환 이후 잔재)
- `domain/file/` 에 `ImageFile` 엔티티와 `ImageFileDto`가 공존
- `domain/token/` 에 `TokenReqDto`가 있으나 **어디서도 참조하지 않음** (삭제 대상)
- `dto/post/`, `dto/visitor/` 는 이미 올바른 위치

## 구현 접근 방식

### 목표 패키지 구조

```
dto/
├── auth/        ← MemberLoginReqDto (AuthController/AuthService에서 사용)
├── category/    ← CategoryReqDto, CategoryResDto, CategoriesResDto
├── comment/     ← CommentReqDto, CommentUpdateReqDto, CommentDeleteReqDto, CommentResDto, CommentWriteResDto
├── file/        ← ImageFileDto
├── post/        ← (기존 유지) PostReqDto, PostResDto, PostDetailResDto, PostListResDto, PostSlugDto, PostForRedis
└── visitor/     ← (기존 유지) VisitorCountDto
```

### 설계 결정

| 결정 | 이유 |
|------|------|
| `MemberLoginReqDto` → `dto/auth/` | 실제 사용처가 `AuthController`/`AuthService`이며 Member 엔티티는 Admin으로 전환됨 |
| `TokenReqDto` 삭제 | 프로젝트 전체에서 미참조 — dead code |
| `domain/member/` 패키지 삭제 | MemberLoginReqDto 이동 후 빈 디렉토리 |
| wildcard import 대응 | `domain.comment.*`, `domain.category.*` 사용 중인 파일은 명시적 import로 분리 |

## 변경 대상 파일 목록

### DTO 이동 (11개 파일)

| 원본 | 이동 후 |
|------|---------|
| `domain/category/CategoryReqDto.java` | `dto/category/CategoryReqDto.java` |
| `domain/category/CategoryResDto.java` | `dto/category/CategoryResDto.java` |
| `domain/category/CategoriesResDto.java` | `dto/category/CategoriesResDto.java` |
| `domain/comment/CommentReqDto.java` | `dto/comment/CommentReqDto.java` |
| `domain/comment/CommentUpdateReqDto.java` | `dto/comment/CommentUpdateReqDto.java` |
| `domain/comment/CommentDeleteReqDto.java` | `dto/comment/CommentDeleteReqDto.java` |
| `domain/comment/CommentResDto.java` | `dto/comment/CommentResDto.java` |
| `domain/comment/CommentWriteResDto.java` | `dto/comment/CommentWriteResDto.java` |
| `domain/member/MemberLoginReqDto.java` | `dto/auth/MemberLoginReqDto.java` |
| `domain/file/ImageFileDto.java` | `dto/file/ImageFileDto.java` |
| `domain/token/TokenReqDto.java` | **삭제** |

### import 수정 대상 (main — 15개 파일)

- `controller/`: AuthController, CategoryController, CommentController, FileUploadController
- `service/`: AuthService, CategoryService, CommentService, FileUploadService
- `service/implementation/`: AuthServiceImpl, CategoryServiceImpl, CommentServiceImpl, FileUploadServiceImpl, PostServiceImpl
- `repository/`: CategoryQuerydslRepository, CommentQuerydslRepository
- `repository/implementation/`: CategoryQuerydslRepositoryImpl, CommentQuerydslRepositoryImpl
- `dto/post/PostReqDto.java` (ImageFileDto import)

### import 수정 대상 (test — 8개 파일)

- `controller/`: AuthControllerTest, CategoryControllerTest, CommentControllerTest, FileUploadControllerTest, PostControllerTest
- `service/implementation/`: AuthServiceImplTest, CategoryServiceImplTest, CommentServiceImplTest
- `repository/`: CategoryQueryRepositoryTest

## 예상 이슈 및 대응

| 이슈 | 대응 |
|------|------|
| `CategoriesResDto`가 `Category` 엔티티를 직접 참조 | `import com.moya.myblogboot.domain.category.Category` 명시 추가 |
| `CommentResDto`가 `Comment` 엔티티를 직접 참조 | `import com.moya.myblogboot.domain.comment.Comment` 명시 추가 |
| `CommentResDto`가 `ModificationStatus` enum 참조 | `import com.moya.myblogboot.domain.post.ModificationStatus` 유지 |
| `ImageFileDto`가 `Post` 엔티티 참조 | `import com.moya.myblogboot.domain.post.Post` 유지 |
| wildcard import (`domain.comment.*`) 사용 파일 | DTO import는 `dto.comment.*`로, 엔티티는 `domain.comment.Comment`로 분리 |
