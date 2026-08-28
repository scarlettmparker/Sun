import { Component } from "react";
import { Card, CardBody } from "@sun/components";
import styles from "./error-boundary.module.css";

type ErrorBoundaryProps = React.PropsWithChildren;

type ErrorBoundaryState = {
  /**
   * Error object if an error has been caught.
   */
  error: Error | null;
};

class ErrorBoundary extends Component<ErrorBoundaryProps, ErrorBoundaryState> {
  constructor(props: ErrorBoundaryProps) {
    super(props);
    this.state = { error: null };
  }

  static getDerivedStateFromError(error: Error): ErrorBoundaryState {
    return { error };
  }

  componentDidCatch(error: Error, info: { componentStack: string }) {
    console.error("[ErrorBoundary]", error.message, info.componentStack);
  }

  render() {
    if (this.state.error) {
      return (
        <div className={styles.error_boundary_card}>
          <Card>
            <CardBody>
              <p style={{ color: "var(--destructive)" }}>
                {this.state.error.message}
              </p>
            </CardBody>
          </Card>
        </div>
      );
    }
    return this.props.children;
  }
}

export default ErrorBoundary;
