import { Container, Button } from "react-bootstrap";
import { useNavigate } from "react-router-dom";
import { useTranslation } from "react-i18next";
import "../styles/not-authorized.css";

function NotAuthorized() {
  const { t } = useTranslation();
  const navigate = useNavigate();

  const handleSignIn = () => {
    navigate('/signin');
  };

  const handleGoHome = () => {
    navigate('/');
  };

  return (
    <div className="not-authorized-container">
      <Container className="text-center py-5">
        <div className="mb-4">
          <h1 className="display-4 text-danger">🔒</h1>
          <h2 className="mb-3">Access Denied</h2>
          <p className="lead">
            You need to be signed in to access this page.
          </p>
          <p className="text-muted">
            Please sign in to continue or return to the home page.
          </p>
        </div>
        
        <div className="d-flex justify-content-center gap-3">
          <Button 
            variant="primary" 
            onClick={handleSignIn}
            className="px-4"
          >
            {t("sign in")}
          </Button>
          <Button 
            variant="outline-secondary" 
            onClick={handleGoHome}
            className="px-4"
          >
            Go to Home
          </Button>
        </div>
      </Container>
    </div>
  );
}

export default NotAuthorized;
