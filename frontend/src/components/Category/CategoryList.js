import React, { useEffect, useState } from "react";
import "../Styles/css/categories.css";
import {
  addNewCategory,
  deleteCategory,
  getCategoriesForAdmin,
} from "../../services/categoryApi";
import {
  Button,
  Container,
  Form,
  InputGroup,
  ListGroup,
  ListGroupItem,
} from "react-bootstrap";
import { useSelector } from "react-redux";
import { selectAccessToken } from "../../redux/userSlice";

export const CategoryList = () => {
  const accessToken = useSelector(selectAccessToken);
  const [category, setCategory] = useState("");
  const [categories, setCategories] = useState([]);

  useEffect(() => {
    getCategoriesForAdmin(accessToken).then((data) => setCategories(data));
  }, [accessToken]);
  const handleOnChange = (e) => {
    e.preventDefault();
    setCategory(e.target.value);
  };
  const handleNewCategory = (e) => {
    addNewCategory(category, accessToken)
      .then((data) => {
        if (data !== null || data !== "") {
          alert("카테고리가 등록되었습니다.");
          setCategory("");
        }
      })
      .catch((error) => {
        alert(error.message);
        console.log(error);
      });
  };

  const handleDeleteCategory = (e) => {
    if (window.confirm("정말로 삭제하시겠습니까?")) {
      deleteCategory(e.target.value, accessToken)
        .then((data) => {
          alert(data);
          window.location.reload();
        })
        .catch((error) => {
          alert(error.message);
          console.log(error);
        });
    }
    return;
  };
  return (
    <Container className="categories-container">
      <Form className="category-form">
        <InputGroup>
          <Form.Control
            aria-label="Default"
            aria-describedby="inputGroup-sizing-default"
            placeholder="새로운 카테고리명을 입력하세요."
            name="categoryName"
            value={category}
            onChange={handleOnChange}
          />
        </InputGroup>
        <Button type="submit" onClick={handleNewCategory}>
          등록하기
        </Button>
      </Form>
      <hr></hr>
      <ListGroup className="list-group list-group-container">
        {categories.map((category) => (
          <ListGroupItem key={category.id} className="list-items">
            <div className="category-name">
              <p>
                {category.name} ({category.boardsCount})
              </p>
            </div>
            <div className="btn-container">
              <Button className="edit-btn">수정</Button>
              <Button
                className="delete-btn"
                disabled={category.boardsCount > 0 ? true : false}
                value={category.id}
                onClick={handleDeleteCategory}
              >
                삭제
              </Button>
            </div>
          </ListGroupItem>
        ))}
      </ListGroup>
    </Container>
  );
};
