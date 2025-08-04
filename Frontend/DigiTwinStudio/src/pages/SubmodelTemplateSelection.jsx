import { Container, Row, Col, Button, Card, Pagination, Spinner, Alert, Form, InputGroup } from "react-bootstrap";
import { Link, useNavigate, useLocation } from "react-router-dom";
import { useState, useEffect } from "react";
import { useTranslation } from "react-i18next";
import searchIcon from "../assets/icons/search.svg";
import "../styles/submodelTemplateSelection.css";

export default function SubmodelTemplateSelection() {
  
  const navigate = useNavigate();
  const location = useLocation();
  const { i18n, t } = useTranslation();
  const [currentPage, setCurrentPage] = useState(1);
  const [templates, setTemplates] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [searchTerm, setSearchTerm] = useState("");
  
  // Get the original form data passed from /create
  const originalFormData = location.state?.formData;

  // Helper function to get description in user's preferred language
  const getLocalizedDescription = (descriptions) => {
    if (!descriptions || typeof descriptions !== 'object') {
      return t("templateSelection.noDescription");
    }
    
    const currentLanguage = i18n.language;
    
    // Try current language first
    if (descriptions[currentLanguage]) {
      return descriptions[currentLanguage];
    }
    
    // Fallback to English
    if (descriptions['en']) {
      return descriptions['en'];
    }
    
    // Fallback to any available language
    const availableLanguages = Object.keys(descriptions);
    if (availableLanguages.length > 0) {
      return descriptions[availableLanguages[0]];
    }
    
    return t("templateSelection.noDescription");
  };

  // Fetch templates from backend API
  useEffect(() => {
    const fetchTemplates = async () => {
      try {
        setLoading(true);
        const response = await fetch('http://localhost:9090/submodels/templates');
        
        if (!response.ok) {
          throw new Error(`HTTP error! status: ${response.status}`);
        }
        
        const data = await response.json();
        setTemplates(data);
        setError(null);
      } catch (err) {
        console.error('Error fetching templates:', err);
        setError(`${t("templateSelection.loadFailed")} ${err.message}`);
      } finally {
        setLoading(false);
      }
    };

    fetchTemplates();
  }, []);
  
  // Handle template selection
  const handleTemplateSelect = (template) => {
    const description = getLocalizedDescription(template.descriptions);
    
    navigate('/templates/create', {
      state: {
        selectedTemplate: {
          id: template.id,
          title: template.name,
          description: description,
          templateData: template // Pass the full template data including JSON
        },
        originalFormData: originalFormData // Pass through the original form data
      }
    });
  };
  const templatesPerPage = 6;
  
  // Filter templates based on search term
  const filteredTemplates = templates.filter(template => {
    const searchLower = searchTerm.toLowerCase();
    const templateName = template.name?.toLowerCase() || '';
    const templateDescription = getLocalizedDescription(template.descriptions).toLowerCase();
    
    return templateName.includes(searchLower) || templateDescription.includes(searchLower);
  });
  
  // Calculate pagination based on filtered templates
  const indexOfLastTemplate = currentPage * templatesPerPage;
  const indexOfFirstTemplate = indexOfLastTemplate - templatesPerPage;
  const currentTemplates = filteredTemplates.slice(indexOfFirstTemplate, indexOfLastTemplate);
  const totalPages = Math.ceil(filteredTemplates.length / templatesPerPage);
  
  // Reset to page 1 when search term changes
  useEffect(() => {
    setCurrentPage(1);
  }, [searchTerm]);
  
  const handlePageChange = (pageNumber) => {
    setCurrentPage(pageNumber);
  };

  // Handle back navigation with preserved form data
  const handleBackToCreate = () => {
    navigate('/create', {
      state: {
        restoredFormData: originalFormData
      }
    });
  };

  return (
    <div className="submodel-template-container">
      <Container className="py-4">
        <Button
          onClick={handleBackToCreate}
          className="mb-3 back-button"
        >
          ← {t("create.buttons.back")}
        </Button>

      {/* Step progress */}
      <div className="d-flex mb-4">
        <div className="text-warning step-progress-item step-progress-left">{t("templateSelection.select")}</div>
        <div className="text-white step-progress-item step-progress-center">{t("create.progress.details")}</div>
        <div className="text-white step-progress-item step-progress-right">{t("create.progress.allDone")}</div>
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
      <h1 className="text-white mb-4">{t("templateSelection.selectModel")}</h1>

      {/* Search Bar */}
      <Row className="mb-4">
        <Col md={8} lg={6}>
          <InputGroup>
            <Form.Control
              type="text"
              placeholder={t("templateSelection.searchPlaceholder")}
              value={searchTerm}
              onChange={(e) => setSearchTerm(e.target.value)}
              className="search-input"
            />
            <InputGroup.Text className="search-icon">
              <img src={searchIcon} alt="Search" width="16" height="16" className="search-icon-img" />
            </InputGroup.Text>
          </InputGroup>
        </Col>
      </Row>

      {/* Results count */}
      {!loading && !error && (
        <div className="text-white mb-3">
          {searchTerm ? (
            <small>
              Found {filteredTemplates.length} template{filteredTemplates.length !== 1 ? 's' : ''} 
              {searchTerm && ` matching "${searchTerm}"`}
            </small>
          ) : (
            <small>{t("templateSelection.showing", { count: templates.length })}</small>
          )}
        </div>
      )}

      {/* Loading state */}
      {loading && (
        <div className="text-center py-5">
          <Spinner animation="border" variant="light" />
          <p className="text-white mt-3">{t("templateSelection.loading")}</p>
        </div>
      )}

      {/* Error state */}
      {error && (
        <Alert variant="danger" className="mb-4">
          {error}
          <Button 
            variant="outline-danger" 
            size="sm" 
            className="ms-3"
            onClick={() => window.location.reload()}
          >
            {t("templateSelection.retry")}
          </Button>
        </Alert>
      )}

      {/* Template cards */}
      {!loading && !error && (
        <>
          {filteredTemplates.length === 0 ? (
            <div className="text-center py-5">
              <div className="text-white">
                <h4>{t("templateSelection.noTemplates")}</h4>
                <p>
                  {searchTerm 
                    ? t("templateSelection.noTemplatesSearch", { searchTerm })
                    : t("templateSelection.noTemplatesAvailable")
                  }
                </p>
                {searchTerm && (
                  <Button 
                    variant="outline-light" 
                    onClick={() => setSearchTerm("")}
                    className="mt-2"
                  >
                    {t("templateSelection.clearSearch")}
                  </Button>
                )}
              </div>
            </div>
          ) : (
            <>
              <Row className="g-3">
                {currentTemplates.map((template, idx) => {
                  // Get description in user's preferred language
                  const description = getLocalizedDescription(template.descriptions);
                  
                  return (
                    <Col md={6} lg={6} key={template.id || idx}>
                      <Card className="text-white h-100 template-card">
                        <Card.Body className="d-flex flex-column">
                          <Card.Title>{template.name}</Card.Title>
                          <Card.Text className="flex-grow-1">
                            {description}
                          </Card.Text>
                          <div className="mt-2">
                            <small>
                              Version: {template.version}.{template.revision}
                            </small>
                          </div>
                          <Button 
                            variant="primary" 
                            className="align-self-start mt-2"
                            onClick={() => handleTemplateSelect(template)}
                          >
                            Select
                          </Button>
                        </Card.Body>
                      </Card>
                    </Col>
                  );
                })}
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
            </>
          )}
        </>
      )}
    </Container>
    </div>
  );
}
