import React from "react";
import PropTypes from "prop-types";
import "../Styles/css/pageButton.css";
import { Button } from "react-bootstrap";
import { useNavigate } from "react-router";
export const PageButton = ({ pageCount, path }) => {
  const navigate = useNavigate();
  const handleOnClick = (page) => {
    navigate(`/${path}p=${page}`);
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

PageButton.propTypes = {
  pageCount: PropTypes.number,
};
