import React from "react";

export const ErrorMessage = ({ message }) => {
  return (
    <div className="alert alert-danger" role="alert">
      {message || "오류가 발생했습니다."}
    </div>
  );
};
