import React from "react";
import { Form, Button, OverlayTrigger, Tooltip, Accordion, Badge } from "react-bootstrap";
import PlusLgIcon from "../../assets/icons/plus-lg.svg?react";
import QuestionCircleIcon from "../../assets/icons/question-circle.svg?react";
import Prop from "./Prop";
import MLP from "./MLP";
import FileInput from "./FileInput";

const Entity = ({ 
  label, 
  helpText, 
  value = [], 
  onChange, 
  entityTemplate,
  className = "",
  showLabel = true,
  showAddButton = true,
  showRemoveButton = true,
  onAdd,
  onRemove
}) => {

  // Generate a new entity instance based on the template
  const generateNewEntity = () => {
    const newEntity = {
      id: Date.now(), // Simple ID generation
      entityType: entityTemplate?.entityType || "SelfManagedEntity",
      globalAssetId: entityTemplate?.globalAssetId || "",
      idShort: entityTemplate?.idShort || "",
      statements: {}
    };

    // Initialize statements based on template
    if (entityTemplate?.statements && Array.isArray(entityTemplate.statements)) {
      entityTemplate.statements.forEach(statement => {
        switch (statement.modelType) {
          case "Property":
            newEntity.statements[statement.idShort] = "";
            break;
          case "MultiLanguageProperty":
            newEntity.statements[statement.idShort] = [{ id: 1, language: "English", value: "" }];
            break;
          case "File":
            newEntity.statements[statement.idShort] = "";
            break;
          case "Entity":
            newEntity.statements[statement.idShort] = [];
            break;
          case "RelationshipElement":
            newEntity.statements[statement.idShort] = {
              first: { type: "GlobalReference", value: "" },
              second: { type: "GlobalReference", value: "" }
            };
            break;
          default:
            newEntity.statements[statement.idShort] = "";
        }
      });
    }

    return newEntity;
  };

  const handleAddEntity = () => {
    const newEntity = generateNewEntity();
    const updatedValue = [...value, newEntity];
    onChange(updatedValue);
    if (onAdd) onAdd();
  };

  const handleRemoveEntity = (entityId) => {
    const updatedValue = value.filter(entity => entity.id !== entityId);
    onChange(updatedValue);
    if (onRemove) onRemove();
  };

  const handleEntityUpdate = (entityId, fieldName, fieldValue) => {
    const updatedValue = value.map(entity => 
      entity.id === entityId 
        ? { ...entity, statements: { ...entity.statements, [fieldName]: fieldValue } }
        : entity
    );
    onChange(updatedValue);
  };

  const handleEntityMetaUpdate = (entityId, metaField, metaValue) => {
    const updatedValue = value.map(entity => 
      entity.id === entityId 
        ? { ...entity, [metaField]: metaValue }
        : entity
    );
    onChange(updatedValue);
  };

  const renderStatementField = (statement, entityId, entityData) => {
    const fieldValue = entityData.statements[statement.idShort];

    switch (statement.modelType) {
      case "Property":
        return (
          <Prop
            key={statement.idShort}
            label={statement.idShort}
            placeholder={statement.value || `Enter ${statement.idShort}`}
            helpText={statement.description ? 
                     (Array.isArray(statement.description) ? 
                      statement.description[0]?.text || "" : 
                      statement.description) : 
                     `Property: ${statement.idShort}`}
            type={statement.valueType === "xs:date" ? "date" : "text"}
            value={fieldValue || ""}
            onChange={(e) => handleEntityUpdate(entityId, statement.idShort, e.target.value)}
            className="mb-3"
          />
        );

      case "MultiLanguageProperty":
        return (
          <div key={statement.idShort} className="mb-3">
            {(fieldValue || []).map((entry, index) => (
              <MLP
                key={entry.id}
                label={statement.idShort}
                placeholder={statement.value && statement.value.length > 0 ? 
                            statement.value[0].text.replace(/"/g, '') : `Enter ${statement.idShort}`}
                helpText={statement.description ? 
                         (Array.isArray(statement.description) ? 
                          statement.description[0]?.text || "" : 
                          statement.description) : 
                         `Multi-language property: ${statement.idShort}`}
                language={entry.language}
                value={entry.value}
                onChange={(e) => {
                  const updatedEntries = fieldValue.map(mlEntry => 
                    mlEntry.id === entry.id ? { ...mlEntry, value: e.target.value } : mlEntry
                  );
                  handleEntityUpdate(entityId, statement.idShort, updatedEntries);
                }}
                onLanguageChange={(newLanguage) => {
                  const updatedEntries = fieldValue.map(mlEntry => 
                    mlEntry.id === entry.id ? { ...mlEntry, language: newLanguage } : mlEntry
                  );
                  handleEntityUpdate(entityId, statement.idShort, updatedEntries);
                }}
                onAdd={() => {
                  const nextId = Math.max(...fieldValue.map(e => e.id)) + 1;
                  const updatedEntries = [...fieldValue, { id: nextId, language: "German", value: "" }];
                  handleEntityUpdate(entityId, statement.idShort, updatedEntries);
                }}
                onRemove={() => {
                  const updatedEntries = fieldValue.filter(mlEntry => mlEntry.id !== entry.id);
                  handleEntityUpdate(entityId, statement.idShort, updatedEntries);
                }}
                showLabel={index === 0}
                showAddButton={index === fieldValue.length - 1}
                showRemoveButton={fieldValue.length > 1}
              />
            ))}
          </div>
        );

      case "File":
        return (
          <FileInput
            key={statement.idShort}
            label={statement.idShort}
            helpText={statement.description ? 
                     (Array.isArray(statement.description) ? 
                      statement.description[0]?.text || "" : 
                      statement.description) : 
                     `File: ${statement.idShort}`}
            contentType={statement.contentType}
            onChange={(fileName) => handleEntityUpdate(entityId, statement.idShort, fileName)}
            className="mb-3"
          />
        );

      case "RelationshipElement":
        return (
          <div key={statement.idShort} className="mb-3">
            <Form.Label className="text-white">
              {statement.idShort}
              {statement.description && (
                <OverlayTrigger
                  placement="top"
                  overlay={
                    <Tooltip>
                      {Array.isArray(statement.description) ? 
                       statement.description[0]?.text || "" : 
                       statement.description}
                    </Tooltip>
                  }
                >
                  <span
                    style={{
                      display: "inline-flex",
                      cursor: "pointer",
                      transform: "scale(1.2)"
                    }}
                    className="ms-2"
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
            <div className="p-3 border border-secondary rounded" style={{ background: "#003368" }}>
              <div className="mb-2">
                <Form.Label className="text-white small">First Reference</Form.Label>
                <Form.Control
                  type="text"
                  placeholder="Enter first reference"
                  value={fieldValue?.first?.value || ""}
                  onChange={(e) => {
                    const updatedValue = {
                      ...fieldValue,
                      first: { type: "GlobalReference", value: e.target.value }
                    };
                    handleEntityUpdate(entityId, statement.idShort, updatedValue);
                  }}
                  style={{ backgroundColor: "#003368", border: "2px solid #1A4D82", color: "white" }}
                />
              </div>
              <div>
                <Form.Label className="text-white small">Second Reference</Form.Label>
                <Form.Control
                  type="text"
                  placeholder="Enter second reference"
                  value={fieldValue?.second?.value || ""}
                  onChange={(e) => {
                    const updatedValue = {
                      ...fieldValue,
                      second: { type: "GlobalReference", value: e.target.value }
                    };
                    handleEntityUpdate(entityId, statement.idShort, updatedValue);
                  }}
                  style={{ backgroundColor: "#003368", border: "2px solid #1A4D82", color: "white" }}
                />
              </div>
            </div>
          </div>
        );

      case "Entity":
        // Nested entities - render them as a simplified entity form
        return (
          <div key={statement.idShort} className="mb-3">
            <Form.Label className="text-white">
              {statement.idShort}
              {statement.description && (
                <OverlayTrigger
                  placement="top"
                  overlay={
                    <Tooltip>
                      {Array.isArray(statement.description) ? 
                       statement.description[0]?.text || "" : 
                       statement.description}
                    </Tooltip>
                  }
                >
                  <span
                    style={{
                      display: "inline-flex",
                      cursor: "pointer",
                      transform: "scale(1.2)"
                    }}
                    className="ms-2"
                  >
                    <QuestionCircleIcon
                      style={{
                        fill: "white"
                      }}
                    />
                  </span>
                </OverlayTrigger>
              )}
              <Badge bg="info" className="ms-2">{statement.entityType || "Entity"}</Badge>
            </Form.Label>
            <div className="p-3 border border-info rounded" style={{ background: "#003368" }}>
              {/* Render nested entity as a collection of entities */}
              <Entity
                label=""
                helpText=""
                value={fieldValue || []}
                onChange={(newValue) => handleEntityUpdate(entityId, statement.idShort, newValue)}
                entityTemplate={statement}
                showLabel={false}
                className=""
              />
            </div>
          </div>
        );

      default:
        return (
          <div key={statement.idShort} className="mb-3">
            <div className="p-3 border border-warning rounded" style={{ background: "#003368" }}>
              <h6 className="text-warning mb-2">
                {statement.idShort} 
                <small className="text-white ms-2">({statement.modelType})</small>
              </h6>
              <p className="text-white small mb-0">
                This statement type is not yet supported in the form builder.
              </p>
            </div>
          </div>
        );
    }
  };

  return (
    <div className={className}>
      {showLabel && (
        <Form.Label className="text-white mb-3">
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
                className="ms-2"
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

      <div className="mb-3">
        {value.length > 0 ? (
          <Accordion>
            {value.map((entity, index) => (
              <Accordion.Item key={entity.id} eventKey={index.toString()}>
                <Accordion.Header>
                  <div className="d-flex align-items-center gap-2 w-100">
                    <Badge bg="info">{entity.entityType}</Badge>
                    <span>{entity.idShort || `Entity ${index + 1}`}</span>
                  </div>
                </Accordion.Header>
                <Accordion.Body style={{ background: "#003368" }}>
                  {/* Remove button inside accordion body */}
                  {showRemoveButton && (
                    <div className="d-flex justify-content-end mb-3">
                      <Button
                        variant="outline-danger"
                        size="sm"
                        onClick={() => handleRemoveEntity(entity.id)}
                      >
                        Remove Entity
                      </Button>
                    </div>
                  )}
                  
                  {/* Entity metadata */}
                  <div className="mb-3">
                    <Prop
                      label="Entity ID Short"
                      placeholder="Enter entity identifier"
                      helpText="Short identifier for this entity"
                      value={entity.idShort || ""}
                      onChange={(e) => handleEntityMetaUpdate(entity.id, 'idShort', e.target.value)}
                      className="mb-3"
                    />
                    <Prop
                      label="Global Asset ID"
                      placeholder="Enter global asset ID"
                      helpText="Global identifier for the asset this entity represents"
                      value={entity.globalAssetId || ""}
                      onChange={(e) => handleEntityMetaUpdate(entity.id, 'globalAssetId', e.target.value)}
                      className="mb-3"
                    />
                  </div>

                  {/* Entity statements */}
                  {entityTemplate?.statements && entityTemplate.statements.length > 0 && (
                    <div>
                      <h6 className="text-white mb-3">Statements</h6>
                      {entityTemplate.statements.map(statement => 
                        renderStatementField(statement, entity.id, entity)
                      )}
                    </div>
                  )}
                </Accordion.Body>
              </Accordion.Item>
            ))}
          </Accordion>
        ) : (
          <div className="p-3 border border-secondary rounded text-center" style={{ background: "#003368" }}>
            <p className="text-white mb-0">No entities added yet</p>
          </div>
        )}
      </div>

      {showAddButton && (
        <Button
          variant="outline-light"
          onClick={handleAddEntity}
          className="d-flex align-items-center gap-2"
        >
          <PlusLgIcon style={{ fill: "white", width: "16px", height: "16px" }} />
          Add {label || "Entity"}
        </Button>
      )}
    </div>
  );
};

export default Entity;
