import { Navbar, Nav, Container, Button, NavDropdown, Image } from "react-bootstrap";
import logo from "../assets/logo.png";
import { Link, useNavigate } from "react-router-dom";
import { useTranslation } from "react-i18next";
import '../styles/navbar.css';
import { useContext } from "react";
import { KeycloakContext } from "../KeycloakContext";

function CustomNavbar() {
  const { i18n, t } = useTranslation();
  const { keycloak, authenticated, ready } = useContext(KeycloakContext);
  const navigate = useNavigate();

  const changeLanguage = (lng) => {
    i18n.changeLanguage(lng);
  };

  const username =
    keycloak?.tokenParsed?.preferred_username || "User";

  const profilePicture =
    keycloak?.tokenParsed?.profilePicture ||
    "https://cdn.pixabay.com/photo/2015/10/05/22/37/blank-profile-picture-973460_960_720.png"; // fallback

  const getLangName = (lng) => {
    switch (lng) {
      case "en": return "English";
      case "de": return "Deutsch";
      default: return "Language";
    }
  };

  const handleLogin = () => {
    navigate('/signin');
  };

  const handleLogout = () => {
    if (keycloak) {
      keycloak.logout();
    }
  };

  return (
    <Navbar bg="primary" variant="dark">
      <Container fluid>
        <Navbar.Brand as={Link} to={authenticated ? "/dashboard" : "/"} className="d-flex align-items-center">
          <img
            alt=""
            src={logo}
            height="50"
            className="d-inline-block align-top"
          />
          <span className="ms-2">DigiTwin Studio</span>
        </Navbar.Brand>
        <nav>
          <Nav.Link as={Link} to="/marketplace" className="text-light">{t("nav.marketplace")}</Nav.Link>
        </nav>
        <Nav className="ms-auto align-items-center">
          <NavDropdown
            title={
    <span className="align-middle">
      {getLangName(i18n.language)}
    </span>
  }
            id="language-dropdown"
            align="end"
            className="language-dropdow white-dropdown"
          >
            <NavDropdown.Item onClick={() => changeLanguage("en")}>
              English
            </NavDropdown.Item>
            <NavDropdown.Item onClick={() => changeLanguage("de")}>
              Deutsch
            </NavDropdown.Item>
          </NavDropdown>

          {authenticated ? (
            <NavDropdown
              align="end"
              title={
                <span className="allign-middle">
                  <Image
                    src={profilePicture}
                    roundedCircle
                    width="45"
                    height="45"
                    className="me-2"
                  />
                  <span>{username}</span>
                </span>
              }
              id="user-dropdown"
              className="ms-3 white-dropdown"
            >
              <NavDropdown.Item onClick={handleLogout}>
                {t("nav.logout")}
              </NavDropdown.Item>
            </NavDropdown>
          ) : (
            <Button
              onClick={handleLogin}
              variant="outline-light"
              className="ms-3"
              disabled={!ready || !keycloak}
            >
              {t("nav.signIn")}
            </Button>
          )}
        </Nav>
      </Container>
    </Navbar>
  );
}

export default CustomNavbar;
