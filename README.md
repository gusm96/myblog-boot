# MyBlog

---

### 목차

1. 프로젝트 개요
2. 주요 기능 소개 
3. 기술적 문제 및 해결
4. 앞으로의 계획

---

### 1. 프로젝트 개요

- **프로젝트 소개**

  - 'MyBlog' 프로젝트에서 저는 클라이언트와 서버의 개발을 통해 많은 경험을 쌓을 수 있었습니다.
  - 서버 개발에서는, 단순히 게시판 CRUD 구현이 목적이 아닌, Spring Boot의 동작 과정을 깊이 있게 이해하고 Spring Data JPA의 성능 최적화 학습을 목표로 했습니다. 또한, RESTful한 API개발, Spring Security를 활용한 보안 및 인증 인가 서비스 구현, 그리고 In Memory 데이터베이스인 Redis의 활용 경험을 쌓는 것 또한 중요한 목표로 두었습니다.
  - 클라이언트 개발에서는 JavaScript 언어와 React.js 라이브러리의 기초적인 학습을 통해 간단한 Fornt-end 개발의 경험을 목표로 두었으며,  클라이언트-서버 아키텍처를 직접 구현하면서, 클라이언트와 서버 간의 네트워크 통신에 대한 깊은 이해와 경험을 쌓는 것을 목표로 했습니다.


- **프로젝트 구조**

<img width="573" alt="Architecture" src="https://github.com/gusm96/myblog-boot/assets/77833389/498b2261-bab6-41c7-95c4-216a7f64b096">

- Client-Server Architecture를 적용하여 클라이언트와 서버간 HTTP통신으로 요청과 응답을 처리하고 있습니다.
- Server는 모든 구성요소와 기능이 하나의 서비스로 통합된 **Monolithic Architecture**로 구성되어 있습니다.
- 관계형 데이터들은 JPA를 통해 엑세스하고 관리합니다.
- "게시글 좋아요"와 같은 빠른 조회 및 수정이 필요한 데이터는 **Redis**를 사용하여 **메모리**에 저장하고 관리하고 있습니다.

### 🛠️ 사용된 기술

- **Backend**
  - Java 17, Spring Boot, Spring Data JPA, MariaDB, Redis
- **Frontend**
  - JavaScript, React.js, Redux
- **Tools**
  - Intellj, Jmeter, Postman

---

### 3. 주요 기능

1. **JWT를 사용한 인증 인가 서비스**

   > RestFul한 서비스를 구현하는 목표로, Spring Security와 JWT (JSON Web Token)를 활용하여 인증 및 인가 기능을 성공적으로 구현하였습니다. JWT의 사용은 클라이언트가 토큰의 정보를 소유함으로써, 서버가 stateless 상태를 유지할 수 있도록 도와줍니다.

   - 사용자 로그인 요청이 발생하면, 먼저 사용자 정보를 검증합니다. 검증이 완료되면, Access Token과 Refresh Token을 생성하게 됩니다.
   - Refresh Token은 보안을 위해 Http only Cookie에 저장하여, HTTP 통신을 통해서만 접근이 가능하도록 합니다.
   - Access Token은 ResponseEntity body에 포함시켜 클라이언트에게 반환합니다.
   - 클라이언트는 Redux 라이브러리를 활용하여 store에 Access Token을 보관합니다.
   - 권한을 요구하는 요청이 발생하면, 클라이언트는 HTTP Header의 Authorization에 Access Token을 포함시켜 요청합니다.
   - Access Token이 만료되면, 서버에서 만료 알림을 보내며 클라이언트는 Refresh Token을 사용해 Access Token을 재발급 받습니다.
   - HTTP only Cookie에 저장된 Refresh Token을 검증하여 Access Token을 재발급합니다.
   - Refresh Token이 만료되면, 쿠키에서 해당 데이터를 삭제하고, 클라이언트에 알려 강제 로그아웃을 진행합니다.
   - 토큰에는 사용자의 Index와 권한(Role) 정보만 포함됩니다. 관리자(ADMIN)와 일반 회원(NORMAL)의 권한에 따라 API 접근이 제한됩니다.

2. **게시글 좋아요 기능**

   > 게시글의 "좋아요" 수를 업데이트하는 과정에서 RDBMS에 지속적으로 접근한다면 DB의 트래픽 부하와 성능 저하가 발생할 수 있습니다. 이를 해결하기 위해 더 빠른 조회와 업데이트로 DB 트래픽을 줄일 수 있는 Redis를 활용하였습니다.

   - 게시글 좋아요 데이터는 'BoardLikeCount'와 'MemberBoardLike' 두 가지로 분류됩니다.

   - 'BoardLikeCount'는 게시글이 생성될 때 함께 생성되며, hashKey로 'count'를 가지고 있습니다. 'count'는 value 값으로서 게시글 좋아요 수를 increment하거나 decrement합니다.

     ```java
     // BoardLikeCountRedisRepositoryImpl.java
         @Override
         public void save(Long boardId) {
             hashOperations.put(BOARD_LIKE_COUNT_KEY + boardId, BOARD_LIKE_COUNT_HASH_KEY, 0L);
         }
     
         @Override
         public Long findBoardLikeCount(Long boardId) {
             return getCount(boardId);
         }
     
         @Override
         public Long incrementBoardLikeCount(Long boardId) {
             hashOperations.increment(BOARD_LIKE_COUNT_KEY + boardId, BOARD_LIKE_COUNT_HASH_KEY, 1L);
             return getCount(boardId);
         }
     
         @Override
         public Long decrementBoardLikeCount(Long boardId) {
             if(getCount(boardId) - 1L < 0) return 0L;
             hashOperations.increment(BOARD_LIKE_COUNT_KEY + boardId, BOARD_LIKE_COUNT_HASH_KEY, -1L);
             return getCount(boardId);
         }
     
         private long getCount(Long boardId) {
             //  찾아온 값을 Number로 캐스팅하고, 그 값을 long 타입으로 변환.
             return ((Number) hashOperations.get(BOARD_LIKE_COUNT_KEY + boardId, BOARD_LIKE_COUNT_HASH_KEY)).longValue();
         }
     
     ```
   
   - 'MemberBoardLike'는 사용자가 게시글에 좋아요를 요청할 때 생성되며, Set 데이터 구조를 가집니다.
   
     ```java
     // MemberBoardLikeRedisRepositoryImpl.java
     		@Override
         public void save(Long memberId, Long boardId) {
             String key = MEMBER_BOARD_LIKE_KEY + memberId;
             redisTemplate.opsForSet().add(key, boardId);
         }
     
         @Override
         public boolean isMember(Long memberId, Long boardId) {
             String key = MEMBER_BOARD_LIKE_KEY + memberId;
             return redisTemplate.opsForSet().isMember(key, boardId);
         }
     
     		@Override
         public void delete(Long memberId, Long boardId) {
             String key = MEMBER_BOARD_LIKE_KEY + memberId;
             redisTemplate.opsForSet().remove(key, boardId);
         }
     ```
     
   - 사용자가 게시글에 좋아요를 요청하면 가장 먼저 권한을 검증합니다. 
   
   - 그 후 게시글의 존재 여부와 요청한 사용자의 게시글 좋아요 데이터의 중복 여부를 검증합니다. 중복되지 않는다면 새로운 데이터를 생성하거나, 기존 데이터에 게시글 Index 값을 Set에 추가합니다.
   
   - 이후 해당 게시글의 'BoardLikeCount'를 찾아와 요청에 따라 incrementBoardLikeCount() 또는 decrementBoardLikeCount()를 실행하고, 수정된 좋아요 수를 클라이언트에게 반환합니다.
   
   - 클라이언트는 반환 받은 좋아요 수를 바탕으로 업데이트를 진행합니다.

-----

### 3.  기술적 문제 및 해결

1. 동시성 문제

   테스트를 진행하면서 '게시글 좋아요' 기능을 JPA와 Redis로 각각 구현한 결과, Jmeter를 사용한 동시성 테스트에서 JPA에서 동시성 이슈가 발생했습니다. Redis로 구현한 기능은 단순한 로직 리팩터링만으로 1000건의 동시 요청에 대해 원활한 수행이 가능했습니다. 그러나 JPA로 구현한 기능은 동시성 이슈를 해결하기 위해 추가적인 처리가 필요했습니다.

   Java는 멀티 스레드 프로그래밍을 지원하지만, 공유 데이터에 동시에 접근할 경우 Race Condition이 발생하여 예상치 못한 결과가 나타날 수 있습니다. 이를 방지하기 위해 JPA에서는 synchronized를 사용하여 특정 메서드 영역을 임계 영역으로 설정하고, wait(), notify() 메서드를 이용하여 스레드 간 접근을 제어하려 했습니다. 그러나 이로 인해 대기열이 길어지면서 데드락(교착상태)이 발생하는 문제가 발생했습니다.

   이러한 상황에서는 비관적 락을 도입하여 데이터 수정 시에 블로킹을 걸어 동시성을 해결했습니다. 비관적 락은 다른 스레드의 접근을 허용하지 않고, 하나의 스레드가 작업을 수행하는 동안 다른 스레드의 접근을 막는 방식입니다. 이를 통해 JPA에서의 동시성 문제를 해결할 수 있었습니다.

   종합적으로, 동시 요청이 많아질수록 JPA는 성능 저하가 발생하고, 이에 대한 대안으로 Redis로 구현한 기능을 사용하게 되었습니다. Redis는 단순하면서도 높은 성능을 제공하여 동시성 이슈를 효과적으로 해결할 수 있었습니다.

   ```java
   @Repository
   @RequiredArgsConstructor
   public class BoardLikeRepositoryImpl implements BoardLikeRepository {
       private final EntityManager em;
   	// ...
       @Override
       public Optional<BoardLike> findByBoardId(Long boardId) {
           try {
               BoardLike boardLike = em.createQuery("select b from BoardLike b where b.board.id =: boardId", BoardLike.class)
                       .setParameter("boardId", boardId)
                   	.setLockMode(LockModeType.PESSIMISTIC_WRITE) // 비관적 락 적용 읽기는 허용, 쓰기 및 수정 락
                       .getSingleResult();
               return Optional.ofNullable(boardLike);
           } catch (NoResultException e) {
               return Optional.empty();
           }
       }
   }
   ```

   [ Redis를 사용해 회원 로그인 및 게시글 좋아요 요청 1000 건 동시 요청 ]

   <img width="754" alt="스크린샷 2023-12-02 011938" src="https://github.com/gusm96/myblog-boot/assets/77833389/0fed3ad7-c01d-4b1b-bf72-214d1317184f">

   <img width="514" alt="스크린샷 2023-12-02 004755" src="https://github.com/gusm96/myblog-boot/assets/77833389/3fa2edd7-3998-4d24-b8e7-bf2a09190800">

   - 평균 로그인 요청 4초 이내, 좋아요 요청 4초 이내

   - 오류 0%

2. Token 저장소
   JWT를 사용하여 Access Token과 Refresh Token을 생성하여 사용자 인증을 구현하면서 Token의 저장소를 결정하는 데 있어 여러 가지 고려 사항이 있었습니다. 초기에는 Access Token을 클라이언트에 전달하여 Redux Store에 저장하고, Refresh Token은 Redis를 활용하여 Memory에 저장하는 방식을 고려했습니다.
   
   그러나 이 방법은 상태를 유지하지 않는 Stateless한 방식이 아니었기 때문에 서버가 상태를 관리하는 방법을 찾아야 했습니다. 이에 따라 Refresh Token을 Memory에 저장하는 대신, 보다 안전한 방법으로 Cookie에 저장하기로 결정했습니다. 여러 저장소 중 Session Storage와 Local Storage도 고려했지만, Http Only 속성을 설정한 Cookie에 저장하는 방법이 XSS 공격에 비교적 안전하다고 판단하여 이를 선택했습니다.
   
   이러한 선택은 사용자 인증과 같이 민감한 정보를 다루는 기능에서 정보 탈취에 대한 위험을 최소화하기 위한 것이었습니다. Http Only 속성을 설정한 Cookie를 사용하면 JavaScript에서 해당 쿠키에 접근할 수 없어 XSS 공격에 대한 방어가 가능합니다.
   
   이와 같은 보안 관련 결정을 내리면서 사용자 인증과 같이 민감한 기능을 구현하기 위해서는 보안에 대한 추가적인 학습이 필요하다는 인식을 얻게 되었습니다. 보안 측면에서의 다양한 고려 사항을 공부하고 적용하는 것은 안전한 웹 애플리케이션을 개발하는 데 중요한 부분이라고 느꼈습니다.

-----

### 4. 앞으로의 계획

- Docker와 Jenkins를 사용해 CI/CD Pipeline 구축

  - 현재 CI/CD 파이프라인을 구축하기 위해 Docker와 Jenkins를 학습하고 있습니다.

  - 아래 사진과 같은 구조로 구축할 계획 입니다.

    ![image](https://github.com/gusm96/myblog-boot/assets/77833389/51b0a0a4-4bb8-429e-a5ce-7a070a411911)

