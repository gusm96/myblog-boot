# RFC-BE-006: N+1 쿼리 점검 및 최적화

> 작업일: 2026-02-25
> 상태: 완료

---

## 점검 대상

게시글 목록 조회 시 `Member`, `Category` 등 연관 엔티티를 개별 조회하는 N+1 문제 여부를 리포지토리 레이어에서 확인.

---

## 점검 결과

### 게시글 목록 쿼리 — N+1 안전

| 쿼리 | 파일 | N+1 여부 |
|------|------|---------|
| `findAll(BoardStatus, Pageable)` | `BoardRepository.java` | 안전 |
| `findAllByCategoryName(String, PageRequest)` | `BoardRepository.java` | 안전 |
| `findBySearchType(Pageable, SearchType, String)` | `BoardQuerydslRepositoryImpl.java` | 안전 |
| `findByDeletionStatus(PageRequest)` | `BoardRepository.java` | 안전 |

**이유**: `BoardResDto`가 Board 엔티티의 스칼라 필드만 접근함.

```java
// BoardResDto.java — member, category 등 연관 엔티티에 접근하지 않음
public BoardResDto(Board board) {
    this.id = board.getId();
    this.title = board.getTitle();
    this.content = board.getContent();
    this.createDate = board.getCreateDate();
    this.updateDate = board.getUpdateDate();
    this.deleteDate = board.getDeleteDate();
    this.boardStatus = board.getBoardStatus();
}
```

> **주의**: 향후 `BoardResDto`에 작성자명(`member.nickname`)이나 카테고리명(`category.name`) 등 연관 필드를 추가할 경우, 해당 목록 쿼리에 Fetch Join을 반드시 추가해야 N+1이 발생하지 않음.

### 게시글 상세 쿼리 — Fetch Join 적용됨

```java
// BoardRepository.java — findById() 오버라이드
@Query("select distinct b from Board b " +
        "join fetch b.member " +
        "left join fetch b.boardLikes boardLike " +
        "left join fetch boardLike.member " +
        "join fetch b.category where b.id = :boardId ")
Optional<Board> findById(@Param("boardId") Long boardId);
```

### 댓글 쿼리 — Fetch Join 적용됨

```java
// CommentQuerydslRepositoryImpl.java
queryFactory.selectDistinct(comment1)
    .leftJoin(comment1.child).fetchJoin()
    .leftJoin(comment1.member).fetchJoin()
    ...
```

### 영구 삭제 쿼리 — 안전

`findByDeleteDate()`는 `board.imageFiles`만 Fetch Join하며,
`Board::deleteBoard()`는 스칼라 필드(`deleteDate`, `boardStatus`)만 변경하므로 N+1 미발생.

---

## 발견된 성능 문제 및 수정

### 1. 검색 카운트 쿼리 비효율 (수정 완료)

**문제**: `findBySearchType()`의 카운트 쿼리가 `fetch().size()`로 전체 엔티티를 메모리에 로드

```java
// 변경 전 — 검색 결과 10,000건이면 10,000개 엔티티를 모두 메모리에 로드
Long count = (long) queryFactory.selectFrom(board)
        .where(condition)
        .fetch().size();

// 변경 후 — DB에서 COUNT 쿼리 실행 (SELECT COUNT(*))
Long count = queryFactory.select(board.count())
        .from(board)
        .where(condition)
        .fetchOne();
```

**영향**: 검색 결과가 많을수록 메모리 사용량과 응답 시간이 급격히 증가하던 문제 해소.

### 2. JPAQueryFactory 스레드 안전성 (수정 완료)

**문제**: 모든 Querydsl 리포지토리에서 `JPAQueryFactory`를 인스턴스 변수로 선언하고 매 메서드 호출마다 재생성

```java
// 변경 전 — 싱글톤 빈의 인스턴스 변수를 매번 덮어씀 (스레드 안전하지 않음)
private final EntityManager em;
private JPAQueryFactory queryFactory;

public List<Board> findByDeleteDate(LocalDateTime deleteDate) {
    queryFactory = new JPAQueryFactory(em); // 동시 호출 시 경합 발생 가능
    ...
}
```

```java
// 변경 후 — Spring 빈으로 주입받아 불변 참조
private final JPAQueryFactory queryFactory;
```

**수정 파일**:

| 파일 | 변경 내용 |
|------|----------|
| `QuerydslConfig.java` (신규) | `JPAQueryFactory` 빈 등록 |
| `BoardQuerydslRepositoryImpl.java` | `final` 필드로 주입, `EntityManager` 제거 |
| `CommentQuerydslRepositoryImpl.java` | 동일 |
| `CategoryQuerydslRepositoryImpl.java` | 동일 |

---

## 수정 파일 요약

| 파일 | 유형 | 변경 내용 |
|------|------|----------|
| `configuration/QuerydslConfig.java` | 신규 | JPAQueryFactory 빈 등록 |
| `repository/implementation/BoardQuerydslRepositoryImpl.java` | 수정 | 카운트 쿼리 최적화 + JPAQueryFactory 주입 |
| `repository/implementation/CommentQuerydslRepositoryImpl.java` | 수정 | JPAQueryFactory 주입 |
| `repository/implementation/CategoryQuerydslRepositoryImpl.java` | 수정 | JPAQueryFactory 주입 |
