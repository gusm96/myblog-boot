# MyBlog

---

### 목차

1. 프로젝트 개요
2. 주요 기능 상세 설명
3. 기술적 문제 및 해결
4. 프로젝트 회고

---

### 1. 프로젝트 개요

- **프로젝트 소개 및 기획 동기**
  - 프로젝트 ‘MyBlog’는 개인 블로그 플랫폼으로 게시글을 작성하고 공유하며 소통하는 서비스를 제공합니다. 
  - 나만의 기술 블로그를 직접 구축하고 운영하며 다양한 기술 스택을 쌓아가고, 여러 개발자들과 소통하며 꾸준히 성장해 나가는 기록을 남기고 싶었습니다.
- **프로젝트 주요 기능**
  - Spring Security와 JWT를 이용하여 인증/인가 기능을 구현하여 보안강화 및 Stateless한 AuthAPI 구축
  - Redis를 사용해 게시글 좋아요 및 조회수 기능 I/O 성능 향상
  - 스케줄링을 사용해 지정 기간 이후 게시글 자동 삭제 기능 구현
  - 



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

### 2. 주요 기능 상세 설명

1. **JWT를 사용한 인증 인가 서비스**

   > Spring Security와 JWT (JSON Web Token)를 활용하여 인증 및 인가 기능을 구현하였습니다. JWT의 사용은 클라이언트가 토큰의 정보를 소유함으로써, 서버가 stateless 상태를 유지할 수 있으며, DB 트래픽을 감소시켜 성능을 개선합니다.

   <a align="center">![JWT인증인가](https://github.com/gusm96/myblog-boot/assets/77833389/6e2f27cb-2376-4809-9a43-7a445f65f3f2)</a>

   

2. **게시글 좋아요 기능**

   > 게시글의 "좋아요" 수를 업데이트하는 과정에서 RDBMS에 지속적으로 접근한다면 DB의 트래픽 부하와 성능 저하가 발생할 수 있습니다. 이를 해결하기 위해 더 빠른 조회와 업데이트로 DB 트래픽을 줄일 수 있는 Redis를 활용하였습니다.
   >
   > '게시글 좋아요'기능은 Redis를 사용해 데이터를 메모리에 저장하고 엑세스 하도록 구현했습니다. 관계형 데이터로 다루면 

   

-----

### 3.  기술적 문제 및 해결

1. 동시성 문제
   처음에는 게시글 좋아요' 기능을 BoardLike Entity를 생성해 JPA를 활용하여 좋아요 수를 증가/감소하는 기능을 구현했습니다. 성능 테스트를 하기 위해 Jmeter를 사용해 100건의 동시 요청 테스트를 진행했고, 그 결과 동시성 이슈가 발생했습니다. 이 문제를 해결하기 위해 두 가지 방법을 고안했습니다. 첫째로는 멀티 스레드를 동기화하여 Thread-safe 하게 구현하는 것이었습니다.

   Java는 프로세스가 멀티 스레드로 구성되어 있어, 공유 데이터에 동시에 접근할 경우 Race Condition이 발생하여 예상치 못한 결과가 나타날 수 있습니다. 이를 방지하기 위해 스레드를 동기화 하기위해 synchronized를 사용하여 특정 메서드 영역을 임계 영역으로 설정하고, wait(), notify() 메서드를 이용하여 스레드 간 접근을 제어하려 했습니다. 그러나 이로 인해 대기열이 길어지면서 데드락(교착상태)이 발생하는 문제가 발생했습니다.

   그래서 Repository에 LockModeType.PESSIMISTIC_WRITE을 사용하여 읽기에는 다른 스레드의 접근을 허용하고 쓰기/수정에는 락을 걸도록 했습니다. 그 결과 100건의 동시 요청에는 문제없이 작업을 수행했으나 그 이상의 동시 요청시에는 여전히 원하는 결과값을 가져올 수 없었습니다. 

   두 번째 방법으로 캐시로 Redis를 사용하는 방법이였습니다. Redis는 기본적으로 싱글 스레드로 동작하기 때문에 동시성 문제를 피할 수 있었고, 빠른 조회와 수정이 가능했습니다. 

   결론적으로 '게시글 좋아요' 기능은 Redis로 구현했으며, 멀티 스레드 환경에서 동시성을 제어하는 방법에 대해 깊이 있는 학습이 필요하다고 느꼈습니다. 

   [ BoardLikeRepositoryImpl.java ]

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

   [ BoardLikeRedisRepositoryImpl.java ]

   ```java
   @Repository
   @RequiredArgsConstructor
   public class BoardLikeRedisRepositoryImpl implements BoardLikeRedisRepository {
   
       private final RedisTemplate<String, Object> redisTemplate;
       private static final String KEY = "boardLike:";
       // Set 데이터 타입 사용
       // Value 저장
       @Override
       public void add(Long boardId, Long memberId) {
           redisTemplate.opsForSet().add(KEY + boardId, memberId);
       }
   	// 좋아요 여부 확인
       @Override
       public boolean isMember(Long boardId, Long memberId) {
           return redisTemplate.opsForSet().isMember(KEY + boardId, memberId);
       }
   	// 좋아요 취소
       @Override
       public void cancel(Long boardId, Long memberId) {
           redisTemplate.opsForSet().remove(KEY + boardId, memberId);
       }
   	// 좋아요 수
       @Override
       public Long getCount(Long boardId) {
           return (long) redisTemplate.opsForSet().members(KEY + boardId).size();
       }
   }
   ```

2. Token 저장소
   JWT를 사용하여 Access Token과 Refresh Token을 생성하여 사용자 인증을 구현하면서 Token의 저장소를 결정하는 데 있어 여러 가지 고려 사항이 있었습니다. 초기에는 Access Token을 클라이언트에 전달하여 Redux Store에 저장하고, Refresh Token은 Redis를 활용하여 Memory에 저장하는 방식을 고려했습니다.

   그러나 이 방법은 상태를 유지하지 않는 Stateless한 방식이 아니었기 때문에 서버가 상태를 관리하는 방법을 찾아야 했습니다. 이에 따라 Refresh Token을 Memory에 저장하는 대신, 보다 안전한 방법으로 Cookie에 저장하기로 결정했습니다. 여러 저장소 중 Session Storage와 Local Storage도 고려했지만, Http Only 속성을 설정한 Cookie에 저장하는 방법이 XSS 공격에 비교적 안전하다고 판단하여 이를 선택했습니다.

   이러한 선택은 사용자 인증과 같이 민감한 정보를 다루는 기능에서 정보 탈취에 대한 위험을 최소화하기 위한 것이었습니다. Http Only 속성을 설정한 Cookie를 사용하면 JavaScript에서 해당 쿠키에 접근할 수 없어 XSS 공격에 대한 방어가 가능합니다.

   이와 같은 보안 관련 결정을 내리면서 사용자 인증과 같이 민감한 기능을 구현하기 위해서는 보안에 대한 추가적인 학습이 필요하다는 인식을 얻게 되었습니다. 보안 측면에서의 다양한 고려 사항을 공부하고 적용하는 것은 안전한 웹 애플리케이션을 개발하는 데 중요한 부분이라고 느꼈습니다.

-----

