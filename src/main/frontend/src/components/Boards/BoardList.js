import React from "react";
import PropTypes from "prop-types";
import moment from "moment";
import { PageButton } from "../PageButton";
import Parser, { domToReact } from "html-react-parser";
const BoardList = ({ boards, pageCount }) => {
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
  return (
    <div>
      <ul>
        {boards.map((board) => (
          <li key={board.id}>
            <a className="board_box" href={`/boards/${board.id}`}>
              <span className="title">{board.title}</span>
              <span>
                {Parser(truncateText(board.content, 100), parserOptions)}
              </span>
              <span className="date">
                {moment(board.upload_date).format("YYYY-MM-DD")}
              </span>
            </a>
          </li>
        ))}
      </ul>
      <PageButton pageCount={pageCount} />
    </div>
  );
};

BoardList.prototype = {
  boards: PropTypes.array,
  pageCount: PropTypes.number,
};

export default BoardList;
