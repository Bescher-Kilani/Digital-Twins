import { Container, Row, Col, Button, Form, Card, Pagination, Dropdown, Toast, ToastContainer, Modal } from "react-bootstrap";
import { useTranslation } from "react-i18next";
import { useState, useEffect, useContext } from "react";
import { useNavigate } from "react-router-dom";
import modelImage from "../assets/homepage_model.png";
import "../styles/dashboard.css";
import OpenIcon from "../assets/icons/arrow-up-right-square-fill.svg?react";
import DownloadIcon from "../assets/icons/arrow-bar-down.svg?react";
import ImportIcon from "../assets/icons/arrow-bar-up.svg?react";
import PlusIcon from "../assets/icons/plus-lg.svg?react";
import TrashIcon from "../assets/icons/trash.svg?react";
import PublishIcon from "../assets/icons/arrow-up-right-square-fill.svg?react";
import GlobeIcon from "../assets/icons/globe.svg?react";
import { KeycloakContext } from "../KeycloakContext";
import { authenticatedFetch } from "../utils/tokenManager";

export default function Dashboard() {
  const { t } = useTranslation();
  const { keycloak, authenticated } = useContext(KeycloakContext);
  const [currentPage, setCurrentPage] = useState(1);
  const [models, setModels] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [toasts, setToasts] = useState([]);
  const [showDeleteModal, setShowDeleteModal] = useState(false);
  const [modelToDelete, setModelToDelete] = useState(null);
  const [searchTerm, setSearchTerm] = useState('');
  const [showPublishModal, setShowPublishModal] = useState(false);
  const [modelToPublish, setModelToPublish] = useState(null);
  const [publishForm, setPublishForm] = useState({ shortDescription: '', tagIds: [] });
  const [availableTags, setAvailableTags] = useState([]);
  const [loadingTags, setLoadingTags] = useState(false);
  const [showUnpublishModal, setShowUnpublishModal] = useState(false);
  const [modelToUnpublish, setModelToUnpublish] = useState(null);
  const navigate = useNavigate();
  const modelsPerPage = 4;
  
  // Fetch models from API
  useEffect(() => {
    const fetchModels = async () => {
      try {
        setLoading(true);
        setError(null);
        
        let response;
        
        if (authenticated) {
          // User is authenticated - use authenticated endpoint ONLY
          console.log('User is authenticated, using authenticated endpoint for models');
          response = await authenticatedFetch('http://localhost:9090/models', {
            method: 'GET'
          }, keycloak);
        } else {
          // User is not authenticated
          console.log('User is not authenticated');
          return;
        }
        
        if (!response.ok) {
          throw new Error(`Failed to fetch models: ${response.status} ${response.statusText}`);
        }
        
        const data = await response.json();
        
        // Transform the API data to match the expected format for the UI
        const transformedModels = data.map(model => ({
          id: model.id,
          title: model.aas?.displayName?.[0]?.text || model.aas?.idShort || t("dashboard.untitledModel"),
          description: model.aas?.description?.[0]?.text || t("dashboard.noDescription"),
          published: model.published || false,
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
  }, [keycloak, authenticated, t]);

  // Function to show toast notifications
  const showToast = (message, variant = 'danger') => {
    const id = Date.now();
    const newToast = {
      id,
      message,
      variant,
      show: true
    };
    setToasts(prev => [...prev, newToast]);
    
    // Auto-hide toast after 5 seconds
    setTimeout(() => {
      setToasts(prev => prev.filter(toast => toast.id !== id));
    }, 5000);
  };

  // Function to manually close a toast
  const closeToast = (id) => {
    setToasts(prev => prev.filter(toast => toast.id !== id));
  };

  // Function to handle file downloads
  const handleDownload = async (model, format) => {
    if (!model.id) {
      showToast(t("dashboard.missingInfo"), 'danger');
      return;
    }

    try {
      // Use the model ID for both modelId and modelIdShort since we don't have separate values
      const url = `http://localhost:9090/models/${model.id}/${model.title}/export/${format}`;
      console.log('Downloading file from:', url);
      
      let response;
      
      if (authenticated) {
        // User is authenticated - use authenticatedFetch with token refresh
        response = await authenticatedFetch(url, {
          method: 'GET'
        }, keycloak);
      } else {
        // User is not authenticated - direct fetch without auth
        response = await fetch(url, {
          method: 'GET'
        });
      }

      if (response.ok) {
        // Get the file blob
        const blob = await response.blob();
        
        // Create download link
        const downloadUrl = window.URL.createObjectURL(blob);
        const link = document.createElement('a');
        link.href = downloadUrl;
        
        // Set filename based on format
        const fileExtension = format.toLowerCase();
        link.download = `${model.title || model.id}.${fileExtension}`;
        
        // Trigger download
        document.body.appendChild(link);
        link.click();
        document.body.removeChild(link);
        
        // Clean up the URL object
        window.URL.revokeObjectURL(downloadUrl);

        showToast(t("dashboard.downloadSuccess", { format }), 'success');
      } else {
        // Handle error response
        const errorData = await response.json().catch(() => ({}));
        const errorMessage = errorData.message || `Error: ${response.status} ${response.statusText}`;
        console.error('Error downloading file:', errorMessage);
        
        if (response.status === 401) {
          showToast(t("dashboard.authRequired"), 'warning');
        } else if (response.status === 403) {
          showToast(t("dashboard.accessDenied"), 'danger');
        } else if (response.status === 404) {
          showToast(t("dashboard.fileNotFound"), 'danger');
        } else {
          showToast(t("dashboard.downloadFailed", { format, errorMessage }), 'danger');
        }
      }
    } catch (error) {
      console.error('Network error downloading file:', error);
      
      // Check if it's a CORS or network error
      if (error.message.includes('Load failed') || error.message.includes('CORS') || error.message.includes('Network request failed')) {
        showToast(t("dashboard.connectionError"), 'danger');
      } else {
        showToast(t("dashboard.networkError"), 'danger');
      }
    }
  };

  // Function to handle model deletion
  const handleDelete = (model) => {
    if (!model.id) {
      showToast(t("dashboard.missingInfoDelete"), 'danger');
      return;
    }

    // Show confirmation modal
    setModelToDelete(model);
    setShowDeleteModal(true);
  };

  // Function to confirm deletion
  const confirmDelete = async () => {
    const model = modelToDelete;
    setShowDeleteModal(false);
    setModelToDelete(null);

    if (!model) return;

    try {
      const url = `http://localhost:9090/models/${model.id}/delete`;
      console.log('Deleting model from:', url);
      
      let response;
      
      if (authenticated) {
        // User is authenticated - use authenticatedFetch with token refresh
        response = await authenticatedFetch(url, {
          method: 'DELETE'
        }, keycloak);
      } else {
        // User is not authenticated - this shouldn't happen for delete operations
        showToast(t("dashboard.authRequired"), 'warning');
        return;
      }

      if (response.ok) {
        // Remove the model from the local state
        setModels(prevModels => prevModels.filter(m => m.id !== model.id));
        showToast(t("dashboard.deleteSuccess", { modelName: model.title }), 'success');

        // If we're on a page that no longer has models after filtering, go to page 1
        const remainingModels = models.filter(m => m.id !== model.id);
        const remainingFilteredModels = remainingModels.filter(m => 
          m.title.toLowerCase().includes(searchTerm.toLowerCase()) ||
          m.description.toLowerCase().includes(searchTerm.toLowerCase())
        );
        const newTotalPages = Math.ceil(remainingFilteredModels.length / modelsPerPage);
        if (currentPage > newTotalPages && newTotalPages > 0) {
          setCurrentPage(1);
        }
      } else {
        // Handle error response
        const errorData = await response.json().catch(() => ({}));
        const errorMessage = errorData.message || `Error: ${response.status} ${response.statusText}`;
        console.error('Error deleting model:', errorMessage);
        
        if (response.status === 401) {
          showToast(t("dashboard.authRequired"), 'warning');
        } else if (response.status === 403) {
          showToast(t("dashboard.accessDenied"), 'danger');
        } else if (response.status === 404) {
          showToast(t("dashboard.modelNotFound"), 'danger');
        } else {
          showToast(t("dashboard.deleteError", { errorMessage }), 'danger');
        }
      }
    } catch (error) {
      console.error('Network error deleting model:', error);
      
      // Check if it's a CORS or network error
      if (error.message.includes('Load failed') || error.message.includes('CORS') || error.message.includes('Network request failed')) {
        showToast(t("dashboard.connectionError"), 'danger');
      } else {
        showToast(t("dashboard.networkErrorDelete"), 'danger');
      }
    }
  };

  // Function to cancel deletion
  const cancelDelete = () => {
    setShowDeleteModal(false);
    setModelToDelete(null);
  };

  // Function to open publish modal
  const openPublishModal = async (model) => {
    setModelToPublish(model);
    setPublishForm({ shortDescription: '', tagIds: [] });
    
    // Fetch available tags
    setLoadingTags(true);
    try {
      const response = await authenticatedFetch('http://localhost:9090/marketplace/tags', {
        method: 'GET'
      }, keycloak);
      
      if (response.ok) {
        const tags = await response.json();
        setAvailableTags(tags);
      } else {
        console.error('Failed to fetch tags:', response.status);
        showToast('Failed to load tags', 'warning');
        setAvailableTags([]);
      }
    } catch (error) {
      console.error('Error fetching tags:', error);
      showToast('Error loading tags', 'danger');
      setAvailableTags([]);
    } finally {
      setLoadingTags(false);
    }
    
    setShowPublishModal(true);
  };

  // Function to close publish modal
  const closePublishModal = () => {
    setShowPublishModal(false);
    setModelToPublish(null);
    setPublishForm({ shortDescription: '', tagIds: [] });
  };

  // Function to handle form input changes
  const handlePublishFormChange = (field, value) => {
    setPublishForm(prev => ({ ...prev, [field]: value }));
  };

  // Function to handle tag selection
  const handleTagSelection = (tagId) => {
    setPublishForm(prev => {
      const isSelected = prev.tagIds.includes(tagId);
      const newTagIds = isSelected 
        ? prev.tagIds.filter(id => id !== tagId)
        : [...prev.tagIds, tagId];
      return { ...prev, tagIds: newTagIds };
    });
  };

  // Function to publish the model
  const handlePublish = async () => {
    if (!modelToPublish || !publishForm.shortDescription.trim()) {
      showToast('Please fill in the description field', 'warning');
      return;
    }

    if (publishForm.tagIds.length === 0) {
      showToast('Please select at least one tag', 'warning');
      return;
    }

    try {
      const publishData = {
        author: keycloak?.tokenParsed?.preferred_username || keycloak?.tokenParsed?.name || 'Unknown Author',
        shortDescription: publishForm.shortDescription.trim(),
        tagIds: publishForm.tagIds
      };

      const response = await authenticatedFetch(`http://localhost:9090/models/${modelToPublish.id}/publish`, {
        method: 'POST',
        body: JSON.stringify(publishData)
      }, keycloak);

      if (response.ok) {
        // Update the model's published status in local state
        setModels(prevModels => 
          prevModels.map(model => 
            model.id === modelToPublish.id 
              ? { ...model, published: true }
              : model
          )
        );
        
        showToast(`Model "${modelToPublish.title}" published successfully!`, 'success');
        closePublishModal();
      } else {
        const errorData = await response.json().catch(() => ({}));
        const errorMessage = errorData.message || `Error: ${response.status} ${response.statusText}`;
        console.error('Error publishing model:', errorMessage);
        
        if (response.status === 401) {
          showToast('Authentication required. Please sign in and try again.', 'warning');
        } else if (response.status === 403) {
          showToast('Access denied. You do not have permission to publish this model.', 'danger');
        } else if (response.status === 404) {
          showToast('Model not found.', 'danger');
        } else if (response.status === 409) {
          showToast('Model is already published.', 'warning');
        } else {
          showToast(`Failed to publish model: ${errorMessage}`, 'danger');
        }
      }
    } catch (error) {
      console.error('Network error publishing model:', error);
      showToast('Network error: Unable to publish model. Please check your connection and try again.', 'danger');
    }
  };

  // Function to open unpublish modal
  const openUnpublishModal = (model) => {
    setModelToUnpublish(model);
    setShowUnpublishModal(true);
  };

  // Function to close unpublish modal
  const closeUnpublishModal = () => {
    setShowUnpublishModal(false);
    setModelToUnpublish(null);
  };

  // Function to confirm unpublish
  const confirmUnpublish = async () => {
    const model = modelToUnpublish;
    setShowUnpublishModal(false);
    setModelToUnpublish(null);

    if (!model) return;

    try {
      const response = await authenticatedFetch(`http://localhost:9090/models/${model.id}/unpublish`, {
        method: 'POST'
      }, keycloak);

      if (response.ok) {
        // Update the model's published status in local state
        setModels(prevModels => 
          prevModels.map(m => 
            m.id === model.id 
              ? { ...m, published: false }
              : m
          )
        );
        
        showToast(`Model "${model.title}" unpublished successfully!`, 'success');
      } else {
        const errorData = await response.json().catch(() => ({}));
        const errorMessage = errorData.message || `Error: ${response.status} ${response.statusText}`;
        console.error('Error unpublishing model:', errorMessage);
        
        if (response.status === 401) {
          showToast('Authentication required. Please sign in and try again.', 'warning');
        } else if (response.status === 403) {
          showToast('Access denied. You do not have permission to unpublish this model.', 'danger');
        } else if (response.status === 404) {
          showToast('Model not found.', 'danger');
        } else {
          showToast(`Failed to unpublish model: ${errorMessage}`, 'danger');
        }
      }
    } catch (error) {
      console.error('Network error unpublishing model:', error);
      showToast('Network error: Unable to unpublish model. Please check your connection and try again.', 'danger');
    }
  };

  // Filter models based on search term
  const filteredModels = models.filter(model => 
    model.title.toLowerCase().includes(searchTerm.toLowerCase()) ||
    model.description.toLowerCase().includes(searchTerm.toLowerCase())
  );

  // Handle search input change
  const handleSearchChange = (e) => {
    setSearchTerm(e.target.value);
    setCurrentPage(1); // Reset to first page when searching
  };
  
  // Calculate pagination
  const indexOfLastModel = currentPage * modelsPerPage;
  const indexOfFirstModel = indexOfLastModel - modelsPerPage;
  const currentModels = filteredModels.slice(indexOfFirstModel, indexOfLastModel);
  const totalPages = Math.ceil(filteredModels.length / modelsPerPage);
  
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
            placeholder={t("dashboard.search") || "Search models..."}
            className="mb-2"
            value={searchTerm}
            onChange={handleSearchChange}
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
              <strong>{t("dashboard.errorLoading")}</strong> {error}
            </div>
          </Col>
        </Row>
      )}

      {/* Loading state */}
      {loading && (
        <Row className="mb-3">
          <Col className="text-center">
            <div className="text-white">{t("dashboard.loading")}</div>
          </Col>
        </Row>
      )}

      {/* Models list */}
      <div className="d-flex flex-column gap-3">
        {!loading && !error && models.length === 0 && (
          <Card className="text-white model-container">
            <Card.Body className="text-center py-5">
              <h5>{t("dashboard.noModels")}</h5>
              <p>{t("dashboard.noModelsDescription")}</p>
              <Button variant="primary" onClick={handleNewModel}>
                <PlusIcon></PlusIcon> {t("dashboard.createFirstModel")}
              </Button>
            </Card.Body>
          </Card>
        )}

        {!loading && !error && models.length > 0 && filteredModels.length === 0 && (
          <Card className="text-white model-container">
            <Card.Body className="text-center py-5">
              <h5>{t("dashboard.noModelsFound")}</h5>
              <p>{t("dashboard.noModelsFoundDescription", { searchTerm })}</p>
              <Button variant="outline-primary" onClick={() => setSearchTerm('')}>
                {t("dashboard.clearSearch")}
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
                <Dropdown>
                  <Dropdown.Toggle size="sm" variant="primary" id="dropdown-basic">
                    <DownloadIcon></DownloadIcon> {t("dashboard.download")}
                  </Dropdown.Toggle>
                  <Dropdown.Menu>
                    <Dropdown.Item onClick={() => handleDownload(model, 'JSON')}>
                      JSON
                    </Dropdown.Item>
                    <Dropdown.Item onClick={() => handleDownload(model, 'AASX')}>
                      AASX
                    </Dropdown.Item>
                  </Dropdown.Menu>
                </Dropdown>
                <Button 
                  size="sm" 
                  variant="danger" 
                  onClick={() => handleDelete(model)}
                  title={t("dashboard.delete")}
                >
                  <TrashIcon></TrashIcon> {t("dashboard.delete")}
                </Button>
                <Button
                  size="sm"
                  variant={model.published ? "warning" : "success"}
                  onClick={() => model.published ? openUnpublishModal(model) : openPublishModal(model)}
                  title={model.published ? t("dashboard.unpublish") : t("dashboard.publish")}>
                  <GlobeIcon style={{ marginRight: "4px" }} />
                  {model.published ? t("dashboard.unpublish") : t("dashboard.publish")}
                </Button>
              </div>
            </Card.Body>
          </Card>
        ))}
      </div>
      
      {/* Pagination */}
      {!loading && totalPages > 1 && filteredModels.length > 0 && (
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

    {/* Delete Confirmation Modal */}
    <Modal show={showDeleteModal} onHide={cancelDelete} centered data-bs-theme="dark">
      <Modal.Header 
        closeButton 
        style={{ 
          background: 'linear-gradient(180deg, rgba(1, 47, 99, 1) 0%, rgba(10, 75, 127, 1) 100%)',
          borderColor: '#0E4175' 
        }}
      >
        <Modal.Title className="text-white">
          <TrashIcon style={{ width: "20px", height: "20px", marginRight: "8px" }} />
          Confirm Delete
        </Modal.Title>
      </Modal.Header>
      <Modal.Body 
        style={{ 
          background: 'linear-gradient(180deg, rgba(0, 55, 106, 1) 0%, rgba(1, 54, 106, 1) 100%)',
          color: 'white' 
        }}
      >
        {modelToDelete && (
          <p>
            {t("dashboard.confirmDelete", { modelName: modelToDelete.title })}
          </p>
        )}
        <p className="text-warning mb-0">
          <small>{modelToDelete?.published ? t("dashboard.undoPublished") : t("dashboard.undo")}</small>
        </p>
      </Modal.Body>
      <Modal.Footer 
        style={{ 
          background: 'linear-gradient(180deg, rgba(0, 55, 106, 1) 0%, rgba(1, 54, 106, 1) 100%)',
          borderColor: '#0E4175' 
        }}
      >
        <Button variant="secondary" onClick={cancelDelete}>
          {t("dashboard.cancel")}
        </Button>
        <Button variant="danger" onClick={confirmDelete}>
          <TrashIcon style={{ width: "16px", height: "16px", marginRight: "4px" }} />
          {t("dashboard.deleteModel")}
        </Button>
      </Modal.Footer>
    </Modal>

    {/* Publish Modal */}
    <Modal show={showPublishModal} onHide={closePublishModal} centered data-bs-theme="dark" size="lg">
      <Modal.Header 
        closeButton 
        style={{ 
          background: 'linear-gradient(180deg, rgba(1, 47, 99, 1) 0%, rgba(10, 75, 127, 1) 100%)',
          borderColor: '#0E4175' 
        }}
      >
        <Modal.Title className="text-white">
          <GlobeIcon style={{ width: "20px", height: "20px", marginRight: "8px" }} />
          Publish Model to Marketplace
        </Modal.Title>
      </Modal.Header>
      <Modal.Body 
        style={{ 
          background: 'linear-gradient(180deg, rgba(0, 55, 106, 1) 0%, rgba(1, 54, 106, 1) 100%)',
          color: 'white' 
        }}
      >
        {modelToPublish && (
          <>
            <div className="mb-3">
              <h6>Model: <strong>{modelToPublish.title}</strong></h6>
              <p className="text-muted small">{modelToPublish.description}</p>
              <p className="text-muted small">
                <strong>Author:</strong> {keycloak?.tokenParsed?.preferred_username || keycloak?.tokenParsed?.name || 'Unknown Author'}
              </p>
            </div>
            
            <Form>
              <Form.Group className="mb-3">
                <Form.Label>Short Description <span className="text-danger">*</span></Form.Label>
                <Form.Control
                  as="textarea"
                  rows={3}
                  placeholder="Enter a short description for the marketplace..."
                  value={publishForm.shortDescription}
                  onChange={(e) => handlePublishFormChange('shortDescription', e.target.value)}
                  style={{ 
                    backgroundColor: 'rgba(255, 255, 255, 0.1)',
                    borderColor: '#0E4175',
                    color: 'white'
                  }}
                />
              </Form.Group>
              
              <Form.Group className="mb-3">
                <Form.Label>Tags <span className="text-danger">*</span></Form.Label>
                {loadingTags ? (
                  <div className="text-center py-3">
                    <div className="spinner-border spinner-border-sm text-primary" role="status">
                      <span className="visually-hidden">Loading tags...</span>
                    </div>
                    <div className="mt-2 small">Loading available tags...</div>
                  </div>
                ) : (
                  <div className="mt-2">
                    {availableTags.length === 0 ? (
                      <p className="text-muted small">No tags available</p>
                    ) : (
                      <div className="d-flex flex-wrap gap-2">
                        {availableTags.map((tag) => (
                          <Form.Check
                            key={tag.id}
                            type="checkbox"
                            id={`tag-${tag.id}`}
                            label={tag.name}
                            checked={publishForm.tagIds.includes(tag.id)}
                            onChange={() => handleTagSelection(tag.id)}
                            className="mb-2"
                            style={{ color: 'white' }}
                          />
                        ))}
                      </div>
                    )}
                  </div>
                )}
              </Form.Group>
              
              <div className="text-muted small">
                <p className="mb-1">
                  <strong>Note:</strong> Once published, your model will be available in the marketplace for other users to discover and download.
                </p>
                <p className="mb-0">
                  Both description and at least one tag are required.
                </p>
              </div>
            </Form>
          </>
        )}
      </Modal.Body>
      <Modal.Footer 
        style={{ 
          background: 'linear-gradient(180deg, rgba(0, 55, 106, 1) 0%, rgba(1, 54, 106, 1) 100%)',
          borderColor: '#0E4175' 
        }}
      >
        <Button variant="secondary" onClick={closePublishModal}>
          Cancel
        </Button>
        <Button 
          variant="success" 
          onClick={handlePublish}
          disabled={!publishForm.shortDescription.trim() || publishForm.tagIds.length === 0}
        >
          <GlobeIcon style={{ width: "16px", height: "16px", marginRight: "4px" }} />
          Publish to Marketplace
        </Button>
      </Modal.Footer>
    </Modal>

    {/* Unpublish Confirmation Modal */}
    <Modal show={showUnpublishModal} onHide={closeUnpublishModal} centered data-bs-theme="dark">
      <Modal.Header 
        closeButton 
        style={{ 
          background: 'linear-gradient(180deg, rgba(1, 47, 99, 1) 0%, rgba(10, 75, 127, 1) 100%)',
          borderColor: '#0E4175' 
        }}
      >
        <Modal.Title className="text-white">
          <GlobeIcon style={{ width: "20px", height: "20px", marginRight: "8px" }} />
          Confirm Unpublish
        </Modal.Title>
      </Modal.Header>
      <Modal.Body 
        style={{ 
          background: 'linear-gradient(180deg, rgba(0, 55, 106, 1) 0%, rgba(1, 54, 106, 1) 100%)',
          color: 'white' 
        }}
      >
        {modelToUnpublish && (
          <>
            <p>
              Are you sure you want to unpublish the model <strong>"{modelToUnpublish.title}"</strong> from the marketplace?
            </p>
            <p className="text-warning mb-0">
              <small>
                <strong>Note:</strong> This will remove your model from the marketplace and make it unavailable for other users to discover and download.
              </small>
            </p>
          </>
        )}
      </Modal.Body>
      <Modal.Footer 
        style={{ 
          background: 'linear-gradient(180deg, rgba(0, 55, 106, 1) 0%, rgba(1, 54, 106, 1) 100%)',
          borderColor: '#0E4175' 
        }}
      >
        <Button variant="secondary" onClick={closeUnpublishModal}>
          Cancel
        </Button>
        <Button variant="warning" onClick={confirmUnpublish}>
          <GlobeIcon style={{ width: "16px", height: "16px", marginRight: "4px" }} />
          Unpublish from Marketplace
        </Button>
      </Modal.Footer>
    </Modal>

    {/* Toast notifications positioned at top right */}
    <ToastContainer 
      position="top-end" 
      className="p-3" 
      style={{ 
        position: 'fixed', 
        top: '20px', 
        right: '20px', 
        zIndex: 9999 
      }}
    >
      {toasts.map((toast) => (
        <Toast
          key={toast.id}
          show={toast.show}
          onClose={() => closeToast(toast.id)}
          bg={toast.variant}
          text={toast.variant === 'warning' ? 'dark' : 'white'}
          autohide
          delay={5000}
        >
          <Toast.Header>
            <strong className="me-auto">
              {toast.variant === 'danger' ? 'Error' : 
               toast.variant === 'warning' ? 'Warning' : 
               toast.variant === 'success' ? 'Success' : 'Notification'}
            </strong>
          </Toast.Header>
          <Toast.Body>
            {toast.message}
          </Toast.Body>
        </Toast>
      ))}
    </ToastContainer>
    </div>
  );
}
