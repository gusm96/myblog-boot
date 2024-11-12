import React, { useEffect, useState } from "react";
import axios from "axios";
import { Container, Row, Col, Card, Spinner } from "react-bootstrap";

const VisitorCount = () => {
  const [visitor, setVisitor] = useState({
    total: "",
    today: "",
    yesterday: "",
  });

  const [loading, setLoading] = useState(true);

  useEffect(() => {
    const fetchVisitorData = async () => {
      try {
        const response = await axios.get(
          "http://localhost:8080/api/v2/visitor-count"
        );
        const { total, today, yesterday } = response.data;
        setVisitor({ total, today, yesterday });
      } catch (error) {
        console.error("Error fetching visitor data:", error);
      } finally {
        setLoading(false);
      }
    };

    fetchVisitorData();
  }, []);

  if (loading) {
    return (
      <div
        className="d-flex justify-content-center align-items-center"
        style={{ height: "100vh" }}
      >
        <Spinner animation="border" role="status">
          <span className="visually-hidden">Loading...</span>
        </Spinner>
      </div>
    );
  }

  return (
    <Container className="mt-5 text-center">
      <Card className="count shadow-sm">
        <Card.Body>
          <p>전체 방문자</p>
          <p className="total">{visitor.total}</p>
          <hr />
          <p>Today: {visitor.today}</p>
          <p>Yesterday: {visitor.yesterday}</p>
        </Card.Body>
      </Card>
    </Container>
  );
};

export default VisitorCount;
