import axios from "axios";
import { useEffect, useState } from "react";
import { useCookies } from "react-cookie";
export const LoginConfirmation = () => {
  const [isLoggedIn, setIsLoggedIn] = useState(false);
  const [cookies] = useCookies(["token"]);
  useEffect(() => {
    // 로그인 상태 및 토큰 유효 확인
    if (cookies.token === null) {
      console.log("Token is null");
      return false;
    } else {
      try {
        axios
          .get("http://localhost:8080/api/v1/token-validation", {
            headers: {
              Authorization: `bearer ${cookies.token}`,
            },
          })
          .then((res) => res.data)
          .then((data) => setIsLoggedIn(data))
          .catch((error) => console.log(error));
      } catch (error) {
        alert("로그인 인증 만료");
        return false;
      }
    }
  }, [cookies]);
  return isLoggedIn;
};
