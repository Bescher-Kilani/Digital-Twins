import React from "react";
import { Form, Row, Col, Button, OverlayTrigger, Tooltip } from "react-bootstrap";
import QuestionCircleIcon from "../../assets/icons/question-circle.svg?react";

const SpecificAssetId = ({
  label,
  placeholder1 = "name",
  placeholder2 = "Value",
  helpText,
  onAdd,
  onRemove,
  showAddButton = false,
  showRemoveButton = true,
  showLabel = false,
  value1 = "",
  value2 = "",
  onChange1 = () => {},
  onChange2 = () => {}
}) => {
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
                <QuestionCircleIcon style={{ fill: "white" }} />
              </span>
            </OverlayTrigger>
          )}
        </Form.Label>
      )}

      <Row>
        <Col sm={5}>
          <Form.Control
            type="text"
            placeholder={placeholder1}
            value={value1}
            onChange={onChange1}
          />
        </Col>
        <Col sm={5}>
          <Form.Control
            placeholder={placeholder2}
            value={value2}
            onChange={onChange2}
          />
        </Col>

        {showRemoveButton && (
          <Col sm={2}>
            <Button variant="outline-secondary" onClick={onRemove} className="w-100">
              Remove
            </Button>
          </Col>
        )}

        {showAddButton && (
          <Col sm={2}>
            <Button
              onClick={onAdd}
              className="w-100"
              style={{
                backgroundColor: "#003368",
                border: "2px solid #1A4D82",
                color: "white"
              }}
            >
              + Add
            </Button>
          </Col>
        )}
      </Row>
    </div>
  );
};


export default SpecificAssetId;
