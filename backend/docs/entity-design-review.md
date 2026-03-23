# Entity 설계 분석 보고서

> 분석 일자: 2026-03-19
> 대상: `myblog-boot` 백엔드 JPA Entity 전체 (7개 Entity + 1개 Base Class + 1개 Redis Entity)

---

## 1. Entity 구조 개요

### 1.1 클래스 다이어그램

```
BaseTimeEntity (MappedSuperclass)
  ├── Board
  └── Member

Board ──(N:1)──▶ Member
Board ──(N:1)──▶ Category
Board ──(1:N)──▶ Comment
Board ──(1:N)──▶ ImageFile
Board ──(1:N)──▶ BoardLike

Comment ──(N:1)──▶ Member
Comment ──(N:1)──▶ Board
Comment ──(N:1)──▶ Comment (self-ref, parent)
Comment ──(1:N)──▶ Comment (self-ref, child)

BoardLike ──(N:1)──▶ Board
BoardLike ──(N:1)──▶ Member

Member ──(1:N)──▶ BoardLike

Category ──(1:N)──▶ Board

VisitorCount (독립 Entity, LocalDate PK)

MemberBoardLike (Redis @RedisHash, 비 JPA)
```

### 1.2 Entity 목록

| Entity | 테이블 | PK 타입 | 상속 | 연관관계 수 |
|--------|--------|---------|------|------------|
| `Board` | board | Long (IDENTITY) | BaseTimeEntity | 5 |
| `Member` | member | Long (IDENTITY) | BaseTimeEntity | 1 |
| `Category` | category | Long (IDENTITY) | - | 1 |
| `Comment` | comment | Long (IDENTITY) | - | 4 (self-ref 포함) |
| `BoardLike` | board_like | Long (IDENTITY) | - | 2 |
| `ImageFile` | image_file | Long (IDENTITY) | - | 1 |
| `VisitorCount` | visitor_count | LocalDate | - | 0 |

---

## 2. 항목별 상세 분석

### 2.1 BaseTimeEntity (MappedSuperclass)

**파일**: `domain/base/BaseTimeEntity.java`

```java
@Getter
@MappedSuperclass
public abstract class BaseTimeEntity {
    @Column(updatable = false)
    private LocalDateTime createDate;
    private LocalDateTime updateDate;
    private LocalDateTime deleteDate;
}
```

#### 평가: 양호 (개선 여지 있음)

| 항목 | 상태 | 설명 |
|------|------|------|
| Soft Delete 지원 | ✅ 양호 | `deleteDate` 필드로 논리 삭제 구현 |
| `@PrePersist` 사용 | ✅ 양호 | 생성 시 `createDate`, `updateDate` 자동 설정 |
| `@PreUpdate` 미사용 | ⚠️ 주의 | `update()` 메서드를 수동 호출해야 `updateDate`가 갱신됨 |
| `@EntityListeners` 미사용 | ℹ️ 참고 | Spring Data JPA의 `@EnableJpaAuditing` + `@EntityListeners(AuditingEntityListener.class)` 조합이 더 표준적 |

**문제점 1: `updateDate` 자동 갱신 안 됨**

현재 `updateDate`를 갱신하려면 `this.update()`를 명시적으로 호출해야 합니다. `Board.updateBoard()`에서는 호출하지만, 다른 변경(예: `updateBoardStatus()`, `updateViews()`, `updateLikes()`)에서는 호출하지 않습니다.

```java
// Board.java - update() 호출 O
public void updateBoard(Category category, String title, String content) {
    this.update(); // ✅ 호출됨
}

// Board.java - update() 호출 X
public void updateBoardStatus(BoardStatus boardStatus) {
    this.boardStatus = boardStatus; // ❌ updateDate 갱신 안 됨
}
```

**권장 개선안**: `@PreUpdate` 콜백 추가로 자동 갱신

```java
@PreUpdate
public void preUpdate() {
    this.updateDate = LocalDateTime.now();
}
```

---

### 2.2 Board Entity (핵심 Entity)

**파일**: `domain/board/Board.java`

#### 2.2.1 연관관계 매핑 분석

| 연관관계 | 타입 | Fetch | Cascade | orphanRemoval | 평가 |
|----------|------|-------|---------|---------------|------|
| `member` | `@ManyToOne` | LAZY | - | - | ✅ 적절 |
| `category` | `@ManyToOne` | LAZY | - | - | ✅ 적절 |
| `comments` | `@OneToMany` | EAGER(기본) | ALL | true | ⚠️ Fetch 전략 |
| `imageFiles` | `@OneToMany` | EAGER(기본) | ALL | true | ⚠️ Fetch 전략 |
| `boardLikes` | `@OneToMany` | EAGER(기본) | ALL | true | ⚠️ Fetch 전략 |

**문제점 2: `@OneToMany` 기본 FetchType (EAGER) 사용 - 심각**

`@OneToMany`의 JPA 기본값은 `FetchType.LAZY`이지만, Hibernate 구현에서 `@OneToMany`는 기본 `LAZY`입니다. 다만 **명시적으로 `fetch = FetchType.LAZY`를 선언하지 않은 점**은 코드 가독성 및 의도 명확성 측면에서 개선이 필요합니다.

> **확인 결과**: Hibernate에서 `@OneToMany`의 기본값은 `LAZY`이므로 동작상 문제는 없으나, 명시적 선언이 Best Practice입니다.

**권장**: 모든 `@OneToMany`에 `fetch = FetchType.LAZY` 명시

```java
@OneToMany(mappedBy = "board", cascade = CascadeType.ALL,
           orphanRemoval = true, fetch = FetchType.LAZY) // 명시적 선언
private List<Comment> comments = new ArrayList<>();
```

#### 2.2.2 양방향 연관관계 동기화 메서드 미흡

Hibernate Best Practice에 따르면, 양방향 연관관계에서는 **양쪽 모두를 동기화하는 헬퍼 메서드**가 필요합니다.

**현재 코드의 문제**:

```java
// Board.java - 단방향만 설정
public void addComment(Comment comment) {
    this.comments.add(comment);
    // ❌ comment.setBoard(this) 누락 — 양쪽 동기화 안 됨
}
```

**Hibernate 공식 권장 패턴**:

```java
public void addComment(Comment comment) {
    this.comments.add(comment);
    comment.setBoard(this); // 양쪽 동기화
}

public void removeComment(Comment comment) {
    this.comments.remove(comment);
    comment.setBoard(null); // 양쪽 동기화
}
```

> `addImageFile()`, `removeImageFile()`도 동일한 문제가 있습니다.

#### 2.2.3 `CascadeType.ALL` 사용 범위

| 컬렉션 | CascadeType.ALL | 적절성 |
|---------|----------------|--------|
| `comments` | ✅ | Board 삭제 시 댓글도 삭제 — 합리적 |
| `imageFiles` | ✅ | Board 삭제 시 이미지 메타도 삭제 — 합리적 |
| `boardLikes` | ✅ | Board 삭제 시 좋아요도 삭제 — 합리적 |

> `CascadeType.ALL`은 `REMOVE`를 포함하므로, 대량 데이터 삭제 시 N+1 DELETE 쿼리가 발생할 수 있습니다. 대량 삭제가 필요한 경우 벌크 연산(`@Modifying @Query`)을 고려해야 합니다.

#### 2.2.4 `views`, `likes` 필드 동시성 문제

```java
private Long views = 0L;
private Long likes = 0L;

public void updateViews(Long views) {
    this.views = views;
}
```

현재 `views`와 `likes`는 외부에서 값을 받아 덮어쓰는 방식입니다. 동시 요청 시 **Lost Update** 가능성이 있습니다. Redis 캐시로 관리하고 있으므로 DB 직접 갱신 시에는 주의가 필요합니다.

#### 2.2.5 `category` nullable 불일치

```java
@JoinColumn(name = "category_id", nullable = false) // DB 제약: NOT NULL
private Category category;

public void removeCategory() {
    this.category = null; // 코드에서 null 설정 → DB 제약 위반 가능
}
```

`Category.@PreRemove`에서 `board.removeCategory()`를 호출하지만, `@JoinColumn(nullable = false)`로 인해 **카테고리 삭제 시 DB 제약 조건 위반 오류가 발생**합니다.

**권장**: `nullable = true`로 변경하거나, 카테고리 삭제 시 게시글을 다른 카테고리로 이전하는 정책 수립

---

### 2.3 Member Entity

**파일**: `domain/member/Member.java`

#### 평가

| 항목 | 상태 | 설명 |
|------|------|------|
| 생성자 접근 제한 | ✅ 양호 | `@NoArgsConstructor(access = PROTECTED)` |
| 비밀번호 필드 | ⚠️ 주의 | `@Column` 제약조건 없음 (nullable, length 미지정) |
| username 유니크 제약 | ❌ 누락 | `@Column(unique = true)` 필요 |
| nickname 유니크 제약 | ⚠️ 검토 필요 | 비즈니스 규칙에 따라 결정 |
| `equals()`/`hashCode()` | ❌ 누락 | `BoardLike`를 `Set`으로 관리하므로 필수 |

**문제점 3: `equals()`/`hashCode()` 미구현 - 심각**

`Member`는 `BoardLike`를 `Set<BoardLike>`로 관리합니다. Hibernate에서 `Set`을 사용하는 Entity는 반드시 `equals()`/`hashCode()`를 올바르게 구현해야 합니다.

Hibernate 공식 문서 권고:
> *"Entity classes should override equals() and hashCode(), especially when associations are represented as sets."*

**권장 구현** (비즈니스 키 기반):

```java
@Override
public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof Member member)) return false;
    return id != null && id.equals(member.getId());
}

@Override
public int hashCode() {
    return getClass().hashCode(); // 상수 hashCode 패턴
}
```

**문제점 4: `username` 유니크 제약 미설정**

```java
private String username; // ❌ @Column(unique = true, nullable = false) 필요
private String password; // ❌ @Column(nullable = false) 필요
private String nickname; // ❌ @Column(nullable = false) 필요
```

**문제점 5: Board와의 양방향 관계 부재**

`Member`는 `Board`와의 `@OneToMany` 관계가 없습니다. `Board.member`는 `@ManyToOne`으로 매핑되어 있지만 `Member` 쪽에는 역방향 참조가 없습니다. 이것은 **의도적인 설계라면 적절**합니다 — 불필요한 양방향은 피하는 것이 좋습니다. `Member`가 작성한 Board 목록은 JPQL이나 Repository 메서드로 조회하면 됩니다.

---

### 2.4 Category Entity

**파일**: `domain/category/Category.java`

#### 평가

| 항목 | 상태 | 설명 |
|------|------|------|
| `name` 유니크 제약 | ❌ 누락 | 카테고리명 중복 방지 필요 |
| 생성자 접근 제한 | ⚠️ 주의 | `@NoArgsConstructor` public — PROTECTED 권장 |
| `@PreRemove` 전략 | ❌ 문제 | Board의 `category_id`가 NOT NULL이므로 실행 시 오류 |
| `name` nullable | ⚠️ 주의 | `@Column(nullable = false)` 미설정 |

**문제점 6: 생성자 접근 수준**

```java
@NoArgsConstructor  // ❌ public 접근 — 무분별한 빈 객체 생성 가능
@AllArgsConstructor // ❌ public 접근 — id까지 포함된 전체 생성자 노출
```

JPA 스펙은 기본 생성자를 요구하지만, `protected`면 충분합니다.

**문제점 7: `@PreRemove`와 NOT NULL 제약 충돌** (2.2.5와 연계)

```java
@PreRemove
private void removeBoards() {
    for (Board board : boards) {
        board.removeCategory(); // category = null 설정
    }
}
// Board의 @JoinColumn(nullable = false)와 충돌
```

---

### 2.5 Comment Entity

**파일**: `domain/comment/Comment.java`

#### 평가

| 항목 | 상태 | 설명 |
|------|------|------|
| Self-referencing 구조 | ✅ 양호 | 대댓글을 위한 parent/child 관계 적절 |
| `@ManyToOne` LAZY | ✅ 양호 | 모든 `@ManyToOne`에 LAZY 적용 |
| `write_date` 네이밍 | ⚠️ 주의 | Java 컨벤션은 camelCase (`writeDate`) |
| `@PrePersist` 미사용 | ⚠️ 주의 | 생성자에서 `LocalDateTime.now()` 직접 호출 |
| `equals()`/`hashCode()` | ⚠️ 주의 | `child`가 `List`이므로 당장 필수는 아니나 권장 |
| BaseTimeEntity 미상속 | ℹ️ 검토 | `write_date`를 직접 관리 — 일관성 측면에서 검토 필요 |

**문제점 8: 필드명 Java 네이밍 컨벤션 위반**

```java
private LocalDateTime write_date; // ❌ snake_case
// 권장: private LocalDateTime writeDate;
// DB 컬럼명은 @Column(name = "write_date")로 분리
```

**문제점 9: 시간 관리 불일치**

`Board`와 `Member`는 `BaseTimeEntity`를 상속하여 `createDate`/`updateDate`를 관리하지만, `Comment`는 자체 `write_date`를 사용합니다. 일관성 있는 설계를 위해 `BaseTimeEntity` 상속을 고려할 수 있습니다.

**문제점 10: `member`와 `board` JoinColumn nullable 설정 누락**

```java
@JoinColumn(name = "member_id")  // ❌ nullable = false 필요
private Member member;

@JoinColumn(name = "board_id")   // ❌ nullable = false 필요
private Board board;
```

댓글은 반드시 작성자와 게시글이 있어야 하므로 NOT NULL 제약이 필요합니다.

---

### 2.6 BoardLike Entity

**파일**: `domain/board/BoardLike.java`

#### 평가

| 항목 | 상태 | 설명 |
|------|------|------|
| 복합 인덱스 | ✅ 양호 | `(board_id, member_id)` 인덱스 설정 |
| Unique 제약 | ❌ 누락 | 동일 사용자의 중복 좋아요 방지 필요 |
| 생성자 접근 제한 | ⚠️ 주의 | `@NoArgsConstructor` public |
| `equals()`/`hashCode()` | ❌ 누락 | Board와 Member 모두 `Set<BoardLike>` 사용 — **필수** |
| JoinColumn nullable | ⚠️ 주의 | `nullable = false` 미설정 |

**문제점 11: Unique Constraint 누락 - 심각**

```java
@Table(indexes = {
    @Index(name = "idx_board_like_member", columnList = "board_id, member_id")
})
// ❌ 인덱스는 있지만 유니크 제약은 없음
```

인덱스만으로는 중복 삽입을 막을 수 없습니다. 애플리케이션 레벨에서 `existsByBoardIdAndMemberId()`로 체크하고 있겠지만, **DB 레벨 유니크 제약**이 데이터 무결성을 보장합니다.

**권장**:

```java
@Table(uniqueConstraints = {
    @UniqueConstraint(name = "uk_board_like_member", columnNames = {"board_id", "member_id"})
})
```

**문제점 12: `equals()`/`hashCode()` 미구현 - 심각**

`Board.boardLikes`와 `Member.boardLikes` 모두 `Set<BoardLike>`로 선언되어 있습니다. `Set`에 담기는 Entity는 반드시 `equals()`/`hashCode()`를 구현해야 합니다.

---

### 2.7 ImageFile Entity

**파일**: `domain/file/ImageFile.java`

#### 평가

| 항목 | 상태 | 설명 |
|------|------|------|
| `@Builder` 클래스 레벨 | ✅ 양호 | 간단한 구조에 적합 |
| LAZY fetching | ✅ 양호 | `@ManyToOne(fetch = LAZY)` |
| 컬럼 제약 | ⚠️ 주의 | `fileName`, `filePath`에 `nullable = false` 미설정 |
| JoinColumn nullable | ⚠️ 주의 | `board_id`에 `nullable = false` 미설정 |
| 업로드 메타 부재 | ℹ️ 참고 | 파일 크기, MIME 타입, 업로드일시 등 메타정보 없음 |

---

### 2.8 VisitorCount Entity

**파일**: `domain/visitor/VisitorCount.java`

#### 평가

| 항목 | 상태 | 설명 |
|------|------|------|
| 자연 키 PK (`LocalDate`) | ✅ 우수 | 날짜별 1건 — 적절한 PK 전략 |
| 정적 팩토리 메서드 | ✅ 우수 | `VisitorCount.of()` 패턴 |
| 독립 Entity | ✅ 양호 | 다른 Entity와 연관 없음 — 적절 |
| 컬럼 제약 | ⚠️ 주의 | `totalVisitors`, `todayVisitors`에 `nullable = false` 미설정 |

---

## 3. 종합 문제 요약

### 3.1 심각도별 분류

#### 🔴 Critical (데이터 무결성/런타임 오류 위험)

| # | 문제 | 영향 범위 | 설명 |
|---|------|----------|------|
| C1 | `equals()`/`hashCode()` 미구현 | Board, Member, BoardLike | `Set` 컬렉션 사용 Entity에 필수. 미구현 시 Hibernate 동작 오류 가능 |
| C2 | Category 삭제 시 NOT NULL 제약 위반 | Category, Board | `@PreRemove`에서 `category = null` 설정하지만 `@JoinColumn(nullable = false)` |
| C3 | BoardLike 유니크 제약 미설정 | BoardLike | 동일 사용자 중복 좋아요 데이터 삽입 가능 |

#### 🟡 Warning (Best Practice 위반)

| # | 문제 | 영향 범위 | 설명 |
|---|------|----------|------|
| W1 | 양방향 연관관계 동기화 미흡 | Board (comments, imageFiles) | 헬퍼 메서드에서 반대쪽 참조 미설정 |
| W2 | `username` 유니크 제약 누락 | Member | DB 레벨 유니크 보장 없음 |
| W3 | `@OneToMany` LAZY 미명시 | Board | 기본값이지만 명시적 선언 권장 |
| W4 | `@PreUpdate` 미사용 | BaseTimeEntity | `updateDate` 수동 관리로 누락 가능성 |
| W5 | 생성자 접근 제한 미설정 | Category, BoardLike | `public` 기본 생성자 노출 |
| W6 | JoinColumn `nullable = false` 누락 | Comment, BoardLike, ImageFile | 필수 관계에 NOT NULL 미설정 |
| W7 | 컬럼 제약 부족 | Member, Category, ImageFile, VisitorCount | `nullable`, `length`, `unique` 등 |

#### 🔵 Info (코드 품질/일관성)

| # | 문제 | 영향 범위 | 설명 |
|---|------|----------|------|
| I1 | `write_date` 네이밍 | Comment | Java 컨벤션 위반 (snake_case) |
| I2 | 시간 관리 불일치 | Comment | `BaseTimeEntity` 미상속, 독자적 시간 관리 |
| I3 | ImageFile 메타 부족 | ImageFile | 파일 크기, MIME 타입 등 없음 |

---

## 4. 쿼리 레벨 분석

### 4.1 BoardRepository JPQL 분석

```java
// findById — join fetch 잘 활용
@Query("select distinct b from Board b " +
    "join fetch b.member " +
    "left join fetch b.boardLikes boardLike " +
    "left join fetch boardLike.member " +
    "join fetch b.category where b.id = :boardId")
Optional<Board> findById(@Param("boardId") Long boardId);
```

| 쿼리 | N+1 방지 | 평가 |
|------|---------|------|
| `findById` | ✅ `join fetch` 4개 사용 | 우수 |
| `findAll` (Page) | ✅ `join fetch` member, category | 양호 |
| `findAllByCategoryName` | ⚠️ fetch join 없음 | N+1 가능 |
| `findByDeletionStatus` | ⚠️ fetch join 없음 | N+1 가능 |

**주의**: `findById`에서 `boardLikes`를 fetch join하지만 `comments`와 `imageFiles`는 하지 않습니다. 이는 의도적일 수 있으나, 해당 컬렉션 접근 시 추가 쿼리가 발생합니다.

### 4.2 MultipleBagFetchException 위험

Hibernate에서 **2개 이상의 `List` 타입 컬렉션을 동시에 fetch join하면 `MultipleBagFetchException`**이 발생합니다.

현재 Board의 컬렉션:
- `boardLikes`: **Set** (✅ 안전)
- `comments`: **List** (⚠️)
- `imageFiles`: **List** (⚠️)

`comments`와 `imageFiles`를 동시에 fetch join하면 오류가 발생합니다. 현재 `findById`에서는 `boardLikes`(Set)만 fetch join하므로 문제없지만, 향후 확장 시 주의가 필요합니다.

**권장**: `comments`를 `Set`으로 변경하거나, 별도 쿼리로 분리

---

## 5. 개선 우선순위 로드맵

### Phase 1: Critical 수정 (즉시)

1. **`BoardLike`에 `equals()`/`hashCode()` 구현** + Unique Constraint 추가
2. **`Board`의 `category` JoinColumn `nullable = true`로 변경** 또는 Category 삭제 정책 변경
3. **`Member`에 `equals()`/`hashCode()` 구현**

### Phase 2: Best Practice 적용 (단기)

4. `BaseTimeEntity`에 `@PreUpdate` 추가
5. 양방향 연관관계 헬퍼 메서드 보강 (`addComment`, `addImageFile` 등)
6. `Member.username`에 `@Column(unique = true, nullable = false)` 추가
7. 모든 `@OneToMany`에 `fetch = FetchType.LAZY` 명시

### Phase 3: 코드 품질 (중기)

8. `Comment.write_date` → `writeDate` 리네이밍
9. `Comment`의 `BaseTimeEntity` 상속 검토
10. 누락된 `@Column(nullable = false)` 일괄 추가
11. 생성자 접근 수준 `PROTECTED`로 통일

---

## 6. 결론

전체적으로 **기본적인 JPA 매핑은 올바르게** 구성되어 있습니다. `@ManyToOne`의 LAZY 전략, `CascadeType.ALL`의 적절한 적용, Querydsl 활용 등이 잘 되어 있습니다.

그러나 **`equals()`/`hashCode()` 미구현, Category 삭제 시 NOT NULL 충돌, BoardLike 유니크 제약 부재**는 운영 환경에서 데이터 무결성 문제를 일으킬 수 있는 Critical 이슈입니다. 이 3가지를 우선적으로 해결하는 것을 권장합니다.

---

*이 분석은 Hibernate ORM 공식 문서 및 Spring Boot JPA Best Practices를 기준으로 작성되었습니다.*
