import { Container, Row, Col, Button, Card, Pagination } from "react-bootstrap";
import { Link } from "react-router-dom";
import { useState } from "react";
import "../styles/submodelTemplateSelection.css";

const templates = [
  {
    title: "Digital Nameplate For Industrial Equipment",
    description: "Identification information of the asset",
  },
  {
    title: "Carbon Footprint",
    description: "Calculation of carbon footprint data",
  },
  {
    title: "Technical Data",
    description: "Technical specifications and parameters",
  },
  {
    title: "Maintenance Information",
    description: "Maintenance schedules and procedures",
  },
  {
    title: "Safety Guidelines",
    description: "Safety protocols and compliance data",
  },
  {
    title: "Energy Efficiency",
    description: "Energy consumption and efficiency metrics",
  },
  {
    title: "Quality Assurance",
    description: "Quality control and testing procedures",
  },
  {
    title: "Supply Chain",
    description: "Supply chain and logistics information",
  },
  {
    title: "Environmental Impact",
    description: "Environmental impact assessment data",
  },
  {
    title: "Performance Metrics",
    description: "Key performance indicators and analytics",
  }
];

export default function SubmodelTemplateSelection() {
  const [currentPage, setCurrentPage] = useState(1);
  const templatesPerPage = 6;
  
  // Calculate pagination
  const indexOfLastTemplate = currentPage * templatesPerPage;
  const indexOfFirstTemplate = indexOfLastTemplate - templatesPerPage;
  const currentTemplates = templates.slice(indexOfFirstTemplate, indexOfLastTemplate);
  const totalPages = Math.ceil(templates.length / templatesPerPage);
  
  const handlePageChange = (pageNumber) => {
    setCurrentPage(pageNumber);
  };

  return (
    <div className="submodel-template-container">
      <Container className="py-4">
        <Button
          as={Link}
          to="/dashboard"
          className="mb-3 back-button"
        >
          ← Back
        </Button>

      {/* Step progress */}
      <div className="d-flex mb-4">
        <div className="text-warning step-progress-item step-progress-left">Select a Template</div>
        <div className="text-white step-progress-item step-progress-center">Fill the details</div>
        <div className="text-white step-progress-item step-progress-right">All done</div>
      </div>

      <div className="d-flex justify-content-between align-items-center mb-4">
        <div
          style={{
            flex: 1,
            height: 4,
            background: "gold",
            marginRight: 4,
            borderRadius: 2,
          }}
        ></div>
        <div
          style={{
            flex: 1,
            height: 4,
            background: "#ccc",
            marginLeft: 4,
            marginRight: 4,
            borderRadius: 2,
          }}
        ></div>
        <div
          style={{
            flex: 1,
            height: 4,
            background: "#ccc",
            marginLeft: 4,
            borderRadius: 2,
          }}
        ></div>
      </div>

      {/* Heading */}
      <h1 className="text-white mb-4">Select a Submodel</h1>

      {/* Template cards */}
      <Row className="g-3">
        {currentTemplates.map((template, idx) => (
          <Col md={6} lg={6} key={idx}>
            <Card className="text-white h-100 template-card">
              <Card.Body className="d-flex flex-column">
                <Card.Title>{template.title}</Card.Title>
                <Card.Text className="flex-grow-1">
                  {template.description}
                </Card.Text>
                <Button variant="primary" className="align-self-start mt-2">
                  Select
                </Button>
              </Card.Body>
            </Card>
          </Col>
        ))}
      </Row>
      
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
