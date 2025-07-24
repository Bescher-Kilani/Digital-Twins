import React from "react";
import { Form, OverlayTrigger, Tooltip } from "react-bootstrap";
import QuestionCircleIcon from "../../assets/icons/question-circle.svg?react";

const Prop = ({ label, placeholder, helpText, type = "text", value, onChange, className = "mb-4" }) => {
  return (
    <Form.Group className={className}>
      <Form.Label className="text-white" style={{ display: "flex", alignItems: "center", gap: "6px" }}>
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
      <Form.Control
        type={type}
        placeholder={placeholder}
        value={value}
        onChange={onChange}
      />
    </Form.Group>
  );
};

export default Prop;
