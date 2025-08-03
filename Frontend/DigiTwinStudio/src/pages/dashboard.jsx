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
  const navigate = useNavigate();
  const modelsPerPage = 4;
  
  // Fetch models from API
  useEffect(() => {
    const fetchModels = async () => {
      try {
        setLoading(true);
        setError(null);
        
        // Debug: Check what's in storage
        console.log('Dashboard debug - authenticated:', authenticated);
        console.log('Dashboard debug - keycloak:', keycloak);
        console.log('Dashboard debug - sessionStorage access_token:', sessionStorage.getItem('access_token'));
        console.log('Dashboard debug - sessionStorage refresh_token:', sessionStorage.getItem('refresh_token'));
        console.log('Dashboard debug - all sessionStorage keys:', Object.keys(sessionStorage));
        
        let response;
        
        if (authenticated) {
          // User is authenticated - use authenticated endpoint ONLY
          console.log('User is authenticated, using authenticated endpoint for models');
          response = await authenticatedFetch('http://localhost:9090/models', {
            method: 'GET'
          }, keycloak);
        } else {
          // User is not authenticated - use guest endpoint ONLY
          console.log('User is not authenticated, using guest endpoint for models');
          // Note: There might not be a guest endpoint for listing models
          // You might need to just show empty state or handle this differently
          setModels([]);
          return;
        }
        
        if (!response.ok) {
          throw new Error(`Failed to fetch models: ${response.status} ${response.statusText}`);
        }
        
        const data = await response.json();
        console.log('Fetched models:', data);
        
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
      showToast('Model information is missing. Cannot download file.', 'danger');
      return;
    }

    try {
      // Use the model ID for both modelId and modelIdShort since we don't have separate values
      const url = `http://localhost:9090/guest/models/${model.id}/${model.title}/export/${format}`;
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
        
        showToast(`${format} file downloaded successfully!`, 'success');
      } else {
        // Handle error response
        const errorData = await response.json().catch(() => ({}));
        const errorMessage = errorData.message || `Error: ${response.status} ${response.statusText}`;
        console.error('Error downloading file:', errorMessage);
        
        if (response.status === 401) {
          showToast('Authentication required. Please sign in and try again.', 'warning');
        } else if (response.status === 403) {
          showToast('Access denied. You do not have permission to download this file.', 'danger');
        } else if (response.status === 404) {
          showToast('File not found. The model may have been deleted.', 'danger');
        } else {
          showToast(`Failed to download ${format} file: ${errorMessage}`, 'danger');
        }
      }
    } catch (error) {
      console.error('Network error downloading file:', error);
      
      // Check if it's a CORS or network error
      if (error.message.includes('Load failed') || error.message.includes('CORS') || error.message.includes('Network request failed')) {
        showToast('Connection error: Unable to reach the server.', 'danger');
      } else {
        showToast('Network error: Unable to download file. Please check your connection and try again.', 'danger');
      }
    }
  };

  // Function to handle model deletion
  const handleDelete = (model) => {
    if (!model.id) {
      showToast('Model information is missing. Cannot delete model.', 'danger');
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
        showToast('Authentication required to delete models.', 'warning');
        return;
      }

      if (response.ok) {
        // Remove the model from the local state
        setModels(prevModels => prevModels.filter(m => m.id !== model.id));
        showToast(`Model "${model.title}" deleted successfully!`, 'success');
        
        // If we're on a page that no longer has models, go to page 1
        const remainingModels = models.filter(m => m.id !== model.id);
        const newTotalPages = Math.ceil(remainingModels.length / modelsPerPage);
        if (currentPage > newTotalPages && newTotalPages > 0) {
          setCurrentPage(1);
        }
      } else {
        // Handle error response
        const errorData = await response.json().catch(() => ({}));
        const errorMessage = errorData.message || `Error: ${response.status} ${response.statusText}`;
        console.error('Error deleting model:', errorMessage);
        
        if (response.status === 401) {
          showToast('Authentication required. Please sign in and try again.', 'warning');
        } else if (response.status === 403) {
          showToast('Access denied. You do not have permission to delete this model.', 'danger');
        } else if (response.status === 404) {
          showToast('Model not found. It may have already been deleted.', 'danger');
        } else {
          showToast(`Failed to delete model: ${errorMessage}`, 'danger');
        }
      }
    } catch (error) {
      console.error('Network error deleting model:', error);
      
      // Check if it's a CORS or network error
      if (error.message.includes('Load failed') || error.message.includes('CORS') || error.message.includes('Network request failed')) {
        showToast('Connection error: Unable to reach the server.', 'danger');
      } else {
        showToast('Network error: Unable to delete model. Please check your connection and try again.', 'danger');
      }
    }
  };

  // Function to cancel deletion
  const cancelDelete = () => {
    setShowDeleteModal(false);
    setModelToDelete(null);
  };
  
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
                  title="Delete model"
                >
                  <TrashIcon></TrashIcon> Delete
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
            Are you sure you want to delete the model <strong>"{modelToDelete.title}"</strong>?
          </p>
        )}
        <p className="text-warning mb-0">
          <small>This action cannot be undone.</small>
        </p>
      </Modal.Body>
      <Modal.Footer 
        style={{ 
          background: 'linear-gradient(180deg, rgba(0, 55, 106, 1) 0%, rgba(1, 54, 106, 1) 100%)',
          borderColor: '#0E4175' 
        }}
      >
        <Button variant="secondary" onClick={cancelDelete}>
          Cancel
        </Button>
        <Button variant="danger" onClick={confirmDelete}>
          <TrashIcon style={{ width: "16px", height: "16px", marginRight: "4px" }} />
          Delete Model
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
