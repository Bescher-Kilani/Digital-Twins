import { Navbar, Nav, Container, Button} from "react-bootstrap";
import logo from "../assets/logo.png";
import { Link } from "react-router-dom";

function CustomNavbar() {
  return (
    <Navbar bg="primary" data-bs-theme="dark">
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
            <Nav.Link as={Link} to="/marketplace" className="text-light">Marketplace</Nav.Link>
          </nav>
          <Nav className="ms-auto">
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
