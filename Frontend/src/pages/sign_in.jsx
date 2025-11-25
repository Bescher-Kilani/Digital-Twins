import { useTranslation } from "react-i18next";
import { Button, Image, Container } from "react-bootstrap";
import Logo from "../assets/Logo_with_different_text.png";
import "../styles/signin.css";
import { useContext } from "react";
import { KeycloakContext } from "../KeycloakContext";
import { useNavigate } from "react-router-dom";

function Signin() {
  const { t } = useTranslation();
  const { keycloak, ready } = useContext(KeycloakContext);
  const navigate = useNavigate();
  const KEYCLOAK_URL =  import.meta.env.VITE_KEYCLOAK_URL || 'http://localhost:8080';

  const handleLogin = () => {
    if (keycloak) {
      keycloak.login();
    }
  };

  const handleRegister = () => {
    if (keycloak) {
      // Use a simpler approach - direct URL to registration
      window.location.href =
          `${KEYCLOAK_URL}/realms/DigiTwinStudio/protocol/openid-connect/registrations?client_id=digitwin-auth&response_type=code&scope=openid%20profile%20email%20custom-profile&redirect_uri=` +
        encodeURIComponent(window.location.origin);
    }
  };

  const handleContinueAsGuest = () => {
    navigate("/create");
  };

  return (
    <div className="signin-page-container">
      <div className="text-center">
        <img src={Logo} alt="Logo" style={{ width: 100, marginBottom: 16 }} />

          <div className="signin-container text-white pt-5 pl-5 pr-5 pb-1">
            <p>{t("signin.title")}</p>
            <div className="mt-4">
              <Button
                variant="primary"
                className="me-2"
                onClick={handleLogin}
                disabled={!ready || !keycloak}
              >
                {t("signin.signIn")}
              </Button>
              <Button
                variant="secondary"
                onClick={handleRegister}
                disabled={!ready || !keycloak}
              >
                {t("signin.register")}
              </Button>
            </div>
            <div className="d-flex align-items-center my-3">
              <div
                className="flex-grow-1 mx-3"
                style={{ borderTop: "1px solid #fff" }}
              ></div>
              <span className="mx-2 text-white">{t("signin.or")}</span>
              <div
                className="flex-grow-1 mx-3"
                style={{ borderTop: "1px solid #fff" }}
              ></div>
            </div>
            <Button
              variant="primary"
              className="me-2"
              onClick={handleContinueAsGuest}
            >
              {t("signin.continueAsGuest")}
            </Button>
            <p className="pt-4">
              {t("signin.policy")}
            </p>
          </div>
        </div>
    </div>
  );
}

export default Signin;
