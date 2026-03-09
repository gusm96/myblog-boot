import React from "react";
import PropTypes from "prop-types";
import moment from "moment";
import Parser, { domToReact } from "html-react-parser";
import "../Styles/css/boardList.css";
const BoardList = ({ boards, path }) => {
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
      {boards.length === 0 ? (
        <h3>게시글이 존재하지 않습니다</h3>
      ) : (
        <div>
          {boards.map((board) => (
            <div key={board.id} className="mb-3 board-item">
              <a href={`${path}/${board.id}`} style={cardStyle}>
                <span className="board-title">{board.title}</span>
                <p className="board-content">
                  {Parser(truncateText(board.content, 500), parserOptions)}
                </p>
                <span className="text-muted board-date">
                  {moment(board.createDate).format("YYYY-MM-DD")}
                </span>
              </a>
            </div>
          ))}
          <hr></hr>
        </div>
      )}
    </div>
  );
};

BoardList.prototype = {
  boards: PropTypes.array,
  path: PropTypes.string,
};

export default BoardList;
