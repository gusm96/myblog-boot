import React from "react";
import PropTypes from "prop-types";
export const PageButton = ({ categoryName, pageCount }) => {
  const handleOnClick = (page) => {
    window.location.href = categoryName
      ? `/${categoryName}?p=${page}`
      : `/boards?p=${page}`;
  };
  const pageButton = () => {
    const buttons = [];
    for (let i = 1; i <= pageCount; i++) {
      buttons.push(
        <button key={i} onClick={() => handleOnClick(i)}>
          {i}
        </button>
      );
    }
    return buttons;
  };
  return <div>{pageButton()}</div>;
};

PageButton.prototype = {
  pageCount: PropTypes.number,
};
