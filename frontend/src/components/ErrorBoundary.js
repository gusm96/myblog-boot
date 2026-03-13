import React, { Component } from "react";

class ErrorBoundary extends Component {
  constructor(props) {
    super(props);
    this.state = { hasError: false, error: null };
  }

  static getDerivedStateFromError(error) {
    return { hasError: true, error };
  }

  componentDidCatch(error, errorInfo) {
    console.error("ErrorBoundary caught an error:", error, errorInfo);
  }

  handleGoHome = () => {
    this.setState({ hasError: false, error: null });
    window.location.href = "/";
  };

  render() {
    if (this.state.hasError) {
      return (
        <div className="container text-center mt-5">
          <div className="alert alert-danger" role="alert">
            <h4 className="alert-heading">오류가 발생했습니다</h4>
            <p>페이지를 불러오는 중 문제가 발생했습니다.</p>
            <hr />
            <button
              className="btn btn-primary"
              onClick={this.handleGoHome}
            >
              홈으로 이동
            </button>
          </div>
        </div>
      );
    }

    return this.props.children;
  }
}

export default ErrorBoundary;
