import { Container } from "react-bootstrap";
import { useTranslation } from "react-i18next";

function Footer() {
  const { t } = useTranslation();

  return (
    <footer className="bg-primary text-white py-3">
      <Container className="text-center">
        {t("footer.madeWith")} <br />
        {t("footer.copyright")}
      </Container>
    </footer>
  );
}

export default Footer;
