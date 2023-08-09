import { useSelector } from "react-redux";
import Redirect from "./Redirect";

export const ProtectedRoute = ({ isLoggedIn, children, adminOnly, path }) => {
  const userType = useSelector((state) => state.user.userType);

  if (isLoggedIn && userType === "ADMIN" && path === "/login/admin") {
    return <Redirect to="/management" />;
  }

  if (!isLoggedIn || (adminOnly && userType !== "ADMIN")) {
    return <Redirect to="/login/admin" />;
  }

  return children;
};
