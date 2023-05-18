import React from "react";
import PropTypes from "prop-types";
import Table from "react-bootstrap/Table";

const PostsList = ({ posts }) => {
  return (
    <Table striped>
      <thead>
        <tr>
          <th>작성일</th>
          <th>제목</th>
        </tr>
      </thead>
      <tbody>
        {posts.map((post) => (
          <tr>
            <td>{post.upload_date}</td>
            <td>
              <a href="/post/{post.bidx}">{post.title}</a>
            </td>
          </tr>
        ))}
      </tbody>
    </Table>
  );
};

PostsList.propTypes = {
  posts: PropTypes.array,
};
export default PostsList;
