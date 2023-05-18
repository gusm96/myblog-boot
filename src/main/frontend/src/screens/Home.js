import React, { useState, useEffect } from "react";
import { Container } from "../components/Styles/Container/Container.style";
import { Header, MainHeader } from "../components/Styles/Header/Header.style";
import axios from "axios";
import PostsList from "../components/Posts/PostsList";
const Home = () => {
  const [posts, setPosts] = useState([]);

  useEffect(() => {
    axios
      .get("/api/v1/posts")
      .then((responce) => responce.data)
      .then((data) => setPosts(data))
      .catch((error) => console.log(error));
  }, []);

  return (
    <Container>
      <Header>
        <MainHeader>
          <PostsList posts={posts} />
        </MainHeader>
      </Header>
    </Container>
  );
};

export default Home;
