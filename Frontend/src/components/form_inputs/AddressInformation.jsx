import React from "react";
import { Form, Row, Col, Button, OverlayTrigger, Tooltip } from "react-bootstrap";
import QuestionCircleIcon from "../../assets/icons/question-circle.svg?react";

const AddressInformation = ({ 
  label, 
  helpText,
  language, 
  onLanguageChange, 
  onRemove, 
  onAdd, 
  showRemoveButton = false, 
  showAddButton = false,
  showLabel = false,
  // Address field values
  street = "",
  streetNumber = "",
  city = "",
  country = "",
  // Address field change handlers
  onStreetChange,
  onStreetNumberChange,
  onCityChange,
  onCountryChange
}) => {
  return (
    <div className="mb-3">
      {showLabel && (
        <h5 className="mb-3" style={{ display: "flex", alignItems: "center", gap: "6px" }}>
          {label}
          {helpText && (
            <OverlayTrigger
              placement="top"
              overlay={<Tooltip id={`tooltip-${label}`}>{helpText}</Tooltip>}
            >
              <span
                style={{
                  display: "inline-flex",
                  cursor: "pointer",
                  transform: "scale(1.2)"
                }}
              >
                <QuestionCircleIcon
                  style={{
                    fill: "white"
                  }}
                />
              </span>
            </OverlayTrigger>
          )}
        </h5>
      )}
      <Form.Group as={Row} className="mb-2">
        <Col sm={3}>
          <Form.Select 
            value={language} 
            onChange={(e) => onLanguageChange(e.target.value)}
            className="white-caret"
          >
            <option value="English">English</option>
            <option value="German">German</option>
            <option value="French">French</option>
            <option value="Spanish">Spanish</option>
            <option value="Italian">Italian</option>
            <option value="Dutch">Dutch</option>
          </Form.Select>
        </Col>
        {showRemoveButton && (
          <Col sm={2}>
            <Button variant="outline-danger" size="sm" onClick={onRemove}>
              Remove
            </Button>
          </Col>
        )}
      </Form.Group>

      <Form.Group as={Row} className="mb-2">
        <Col md={6}>
          <Form.Control 
            placeholder="ex. 1234 Elm Street" 
            value={street}
            onChange={(e) => onStreetChange && onStreetChange(e.target.value)}
          />
        </Col>
        <Col md={6}>
          <Form.Control 
            placeholder="ex. 1234" 
            value={streetNumber}
            onChange={(e) => onStreetNumberChange && onStreetNumberChange(e.target.value)}
          />
        </Col>
      </Form.Group>

      <Form.Group as={Row} className="mb-2">
        <Col md={6}>
          <Form.Control 
            placeholder="ex. Sampleville" 
            value={city}
            onChange={(e) => onCityChange && onCityChange(e.target.value)}
          />
        </Col>
        <Col md={6}>
          <Form.Control 
            placeholder="ex. DE" 
            value={country}
            onChange={(e) => onCountryChange && onCountryChange(e.target.value)}
          />
        </Col>
      </Form.Group>

      {showAddButton && (
        <Button 
          onClick={onAdd}
          style={{
            backgroundColor: "#003368",
            border: "2px solid #1A4D82",
            color: "white"
          }}
        >
          + Add
        </Button>
      )}
    </div>
  );
};

export default AddressInformation;
