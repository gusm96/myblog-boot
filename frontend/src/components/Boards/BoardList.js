import React from "react";
import PropTypes from "prop-types";
import dayjs from "dayjs";
import Parser, { domToReact } from "html-react-parser";
import { Link } from "react-router";
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
    const plainText = text.replace(/<[^>]*>/g, "");
    if (plainText.length <= maxLength) {
      return plainText;
    }
    return plainText.slice(0, maxLength) + "...";
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
              <Link to={`${path}/${board.id}`} style={cardStyle}>
                <span className="board-title">{board.title}</span>
                <p className="board-content">
                  {Parser(truncateText(board.content, 500), parserOptions)}
                </p>
                <span className="text-muted board-date">
                  {dayjs(board.createDate).format("YYYY-MM-DD")}
                </span>
              </Link>
            </div>
          ))}
          <hr></hr>
        </div>
      )}
    </div>
  );
};

BoardList.propTypes = {
  boards: PropTypes.array,
  path: PropTypes.string,
};

export default BoardList;
