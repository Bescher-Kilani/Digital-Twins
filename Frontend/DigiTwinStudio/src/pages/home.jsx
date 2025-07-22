import { useTranslation } from "react-i18next";
import "../styles/index.css";
import { Button, Image, Container, Row, Col } from "react-bootstrap";
import homepage_image from "../assets/homepage_model.png";
import CheckIcon from "../assets/icons/check.svg?react";
import { Link, useNavigate } from "react-router-dom";

function Home() {
  const { t } = useTranslation();
  const navigate = useNavigate();

  const handleCreatePage = () => {
    navigate('/create');
  };

  return (
    <div>
      <div className="create-container text-white">
        <Container fluid>
          <Row>
            <Col md={6}>
              <div className="text-container">
                <h1>{t("home.hero.create")}</h1>
                <p>{t("home.hero.createText")}</p>
                <Button 
                  variant="primary"
                  onClick={handleCreatePage}
                >{t("home.hero.startNow")}</Button>
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
        <h1>{t("home.why.studio.title")}</h1>
        <div className="features-section pt-2">
          <Container fluid>
            <Row className="text-center justify-content-around">
              <Col md={3} lg={2} className="p-3 custom-box">
                <h3>{t("home.why.studio.features.fillOnly.title")}</h3>
                <p>{t("home.why.studio.features.fillOnly.description")}</p>
              </Col>
              <Col md={3} lg={2} className="p-3 custom-box">
                <h3>{t("home.why.studio.features.easyToUse.title")}</h3>
                <p>{t("home.why.studio.features.easyToUse.description")}</p>
              </Col>
              <Col md={3} lg={2} className="p-3 custom-box">
                <h3>{t("home.why.studio.features.advanced.title")}</h3>
                <p>{t("home.why.studio.features.advanced.description")}</p>
              </Col>
            </Row>
          </Container>
        </div>
      </div>
      <div className="pt-2">
        <h1 className="text-center">{t("home.why.digitalTwin.title")}</h1>

        <div
          className="d-flex flex-column align-items-start mx-auto"
          style={{ width: "fit-content" }}
        >
          <div className="d-flex align-items-center gap-2">
            <CheckIcon style={{ color: "green", width: 36, height: 36 }} />
            <span>{t("home.why.digitalTwin.features.optimization")}</span>
          </div>
          <div className="d-flex align-items-center gap-2">
            <CheckIcon style={{ color: "green", width: 36, height: 36 }} />
            <span>{t("home.why.digitalTwin.features.costReduction")}</span>
          </div>
          <div className="d-flex align-items-center gap-2">
            <CheckIcon style={{ color: "green", width: 36, height: 36 }} />
            <span>{t("home.why.digitalTwin.features.dataVisualization")}</span>
          </div>
        </div>
        <div className="text-center pt-2 pb-3">
          <p>{t("home.why.digitalTwin.more")}</p>
          <Button 
            variant="primary"
            onClick={handleCreatePage}
          >{t("home.hero.startNow")}</Button>
        </div>
      </div>
    </div>
  );
}

export default Home;
