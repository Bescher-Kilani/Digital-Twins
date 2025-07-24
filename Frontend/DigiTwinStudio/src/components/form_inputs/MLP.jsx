import React from "react";
import { Form, Row, Col, Button, OverlayTrigger, Tooltip } from "react-bootstrap";
import QuestionCircleIcon from "../../assets/icons/question-circle.svg?react";

const MLP = ({ 
  label, 
  placeholder, 
  helpText,
  language, 
  onLanguageChange, 
  onAdd, 
  onRemove, 
  showAddButton = false, 
  showRemoveButton = false,
  showLabel = false,
  value = "",
  onChange = () => {}
}) => {
  const availableLanguages = ["English", "German", "French", "Spanish", "Italian", "Dutch"];

  return (
    <div className="mb-3">
      {showLabel && (
        <Form.Label className="text-white" style={{ display: "flex", alignItems: "center", gap: "6px", marginBottom: "8px" }}>
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
        </Form.Label>
      )}
      <Row className="mb-2">
        <Col sm={3}>
          <Form.Select 
            value={language} 
            onChange={(e) => onLanguageChange(e.target.value)}
            className="white-caret"
          >
            {availableLanguages.map(lang => (
              <option key={lang} value={lang}>{lang}</option>
            ))}
          </Form.Select>
        </Col>
        <Col sm={7}>
          <Form.Control 
            placeholder={placeholder} 
            value={value}
            onChange={onChange}
          />
        </Col>
        <Col sm={2}>
          {showRemoveButton && (
            <Button variant="outline-secondary" onClick={onRemove}>
              Remove
            </Button>
          )}
        </Col>
      </Row>
      {showAddButton && (
        <Row>
          <Col>
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
          </Col>
        </Row>
      )}
    </div>
  );
};

export default MLP;
