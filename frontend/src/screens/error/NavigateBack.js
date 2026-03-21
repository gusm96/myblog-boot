import { useEffect } from "react";
import { useNavigate } from "react-router";

export const NavigateBack = () => {
  const navigate = useNavigate();
  useEffect(() => {
    navigate(-1);
  }, [navigate]);
};
