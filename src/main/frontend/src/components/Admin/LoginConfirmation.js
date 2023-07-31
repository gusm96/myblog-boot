import axios from "axios";
import { useState, useEffect } from "react";
import { useCookies } from "react-cookie";
import { ADMIN_VALIDATION } from "../../apiConfig";

export const LoginConfirmation = () => {
  const [isLoggedIn, setIsLoggedIn] = useState(false);
  const [cookies, setCookie, removeCookie] = useCookies(["token"]);

  useEffect(() => {
    if (!cookies.access_token) {
      setIsLoggedIn(false);
    } else {
      try {
        axios
          .get(ADMIN_VALIDATION, {
            headers: {
              Authorization: `bearer ${cookies.access_token}`,
            },
          })
          .then((res) => res.data)
          .then((data) => setIsLoggedIn(data))
          .catch((error) => {
            console.log(error);
            if (error.response.status === 401) {
              alert("인증이 만료되었습니다.");
            }
            setIsLoggedIn(false);
          });
      } catch (error) {
        alert("로그인 인증 만료");
        setIsLoggedIn(false);
      }
    }
  }, []);

  return isLoggedIn;
};
