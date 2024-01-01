import React from "react";
import PropTypes from "prop-types";
import "../Styles/css/pageButton.css";
import { Button } from "react-bootstrap";
export const PageButton = ({ pageCount, path }) => {
  const handleOnClick = (page) => {
    window.location.href = `${path}p=${page}`;
  };
  const pageButton = () => {
    const buttons = [];
    for (let i = 1; i <= pageCount; i++) {
      buttons.push(
        <Button key={i} onClick={() => handleOnClick(i)}>
          {i}
        </Button>
      );
    }
    return buttons;
  };
  return <div className="page-buttons-container">{pageButton()}</div>;
};

PageButton.prototype = {
  pageCount: PropTypes.number,
};
