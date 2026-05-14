"use client";

import { useEffect, useState } from "react";
import { Spinner } from "react-bootstrap";
import apiClient from "@/lib/apiClient";

interface VisitorData {
  total: number | null;
  today: number | null;
  yesterday: number | null;
}

export function VisitorCount() {
  const [visitor, setVisitor] = useState<VisitorData>({
    total: null,
    today: null,
    yesterday: null,
  });
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    const controller = new AbortController();

    const fetchVisitorData = async () => {
      try {
        const response = await apiClient.get("/api/v2/visitor-count", {
          signal: controller.signal,
        });
        const { total, today, yesterday } = response.data;
        setVisitor({ total, today, yesterday });
        setLoading(false);
      } catch (error: unknown) {
        // StrictMode 이중 호출로 인한 요청 취소(ERR_CANCELED)는 무시
        if ((error as { code?: string })?.code === "ERR_CANCELED") return;
        setLoading(false);
      }
    };

    fetchVisitorData();

    return () => controller.abort();
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
}
