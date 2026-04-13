import { BrowserRouter as Router, Routes, Route } from "react-router";
import Home from "./screens/Home";
import { Management } from "./screens/Management";
import { BoardEditForm } from "./components/Boards/BoardEditForm";
import { UserLayout } from "./components/Layout/UserLayout";
import { ProtectedRoute } from "./components/Layout/ProtectedRoute";
import { LoginForm } from "./screens/Member/LoginForm";
import { useDispatch, useSelector } from "react-redux";
import { selectAccessToken, selectIsLoggedIn } from "./redux/userSlice";
import { useEffect } from "react";
import { reissuingAccessToken } from "./services/authApi";
import { updateUserAccessToken, userLogout } from "./redux/authAction";
import { setAuthToken } from "./services/apiClient";
import { NotFound } from "./screens/error/NotFound";
import { NavigateBack } from "./screens/error/NavigateBack";
import { CategoryList } from "./components/Category/CategoryList";
import { TemporaryStorage } from "./screens/TemporaryStorage";
import { SearchPage } from "./screens/SearchPage";
import { QueryClient } from "@tanstack/react-query";
import { PersistQueryClientProvider } from "@tanstack/react-query-persist-client";
import { createSyncStoragePersister } from "@tanstack/query-sync-storage-persister";
import { BoardDetail } from "./components/Boards/BoardDetail";
import { PageByCategory } from "./screens/PageByCategory";
import BoardEditor from "./components/Boards/BoardEditor";
import ErrorBoundary from "./components/ErrorBoundary";

// context7 TanStack Query 공식 문서 기반:
// CSR SPA에서 QueryClient는 모듈 최상위에서 1회 생성 (렌더마다 재생성 방지)
// gcTime은 persistQueryClient maxAge(24h)와 동일하게 설정 — GC로 삭제 시 localStorage도 동기화되므로 maxAge 이상이어야 함
const PERSIST_MAX_AGE = 1000 * 60 * 60 * 24; // 24시간

const queryClient = new QueryClient({
  defaultOptions: {
    queries: {
      staleTime:            1000 * 60 * 10,  // 10분: 기본 fresh 유지 시간
      gcTime:               PERSIST_MAX_AGE, // 24시간: maxAge와 동일 (GC → localStorage 동기화 방지)
      retry:                1,               // 실패 시 1회만 재시도 (기본 3회 감소)
      refetchOnWindowFocus: false,           // 탭 전환 시 자동 재요청 OFF
      refetchOnReconnect:   true,            // 네트워크 재연결 시 재요청 ON
    },
  },
});

// localStorage persister — 게시글 목록·카테고리 캐시를 새로고침 후에도 복원
const persister = createSyncStoragePersister({
  storage: window.localStorage,
});

export default App;

function App() {
  const isLoggedIn = useSelector(selectIsLoggedIn);
  const access_token = useSelector(selectAccessToken);
  const dispatch = useDispatch();
  // accessToken 변경 시 apiClient interceptor에 동기화
  useEffect(() => {
    setAuthToken(access_token);
  }, [access_token]);

  useEffect(() => {
    // 로그인 상태면 앱 마운트 시 access Token 1회 갱신.
    if (isLoggedIn) {
      reissuingAccessToken()
        .then((data) => {
          dispatch(updateUserAccessToken(data));
        })
        .catch((error) => {
          if (error.response?.status === 401) {
            alert("토큰이 만료되어 로그아웃 합니다.");
            dispatch(userLogout());
          }
        });
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [isLoggedIn]);

  return (
    <PersistQueryClientProvider
      client={queryClient}
      persistOptions={{
        persister,
        maxAge: PERSIST_MAX_AGE,
        dehydrateOptions: {
          // 게시글 목록·카테고리만 localStorage에 저장
          // 게시글 본문 HTML(대용량)·댓글·좋아요 등은 제외
          shouldDehydrateQuery: (query) => {
            const key = query.queryKey;
            if (key[0] === "boards" && key[1] === "list") return true;
            if (key[0] === "categories") return true;
            return false;
          },
        },
      }}
    >
      <ErrorBoundary>
        <Router>
          <Routes>
          {/* 일반 사용자 전용 페이지 */}
          <Route path="/" element={<UserLayout />}>
            <Route index element={<Home />} />
            <Route path="boards" element={<Home />} />
            <Route path=":categoryName" element={<PageByCategory />} />
            <Route path="/search" element={<SearchPage />} />
            <Route path="boards/:boardId" element={<BoardDetail />} />
            <Route
              path="login"
              element={isLoggedIn ? <NavigateBack /> : <LoginForm />}
            />
            <Route path="*" element={<NotFound />} />
          </Route>

          {/* 관리자 전용 페이지 */}
          <Route path="/management" element={<ProtectedRoute />}>
            <Route index element={<Management />} />
            <Route path="new-post" element={<BoardEditor />} />
            <Route path="boards" element={<Management />} />
            <Route path="boards/:boardId" element={<BoardEditForm />} />
            <Route path="categories" element={<CategoryList />} />
            <Route path="temporary-storage" element={<TemporaryStorage />} />
            <Route path="*" element={<NotFound />} />
          </Route>
          </Routes>
        </Router>
      </ErrorBoundary>
    </PersistQueryClientProvider>
  );
}
