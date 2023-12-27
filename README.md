# MyBlog

---

### 1. 프로젝트 개요

- **프로젝트 소개**
  
  - 프로젝트 ‘MyBlog’는 개인 블로그 플랫폼으로 게시글을 작성하고 공유하며 소통하는 서비스를 제공합니다.
  
- **프로젝트 구조**

  <img width="816" alt="스크린샷 2023-12-27 174137" src="https://github.com/gusm96/myblog-boot/assets/77833389/e88733f7-0fba-4f3c-8b2d-cb9fa947e75e">


* **서버 CI/CD 구조**

  <img width="954" alt="스크린샷 2023-12-27 151745" src="https://github.com/gusm96/myblog-boot/assets/77833389/4230cde7-a02f-4a43-80d9-4d390e00798a">

- **플로우 차트**

  ![%ED%94%8C%EB%A1%9C%EC%9A%B0_%EC%B0%A8%ED%8A%B8](https://github.com/gusm96/myblog-boot/assets/77833389/ae414311-b831-4616-bc07-0213ccb112fa)

- **프로젝트 주요 기능**
  - Spring Security와 JWT를 사용해 인증/인가 필터 구현
  - Auth API (회원 및 인증 관련 API)
    - 회원 등록, 로그인, 로그아웃 ,토큰 검증 , 토큰 재발급
  - Board API (게시글 CRUD API)
    - 게시글 조회(모든 게시글, 카테고리별 게시글, 검색 게시글, 게시글 상세)
    - 게시글 업로드, 게시글 수정, 게시글 삭제
    - Redis 캐싱 전략을 사용한 게시글 조회/좋아요 기능
    - 스케줄러을 사용해 주기적으로 DB 갱신
      - 게시글 삭제 요청 15일 이후 자동 삭제
      - 게시글 조회수/좋아요 메모리 → DB 갱신
  - FileUpload API 
    - AWS S3 이미지 파일 업로드 / 삭제 기능
  - Category API (카테고리 CRUD API)
    - 카테고리 리스트 조회, 등록, 수정, 삭제
  - Comment API (댓글 CRUD API)
    - 댓글 작성
      - 대댓글 작성
    - 댓글 리스트 조회, 수정, 삭제

- **기술 스택**
  - Backend
    - Java 17, Spring Boot, Spring Data JPA, MariaDB, Redis

  - Frontend
    - JavaScript, React.js, Redux
  - DevOps
    - Docker, Jenkins, AWS EC2, AWS S3
  - Tools
    - Intellj, Jmeter, Postman


---

### 2. 주요 기능 상세 설명

1. **JWT를 사용한 인증 인가 서비스**

   > Spring Security와 JWT (JSON Web Token)를 활용하여 인증 및 인가 기능을 구현하였습니다. JWT를 클라이언트가 소유하도록 구현함으로써 서버가 stateless 상태를 유지할 수 있으며, 불필요한 DB 트래픽을 최소화했습니다. 

   - JWT claims에 회원ID값과, 권한만 저장하여 최소한의 정보만 소유하도록 했습니다.
   - Access Token과 Refresh Token을 생성하고 Access Token은 Redux store에 저장하고 관리하며 짧은 생명 주기를 가져 만료시 Refresh Token을 사용해 재발급 받습니다.
   - Refresh Token은 HttpOnly Cookie에 저장하여 HTTP 통신으로만 접근 가능하여 XSS로부터 보안을 강화 했습니다.

   <a align="center">![JWT인증인가](https://github.com/gusm96/myblog-boot/assets/77833389/6e2f27cb-2376-4809-9a43-7a445f65f3f2)</a>

   

2. **Redis를 사용한 게시글 조회수/좋아요 기능**

   > ‘게시글 조회수/좋아요’기능은 빠른 조회와 수정을 요구하는 기능이며, 멀티 스레드 환경에서 동시성 제어가 필요 했습니다. 그래서 싱글 스레드로 동작하는 Redis를 캐시로 사용해 동시성을 제어하고  I/O 성능을 향상시켰습니다. 또한 메모리 용량 관리 및 데이터 정합성을 위해 스케줄러로 주기적으로 메모리의 데이터를 DB에 갱신하도록 구현했습니다.

   - Lazy Loading, Write-Behind 캐싱 전략을 적용 했습니다.

   [ 게시글 조회(조회수 증가) 기능 ]

   ![Untitled 2](https://github.com/gusm96/myblog-boot/assets/77833389/ff2c2ee6-4850-402e-bbb5-99d2a3bb7272)

   [ 기존 버전의 조회 코드 ]

   ```java
   // 게시글 상세 조회 V2
       @Override
       @Transactional
       public BoardDetailResDto boardToResponseDto(Long boardId) {
           // Board Entity 조회
           Board findBoard = retrieveBoardById(boardId);
           // Redis에 저장된 좋아요 수
           Long likes = getBoardLikeCount(boardId);
           // Redis에서 조회수 가져오기
           Long views = getViews(findBoard);
   
           // 응답용 DTO 객체로 변환
           return BoardDetailResDto.builder()
                   .board(findBoard)
                   .likes(likes)
                   .views(views)
                   .build();
       }
       // Redis에서 조회수 가져오기
       private Long getViews(Board board) {
           Long views = boardRedisRepository.getViews(board.getId());
           // 조회수 갱신 로직
           if (views == null || views < board.getViews()) {
               // 조회수가 null 이거나 DB에 저장된 값보다 작은 경우 캐시에 DB 데이터 저장
               views = boardRedisRepository.setViews(board.getId(), board.getViews() + 1L);
           } else {
               // 조회수 증가 후 결과 값 반환
               views = boardRedisRepository.viewsIncrement(board.getId());
           }
           return views;
       }
   ```

   [ 리팩터링 후 새로운 버전의 조회 코드 ]

   ```Java
   // 게시글 상세 조회 V3
       @Override
       public BoardResDtoV2 retrieveBoardDetail(Long boardId) {
           Optional<BoardForRedis> boardForRedis = boardRedisRepository.findById(boardId);
           // Memory에 데이터 없으면 DB에서 조회
           if(boardForRedis.isEmpty()){
               Board board = retrieveBoardById(boardId);
               BoardForRedis saveBoard = boardRedisRepository.save(board);
               return BoardResDtoV2.builder().boardForRedis(saveBoard).build();
           }
           return BoardResDtoV2.builder().boardForRedis(boardForRedis.get()).build();
       }
   ```

   [ 게시글 조회수 성능 비교 테스트 ]

   테스트 시나리오

   *1000건의 동시 요청을 10회 반복해서 요청하도록 설정했습니다.*

   ![Untitled 5](https://github.com/gusm96/myblog-boot/assets/77833389/e98ba722-ae52-46ec-9a49-344b84aa3eb0)

   ![%EC%8A%A4%ED%81%AC%EB%A6%B0%EC%83%B7_2023-12-21_002512](https://github.com/gusm96/myblog-boot/assets/77833389/4cedce94-3367-4af8-b29e-6b4c9f77ccbc)

   |                | 평균 응답 속도 (ms) | TPS        |
   | -------------- | ------------------- | ---------- |
   | 게시글 조회 V2 | 789 ms              | 1171.2 TPS |
   | 게시글 조회 V3 | 174 ms              | 4882.8 TPS |

   - 게시글 조회 V2 데이터 조회

   ![%EC%8A%A4%ED%81%AC%EB%A6%B0%EC%83%B7_2023-12-21_002555](https://github.com/gusm96/myblog-boot/assets/77833389/4e83fc58-994c-42b7-9965-b4e0ee113128)

   - 게시글 조회 V3 데이터 조회

   ![Untitled 6](https://github.com/gusm96/myblog-boot/assets/77833389/bcd8d420-05e8-4330-8dee-85bb84472ce5)

   

   [👆게시글 좋아요 코드 보기](https://github.com/gusm96/myblog-boot/blob/master/src/main/java/com/moya/myblogboot/service/implementation/BoardLikeServiceImpl.java)

3. **스케줄링**

   > 게시글 삭제 요청 후 15일 이 지난 게시글을 DB에서 삭제하도록 구현했으며, Redis를 사용해 메모리에 저장한 게시글 조회수/좋아요 데이터를 DB에 주기적으로 업데이트 하도록 구현했습니다.

   [👆스케줄링 코드 보기](https://github.com/gusm96/myblog-boot/blob/master/src/main/java/com/moya/myblogboot/service/ScheduledTaskService.java)

### 3.  기술적 문제 및 해결

1. **동시성 문제 해결 및 성능 개선** 

   게시글 조회수/좋아요 기능을 JPA로 구현하고서 Jmeter를 사용해 서버 부하 테스트를 진행했을 때, 동시성 문제를 발견할 수 있었습니다. 이러한 동시성 문제를 해결하기 위해 다음과 같이 다양한 방법을 시도해보았습니다.

   **[ 멀티 스레드 동기화 (블로킹) ]**

   첫 번째 방법으로는 게시글 데이터를 조회하고 수정할 때 synchronized를 사용하여 임계 영역을 지정하고 wait(), notify() 메서드를 이용하여 대기열을 구성하여 하나의 스레드가 데이터에 접근하고 작업을 수행할 때 다른 스레드의 접근을 제어하려 했으나, 이로 인해 대기열이 길어지면서 성능이 저하되었습니다.

   **[ 트랜잭션 로킹 (비관적 락) ]**

   두 번째로는 트랜잭션에 비관적 락(LockModeType.PESSIMISTIC_WRITE)을 사용하여 읽기에는 다른 스레드의 접근을 허용하고 쓰기/수정에는 락을 걸도록 구현했습니다. 100건의 동시 요청에는 문제가 없었지만, 더 많은 동시 요청 시에는 락 대기가 길어져 작업이 정상적으로 수행되지 않았습니다.

   **[ Redis 사용 ]**

   세 번째 방법으로는 싱글 스레드로 동작하는 Redis를 사용하는 것이었습니다. 데이터에 접근할 때 싱글 스레드로 작업을 수행하기 때문에 동시성 문제를 해결할 수 있었으며, In Memory 데이터베이스로 데이터를 메모리에 저장하고 관리하기 때문에 데이터 조회 및 수정을 빠르게 수행할 수 있습니다. 결론적으로 Redis를 사용했고, 캐싱 전략으로 Lazy Loading전략과 Write Behind전략을 조합하여 동시성을 해결하고 I/O 성능 향상 및 데이터 정합성을 보장하도록 구현했습니다.

2. **JPA N+1 문제 해결**

   댓글 조회 기능을 구현하면서 부모 댓글과 자식 댓글을 한 번에 조회하면서 발생한 N+1 문제를

   지연 로딩( Lazy Loading ) 과 Fetch Join을 사용해 해결했습니다.

   연관 엔티티의 FetchType을 LAZY로 변경하고 Repository 쿼리에 Fetch Join을 사용하여 필요한 데이터를 한 번에 조회할 수 있도록 하였습니다.

   그 결과 불필요한 쿼리 발생을 줄이고 데이터 조회 성능을 향상할 수 있었습니다.

   ```java
   // CommentRepository
   		@Query("SELECT c FROM Comment c " +
               "LEFT JOIN FETCH c.parent " +
               "LEFT JOIN FETCH c.child " +
               "WHERE c.board.id = :boardId " +
               "AND (c.parent IS NULL OR c.parent.id IS NULL) " + // 부모 댓글만 조회
               "ORDER BY c.write_date DESC") // 작성일 기준 내림차순 정렬
       List<Comment> findAllByBoardId(Long boardId);
   
   // CommentServiceImpl
   		// 댓글 리스트
       @Override
       public List<CommentResDto> getCommentList(Long boardId) {
   				// 해당 boardID의 댓글 조회
           List<Comment> comments = commentRepository.findAllByBoardId(boardId);
   				// DTO 객체로 변환
           return comments.stream()
                   .map(parent -> CommentResDto.builder()
                           .comment(parent)
                           .child(parent.getChild().stream().map(CommentResDto::of).collect(Collectors.toList()))
                           .build())
                   .collect(Collectors.toList());
       }
   ```

-----

### 4. 프로젝트  후 느낀 점

- 단순히 게시판 CRUD 기능을 구현하고 싶어서 시작한 프로젝트가 아닌 다양한 기술들을 학습하고 적용해 나가며 기술적 역량을 높이고자 진행한 프로젝트였습니다. 처음으로 Spring Boot와 JPA를 사용해 보면서 프로젝트 설정에 대한 편리함과, SQL 쿼리에서 벗어나 개발에 더 집중할 수 있어서 좋은 경험이 되었습니다. 순수 JPA로 시작해 Spring Data JPA로 리팩터링하고 @Query를 사용해 필요한 기능에서는 직접 쿼리문을 작성하기도 했으며, N+1 문제를 해결해 나가며 다양한 경험을 쌓을 수 있었습니다. 반면에 JPA를 사용하며 몇 가지 단점도 느낄 수 있었습니다. 객체 간 관계 설정을 잘못하면 성능 저하가 발생할 수 있고, 복잡한 쿼리를 처리하기에 어려움을 겪을 수 있었습니다. 그래서 현재 동적 쿼리와, 복잡한 쿼리를 조금 더 쉽게 다를 수 있게 Querydsl을 학습 중이며, 객체 간 관계 설계에 있어서 더 많은 경험과 학습이 필요하다고 느꼈습니다.
- Redis를 사용해 성능 개선을 한 경험이 정말 좋았습니다. 캐시를 잘 활용하면 시스템 성능을 크게 개선할 수 있음을 알 수 있었습니다. 그뿐만 아니라 Redis를 학습함으로써 프로세스와 스레드에 대해서 깊이 있게 학습하고 멀티 스레드 환경에서 동시성 제어 방법들에 대해서 알 수 있는 중요한 경험이 되었습니다.
- 클라이언트 개발을 통해서 JavaScript 언어에 친숙해지고, React.js 라이브러리를 경험할 수 있어서 좋았습니다. 백엔드 개발자로서 프론트엔드의 업무를 간접적으로 경험할 수 있었고, 클라이언트와 서버 간의 HTTP 통신에 대해서 깊이 있게 학습할 수 있는 좋은 기회가 되었습니다.
- AWS EC2를 이용해 Linux 환경에서 Docker와 Jenkins를 다루며 자동 배포화를 구현해보는 경험을 할 수 있었습니다. 이를 통해 클라우드 환경에서 Docker를 활용하면 시스템 관리가 얼마나 효율적으로 이루어지는지를 알 수 있었습니다. 

