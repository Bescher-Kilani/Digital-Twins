import { useContext } from "react";
import { KeycloakContext } from "../KeycloakContext";
import NotAuthorized from "../pages/NotAuthorized";

function ProtectedRoute({ children }) {
  const { authenticated, ready } = useContext(KeycloakContext);

  // Show loading state while authentication is being checked
  if (!ready) {
    return (
      <div className="d-flex justify-content-center align-items-center" style={{ minHeight: '200px' }}>
        <div className="spinner-border text-primary" role="status">
          <span className="visually-hidden">Loading...</span>
        </div>
      </div>
    );
  }

  // If not authenticated, show not authorized page
  if (!authenticated) {
    return <NotAuthorized />;
  }

  // If authenticated, render the protected content
  return children;
}

export default ProtectedRoute;
