import { useEffect } from "react";
import { useNavigate } from "react-router-dom";

export const NavigateBack = () => {
  const navigate = useNavigate();
  useEffect(() => {
    navigate(-1);
  }, [navigate]);
};
