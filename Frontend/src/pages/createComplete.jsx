import React, { useContext } from "react";
import { Container, Button, Card, Toast, ToastContainer } from "react-bootstrap";
import { Link, useLocation } from "react-router-dom";
import "../styles/createComplete.css";
import ChevronLeftIcon from "../assets/icons/chevron-left.svg?react";
import DownloadIcon from "../assets/icons/arrow-bar-down.svg?react";
import { KeycloakContext } from "../KeycloakContext";
import { authenticatedFetch } from "../utils/tokenManager";
import { useTranslation } from "react-i18next";

export default function CreateComplete() {
  const location = useLocation();
  const { keycloak, authenticated } = useContext(KeycloakContext);
  const { t } = useTranslation();
  const modelName = location.state?.modelName || "Untitled Model";
  const modelId = location.state?.modelId;
  const modelIdShort = location.state?.modelIdShort;
  const API_URL = import.meta.env.VITE_BACKEND_URL || 'http://localhost:9090';

  // State for toast notifications
  const [toasts, setToasts] = React.useState([]);

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
  const handleDownload = async (format) => {
    if (!modelId || !modelIdShort) {
      showToast(t('createComplete.toasts.missingInfo'), 'danger');
      return;
    }

    try {
      let response;
      let url;

      if (authenticated) {
        // User is authenticated - use authenticated endpoint with authenticatedFetch
        url = `${API_URL}/models/${modelId}/${modelIdShort}/export/${format}`;
        console.log('Downloading file from authenticated endpoint:', url);
        
        response = await authenticatedFetch(url, {
          method: 'GET'
        }, keycloak);
      } else {
        // User is not authenticated - use guest endpoint with regular fetch
        url = `${API_URL}/guest/models/${modelId}/${modelIdShort}/export/${format}`;
        console.log('Downloading file from guest endpoint:', url);
        
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
        link.download = `${modelName || modelIdShort}.${fileExtension}`;
        
        // Trigger download
        document.body.appendChild(link);
        link.click();
        document.body.removeChild(link);
        
        // Clean up the URL object
        window.URL.revokeObjectURL(downloadUrl);
        
        showToast(t('createComplete.toasts.downloadSuccess', { format }), 'success');
      } else {
        // Handle error response
        const errorData = await response.json().catch(() => ({}));
        const errorMessage = errorData.message || `Error: ${response.status} ${response.statusText}`;
        console.error('Error downloading file:', errorMessage);
        
        if (response.status === 401) {
          showToast(t('createComplete.toasts.authRequired'), 'warning');
        } else if (response.status === 403) {
          showToast(t('createComplete.toasts.accessDenied'), 'danger');
        } else if (response.status === 404) {
          showToast(t('createComplete.toasts.fileNotFound'), 'danger');
        } else {
          showToast(t('createComplete.toasts.downloadFailed', { format, errorMessage }), 'danger');
        }
      }
    } catch (error) {
      console.error('Network error downloading file:', error);
      
      // Check if it's a CORS or network error
      if (error.message.includes('Load failed') || error.message.includes('CORS') || error.message.includes('Network request failed')) {
        showToast(t('createComplete.toasts.connectionError'), 'danger');
      } else {
        showToast(t('createComplete.toasts.networkError'), 'danger');
      }
    }
  };

  return (
    <div className="create-template-container">
        <Container className="py-4">
      <div className="d-flex mb-1">
        <div className="text-success step-progress-item step-progress-left">{t('createComplete.progress.details')}</div>
        <div className="text-success step-progress-item step-progress-right">{t('createComplete.progress.allDone')}</div>
      </div>

      <div className="d-flex justify-content-between align-items-center mb-4">
        <div className="progress-bar-section progress-bar-left" />
        <div className="progress-bar-section progress-bar-right" />
      </div>

      {/* Card with message */}
      <Card className="text-white mx-auto mb-4 create-complete-card">
        <Card.Body className="d-flex flex-column">
          <div>
            <h1>{t('createComplete.title')}</h1>
            <h3 className="mt-4">
              {t('createComplete.subtitle', { modelName })}
            </h3>
            {authenticated && (
              <p className="mt-3">{t('createComplete.dashboardAccess')}</p>
            )}
            {(!modelId || !modelIdShort) && (
              <p className="mt-3 text-warning">
                <small>{t('createComplete.incompleteInfo')}</small>
              </p>
            )}
          </div>

          {/* Buttons */}
          <div className="mt-auto">
      {authenticated && (
        <Button as={Link} to="/dashboard" className="mb-2 create-complete-button">
          <ChevronLeftIcon style={{ fill: "white", width: "16px", height: "16px" }}/>
          {t('createComplete.buttons.backToDashboard')}
        </Button>
      )}
      <Button 
        className="mb-2 create-complete-button"
        onClick={() => handleDownload('AASX')}
        disabled={!modelId || !modelIdShort}
      >
        <DownloadIcon style={{ fill: "white", width: "16px", height: "16px" }}/>
        {t('createComplete.buttons.downloadAASX')}
        </Button>
      <Button 
        className="mb-2 create-complete-button"
        onClick={() => handleDownload('JSON')}
        disabled={!modelId || !modelIdShort}
      >
        <DownloadIcon style={{ fill: "white", width: "16px", height: "16px" }}/>
        {t('createComplete.buttons.downloadJSON')}
        </Button>
    </div>
        </Card.Body>
      </Card>

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
                {toast.variant === 'danger' ? t('createComplete.toasts.titles.error') : 
                 toast.variant === 'warning' ? t('createComplete.toasts.titles.warning') : 
                 toast.variant === 'success' ? t('createComplete.toasts.titles.success') : t('createComplete.toasts.titles.notification')}
              </strong>
            </Toast.Header>
            <Toast.Body>
              {toast.message}
            </Toast.Body>
          </Toast>
        ))}
      </ToastContainer>
    </Container>
    </div>
  );
}
