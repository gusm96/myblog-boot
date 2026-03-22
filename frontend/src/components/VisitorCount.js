import React, { useEffect, useState } from "react";
import axios from "axios";
import { Spinner } from "react-bootstrap";
import { BASE_URL } from "../apiConfig";
import "./Styles/css/visitorCount.css";

const VisitorCount = () => {
  const [visitor, setVisitor] = useState({
    total: null,
    today: null,
    yesterday: null,
  });
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    const fetchVisitorData = async () => {
      try {
        const response = await axios.get(
          `${BASE_URL}/api/v2/visitor-count`,
          { withCredentials: true }
        );
        const { total, today, yesterday } = response.data;
        setVisitor({ total, today, yesterday });
      } catch (error) {
        // silent fail
      } finally {
        setLoading(false);
      }
    };
    fetchVisitorData();
  }, []);

  if (loading) {
    return (
      <div className="visitor-card visitor-card--loading">
        <Spinner animation="border" role="status" />
      </div>
    );
  }

  return (
    <div className="visitor-card">
      <div className="visitor-card__label">// visitors</div>
      <div className="visitor-stat visitor-stat--total">
        <span className="visitor-stat__key">total</span>
        <span className="visitor-stat__value">{visitor.total ?? "—"}</span>
      </div>
      <div className="visitor-card__divider" />
      <div className="visitor-stat">
        <span className="visitor-stat__key">today</span>
        <span className="visitor-stat__value visitor-stat__value--accent">
          {visitor.today ?? "—"}
        </span>
      </div>
      <div className="visitor-stat">
        <span className="visitor-stat__key">yesterday</span>
        <span className="visitor-stat__value">{visitor.yesterday ?? "—"}</span>
      </div>
    </div>
  );
};

export default VisitorCount;
