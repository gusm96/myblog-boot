# N+1 쿼리 전수 점검 보고서

> 작성일: 2026-03-28
> 대상: Repository / Service / DTO 전체 계층
> 점검 방법: Entity 관계 → Repository 쿼리 → DTO 변환 → Service 호출 흐름 추적

---

## 1. Entity 관계 맵 (모두 LAZY)

| Entity | 관계 | 대상 | Fetch Type |
|--------|------|------|-----------|
| Board | @ManyToOne | Member | LAZY |
| Board | @ManyToOne | Category | LAZY |
| Board | @OneToMany | BoardLike | LAZY (default) |
| Board | @OneToMany | ImageFile | LAZY (default) |
| Board | @OneToMany | Comment | LAZY (default) |
| Comment | @ManyToOne | Member | LAZY |
| Comment | @ManyToOne | Board | LAZY |
| Comment | @ManyToOne | Comment (parent) | LAZY |
| Comment | @OneToMany | Comment (child) | LAZY (default) |
| Category | @OneToMany | Board | LAZY (default) |
| BoardLike | @ManyToOne | Board | LAZY |
| BoardLike | @ManyToOne | Member | LAZY |

---

## 2. DTO 관계 접근 분석

DTO 변환 시 Lazy 연관을 건드리는지가 N+1 발생 여부를 결정함.

| DTO | 접근하는 연관 | N+1 위험 |
|-----|-------------|---------|
| `BoardResDto.of(board)` | **없음** (id, title, content, dates, status만) | 없음 |
| `BoardForRedis(board)` | **없음** (id, title, content, views, likes, dates만) | 없음 |
| `BoardDetailResDto(boardForRedis)` | **없음** (BoardForRedis DTO에서 읽음, Entity 아님) | 없음 |
| `CommentResDto.of(comment)` | `comment.getMember().getNickname()`, `comment.getChild().size()` | **있음** |
| `CategoriesResDto(category)` | `category.getBoards().size()` | **있음** |

---

## 3. Repository 쿼리 전수 점검

### BoardRepository

| 메서드 | Fetch Join | DTO 접근 | 판정 |
|--------|-----------|---------|------|
| `findById(boardId)` | member, category, boardLikes.member | BoardForRedis (연관 미접근) | **SAFE** |
| `findAll(boardStatus, pageable)` | member, category + countQuery 분리 | BoardResDto (연관 미접근) | **SAFE** |
| `findAllByCategoryName(...)` | ~~없음~~ → **member, category 추가** | BoardResDto (연관 미접근) | **수정됨** |
| `findByDeletionStatus(...)` | ~~없음~~ → **member, category 추가** | BoardResDto (연관 미접근) | **수정됨** |

### BoardQuerydslRepository

| 메서드 | Fetch Join | DTO 접근 | 판정 |
|--------|-----------|---------|------|
| `findByDeleteDate(deleteDate)` | imageFiles | 삭제 작업 (DTO 변환 없음) | **SAFE** |
| `findBySearchType(...)` | ~~없음~~ → **member, category 추가** | BoardResDto (연관 미접근) | **수정됨** |

### CommentRepository / CommentQuerydslRepository

| 메서드 | Fetch Join | DTO 접근 | 판정 |
|--------|-----------|---------|------|
| `findAllByBoardId(boardId)` (Querydsl) | child, member | CommentResDto (member, child 접근) | **SAFE** (fetch됨) |
| `findChildByParentId(parentId)` (Querydsl) | parent, member | CommentResDto (member 접근) | **SAFE** (fetch됨) |
| `findById(id)` (Spring Data 기본) | 없음 | `retrieve()`에서 사용 | 호출처 따라 다름 (아래 분석) |
| `findByIdWithMember(id)` | **member (신규 추가)** | `retrieveWithMember()`에서 사용 | **수정됨** |

### CategoryRepository / CategoryQuerydslRepository

| 메서드 | Fetch Join | DTO 접근 | 판정 |
|--------|-----------|---------|------|
| `findById(id)` (오버라이드) | boards | `retrieve()` → `delete()`에서 `getBoards().size()` 접근 | **SAFE** (fetch됨) |
| `findByName(name)` | 없음 | 단건 조회, 연관 미접근 | **SAFE** |
| `findCategoriesWithViewBoards()` (Querydsl) | boards | CategoriesResDto (boards.size() 접근) | **SAFE** (fetch됨) |
| `findAllDto()` (Querydsl) | boards | CategoriesResDto (boards.size() 접근) | **SAFE** (fetch됨) |

### BoardLikeRepository

| 메서드 | Fetch Join | DTO 접근 | 판정 |
|--------|-----------|---------|------|
| `existsByBoardIdAndMemberId(...)` | N/A (exists 쿼리) | boolean 반환 | **SAFE** |
| `findByBoardIdAndMemberId(...)` | 없음 | 삭제만 수행, 연관 미접근 | **SAFE** |

---

## 4. Service 호출 흐름 추적

### BoardServiceImpl

| 메서드 | 호출 쿼리 | N+1 위험 |
|--------|----------|---------|
| `retrieveAll()` | `findAll(status, pageable)` → fetch join 있음 | **SAFE** |
| `retrieveAllByCategory()` | `findAllByCategoryName()` → **fetch join 추가됨** | **수정됨** |
| `retrieveAllBySearched()` | `findBySearchType()` → **fetch join 추가됨** | **수정됨** |
| `retrieveAllDeleted()` | `findByDeletionStatus()` → **fetch join 추가됨** | **수정됨** |
| `getBoardDetail()` | Redis 캐시 조회 (DB 미접근) | **SAFE** |
| `getBoardDetailAndIncrementViews()` | Redis 캐시 조회 | **SAFE** |
| `write()` | `authService.retrieve()` + `categoryService.retrieve()` 개별 로드 | **SAFE** |
| `edit()` | `findById()` → fetch join 있음, `getMember().getId()` | **SAFE** |
| `delete()` | `findById()` → fetch join 있음, `getMember().getId()` | **SAFE** |
| `deletePermanently()` | `findByDeleteDate()` → imageFiles fetch join | **SAFE** |

### CommentServiceImpl

| 메서드 | 호출 쿼리 | N+1 위험 |
|--------|----------|---------|
| `retrieveAll()` | `findAllByBoardId()` → Querydsl fetch join (child, member) | **SAFE** |
| `retrieveAllChild()` | `findChildByParentId()` → Querydsl fetch join (parent, member) | **SAFE** |
| `write()` | `retrieve()` → default findById, member 미접근 (`addChildComment`만) | **SAFE** |
| `update()` | ~~`retrieve()`~~ → **`retrieveWithMember()`로 변경** | **수정됨** |
| `delete()` | ~~`retrieve()`~~ → **`retrieveWithMember()`로 변경** | **수정됨** |

### CategoryServiceImpl

| 메서드 | 호출 쿼리 | N+1 위험 |
|--------|----------|---------|
| `retrieveAll()` | `findAll()` (Spring Data 기본) → `CategoryResDto.of()` (name만 접근) | **SAFE** |
| `retrieveAllWithViewBoards()` | Querydsl fetch join (boards) | **SAFE** |
| `retrieveDto()` | Querydsl fetch join (boards) | **SAFE** |
| `update()` | `retrieve()` → 오버라이드된 findById (boards fetch join), 연관 미접근 | **SAFE** |
| `delete()` | `retrieve()` → 오버라이드된 findById (boards fetch join), `getBoards().size()` 접근 | **SAFE** (fetch됨) |

### BoardLikeServiceImpl

| 메서드 | 호출 쿼리 | N+1 위험 |
|--------|----------|---------|
| `addLikes()` | `existsByBoardIdAndMemberId()` + `findById()` (fetch join 있음) | **SAFE** |
| `cancelLikes()` | `existsByBoardIdAndMemberId()` + `findById()` (fetch join 있음) | **SAFE** |

---

## 5. 수정 내역 요약

### 5-1. `BoardRepository.findAllByCategoryName()` — fetch join + countQuery 추가

**파일**: `repository/BoardRepository.java:30-33`

```java
// Before
@Query("select b from Board b where b.category.name = :categoryName and b.boardStatus = 'VIEW'")

// After
@Query(value = "select b from Board b join fetch b.member join fetch b.category " +
        "where b.category.name = :categoryName and b.boardStatus = 'VIEW'",
        countQuery = "select count(b) from Board b where b.category.name = :categoryName and b.boardStatus = 'VIEW'")
```

### 5-2. `BoardRepository.findByDeletionStatus()` — fetch join + countQuery 추가

**파일**: `repository/BoardRepository.java:35-38`

```java
// Before
@Query("select b from Board b where b.deleteDate is not null")

// After
@Query(value = "select b from Board b join fetch b.member join fetch b.category " +
        "where b.deleteDate is not null",
        countQuery = "select count(b) from Board b where b.deleteDate is not null")
```

### 5-3. `BoardQuerydslRepositoryImpl.findBySearchType()` — fetch join 추가

**파일**: `repository/implementation/BoardQuerydslRepositoryImpl.java:38-45`

```java
// Before
List<Board> boards = queryFactory.selectFrom(board)
        .where(condition)
        ...

// After
List<Board> boards = queryFactory.selectFrom(board)
        .join(board.member).fetchJoin()
        .join(board.category).fetchJoin()
        .where(condition)
        ...
```

> ManyToOne fetch join은 결과 행 수를 변경하지 않으므로 pagination과 안전하게 호환됨.
> count 쿼리는 fetch join 없이 별도 실행되므로 영향 없음.

### 5-4. `CommentRepository.findByIdWithMember()` — 신규 추가

**파일**: `repository/CommentRepository.java:12-13`

```java
@Query("select c from Comment c join fetch c.member where c.id = :id")
Optional<Comment> findByIdWithMember(@Param("id") Long id);
```

### 5-5. `CommentServiceImpl.update()` / `delete()` — `retrieveWithMember()` 사용

**파일**: `service/implementation/CommentServiceImpl.java:71, 83, 103-106`

```java
// Before: retrieve() → default findById → getMember().getId() 시 lazy load 추가 쿼리
// After:  retrieveWithMember() → findByIdWithMember → member 즉시 로드

private Comment retrieveWithMember(Long commentId) {
    return commentRepository.findByIdWithMember(commentId).orElseThrow(()
            -> new EntityNotFoundException("해당 댓글은 삭제되었거나 존재하지 않습니다."));
}
```

---

## 6. P1-4 계획서 오류 정정

계획서(260326)의 P1-4에서는:

> `convertToBoardListResDto()`에서 `BoardResDto.of(board)` 호출 시 `board.getMember()`, `board.getCategory()`가 Lazy 로딩되어 게시글 수(8개)만큼 추가 쿼리 발생.

이 진단은 **사실과 다름**. `BoardResDto.of(board)`는 `id`, `title`, `content`, `createDate`, `updateDate`, `deleteDate`, `boardStatus`만 접근하며, `getMember()` / `getCategory()`를 **호출하지 않음**.

따라서 **현재 코드에서 실제 N+1은 발생하지 않으나**, DTO가 향후 작성자명이나 카테고리명을 포함하도록 변경될 경우 즉시 N+1이 발생하므로 **방어적 fetch join 추가**로 수정함.

---

## 7. 추가 발견 사항 (N+1 외)

### 7-1. `findBySearchType()` — `boardStatus = VIEW` 필터 누락

`findAll()`과 `findAllByCategoryName()`은 `boardStatus = VIEW` 조건을 포함하지만,
`findBySearchType()`에는 이 조건이 없어 숨김/삭제된 게시글도 검색 결과에 포함될 수 있음.

> 이 이슈는 N+1과 무관하므로 본 수정에서 제외. 별도 확인 필요.

### 7-2. `CategoryRepository` — `@Param` import 오류

```java
import io.lettuce.core.dynamic.annotation.Param; // ❌ Lettuce(Redis) 패키지
```

올바른 import: `org.springframework.data.repository.query.Param`

현재 동작하는 이유는 Spring Data가 파라미터 이름을 자동 감지(`-parameters` 컴파일 옵션)하기 때문이나, 명시적으로 수정 권장.

---

## 8. 최종 현황

| 구분 | 수정 전 | 수정 후 |
|------|--------|--------|
| fetch join 누락 쿼리 | 4개 | 0개 |
| Lazy load 추가 쿼리 발생 지점 | 1개 (CommentServiceImpl) | 0개 |
| 전체 컴파일 | — | **성공** |
