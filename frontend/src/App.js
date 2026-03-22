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
import { JoinForm } from "./screens/Member/JoinForm";
import { NotFound } from "./screens/error/NotFound";
import { NavigateBack } from "./screens/error/NavigateBack";
import { CategoryList } from "./components/Category/CategoryList";
import { TemporaryStorage } from "./screens/TemporaryStorage";
import { SearchPage } from "./screens/SearchPage";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { BoardDetailV2 } from "./components/Boards/BoardDetailV2";
import { PageByCategory } from "./screens/PageByCategory";
import BoardEditor from "./components/Boards/BoardEditor";
import ErrorBoundary from "./components/ErrorBoundary";

// context7 TanStack Query 공식 문서 기반:
// CSR SPA에서 QueryClient는 모듈 최상위에서 1회 생성 (렌더마다 재생성 방지)
const queryClient = new QueryClient({
  defaultOptions: {
    queries: {
      staleTime:            1000 * 60 * 3,   // 3분: 기본 fresh 유지 시간
      gcTime:               1000 * 60 * 10,  // 10분: 미사용 캐시 보관 시간
      retry:                1,               // 실패 시 1회만 재시도 (기본 3회 감소)
      refetchOnWindowFocus: false,           // 탭 전환 시 자동 재요청 OFF
      refetchOnReconnect:   true,            // 네트워크 재연결 시 재요청 ON
    },
  },
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
    // 로그인 상태면 access Token 주기적으로 검증.
    if (isLoggedIn && access_token !== null) {
      reissuingAccessToken()
        .then((data) => {
          dispatch(updateUserAccessToken(data));
        })
        .catch((error) => {
          if (error.response.status === 401 || error.response.status === 500) {
            alert("토큰이 만료되어 로그아웃 합니다.");
            dispatch(userLogout());
          }
        });
    }
  }, [isLoggedIn, access_token, dispatch]);

  return (
    <QueryClientProvider client={queryClient}>
      <ErrorBoundary>
        <Router>
          <Routes>
          {/* 일반 사용자 전용 페이지 */}
          <Route path="/" element={<UserLayout />}>
            <Route index element={<Home />} />
            <Route path="boards" element={<Home />} />
            <Route path=":categoryName" element={<PageByCategory />} />
            <Route path="/search" element={<SearchPage />} />
            <Route path="boards/:boardId" element={<BoardDetailV2 />} />
            <Route
              path="login"
              element={isLoggedIn ? <NavigateBack /> : <LoginForm />}
            />
            <Route
              path="join"
              element={isLoggedIn ? <NavigateBack /> : <JoinForm />}
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
    </QueryClientProvider>
  );
}
