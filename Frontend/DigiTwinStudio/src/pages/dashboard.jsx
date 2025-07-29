import { Container, Row, Col, Button, Form, Card, Pagination } from "react-bootstrap";
import { useTranslation } from "react-i18next";
import { useState } from "react";
import { useNavigate } from "react-router-dom";
import modelImage from "../assets/homepage_model.png";
import "../styles/dashboard.css";
import OpenIcon from "../assets/icons/arrow-up-right-square-fill.svg?react";
import DownloadIcon from "../assets/icons/arrow-bar-down.svg?react";
import ImportIcon from "../assets/icons/arrow-bar-up.svg?react";
import PlusIcon from "../assets/icons/plus-lg.svg?react";

const models = [
  {
    title: "Building 1",
    description: "Digital Twin of a building",
    lastEdit: "26. June 2025",
  },
  {
    title: "Industrial Plant",
    description: "Digital Twin of an industrial facility",
    lastEdit: "25. June 2025",
  },
  {
    title: "Smart City Model",
    description: "Urban planning digital twin",
    lastEdit: "24. June 2025",
  },
  {
    title: "Manufacturing Line",
    description: "Production line digital twin",
    lastEdit: "23. June 2025",
  },
  {
    title: "Energy Grid",
    description: "Power distribution system model",
    lastEdit: "22. June 2025",
  },
  {
    title: "Transportation Hub",
    description: "Airport terminal digital twin",
    lastEdit: "21. June 2025",
  },
  {
    title: "Healthcare Facility",
    description: "Hospital building model",
    lastEdit: "20. June 2025",
  },
  {
    title: "Educational Campus",
    description: "University campus digital twin",
    lastEdit: "19. June 2025",
  },
  {
    title: "Retail Complex",
    description: "Shopping center model",
    lastEdit: "18. June 2025",
  },
  {
    title: "Residential Area",
    description: "Housing development digital twin",
    lastEdit: "17. June 2025",
  },
];

export default function Dashboard() {
  const { t } = useTranslation();
  const [currentPage, setCurrentPage] = useState(1);
  const navigate = useNavigate();
  const modelsPerPage = 4;
  
  // Calculate pagination
  const indexOfLastModel = currentPage * modelsPerPage;
  const indexOfFirstModel = indexOfLastModel - modelsPerPage;
  const currentModels = models.slice(indexOfFirstModel, indexOfLastModel);
  const totalPages = Math.ceil(models.length / modelsPerPage);
  
  const handlePageChange = (pageNumber) => {
    setCurrentPage(pageNumber);
  };

  const handleNewModel = () => {
    navigate('/create');
  };

  return (
    <div className="dashboard-container">
        <Container className="py-4">
      <h2 className="text-white mb-3">{t("dashboard.yourModels")}</h2>

      {/* Actions */}
      <Row className="mb-3">
        <Col md={8}>
          <Form.Control
            type="text"
            placeholder="Search"
            className="mb-2"
          />
        </Col>
        <Col md={4} className="text-md-end">
          <Button variant="primary" className="me-2">
            <ImportIcon></ImportIcon> {t("dashboard.importModel")}
          </Button>
          <Button variant="primary" onClick={handleNewModel}><PlusIcon></PlusIcon> {t("dashboard.newModel")}</Button>
        </Col>
      </Row>

      {/* Models list */}
      <div className="d-flex flex-column gap-3">
        {currentModels.map((model, index) => (
          <Card key={index} className="text-white model-container">
            <Card.Body className="d-flex align-items-center">
              <img
                src={modelImage}
                alt="Model"
                style={{ height: 100, marginRight: "1rem" }}
              />
              <div className="flex-grow-1">
                <h5 className="mb-1">{model.title}</h5>
                <p className="mb-1">{model.description}</p>
                <small>{t("dashboard.lastEdit")}: {model.lastEdit}</small>
              </div>
              <div className="d-flex flex-column gap-2">
                <Button size="sm" variant="primary">
                  <OpenIcon></OpenIcon> {t("dashboard.open")}
                </Button>
                <Button size="sm" variant="primary">
                  <DownloadIcon></DownloadIcon> {t("dashboard.download")}
                </Button>
              </div>
            </Card.Body>
          </Card>
        ))}
      </div>
      
      {/* Pagination */}
      {totalPages > 1 && (
        <Row className="mt-4">
          <Col className="d-flex justify-content-center">
            <Pagination>
              <Pagination.First 
                onClick={() => handlePageChange(1)} 
                disabled={currentPage === 1}
              />
              <Pagination.Prev 
                onClick={() => handlePageChange(currentPage - 1)} 
                disabled={currentPage === 1}
              />
              
              {[...Array(totalPages)].map((_, index) => {
                const pageNumber = index + 1;
                return (
                  <Pagination.Item
                    key={pageNumber}
                    active={pageNumber === currentPage}
                    onClick={() => handlePageChange(pageNumber)}
                  >
                    {pageNumber}
                  </Pagination.Item>
                );
              })}
              
              <Pagination.Next 
                onClick={() => handlePageChange(currentPage + 1)} 
                disabled={currentPage === totalPages}
              />
              <Pagination.Last 
                onClick={() => handlePageChange(totalPages)} 
                disabled={currentPage === totalPages}
              />
            </Pagination>
          </Col>
        </Row>
      )}
    </Container>
    </div>
  );
}
