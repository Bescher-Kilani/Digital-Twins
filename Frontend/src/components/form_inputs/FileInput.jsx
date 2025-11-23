import React from "react";
import { Form, OverlayTrigger, Tooltip } from "react-bootstrap";
import QuestionCircleIcon from "../../assets/icons/question-circle.svg?react";

export default function FileInput({
  label,
  helpText,
  contentType,
  onChange,
  className = ""
}) {
  return (
    <div className={`mb-4 ${className}`}>
      <Form.Group>
        <Form.Label className="text-white" style={{ display: "flex", alignItems: "center", gap: "6px" }}>
          {label}
          {helpText && (
            <OverlayTrigger
              placement="top"
              overlay={<Tooltip>{helpText}</Tooltip>}
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
          type="file"
          accept={contentType ? contentType.split(',').map(type => type.trim()).join(',') : "*/*"}
          onChange={(e) => {
            const file = e.target.files[0];
            onChange(file ? file.name : "");
          }}
          className="bg-dark text-white border-secondary"
        />
        <Form.Text className="text-white">
          {contentType && `Accepted types: ${contentType}`}
        </Form.Text>
      </Form.Group>
    </div>
  );
}
