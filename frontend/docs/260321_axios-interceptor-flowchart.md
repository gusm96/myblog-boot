# Axios Interceptor 도입 — 플로우차트

## Before: 토큰을 각 컴포넌트에서 직접 전달

```mermaid
flowchart TD
    Redux[("Redux Store<br/>accessToken")] -->|useSelector| A
    Redux -->|useSelector| B
    Redux -->|useSelector| C
    Redux -->|useSelector| D

    A[BoardEditForm] -->|accessToken 파라미터| API1["boardApi.js<br/>getBoardForAdmin(boardId, accessToken)<br/>editBoard(boardId, board, html, accessToken)<br/>deleteBoard(boardId, accessToken)"]
    B[BoardLike] -->|accessToken 파라미터| API2["boardApi.js<br/>addBoardLike(boardId, accessToken)<br/>cancelBoardLike(boardId, accessToken)"]
    C[CategoryList] -->|accessToken 파라미터| API3["categoryApi.js<br/>getCategoriesForAdmin(accessToken)<br/>addNewCategory(name, accessToken)<br/>deleteCategory(id, accessToken)"]
    D[ProtectedRoute] -->|accessToken 파라미터| API4["authApi.js<br/>getRoleFromToken(accessToken)"]

    API1 -->|"headers: Authorization: bearer token"| Server[("백엔드 서버")]
    API2 -->|"headers: Authorization: bearer token"| Server
    API3 -->|"headers: Authorization: bearer token"| Server
    API4 -->|"headers: Authorization: bearer token"| Server

    style Redux fill:#f9a,stroke:#c00
    style Server fill:#adf,stroke:#06c
```

**문제점**
- `accessToken`이 Redux → 컴포넌트 → 함수 파라미터 → axios 헤더 경로로 매번 수동 전달
- 13개 컴포넌트 × N개 API 함수마다 `Authorization` 헤더 중복 작성
- 토큰 갱신 시 모든 호출부가 영향을 받음

---

## After: Interceptor가 토큰 주입 자동화

```mermaid
flowchart TD
    Redux[("Redux Store<br/>accessToken")]

    subgraph App["App.js (최상위)"]
        direction TB
        UE["useEffect<br/>토큰 변경 감지"]
    end

    subgraph ApiClient["apiClient.js (싱글톤)"]
        direction TB
        T["_token 저장소"]
        IC["Request Interceptor<br/>Authorization 헤더 자동 주입"]
        AX["axios 인스턴스"]
        T --> IC
        IC --> AX
    end

    Redux -->|useSelector| UE
    UE -->|setAuthToken 호출| T

    A[BoardEditForm] -->|"getBoardForAdmin(boardId)"| AX
    B[BoardLike] -->|"addBoardLike(boardId)"| AX
    C[CategoryList] -->|"getCategoriesForAdmin()"| AX
    D[ProtectedRoute] -->|"getRoleFromToken()"| AX

    AX -->|Authorization 헤더 자동 포함| Server[("백엔드 서버")]

    style Redux fill:#f9a,stroke:#c00
    style Server fill:#adf,stroke:#06c
    style T fill:#efe,stroke:#090
    style IC fill:#efe,stroke:#090
    style AX fill:#efe,stroke:#090
```

---

## 토큰 동기화 상세 흐름

```mermaid
sequenceDiagram
    participant User as 사용자
    participant Redux as Redux Store
    participant App as App.js
    participant Client as apiClient.js
    participant API as "boardApi / authApi / categoryApi"
    participant Server as 백엔드 서버

    User->>Redux: 로그인 후 accessToken 저장
    Redux-->>App: useSelector 반응
    App->>Client: setAuthToken(accessToken)
    Note over Client: _token = accessToken

    User->>API: getBoardForAdmin(boardId)
    API->>Client: apiClient.get /api/v1/management/boards/1
    Note over Client: Interceptor 실행
    Note over Client: Authorization 헤더 자동 주입
    Client->>Server: GET /api/v1/management/boards/1
    Note over Client,Server: Authorization: bearer xxx
    Server-->>Client: 200 OK
    Client-->>API: res.data
    API-->>User: 게시글 데이터

    Note over App,Client: 토큰 재발급 시
    App->>Client: setAuthToken(newAccessToken)
    Note over Client: _token 갱신 완료
    Note over Client: 이후 모든 요청에 새 토큰 자동 적용
```

