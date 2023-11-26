# MyBlog

---

### 목차

1. 프로젝트 개요
2. 프로젝트 구조
3. 주요 기능 소개
4. 배운 점 / 앞으로의 계획

---

### 1. 프로젝트 개요

- **프로젝트 소개**

  - “MyBlog” 프로젝트에서 저는 클라이언트와 서버의 개발을 통해 많은 경험을 쌓을 수 있었습니다.
  - 서버 개발 측면에서는, 단순한 게시판 CRUD 구현을 넘어서서, Spring Boot와 Spring Data JPA를 깊이 있게 학습하고 숙련도를 향상시키는 것을 목표로 했습니다. 또한, RestAPI 개발, Spring Security를 활용한 보안 및 인증 인가 서비스 구현, 그리고 인메모리 데이터베이스인 Redis의 활용 경험을 쌓는 것 또한 중요한 목표였습니다.
  - 클라이언트 개발에서는 JavaScript 언어의 숙련도 향상과 React.js 라이브러리의 활용을 위한 학습을 집중적으로 진행하였습니다. 더불어, 클라이언트-서버 아키텍처를 직접 구현하면서, 클라이언트와 서버 간의 네트워크 통신에 대한 깊은 이해와 경험을 쌓는 것을 목표로 설정하였습니다.

  

- **프로젝트 목적**

  - Java 언어 숙련도 향상.
  - RDB, NoSql 사용 숙련도 향상.
  - Spring boot , Spring Data JPA (ORM)동작 원리 학습 및 사용 숙련도 향상.
  - Rest API 이해와 응용
  - Spring Security 보안 학습
    - JWT(Json Web Token)를 사용한 인증 인가 구현
    - Bcrypt를 사용한 비밀번호 암호화 구현
  - FrontEnd 학습
    - Javascript 언어 숙련도 향상.
    - React.js 라이브러리 동작 원리 학습 및 사용 숙련도 향상.
    - Redux ( 상태 관리 )
  - Clinet - Server간 네트워크 통신 학습
  - Docker 컨테이너 배포 경험.

---

### 2. 프로젝트 구조

<img width="573" alt="Architecture" src="https://github.com/gusm96/myblog-boot/assets/77833389/498b2261-bab6-41c7-95c4-216a7f64b096">

- Client-Server Architecture를 적용하여 클라이언트와 서버간 HTTP통신으로 요청과 응답을 처리하고 있습니다.
- Server는 모든 구성요소와 기능이 하나의 서비스로 통합된 **Monolithic Architecture**로 구성되어 있어, 각 기능의 상호작용이 원활하며, 개발과 테스트, 배포가 간편합니다.
- 관계형 데이터들은 JPA를 통해 관리하고 엑세스 합니다.
- "게시글 좋아요"와 같은 빠른 조회가 필요한 데이터는 **Redis**를 사용하여 **메모리**에 저장하고 관리하고 있습니다.

#### 데이터 모델링

<img width="928" alt="ERD" src="https://github.com/gusm96/myblog-boot/assets/77833389/3c6beff5-9523-4bdb-8c8a-2038a652fb31">

### 🛠️ 사용된 기술

- **Backend**
  - Java 17, Spring Boot, Spring Data JPA, MariaDB, Redis
- **Frontend**
  - JavaScript, React.js, Redux
- **DevOps**
  - Docker
- **Tools**
  - Intellj

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

   > 게시글의 "좋아요" 수를 업데이트하는 과정에서 RDBMS에 지속적으로 접근한다면 DB의 트래픽 부하와 성능 저하가 발생할 수 있습니다. 이를 해결하기 위해 더 빠른 조회와 업데이트가 가능하며, DB 트래픽을 줄일 수 있는 Redis를 활용하였습니다.

   - 게시글 좋아요 데이터는 'BoardLikeCount'와 'MemberBoardLike' 두 가지로 분류됩니다.

   - 'BoardLikeCount'는 게시글이 생성될 때 함께 생성되며, hashKey로 'count'를 가지고 있습니다. 'count'는 value 값으로서 게시글 좋아요 수를 increment하거나 decrement합니다.

     ```java
     // BoardLikeCountRedisRepositoryImpl.java
     
     		@Override
         public void save(Long boardId) {
             String key = BOARD_LIKE_COUNT_KEY + boardId;
             hashOperations.put(key, BOARD_LIKE_COUNT_HASH_KEY, 0L);
         }
     
         @Override
         public Long findBoardLikeCount(Long boardId) {
             String key = BOARD_LIKE_COUNT_KEY + boardId;
             return ((Number) hashOperations.get(key, BOARD_LIKE_COUNT_HASH_KEY)).longValue();
         }
     
         @Override
         public void update(Long boardId, Long count) {
             String key = BOARD_LIKE_COUNT_KEY + boardId;
             hashOperations.put(key, BOARD_LIKE_COUNT_HASH_KEY, count);
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

   - 사용자가 게시글에 좋아요를 요청하면 가장 먼저 권한을 검증합니다. 그 후 게시글의 존재 여부와 요청한 사용자의 게시글 좋아요 데이터의 중복 여부를 검증합니다. 중복되지 않는다면 새로운 데이터를 생성하거나, 기존 데이터에 게시글 Index 값을 Set 데이터에 추가합니다.

   - 이후 해당 게시글의 'BoardLikeCount'를 찾아와 요청에 따라 increment() 또는 decrement()를 실행하고, 총 좋아요 수를 클라이언트에게 반환합니다.

   - 클라이언트는 반환 받은 좋아요 수를 바탕으로 업데이트를 진행합니다.
