import React from "react";
import PropTypes from "prop-types";
import Table from "react-bootstrap/Table";
import { Link } from "react-router-dom";
import moment from "moment";
import { PageButton } from "../PageButton";
const BoardList = ({ boards, pageCount }) => {
  return (
    <div>
      <Table striped>
        <thead>
          <tr>
            <th>작성일</th>
            <th>제목</th>
          </tr>
        </thead>
        <tbody>
          {boards.map((board) => (
            <tr key={board.id}>
              <td>{moment(board.upload_date).format("YYYY-MM-DD")}</td>
              <td>
                <Link to={`/${board.id}`} key={board.id}>
                  {board.title}
                </Link>
              </td>
            </tr>
          ))}
        </tbody>
      </Table>
      <PageButton pageCount={pageCount} />
    </div>
  );
};

BoardList.prototype = {
  boards: PropTypes.array,
  pageCount: PropTypes.number,
};

export default BoardList;
