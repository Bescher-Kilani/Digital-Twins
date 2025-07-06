import { Navbar, Nav, Container, Button, NavDropdown} from "react-bootstrap";
import logo from "../assets/logo.png";
import { Link } from "react-router-dom";
import { useTranslation } from "react-i18next";
import '../styles/navbar.css';

function CustomNavbar() {

  const { i18n, t } = useTranslation();

  const changeLanguage = (lng) => {
    i18n.changeLanguage(lng);
  };

  const getLangName = (lng) => {
  switch (lng) {
    case "en": return "English";
    case "de": return "Deutsch";
    default: return "Language";
  }
};

  return (
    <Navbar bg="primary" variant="dark">
        <Container fluid>
          <Navbar.Brand href="#home" className="d-flex align-items-center">
            <img
              alt=""
              src={logo}
              width="50"
              height="50"
              className="d-inline-block align-top"
            />
            <span className="ms-2">DigiTwin Studio</span>
          </Navbar.Brand>
          <nav>
            <Nav.Link as={Link} to="/marketplace" className="text-light">{t("marketplace")}</Nav.Link>
          </nav>
          <Nav className="ms-auto">

            <NavDropdown
              title={getLangName(i18n.language)}
              id="language-dropdown"
              align="end"
              className="language-dropdown"
            >
              <NavDropdown.Item onClick={() => changeLanguage("en")}>
                English
              </NavDropdown.Item>
              <NavDropdown.Item onClick={() => changeLanguage("de")}>
                Deutsch
              </NavDropdown.Item>
            </NavDropdown>

            <Button
              as={Link}
              to="/signin"
              variant="outline-light"
              className="ms-3"
            >
              Sign In
            </Button>
          </Nav>
        </Container>
      </Navbar>
  );
}

export default CustomNavbar;
