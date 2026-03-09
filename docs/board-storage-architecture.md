# Board 엔티티 저장소 아키텍처 분석 및 개선 방향

> 작성일: 2026-03-01
> 대상 프로젝트: myblog-boot (Spring Boot 3.0.4 + MariaDB + Redis)

---

## 1. 현재 아키텍처 전체 구조

```
클라이언트 요청
      │
      ▼
 BoardController
      │
      ▼
 BoardServiceImpl
      │
      ├──────────────────────┐
      │                      │
      ▼                      ▼
 BoardRepository      BoardRedisRepository
 (MariaDB / JPA)      (Redis / Lettuce)
      │                      │
      ▼                      ▼
┌─────────────┐       ┌─────────────────┐
│   MariaDB   │       │      Redis      │
│             │       │                 │
│ board       │       │ board:{id}      │
│ ├ title     │◄──────│ ├ views         │
│ ├ content   │  sync │ ├ likes         │
│ ├ views     │       │ └ ...           │
│ ├ likes     │       │                 │
│ └ ...       │       │ {key}:views     │
└─────────────┘       │ (조회수 중복방지)│
                      └─────────────────┘
```

현재 프로젝트는 이미 **MariaDB + Redis 이중 저장소** 구조를 사용하고 있습니다.
이 사실이 이후 아키텍처 결정의 핵심 전제가 됩니다.

---

## 2. Board 엔티티 및 연관 관계 전체 맵

```
┌─────────────────────────────────────────────────────────────┐
│                         Board                               │
│                                                             │
│  id         : Long (PK)                                     │
│  title      : String (varchar 255)                          │
│  content    : String (longtext)  ← 핵심 논의 대상           │
│  views      : Long               ← Redis에서 관리 중        │
│  likes      : Long               ← Redis에서 관리 중        │
│  boardStatus: BoardStatus (VIEW / HIDE)                     │
│  createDate : LocalDateTime      ← BaseTimeEntity 상속      │
│  updateDate : LocalDateTime                                 │
│  deleteDate : LocalDateTime      ← soft delete             │
│                                                             │
│  member   : Member   (N:1, LAZY)                           │
│  category : Category (N:1, LAZY)                           │
│  boardLikes: Set<BoardLike> (1:N, CASCADE ALL)             │
│  imageFiles: List<ImageFile> (1:N, CASCADE ALL)            │
│  comments  : List<Comment>  (1:N, CASCADE ALL)             │
└─────────────────────────────────────────────────────────────┘
         │                │              │              │
         │ N:1            │ N:1          │ 1:N          │ 1:N
         ▼                ▼              ▼              ▼
    ┌─────────┐     ┌──────────┐  ┌──────────┐  ┌──────────┐
    │ Member  │     │ Category │  │BoardLike │  │ImageFile │
    │         │     │          │  │          │  │          │
    │ id      │     │ id       │  │ id       │  │ id       │
    │ username│     │ name     │  │ board_id │  │ fileName │
    │ password│     │ boards   │  │ member_id│  │ filePath │
    │ nickname│     │ (1:N)    │  │          │  │ board_id │
    │ role    │     │          │  │ @Index   │  │          │
    └─────────┘     └──────────┘  │(board,   │  └──────────┘
         │                        │ member)  │       │
         │ 1:N                    └──────────┘       │ S3
         ▼                                           ▼
    ┌──────────┐                              ┌──────────────┐
    │ Comment  │                              │   AWS S3     │
    │          │                              │ (실제 이미지) │
    │ id       │                              └──────────────┘
    │ comment  │
    │ parent   │ ◄─── 자기참조 (계층형 댓글)
    │ child    │ (1:N, CASCADE REMOVE)
    │ board_id │
    │ member_id│
    └──────────┘
```

---

## 3. 실제 성능 문제 분석

"longtext를 RDB에 저장하는 게 비효율적인가?"라는 질문에 답하려면,
**어떤 쿼리가 실제로 실행되는지**를 먼저 분석해야 합니다.

### 3.1 InnoDB의 longtext 저장 방식

MariaDB InnoDB는 `longtext` 컬럼이 일정 크기(약 768바이트, ROW_FORMAT에 따라 다름)를 초과하면
**오버플로우 페이지(off-page)** 에 저장하고, 메인 레코드에는 20바이트 포인터만 남깁니다.

```
board 테이블 B-tree 페이지 (메인 레코드)
┌──────────┬───────────────┬───────┬──────────────────────────────┐
│ board_id │ title         │ views │ content → [20byte 포인터]    │
│ 8 bytes  │ varchar(255)  │ 8byte │          (실제 데이터 없음)   │
└──────────┴───────────────┴───────┴──────────────────────────────┘
                                                    │
                                 오버플로우 페이지 ──┘
                                 (실제 content 저장)
```

**결론**: `SELECT board_id, title, views FROM board` 같이 `content`를 포함하지 않는 쿼리는
longtext 데이터를 전혀 읽지 않으므로 **목록 조회 성능에 longtext는 영향 없음**

---

### 3.2 현재 코드에서 발견된 실제 문제들

#### 문제 1: content LIKE 검색 (Critical)

```java
// BoardQuerydslRepositoryImpl.java
// CONTENT 검색
board.content.contains(contents)  // → LIKE '%keyword%' on longtext
```

이 쿼리는 실제로 아래 SQL을 생성합니다:

```sql
SELECT * FROM board WHERE content LIKE '%keyword%' AND board_status = 'VIEW'
```

`LIKE '%keyword%'`는 **인덱스를 전혀 사용하지 못하며**, 모든 행의 longtext를 읽어야 합니다.
게시글이 수천~수만 건으로 늘어나면 이 쿼리가 가장 먼저 병목이 됩니다.

---

#### 문제 2: views/likes 컬럼의 이중 관리 (Medium)

현재 `views`와 `likes`는 **MariaDB 컬럼과 Redis** 양쪽에 모두 존재합니다.

```java
// BoardServiceImpl.java
// Redis에서 조회수 증가
BoardForRedis cached = boardRedisRepository.incrementViews(boardForRedis);

// 동기화는 @Async로 비동기 처리
@Async
public void updateBoardForRedis(Board board) { ... }
```

```java
// BoardRedisRepositoryImpl.java
// Redis key 구조
"board:{boardId}"         → BoardForRedis 객체 (직렬화)
"{key}:views"             → 조회수 중복 방지용 IP 집합
```

Board 테이블의 `views`, `likes` 컬럼이 실제로 언제, 어떻게 DB에 반영되는지
명확한 배치 동기화 전략이 부재하면 **데이터 정합성 리스크**가 발생합니다.

---

#### 문제 3: Board 상세 조회 시 N+1 가능성 (Low~Medium)

```java
// BoardRepository의 커스텀 findById
// Fetch Join으로 member, boardLikes, category를 한 번에 로드하지만
// comments와 imageFiles는 별도로 로드될 수 있음
```

상세 조회 시 comments가 LAZY 로딩이면 게시글 1건 조회 후
댓글 개수만큼 추가 쿼리가 발생할 수 있습니다.

---

#### 문제 4: BoardStatus가 HIDE만 존재 (설계 관찰)

```java
public enum BoardStatus {
    VIEW, HIDE
}
// 삭제는 deleteDate (soft delete) 방식
```

삭제 상태와 공개 상태가 분리된 방식은 올바르지만,
`findByDeletionStatus`에서 `deleteDate IS NOT NULL`로 필터링하면
삭제된 게시글을 포함한 전체 테이블 스캔이 발생할 수 있습니다.

---

## 4. MongoDB 도입 시나리오별 분석

### 4.1 시나리오 A: Board 전체를 MongoDB로 이관

```
// 이관 후 데이터 모델 (MongoDB)
{
  "_id": ObjectId,
  "title": "게시글 제목",
  "content": "longtext 내용...",
  "views": 100,
  "likes": 50,
  "boardStatus": "VIEW",
  "memberId": 1,          // ← 참조만 저장
  "categoryId": 3,        // ← 참조만 저장
  "createDate": ISODate,
  "deleteDate": null
}
```

**장점**:
- content가 BSON 문서로 자연스럽게 저장됨
- 스키마 변경(필드 추가) 시 마이그레이션 불필요
- 단일 문서 조회는 RDB JOIN보다 단순

**단점 (이 프로젝트에서 결정적)**:

| 현재 기능 | MongoDB 이관 후 문제 |
|-----------|---------------------|
| `Category` ↔ `Board` JPA JOIN | JPA 연관관계 사용 불가 → 애플리케이션 레벨 조인 필요 |
| `findCategoriesWithViewBoards()` Querydsl | MongoDB ↔ MariaDB 크로스 DB 조인 불가 |
| `Comment` CASCADE (Board 삭제 시 연쇄) | JPA CASCADE 사용 불가 → 수동 처리 필요 |
| `BoardLike` Board/Member 양쪽 외래키 | 무결성 보장 불가 |
| `ImageFile` Board에 CASCADE ALL | 수동 삭제 로직 필요 |
| Querydsl 동적 검색 | Spring Data MongoDB Criteria로 전면 재작성 |
| `@Transactional` 원자성 | MongoDB ← → MariaDB 분산 트랜잭션 복잡도 급증 |

**결론**: Board 전체 이관은 기존 코드베이스를 거의 전면 재작성해야 합니다.
이 프로젝트의 구조적 특성상 **득보다 실이 압도적으로 큼**.

---

### 4.2 시나리오 B: content만 MongoDB로 분리 (Hybrid)

```
MariaDB (board 테이블)          MongoDB (board_content 컬렉션)
─────────────────────          ─────────────────────────────
board_id (PK)            ──→  { board_id: 1,
title                           content: "...",
board_status                    revision_history: [   ← 추가 가능
views                             { content: "이전 내용",
likes                               modified_at: ISODate }
member_id (FK)                  ]
category_id (FK)              }
create_date
delete_date
```

**장점**:
- 관계형 데이터(카테고리, 멤버, 좋아요, 댓글)는 MariaDB에서 그대로 관리
- Board의 스키마 유연성 확보
- 개정 이력(revision history) 같은 배열 데이터 자연스럽게 저장 가능

**단점**:

```java
// 게시글 상세 조회 시 두 DB를 반드시 순차 조회해야 함
Board board = boardRepository.findById(boardId);      // MariaDB
String content = contentRepository.findByBoardId(boardId); // MongoDB

// 게시글 작성 시 두 DB에 모두 저장해야 함
// → 둘 중 하나 실패 시 정합성 깨짐 (분산 트랜잭션)
boardRepository.save(board);           // MariaDB 성공
contentRepository.save(boardContent);  // MongoDB 실패 → content 유실
```

`@Transactional`이 MongoDB에는 적용되지 않으므로
두 저장소 간 정합성을 애플리케이션에서 직접 보장해야 하는 복잡도가 생깁니다.

---

### 4.3 시나리오 C: 현재 구조 점진적 개선 (권장)

MongoDB 도입 없이 현재 MariaDB + Redis 구조에서 실제 병목을 해결합니다.

---

## 5. 권장 개선 방향 (단계별 로드맵)

### Phase 1: 즉시 개선 가능한 사항

#### 1-1. content 검색 → Full-Text Index 적용

```sql
-- MariaDB Full-Text Index 생성
ALTER TABLE board ADD FULLTEXT INDEX ft_board_content (title, content);

-- 검색 쿼리
SELECT board_id, title FROM board
WHERE MATCH(title, content) AGAINST('keyword' IN BOOLEAN MODE)
  AND board_status = 'VIEW';
```

```java
// BoardRepository에 네이티브 쿼리 추가
@Query(value = """
    SELECT b FROM Board b
    WHERE MATCH(b.title, b.content) AGAINST(:keyword IN BOOLEAN MODE)
      AND b.boardStatus = 'VIEW'
    """, nativeQuery = true)
Page<Board> findByFullTextSearch(@Param("keyword") String keyword, Pageable pageable);
```

**효과**: LIKE 전체 스캔 제거, 대용량 content 검색 수십~수백 배 성능 향상

---

#### 1-2. deleteDate 인덱스 추가

```sql
-- 삭제된 게시글 조회 최적화
CREATE INDEX idx_board_delete_date ON board(delete_date);
```

---

#### 1-3. views/likes DB 동기화 전략 명확화

현재 Redis에서 views/likes를 관리하지만 MariaDB 컬럼 동기화 시점이 불명확합니다.

```java
// 권장: 스케줄러로 일괄 동기화 (현재 @Async 방식 보완)
@Scheduled(fixedDelay = 300_000) // 5분마다
@Transactional
public void syncViewsAndLikesToDb() {
    Set<Long> cachedBoardIds = boardRedisRepository.getKeys("board:*");
    for (Long boardId : cachedBoardIds) {
        boardRedisRepository.findOne(boardId).ifPresent(cached -> {
            boardRepository.findById(boardId).ifPresent(board -> {
                board.updateViews(cached.getViews());
                board.updateLikes(cached.getLikes());
            });
        });
    }
}
```

---

### Phase 2: 단기 개선 (검색 품질 향상)

#### 2-1. Elasticsearch 연동 (content 검색 전용)

Full-Text Index로도 부족한 경우(형태소 분석, 유사어 검색, 관련도 랭킹 등) Elasticsearch를 추가합니다.
이는 MongoDB 도입보다 실질적 가치가 높은 NoSQL 선택입니다.

```
MariaDB                    Redis                  Elasticsearch
────────────               ──────────             ─────────────
board (메타데이터)          board:{id}             board_index
 ├ board_id ──────────────────────────────────→   ├ board_id
 ├ title    ──→ (동기화)                           ├ title
 ├ content  ──→ (동기화)                           ├ content (형태소 분석)
 ├ views    ←── (배치 sync)                        └ analyzed_content
 └ likes    ←── (배치 sync)
```

```java
// 검색은 Elasticsearch
@GetMapping("/api/v1/boards/search")
public ResponseEntity<BoardListResDto> search(
        @RequestParam SearchType type,
        @RequestParam String contents,
        @RequestParam int page) {
    if (type == SearchType.CONTENT) {
        return ResponseEntity.ok(boardSearchService.searchByElasticsearch(contents, page));
    }
    return ResponseEntity.ok(boardService.retrieveAllBySearched(type, contents, page));
}
```

---

### Phase 3: 중장기 개선 (필요시)

#### 3-1. 수직 분할 (Vertical Partitioning) — MongoDB 없이 RDB 내에서 해결

MongoDB를 추가하는 대신, **MariaDB 내에서 테이블을 분리**합니다.

```sql
-- 현재: board 테이블에 content 포함
-- 개선: board_content 테이블 분리

CREATE TABLE board_content (
    board_id  BIGINT NOT NULL PRIMARY KEY,
    content   LONGTEXT NOT NULL,
    FOREIGN KEY (board_id) REFERENCES board(board_id) ON DELETE CASCADE
);

ALTER TABLE board DROP COLUMN content;
```

```java
// Board 엔티티
@Entity
public class Board extends BaseTimeEntity {
    // ... 기존 필드들 (content 제외)

    @OneToOne(fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    @JoinColumn(name = "board_id")
    private BoardContent boardContent;
}

// BoardContent 엔티티 (신규)
@Entity
public class BoardContent {
    @Id
    private Long boardId;

    @Lob
    private String content;

    @OneToOne(fetch = FetchType.LAZY)
    @MapsId
    @JoinColumn(name = "board_id")
    private Board board;
}
```

**효과**:
- 목록 조회 시 `board` 테이블만 스캔 → `board_content` 오버플로우 페이지 I/O 완전 차단
- 상세 조회 시만 JOIN으로 `content` 로드
- JPA 관계/트랜잭션/Querydsl 그대로 유지

---

#### 3-2. MongoDB 도입이 진짜 유효한 경우

다음 요구사항이 생긴다면 그때 도입을 검토하는 것이 합리적입니다:

```
✅ 게시글마다 다른 메타데이터가 필요한 경우
   예) 일반 글: { title, content }
       레시피: { title, content, ingredients: [...], cookingTime: 30 }
       여행기: { title, content, locations: [{ lat, lng, name }] }

✅ 게시글 개정 이력(revision history) 기능 추가 시
   { content: "최신 내용",
     history: [{ content: "이전 내용", modifiedAt: ... }] }

✅ 게시글 검색 인덱스를 MongoDB Atlas Search로 구성하는 경우

✅ 독립적으로 스케일 아웃이 필요한 대용량 content 저장소가 필요한 경우
```

---

## 6. 아키텍처 방향 결정 매트릭스

| 개선 항목 | 난이도 | 효과 | 기존 코드 영향 | 권장 시기 |
|-----------|--------|------|---------------|----------|
| Full-Text Index 추가 | ⭐ | ⭐⭐⭐⭐ | 없음 | 즉시 |
| deleteDate 인덱스 | ⭐ | ⭐⭐ | 없음 | 즉시 |
| views/likes 배치 동기화 | ⭐⭐ | ⭐⭐⭐ | 최소 | 단기 |
| 수직 분할 (board_content 분리) | ⭐⭐⭐ | ⭐⭐⭐ | JPA 수정 필요 | 단기~중기 |
| Elasticsearch 연동 | ⭐⭐⭐ | ⭐⭐⭐⭐⭐ | 검색 서비스 추가 | 중기 |
| Board 전체 → MongoDB | ⭐⭐⭐⭐⭐ | ⭐ | 전면 재작성 | 권장하지 않음 |
| content만 → MongoDB | ⭐⭐⭐⭐ | ⭐⭐ | 서비스 레이어 수정 | 필요시 검토 |

---

## 7. 최종 결론

### "longtext를 RDB에 저장하는 게 비효율적인가?"

**저장 자체는 비효율적이지 않습니다.**
InnoDB의 off-page 저장으로 목록 조회 I/O에는 영향이 없고,
단건 상세 조회는 MongoDB 도입으로도 큰 차이가 없습니다.

### 실제 비효율의 원인

1. **content LIKE 검색** — 이것이 진짜 병목. Full-Text Index 또는 Elasticsearch로 해결
2. **views/likes 이중 관리** — 배치 동기화 전략 명확화로 해결
3. **deleteDate 인덱스 미적용** — 인덱스 추가로 즉시 해결

### MongoDB 도입에 대한 결론

현재 프로젝트는 **Board ↔ Category ↔ Member ↔ Comment ↔ BoardLike** 사이의
강한 관계형 의존성이 있고, JPA + Querydsl로 정교하게 관리되고 있습니다.
게다가 **Redis라는 NoSQL을 이미 핵심 캐시 레이어로 사용 중**입니다.

여기에 MongoDB를 추가하면 세 가지 저장소(MariaDB + Redis + MongoDB)가
트랜잭션 없이 연동되어야 하는 복잡도를 감수해야 합니다.

**지금 단계에서 가장 가성비 높은 선택:**

```
1단계  Full-Text Index         → content 검색 즉시 해결
2단계  views/likes 동기화 정리 → 데이터 정합성 확보
3단계  Elasticsearch           → 검색 품질이 중요해질 때
4단계  수직 분할               → 스케일이 필요해질 때
MongoDB                        → 스키마 유연성/개정이력이 필요해질 때
```

MongoDB는 나쁜 선택이 아니라 **지금 이 프로젝트에서 우선순위가 낮은** 선택입니다.
