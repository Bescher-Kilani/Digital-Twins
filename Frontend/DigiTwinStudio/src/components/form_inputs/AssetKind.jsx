import React from "react";
import { Form, OverlayTrigger, Tooltip } from "react-bootstrap";
import QuestionCircleIcon from "../../assets/icons/question-circle.svg?react";

const AssetKind = ({
  label,
  helpText,
  value = "",
  onChange = () => { }
}) => {

  return (
    <div className="mb-3">
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
      <Form.Select
        value={value}
        onChange={(e) => onChange(e.target.value)}
        className="white-caret"
      >
        <option value="Type">Type</option>
        <option value="Instance">Instance</option>
        <option value="Role">Role</option>
        <option value="NotApplicable">NotApplicable</option>
      </Form.Select>
    </div>
  );
};

export default AssetKind;
