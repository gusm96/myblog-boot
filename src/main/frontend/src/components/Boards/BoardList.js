import React from "react";
import PropTypes from "prop-types";
import Table from "react-bootstrap/Table";

const BoardList = ({ boards }) => {
  return (
    <Table striped>
      <thead>
        <tr>
          <th>작성일</th>
          <th>제목</th>
        </tr>
      </thead>
      <tbody>
        {boards.map((board) => (
          <tr>
            <td>{board.upload_date}</td>
            <td>
              <a href="/board/{board.id}">{board.title}</a>
            </td>
          </tr>
        ))}
      </tbody>
    </Table>
  );
};

BoardList.propTypes = {
  boards: PropTypes.array,
};
export default BoardList;
