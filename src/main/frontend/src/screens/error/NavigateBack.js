import { useEffect } from "react";
import { useNavigate } from "react-router-dom";

export const NavigateBack = () => {
  const navigate = useNavigate();
  useEffect(() => {
    alert("이미 로그인 했습니다.");
    navigate(-1);
  }, [navigate]);
};
