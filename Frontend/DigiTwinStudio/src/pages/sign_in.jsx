import { useTranslation } from "react-i18next";
import { Button, Image, Container } from "react-bootstrap";
import Logo from "../assets/logo_with_different_text.png";
import "../styles/signin.css";
import { useContext } from "react";
import { KeycloakContext } from "../KeycloakContext";
import { useNavigate } from "react-router-dom";

function Signin() {
  const { t } = useTranslation();
  const { keycloak, ready } = useContext(KeycloakContext);
  const navigate = useNavigate();

  const handleLogin = () => {
    if (keycloak) {
      keycloak.login();
    }
  };

  const handleRegister = () => {
    if (keycloak) {
      keycloak.register();
    }
  };

  const handleContinueAsGuest = () => {
    navigate('/');
  };

  return (
    <div>
      <div
        className="d-flex justify-content-center align-items-center"
        style={{ height: "100vh" }}
      >
        <div className="text-center">
          <img src={Logo} alt="Logo" style={{ width: 100, marginBottom: 16 }} />

          <div className="signin-container text-white p-5">
            <p>
              We recommend signing into the website to experience all of the
              features we offer such as saving your files, but you can continue
              as a guest if you wish
            </p>
            <Button 
              variant="primary" 
              className="me-2"
              onClick={handleLogin}
              disabled={!ready || !keycloak}
            >
              Sign in
            </Button>
            <Button 
              variant="secondary"
              onClick={handleRegister}
              disabled={!ready || !keycloak}
            >
              Register
            </Button>
            <div className="d-flex align-items-center my-3">
              <div
                className="flex-grow-1"
                style={{ borderTop: "1px solid #fff" }}
              ></div>
              <span className="mx-2 text-white">or</span>
              <div
                className="flex-grow-1"
                style={{ borderTop: "1px solid #fff" }}
              ></div>
            </div>
            <Button 
              variant="primary" 
              className="me-2"
              onClick={handleContinueAsGuest}
            >
              Continue as Guest
            </Button>
          </div>
        </div>
      </div>
    </div>
  );
}

export default Signin;
