import { Container, Row, Col, Button, Form, Card, Pagination } from "react-bootstrap";
import { useTranslation } from "react-i18next";
import { useState, useEffect, useContext } from "react";
import { useNavigate } from "react-router-dom";
import modelImage from "../assets/homepage_model.png";
import "../styles/dashboard.css";
import OpenIcon from "../assets/icons/arrow-up-right-square-fill.svg?react";
import DownloadIcon from "../assets/icons/arrow-bar-down.svg?react";
import ImportIcon from "../assets/icons/arrow-bar-up.svg?react";
import PlusIcon from "../assets/icons/plus-lg.svg?react";
import { KeycloakContext } from "../KeycloakContext";

export default function Dashboard() {
  const { t } = useTranslation();
  const { keycloak, authenticated } = useContext(KeycloakContext);
  const [currentPage, setCurrentPage] = useState(1);
  const [models, setModels] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const navigate = useNavigate();
  const modelsPerPage = 4;
  
  // Fetch models from API
  useEffect(() => {
    const fetchModels = async () => {
      try {
        setLoading(true);
        setError(null);
        
        // Prepare headers
        const headers = {
          'Content-Type': 'application/json'
        };
        
        // Add Authorization header if user is logged in
        let token = null;
        
        // Try multiple sources for the token
        token = sessionStorage.getItem('access_token') || 
                localStorage.getItem('authToken') || 
                (keycloak && keycloak.token) ||
                null;
        
        if (token) {
          headers['Authorization'] = `Bearer ${token}`;
        }
        
        const response = await fetch('http://localhost:9090/models', {
          method: 'GET',
          headers: headers
        });
        
        if (!response.ok) {
          throw new Error(`Failed to fetch models: ${response.status} ${response.statusText}`);
        }
        
        const data = await response.json();
        
        // Transform the API data to match the expected format for the UI
        const transformedModels = data.map(model => ({
          id: model.id,
          title: model.aas?.displayName?.[0]?.text || model.aas?.idShort || 'Untitled Model',
          description: model.aas?.description?.[0]?.text || 'No description available',
          lastEdit: model.updatedAt ? new Date(model.updatedAt).toLocaleDateString('en-US', {
            day: 'numeric',
            month: 'long',
            year: 'numeric'
          }) : 'Unknown'
        }));
        
        setModels(transformedModels);
      } catch (err) {
        console.error('Error fetching models:', err);
        setError(err.message);
        // Fall back to empty array on error
        setModels([]);
      } finally {
        setLoading(false);
      }
    };

    fetchModels();
  }, [keycloak, authenticated]);
  
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

      {/* Error message */}
      {error && (
        <Row className="mb-3">
          <Col>
            <div className="alert alert-danger" role="alert">
              <strong>Error loading models:</strong> {error}
            </div>
          </Col>
        </Row>
      )}

      {/* Loading state */}
      {loading && (
        <Row className="mb-3">
          <Col className="text-center">
            <div className="text-white">Loading models...</div>
          </Col>
        </Row>
      )}

      {/* Models list */}
      <div className="d-flex flex-column gap-3">
        {!loading && !error && models.length === 0 && (
          <Card className="text-white model-container">
            <Card.Body className="text-center py-5">
              <h5>No models created yet</h5>
              <p>You haven't created any digital twin models yet. Get started by creating your first model!</p>
              <Button variant="primary" onClick={handleNewModel}>
                <PlusIcon></PlusIcon> Create your first model
              </Button>
            </Card.Body>
          </Card>
        )}
        
        {!loading && currentModels.map((model, index) => (
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
      {!loading && totalPages > 1 && models.length > 0 && (
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
