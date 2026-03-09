import { useQuery } from "@tanstack/react-query";
import React from "react";
import { getCategoriesV2 } from "../../services/categoryApi";
import { ListGroup, ListGroupItem } from "react-bootstrap";
import { Link } from "react-router-dom";

export const CategoryNavV2 = () => {
  const { isPending, isError, data, error } = useQuery({
    queryKey: ["categories"],
    queryFn: getCategoriesV2,
    staleTime: 600000,
  });

  if (isPending) {
    return <div>Loading...</div>;
  }
  if (isError) {
    return <div>Error : {error.message}</div>;
  }
  return (
    <ListGroup id="category-list-group">
      <ListGroupItem className="category-list-item">
        <Link className="category-link" to={"/"}>
          전체보기
        </Link>
      </ListGroupItem>
      {data.map((c) => (
        <ListGroupItem key={c.id} className="category-list-item">
          <Link className="category-link" to={`/${c.name}`}>
            {c.name} ({c.boardsCount})
          </Link>
        </ListGroupItem>
      ))}
    </ListGroup>
  );
};
