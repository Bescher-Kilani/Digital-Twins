import React, { useContext } from "react";
import { Container, Button, Card } from "react-bootstrap";
import { Link, useLocation } from "react-router-dom";
import "../styles/createComplete.css";
import ChevronLeftIcon from "../assets/icons/chevron-left.svg?react";
import DownloadIcon from "../assets/icons/arrow-bar-down.svg?react";
import { KeycloakContext } from "../KeycloakContext";

export default function CreateComplete() {
  const location = useLocation();
  const { authenticated } = useContext(KeycloakContext);
  const modelName = location.state?.modelName || "Untitled Model";

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
          </div>

          {/* Buttons */}
          <div className="mt-auto">
      {authenticated && (
        <Button as={Link} to="/dashboard" className="mb-2 create-complete-button">
          <ChevronLeftIcon style={{ fill: "white", width: "16px", height: "16px" }}/>
          Back to Dashboard
        </Button>
      )}
      <Button className="mb-2 create-complete-button">
        <DownloadIcon style={{ fill: "white", width: "16px", height: "16px" }}/>
        Download AASX
        </Button>
      <Button className="mb-2 create-complete-button">
        <DownloadIcon style={{ fill: "white", width: "16px", height: "16px" }}/>
        Download JSON
        </Button>
    </div>
        </Card.Body>
      </Card>
    </Container>
    </div>
  );
}
