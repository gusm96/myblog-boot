import React from "react";
import PropTypes from "prop-types";
import moment from "moment";
import { PageButton } from "../PageButton";
import Parser, { domToReact } from "html-react-parser";
import "../Styles/css/boardList.css";
const BoardList = ({ boards, pageCount, categoryName }) => {
  const parserOptions = {
    replace: ({ attribs, children }) => {
      if (attribs) {
        return <span>{domToReact(children, parserOptions)}</span>;
      }
    },
  };
  const truncateText = (text, maxLength) => {
    text.replace(/<[^>]*>/g, "");
    if (text.length <= maxLength) {
      return text;
    } else {
      return text.slice(0, maxLength) + "...";
    }
  };
  const cardStyle = {
    textDecoration: "none",
  };

  return (
    <div className="board-list">
      {boards.map((board) => (
        <div key={board.id} className="mb-3 board-item">
          <a href={`/boards/${board.id}`} style={cardStyle}>
            <span className="board-title">{board.title}</span>
            <p className="board-content">
              {Parser(truncateText(board.content, 500), parserOptions)}
            </p>
            <span className="text-muted board-date">
              {moment(board.upload_date).format("YYYY-MM-DD")}
            </span>
          </a>
        </div>
      ))}
      <PageButton
        pageCount={pageCount}
        categoryName={categoryName ? categoryName : null}
      />
    </div>
  );
};

BoardList.prototype = {
  boards: PropTypes.array,
  pageCount: PropTypes.number,
  categoryName: PropTypes.string,
};

export default BoardList;
