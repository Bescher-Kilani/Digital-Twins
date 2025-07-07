import { useTranslation } from "react-i18next";
import "../styles/index.css";
import { Button, Image, Container, Row, Col } from "react-bootstrap";
import homepage_image from "../assets/homepage_model.png";
import CheckIcon from "../assets/icons/check.svg?react";

function Home() {
  const { t } = useTranslation();

  return (
    <div>
      <div className="create-container text-white">
        <Container fluid>
          <Row>
            <Col md={6}>
              <div className="text-container">
                <h1>{t("create")}</h1>
                <p>{t("create text")}</p>
                <Button variant="primary">{t("start now")}</Button>
              </div>
            </Col>
            <Col md={6} className="g-0">
              <div className="image-container">
                <Image src={homepage_image} />
              </div>
            </Col>
          </Row>
        </Container>
      </div>
      <div className="text-center pt-2">
        <h1>{t("why 1")}</h1>
        <div className="features-section pt-2">
          <Container fluid>
            <Row className="text-center justify-content-around">
              <Col md={3} lg={2} className="p-3 custom-box">
                <h3>{t("why text 1")}</h3>
                <p>{t("why desc 1")}</p>
              </Col>
              <Col md={3} lg={2} className="p-3 custom-box">
                <h3>{t("why text 2")}</h3>
                <p>{t("why desc 2")}</p>
              </Col>
              <Col md={3} lg={2} className="p-3 custom-box">
                <h3>{t("why text 3")}</h3>
                <p>{t("why desc 3")}</p>
              </Col>
            </Row>
          </Container>
        </div>
      </div>
      <div className="pt-2">
        <h1 className="text-center">{t("why 2")}</h1>

        <div
          className="d-flex flex-column align-items-start mx-auto"
          style={{ width: "fit-content" }}
        >
          <div className="d-flex align-items-center gap-2">
            <CheckIcon style={{ color: "green", width: 36, height: 36 }} />
            <span>{t("feature 1")}</span>
          </div>
          <div className="d-flex align-items-center gap-2">
            <CheckIcon style={{ color: "green", width: 36, height: 36 }} />
            <span>{t("feature 2")}</span>
          </div>
          <div className="d-flex align-items-center gap-2">
            <CheckIcon style={{ color: "green", width: 36, height: 36 }} />
            <span>{t("feature 3")}</span>
          </div>
        </div>
        <div className="text-center pt-2">
          <p>{t("more")}</p>
          <Button variant="primary">{t("start now")}</Button>
        </div>
      </div>
    </div>
  );
}

export default Home;
