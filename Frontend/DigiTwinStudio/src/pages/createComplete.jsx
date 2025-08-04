import React, { useContext } from "react";
import { Container, Button, Card, Toast, ToastContainer } from "react-bootstrap";
import { Link, useLocation } from "react-router-dom";
import "../styles/createComplete.css";
import ChevronLeftIcon from "../assets/icons/chevron-left.svg?react";
import DownloadIcon from "../assets/icons/arrow-bar-down.svg?react";
import { KeycloakContext } from "../KeycloakContext";
import { authenticatedFetch } from "../utils/tokenManager";

export default function CreateComplete() {
  const location = useLocation();
  const { keycloak, authenticated } = useContext(KeycloakContext);
  const modelName = location.state?.modelName || "Untitled Model";
  const modelId = location.state?.modelId;
  const modelIdShort = location.state?.modelIdShort;

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
      showToast('Model information is missing. Cannot download file.', 'danger');
      return;
    }

    try {
      let response;
      let url;

      if (authenticated) {
        // User is authenticated - use authenticated endpoint with authenticatedFetch
        url = `http://localhost:9090/models/${modelId}/${modelIdShort}/export/${format}`;
        console.log('Downloading file from authenticated endpoint:', url);
        
        response = await authenticatedFetch(url, {
          method: 'GET'
        }, keycloak);
      } else {
        // User is not authenticated - use guest endpoint with regular fetch
        url = `http://localhost:9090/guest/models/${modelId}/${modelIdShort}/export/${format}`;
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

  return (
    <div className="create-template-container">
        <Container className="py-4">
      <div className="d-flex mb-1">
        <div className="text-success step-progress-item step-progress-left">Fill the Details</div>
        <div className="text-success step-progress-item step-progress-right">All done</div>
      </div>

      <div className="d-flex justify-content-between align-items-center mb-4">
        <div className="progress-bar-section progress-bar-left" />
        <div className="progress-bar-section progress-bar-right" />
      </div>

      {/* Card with message */}
      <Card className="text-white mx-auto mb-4 create-complete-card">
        <Card.Body className="d-flex flex-column">
          <div>
            <h1>Model Created</h1>
            <h3 className="mt-4">
              Your model with the Name <strong>{modelName}</strong> has been created and is ready for download.
            </h3>
            {authenticated && (
              <p className="mt-3">You can access this model from your dashboard at any time.</p>
            )}
            {(!modelId || !modelIdShort) && (
              <p className="mt-3 text-warning">
                <small>Model information is incomplete. Download functionality may not be available.</small>
              </p>
            )}
          </div>

          {/* Buttons */}
          <div className="mt-auto">
      {authenticated && (
        <Button as={Link} to="/dashboard" className="mb-2 create-complete-button">
          <ChevronLeftIcon style={{ fill: "white", width: "16px", height: "16px" }}/>
          Back to Dashboard
        </Button>
      )}
      <Button 
        className="mb-2 create-complete-button"
        onClick={() => handleDownload('AASX')}
        disabled={!modelId || !modelIdShort}
      >
        <DownloadIcon style={{ fill: "white", width: "16px", height: "16px" }}/>
        Download AASX
        </Button>
      <Button 
        className="mb-2 create-complete-button"
        onClick={() => handleDownload('JSON')}
        disabled={!modelId || !modelIdShort}
      >
        <DownloadIcon style={{ fill: "white", width: "16px", height: "16px" }}/>
        Download JSON
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
    </Container>
    </div>
  );
}
