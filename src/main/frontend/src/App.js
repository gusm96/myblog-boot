import React, { useEffect, useState } from "react";
import { BrowserRouter as Router, Routes, Route } from "react-router-dom";
import axios from "axios";
import Home from "./screens/Home";
import NavBarElements from "./components/Navbar/NavBarElements";

export default App;

function App() {
  // Gets the list of Posts from the Server
  /*const [posts, setPosts] = useState([]);

  useEffect(() => {
    axios
      .get("/api/v1/posts")
      .then((response) => response.data)
      .then((data) => setPosts(data))
      .catch((error) => console.log(error));
  }, []);
  return (
    <div>
      <h1>Welcome Moya's Tech Blog</h1>
      <hr />
      <PostList posts={posts} />
    </div>
  );*/
  return (
    <Router>
      <NavBarElements />
      <Routes>
        <Route path="/" element={<Home />} />
      </Routes>
    </Router>
  );
}
