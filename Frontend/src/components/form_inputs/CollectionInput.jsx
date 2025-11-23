import React from "react";
import { Card, Button, Form, OverlayTrigger, Tooltip, Accordion, Badge } from "react-bootstrap";
import Prop from "./Prop";
import MLP from "./MLP";
import FileInput from "./FileInput";
import PlusLgIcon from "../../assets/icons/plus-lg.svg?react";
import QuestionCircleIcon from "../../assets/icons/question-circle.svg?react";
// Using a simple × symbol instead of icon for remove button

export default function CollectionInput({
  label,
  helpText,
  value = [],
  onChange,
  // eslint-disable-next-line no-unused-vars
  collectionType = "SubmodelElementCollection", // or "SubmodelElementList"
  elementTemplate,
  className = ""
}) {
  // Add a new collection entry
  const addEntry = () => {
    const newEntry = {
      id: Date.now(), // Use timestamp as unique ID
      data: initializeEntryData(elementTemplate)
    };
    onChange([...value, newEntry]);
  };

  // Remove a collection entry
  const removeEntry = (entryId) => {
    onChange(value.filter(entry => entry.id !== entryId));
  };

  // Update a specific entry's data
  const updateEntry = (entryId, field, fieldValue) => {
    onChange(value.map(entry => 
      entry.id === entryId 
        ? { ...entry, data: { ...entry.data, [field]: fieldValue } }
        : entry
    ));
  };

  // Initialize data structure for a new entry based on template
  const initializeEntryData = (template) => {
    const data = {};
    if (template && template.value && Array.isArray(template.value)) {
      template.value.forEach(element => {
        switch (element.modelType) {
          case "Property":
            data[element.idShort] = "";
            break;
          case "MultiLanguageProperty":
            data[element.idShort] = [{ id: 1, language: "English", value: "" }];
            break;
          case "File":
            data[element.idShort] = "";
            break;
          case "SubmodelElementCollection":
            // For nested collections, initialize based on their nested elements
            if (element.value && Array.isArray(element.value)) {
              const nestedData = {};
              element.value.forEach(nestedElement => {
                switch (nestedElement.modelType) {
                  case "Property":
                    nestedData[nestedElement.idShort] = "";
                    break;
                  case "MultiLanguageProperty":
                    nestedData[nestedElement.idShort] = [{ id: 1, language: "English", value: "" }];
                    break;
                  case "File":
                    nestedData[nestedElement.idShort] = "";
                    break;
                  default:
                    nestedData[nestedElement.idShort] = "";
                }
              });
              data[element.idShort] = nestedData;
            } else {
              data[element.idShort] = {};
            }
            break;
          case "SubmodelElementList":
            // For nested lists, initialize based on their template structure
            data[element.idShort] = [];
            break;
          default:
            data[element.idShort] = "";
        }
      });
    }
    return data;
  };

  // Render a field within a collection entry
  const renderCollectionField = (element, entryId, entryData) => {
    const fieldData = entryData[element.idShort];
    
    switch (element.modelType) {
      case "Property":
        return (
          <Prop
            key={element.idShort}
            label={element.idShort}
            placeholder={element.value || `Enter ${element.idShort}`}
            helpText={element.description ? 
                     (Array.isArray(element.description) ? 
                      element.description[0]?.text || "" : 
                      element.description) : 
                     `Property: ${element.idShort}`}
            type={element.valueType === "xs:date" ? "date" : "text"}
            value={fieldData || ""}
            onChange={(e) => updateEntry(entryId, element.idShort, e.target.value)}
            className="mb-3"
          />
        );

      case "MultiLanguageProperty":
        return (
          <div key={element.idShort} className="mb-3">
            {(fieldData || [{ id: 1, language: "English", value: "" }]).map((mlpEntry, index) => (
              <MLP
                key={mlpEntry.id}
                label={element.idShort}
                placeholder={element.value && element.value.length > 0 ? 
                            element.value[0].text.replace(/"/g, '') : `Enter ${element.idShort}`}
                helpText={element.description ? 
                         (Array.isArray(element.description) ? 
                          element.description[0]?.text || "" : 
                          element.description) : 
                         `Multi-language property: ${element.idShort}`}
                language={mlpEntry.language}
                value={mlpEntry.value}
                onChange={(e) => {
                  const updatedMLP = fieldData.map(item => 
                    item.id === mlpEntry.id ? { ...item, value: e.target.value } : item
                  );
                  updateEntry(entryId, element.idShort, updatedMLP);
                }}
                onLanguageChange={(newLanguage) => {
                  const updatedMLP = fieldData.map(item => 
                    item.id === mlpEntry.id ? { ...item, language: newLanguage } : item
                  );
                  updateEntry(entryId, element.idShort, updatedMLP);
                }}
                onAdd={() => {
                  const availableLanguages = ["German", "French", "Spanish", "Italian", "Dutch"];
                  const usedLanguages = fieldData.map(item => item.language);
                  const nextLanguage = availableLanguages.find(lang => !usedLanguages.includes(lang)) || "German";
                  const nextId = Math.max(...fieldData.map(item => item.id)) + 1;
                  const updatedMLP = [...fieldData, { id: nextId, language: nextLanguage, value: "" }];
                  updateEntry(entryId, element.idShort, updatedMLP);
                }}
                onRemove={() => {
                  const updatedMLP = fieldData.filter(item => item.id !== mlpEntry.id);
                  updateEntry(entryId, element.idShort, updatedMLP);
                }}
                showLabel={index === 0}
                showAddButton={index === fieldData.length - 1}
                showRemoveButton={fieldData.length > 1}
              />
            ))}
          </div>
        );

      case "File":
        return (
          <FileInput
            key={element.idShort}
            label={element.idShort}
            helpText={element.description ? 
                     (Array.isArray(element.description) ? 
                      element.description[0]?.text || "" : 
                      element.description) : 
                     `File: ${element.idShort} (${element.contentType || 'unknown type'})`}
            contentType={element.contentType}
            onChange={(fileName) => updateEntry(entryId, element.idShort, fileName)}
            className="mb-3"
          />
        );

      case "SubmodelElementCollection":
        // For nested collections, render their fields directly instead of creating another CollectionInput
        return (
          <div key={element.idShort} className="mb-3">
            <h6 className="text-info mb-2" style={{ display: "flex", alignItems: "center", gap: "6px" }}>
              {element.idShort}
              {element.description && (
                <OverlayTrigger
                  placement="top"
                  overlay={<Tooltip>{Array.isArray(element.description) ? element.description[0]?.text || "" : element.description}</Tooltip>}
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
            </h6>
            <div className="border border-info rounded p-2 mb-2">
              {element.value && Array.isArray(element.value) ? (
                element.value.map(nestedElement => {
                  // Use the nested collection data for rendering
                  const nestedEntryData = fieldData || {};
                  switch (nestedElement.modelType) {
                    case "Property": {
                      return (
                        <Prop
                          key={nestedElement.idShort}
                          label={nestedElement.idShort}
                          placeholder={nestedElement.value || `Enter ${nestedElement.idShort}`}
                          helpText={nestedElement.description ? 
                                   (Array.isArray(nestedElement.description) ? 
                                    nestedElement.description[0]?.text || "" : 
                                    nestedElement.description) : 
                                   `Property: ${nestedElement.idShort}`}
                          type={nestedElement.valueType === "xs:date" ? "date" : "text"}
                          value={nestedEntryData[nestedElement.idShort] || ""}
                          onChange={(e) => {
                            const updatedNestedData = { ...nestedEntryData, [nestedElement.idShort]: e.target.value };
                            updateEntry(entryId, element.idShort, updatedNestedData);
                          }}
                          className="mb-3"
                        />
                      );
                    }
                    case "MultiLanguageProperty": {
                      const mlpData = nestedEntryData[nestedElement.idShort] || [{ id: 1, language: "English", value: "" }];
                      return mlpData.map((mlpEntry, index) => (
                        <MLP
                          key={`${nestedElement.idShort}-${mlpEntry.id}`}
                          label={nestedElement.idShort}
                          placeholder={nestedElement.value && nestedElement.value.length > 0 ? 
                                      nestedElement.value[0].text.replace(/"/g, '') : `Enter ${nestedElement.idShort}`}
                          helpText={nestedElement.description ? 
                                   (Array.isArray(nestedElement.description) ? 
                                    nestedElement.description[0]?.text || "" : 
                                    nestedElement.description) : 
                                   `Multi-language property: ${nestedElement.idShort}`}
                          language={mlpEntry.language}
                          value={mlpEntry.value}
                          onChange={(e) => {
                            const updatedMLP = mlpData.map(item => 
                              item.id === mlpEntry.id ? { ...item, value: e.target.value } : item
                            );
                            const updatedNestedData = { ...nestedEntryData, [nestedElement.idShort]: updatedMLP };
                            updateEntry(entryId, element.idShort, updatedNestedData);
                          }}
                          onLanguageChange={(newLanguage) => {
                            const updatedMLP = mlpData.map(item => 
                              item.id === mlpEntry.id ? { ...item, language: newLanguage } : item
                            );
                            const updatedNestedData = { ...nestedEntryData, [nestedElement.idShort]: updatedMLP };
                            updateEntry(entryId, element.idShort, updatedNestedData);
                          }}
                          onAdd={() => {
                            const availableLanguages = ["German", "French", "Spanish", "Italian", "Dutch"];
                            const usedLanguages = mlpData.map(item => item.language);
                            const nextLanguage = availableLanguages.find(lang => !usedLanguages.includes(lang)) || "German";
                            const nextId = Math.max(...mlpData.map(item => item.id)) + 1;
                            const updatedMLP = [...mlpData, { id: nextId, language: nextLanguage, value: "" }];
                            const updatedNestedData = { ...nestedEntryData, [nestedElement.idShort]: updatedMLP };
                            updateEntry(entryId, element.idShort, updatedNestedData);
                          }}
                          onRemove={() => {
                            const updatedMLP = mlpData.filter(item => item.id !== mlpEntry.id);
                            const updatedNestedData = { ...nestedEntryData, [nestedElement.idShort]: updatedMLP };
                            updateEntry(entryId, element.idShort, updatedNestedData);
                          }}
                          showLabel={index === 0}
                          showAddButton={index === mlpData.length - 1}
                          showRemoveButton={mlpData.length > 1}
                        />
                      ));
                    }
                    case "File": {
                      return (
                        <FileInput
                          key={nestedElement.idShort}
                          label={nestedElement.idShort}
                          helpText={nestedElement.description ? 
                                   (Array.isArray(nestedElement.description) ? 
                                    nestedElement.description[0]?.text || "" : 
                                    nestedElement.description) : 
                                   `File: ${nestedElement.idShort} (${nestedElement.contentType || 'unknown type'})`}
                          contentType={nestedElement.contentType}
                          onChange={(fileName) => {
                            const updatedNestedData = { ...nestedEntryData, [nestedElement.idShort]: fileName };
                            updateEntry(entryId, element.idShort, updatedNestedData);
                          }}
                          className="mb-3"
                        />
                      );
                    }
                    default: {
                      return (
                        <div key={nestedElement.idShort} className="mb-3">
                          <Form.Group>
                            <Form.Label className="text-white small">
                              {nestedElement.idShort} ({nestedElement.modelType})
                            </Form.Label>
                            <Form.Control
                              type="text"
                              placeholder={`Enter ${nestedElement.idShort}`}
                              value={nestedEntryData[nestedElement.idShort] || ""}
                              onChange={(e) => {
                                const updatedNestedData = { ...nestedEntryData, [nestedElement.idShort]: e.target.value };
                                updateEntry(entryId, element.idShort, updatedNestedData);
                              }}
                              style={{ backgroundColor: "#003368", border: "2px solid #1A4D82", color: "white" }}
                            />
                          </Form.Group>
                        </div>
                      );
                    }
                  }
                })
              ) : (
                <p className="text-muted small mb-0">No nested elements found</p>
              )}
            </div>
          </div>
        );

      case "SubmodelElementList":
        // For nested lists, render their template fields directly
        return (
          <div key={element.idShort} className="mb-3">
            <h6 className="text-warning mb-2" style={{ display: "flex", alignItems: "center", gap: "6px" }}>
              {element.idShort}
              {element.description && (
                <OverlayTrigger
                  placement="top"
                  overlay={<Tooltip>{Array.isArray(element.description) ? element.description[0]?.text || "" : element.description}</Tooltip>}
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
            </h6>
            <div className="border border-warning rounded p-2 mb-2">
              {element.value && Array.isArray(element.value) ? (
                element.value.map(listElement => 
                  renderCollectionField(listElement, entryId, entryData)
                )
              ) : (
                <p className="text-muted small mb-0">No list elements found</p>
              )}
            </div>
          </div>
        );

      default:
        return (
          <div key={element.idShort} className="mb-3">
            <Form.Group>
              <Form.Label className="text-white small">
                {element.idShort} ({element.modelType})
              </Form.Label>
              <Form.Control
                type="text"
                placeholder={`Enter ${element.idShort}`}
                value={fieldData || ""}
                onChange={(e) => updateEntry(entryId, element.idShort, e.target.value)}
                style={{ backgroundColor: "#003368", border: "2px solid #1A4D82", color: "white" }}
              />
            </Form.Group>
          </div>
        );
    }
  };

  return (
    <div className={`mb-4 ${className}`}>
      <div className="d-flex justify-content-between align-items-center mb-3">
        <h6 className="text-white mb-0" style={{ display: "flex", alignItems: "center", gap: "6px" }}>
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
        </h6>
        <Button
          variant="outline-primary"
          size="sm"
          onClick={addEntry}
          className="d-flex align-items-center gap-1"
        >
          <PlusLgIcon style={{ fill: "currentColor", width: "14px", height: "14px" }} />
          Add Entry
        </Button>
      </div>

      {value.length === 0 && (
        <div className="p-3 border border-secondary rounded text-center" style={{ background: "#003368" }}>
          <p className="text-white mb-0 small">
            No entries added yet. Click "Add Entry" to create the first entry.
          </p>
        </div>
      )}

      {value.length > 0 && (
        <Accordion>
          {value.map((entry, index) => (
            <Accordion.Item key={entry.id} eventKey={index.toString()}>
              <Accordion.Header>
                <div className="d-flex align-items-center gap-2 w-100">
                  <Badge bg="primary">Collection</Badge>
                  <span>Entry {index + 1}</span>
                </div>
              </Accordion.Header>
              <Accordion.Body style={{ background: "#003368" }}>
                {/* Remove button inside accordion body */}
                <div className="d-flex justify-content-end mb-3">
                  <Button
                    variant="outline-danger"
                    size="sm"
                    onClick={() => removeEntry(entry.id)}
                  >
                    Remove Entry
                  </Button>
                </div>
                
                {/* Collection fields */}
                {elementTemplate && elementTemplate.value && Array.isArray(elementTemplate.value) ? (
                  elementTemplate.value.map(element => 
                    renderCollectionField(element, entry.id, entry.data)
                  )
                ) : (
                  <p className="text-white small mb-0">
                    No template structure available for this collection.
                  </p>
                )}
              </Accordion.Body>
            </Accordion.Item>
          ))}
        </Accordion>
      )}
    </div>
  );
}
