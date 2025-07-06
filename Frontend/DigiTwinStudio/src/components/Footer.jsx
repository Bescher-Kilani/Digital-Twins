import { Container } from "react-bootstrap";

function Footer() {
  return (
    <footer className="bg-primary text-white mt-5 py-3">
      <Container className="text-center">
        Made with ❤️ by the PSE Team <br />
        © {new Date().getFullYear()} Fraunhofer IOSB. All rights reserved.
      </Container>
    </footer>
  );
}

export default Footer;
