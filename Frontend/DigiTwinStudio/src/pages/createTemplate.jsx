import React, { useState, useEffect } from "react";
import { Container, Row, Col, Button, Form, Card, OverlayTrigger, Tooltip } from "react-bootstrap";
import { Link, useNavigate, useLocation } from "react-router-dom";
import AddressInformation from "../components/form_inputs/AddressInformation";
import Prop from "../components/form_inputs/Prop";
import MLP from "../components/form_inputs/MLP";
import FileInput from "../components/form_inputs/FileInput";
import CollectionInput from "../components/form_inputs/CollectionInput";
import ChevronLeftIcon from "../assets/icons/chevron-left.svg?react";
import ChevronRightIcon from "../assets/icons/chevron-right.svg?react";
import ChevronDownIcon from "../assets/icons/chevron-down.svg?react";
import FloppyFillIcon from "../assets/icons/floppy-fill.svg?react";
import QuestionCircleIcon from "../assets/icons/question-circle.svg?react";
import "../styles/createTemplate.css";

// Function to extract cardinality from qualifiers
const extractCardinality = (element) => {
  if (!element.qualifiers || !Array.isArray(element.qualifiers)) {
    return "Unknown";
  }
  
  const cardinalityQualifier = element.qualifiers.find(
    qualifier => qualifier.type === "SMT/Cardinality"
  );
  
  return cardinalityQualifier?.value || "Unknown";
};

// Function to parse AAS submodel elements and convert them to form configuration
const parseSubmodelElements = (submodelElements) => {
  const fields = [];
  
  if (!submodelElements || !Array.isArray(submodelElements)) {
    return fields;
  }
  
  submodelElements.forEach(element => {
    const cardinality = extractCardinality(element);
    
    switch (element.modelType) {
      case "Property":
        fields.push({
          name: element.idShort,
          type: "prop",
          label: element.idShort,
          placeholder: element.value || `Enter ${element.idShort}`,
          tooltip: element.description ? 
                   (Array.isArray(element.description) ? 
                    element.description[0]?.text || "" : 
                    element.description) : 
                   `Property: ${element.idShort}`,
          valueType: element.valueType || "xs:string",
          cardinality: cardinality,
          originalElement: element
        });
        break;
        
      case "MultiLanguageProperty":
        fields.push({
          name: element.idShort,
          type: "multiLanguage",
          label: element.idShort,
          placeholder: element.value && element.value.length > 0 ? 
                      element.value[0].text.replace(/"/g, '') : `Enter ${element.idShort}`,
          tooltip: element.description ? 
                   (Array.isArray(element.description) ? 
                    element.description[0]?.text || "" : 
                    element.description) : 
                   `Multi-language property: ${element.idShort}`,
          cardinality: cardinality,
          originalElement: element
        });
        break;
        
      case "File":
        fields.push({
          name: element.idShort,
          type: "file",
          label: element.idShort,
          placeholder: `Upload ${element.contentType || 'file'}`,
          tooltip: element.description ? 
                   (Array.isArray(element.description) ? 
                    element.description[0]?.text || "" : 
                    element.description) : 
                   `File: ${element.idShort} (${element.contentType || 'unknown type'})`,
          contentType: element.contentType,
          cardinality: cardinality,
          originalElement: element
        });
        break;
        
      case "SubmodelElementList":
        fields.push({
          name: element.idShort,
          type: "list",
          label: element.idShort,
          tooltip: element.description ? 
                   (Array.isArray(element.description) ? 
                    element.description[0]?.text || "" : 
                    element.description) : 
                   `List: ${element.idShort}`,
          cardinality: cardinality,
          elementTemplate: element,
          originalElement: element
        });
        break;
        
      case "SubmodelElementCollection":
        // Handle simple collections (like AddressInformation) that don't have nested elements in template
        if (element.idShort === "AddressInformation" && (!element.value || element.value.length === 0)) {
          fields.push({
            name: element.idShort,
            type: "address",
            label: "Address Information",
            tooltip: element.description ? 
                     (Array.isArray(element.description) ? 
                      element.description[0]?.text || "" : 
                      element.description) : 
                     "Address information details",
            cardinality: cardinality,
            originalElement: element
          });
        } else {
          // Handle complex collections with multiple elements
          fields.push({
            name: element.idShort,
            type: "collection",
            label: element.idShort,
            tooltip: element.description ? 
                     (Array.isArray(element.description) ? 
                      element.description[0]?.text || "" : 
                      element.description) : 
                     `Collection: ${element.idShort}`,
            cardinality: cardinality,
            elementTemplate: element,
            originalElement: element
          });
        }
        break;
        
      default:
        // Handle unknown types as properties
        fields.push({
          name: element.idShort,
          type: "prop",
          label: element.idShort,
          placeholder: "",
          tooltip: `${element.modelType}: ${element.idShort}`,
          cardinality: cardinality,
          originalElement: element
        });
    }
  });
  
  return fields;
};

// Function to get template configuration from selected template data
const getTemplateConfig = (selectedTemplate) => {
  if (!selectedTemplate || !selectedTemplate.templateData) {
    return {
      title: "Template Configuration",
      description: "",
      requiredFields: [],
      advancedFields: [],
      originalTemplate: null
    };
  }
  
  const templateData = selectedTemplate.templateData;
  const submodelElements = templateData.json?.submodelElements || [];
  const allFields = parseSubmodelElements(submodelElements);
  
  // Separate fields by cardinality - "One" and "OneToMany" are required fields
  const requiredFields = allFields.filter(field => 
    field.cardinality === "One" || field.cardinality === "OneToMany"
  );
  const advancedFields = allFields.filter(field => 
    field.cardinality !== "One" && field.cardinality !== "OneToMany"
  );
  
  return {
    title: templateData.name || selectedTemplate.title || "Template Configuration",
    description: selectedTemplate.description || "",
    requiredFields: requiredFields,
    advancedFields: advancedFields,
    originalTemplate: templateData
  };
};

export default function CreateTemplate() {
  const navigate = useNavigate();
  const location = useLocation();
  
  // Get selected template from location state (passed from /templates)
  const selectedTemplate = location.state?.selectedTemplate;
  
  // Check if we're editing an existing template
  const editingTemplate = location.state?.editingTemplate;
  
  // Form configuration from API - dynamically set based on selected template
  const [formConfig, setFormConfig] = useState(() => getTemplateConfig(selectedTemplate));
  
  // Unified form state - dynamically initialized from API
  const [formData, setFormData] = useState({});
  
  // Advanced fields state
  const [showAdvancedFields, setShowAdvancedFields] = useState(false);

  // TODO: Replace with actual API call to get template fields
  useEffect(() => {
    if (selectedTemplate) {
      // In real implementation, fetch template fields from backend
      // const fetchTemplateFields = async () => {
      //   const response = await fetch(`/api/templates/${selectedTemplate.id}/fields`);
      //   const templateConfig = await response.json();
      //   setFormConfig(templateConfig);
      // };
      // fetchTemplateFields();
      
      // For now, use parsed data from selected template
      const templateConfig = getTemplateConfig(selectedTemplate);
      setFormConfig(templateConfig);
      console.log('Selected template:', selectedTemplate);
    }
  }, [selectedTemplate]);

  // Initialize form data based on API configuration
  useEffect(() => {
    const initializeFormData = () => {
      const initialData = {};
      
      // Initialize both required and advanced fields
      const allFields = [...formConfig.requiredFields, ...formConfig.advancedFields];
      
      allFields.forEach(field => {
        switch (field.type) {
          case "prop": {
            // If editing, use existing data; otherwise initialize with empty value
            initialData[field.name] = editingTemplate?.data?.data?.[field.name] || "";
            break;
          }
          case "multiLanguage": {
            // If editing, use existing data; otherwise initialize with empty values
            initialData[field.name] = editingTemplate?.data?.data?.[field.name] || [{ id: 1, language: "English", value: "" }];
            break;
          }
          case "address": {
            initialData[field.name] = editingTemplate?.data?.data?.[field.name] || [{ 
              id: 1, 
              language: "English",
              street: "",
              streetNumber: "",
              city: "",
              country: ""
            }];
            break;
          }
          case "file": {
            initialData[field.name] = editingTemplate?.data?.data?.[field.name] || "";
            break;
          }
          case "list": {
            // Initialize lists with existing data or empty array
            initialData[field.name] = editingTemplate?.data?.data?.[field.name] || [];
            break;
          }
          case "collection": {
            // Initialize collections with existing data or appropriate defaults
            if (field.type === "address" || field.name === "AddressInformation") {
              // Keep the existing address handling
              initialData[field.name] = editingTemplate?.data?.data?.[field.name] || [{ 
                id: 1, 
                language: "English",
                street: "",
                streetNumber: "",
                city: "",
                country: ""
              }];
            } else {
              // For other collections, initialize with existing data or empty array
              initialData[field.name] = editingTemplate?.data?.data?.[field.name] || [];
            }
            break;
          }
          default: {
            initialData[field.name] = editingTemplate?.data?.data?.[field.name] || "";
          }
        }
      });
      
      setFormData(initialData);
    };

    if (formConfig.requiredFields.length > 0 || formConfig.advancedFields.length > 0) {
      initializeFormData();
    }
  }, [formConfig, editingTemplate]);

  // Generic field update function
  const updateField = (fieldName, value) => {
    setFormData(prev => ({ ...prev, [fieldName]: value }));
  };

  // Generic multi-language field update function
  const updateMultiLanguageField = (fieldName, id, updates) => {
    setFormData(prev => ({
      ...prev,
      [fieldName]: prev[fieldName].map(entry => 
        entry.id === id ? { ...entry, ...updates } : entry
      )
    }));
  };

  // Generic add entry function
  const addMultiLanguageEntry = (fieldName) => {
    const availableLanguages = ["German", "French", "Spanish", "Italian", "Dutch"];
    const usedLanguages = formData[fieldName].map(entry => entry.language);
    const nextLanguage = availableLanguages.find(lang => !usedLanguages.includes(lang)) || "German";
    
    // Get the next sequential ID
    const existingIds = formData[fieldName].map(entry => entry.id);
    const nextId = Math.max(...existingIds) + 1;
    
    const newEntry = {
      id: nextId,
      language: nextLanguage,
      ...(fieldName === 'addressInformation' ? {
        street: "",
        streetNumber: "",
        city: "",
        country: ""
      } : { value: "" })
    };
    
    setFormData(prev => ({
      ...prev,
      [fieldName]: [...prev[fieldName], newEntry]
    }));
  };

  // Generic remove entry function
  const removeMultiLanguageEntry = (fieldName, id) => {
    setFormData(prev => ({
      ...prev,
      [fieldName]: prev[fieldName].filter(entry => entry.id !== id)
    }));
  };

  // Handle save and redirect back to /create
  const handleSave = () => {
    const templateData = {
      title: formConfig.title,
      selectedTemplate: selectedTemplate,
      data: formData,
      savedAt: new Date().toISOString()
    };

    // Get the original form data passed from /create via /templates
    const originalFormData = location.state?.originalFormData || location.state?.formData;

    // If editing, include the template index to update
    const stateData = { 
      templateData: templateData,
      originalFormData: originalFormData
    };

    // If editing an existing template, pass the index
    if (editingTemplate) {
      stateData.editingTemplateIndex = editingTemplate.index;
    }

    // Navigate back to /create with template data and original form data
    navigate('/create', { 
      state: stateData
    });
  };

  // Handle back navigation with preserved form data
  const handleBackNavigation = () => {
    const originalFormData = location.state?.originalFormData || location.state?.formData;
    const currentSubmodelTemplates = location.state?.currentSubmodelTemplates || [];
    
    if (editingTemplate) {
      // If editing, go back to create page with preserved form data and templates
      // Need to pass templates here since we're mid-edit and sessionStorage might be stale
      navigate('/create', {
        state: {
          restoredFormData: originalFormData,
          restoredSubmodelTemplates: currentSubmodelTemplates
        }
      });
    } else {
      // If creating new template, go back to template selection with preserved form data
      // Don't need to pass templates since they're preserved in sessionStorage
      navigate('/templates', {
        state: {
          formData: originalFormData,
          currentSubmodelTemplates: currentSubmodelTemplates
        }
      });
    }
  };

  // Dynamic field rendering based on API configuration
  const renderField = (fieldConfig) => {
    const fieldData = formData[fieldConfig.name];
    
    if (fieldData === undefined) return null; // Wait for form data to initialize

    switch (fieldConfig.type) {
      case "prop":
        return (
          <Prop
            key={fieldConfig.name}
            label={fieldConfig.label}
            placeholder={fieldConfig.valueType === "xs:date" ? "YYYY-MM-DD" : fieldConfig.placeholder}
            helpText={fieldConfig.tooltip}
            type={fieldConfig.valueType === "xs:date" ? "date" : "text"}
            value={fieldData}
            onChange={(e) => updateField(fieldConfig.name, e.target.value)}
            className="mb-4"
          />
        );

      case "multiLanguage":
        return fieldData.map((entry, index) => (
          <MLP
            key={entry.id}
            label={fieldConfig.label}
            placeholder={fieldConfig.placeholder}
            helpText={fieldConfig.tooltip}
            language={entry.language}
            value={entry.value}
            onChange={(e) => updateMultiLanguageField(fieldConfig.name, entry.id, { value: e.target.value })}
            onLanguageChange={(newLanguage) => updateMultiLanguageField(fieldConfig.name, entry.id, { language: newLanguage })}
            onAdd={() => addMultiLanguageEntry(fieldConfig.name)}
            onRemove={() => removeMultiLanguageEntry(fieldConfig.name, entry.id)}
            showLabel={index === 0}
            showAddButton={index === fieldData.length - 1}
            showRemoveButton={fieldData.length > 1}
          />
        ));

      case "address":
        return fieldData.map((entry, index) => (
          <AddressInformation
            key={entry.id}
            label={fieldConfig.label}
            helpText={fieldConfig.tooltip}
            language={entry.language}
            street={entry.street || ""}
            streetNumber={entry.streetNumber || ""}
            city={entry.city || ""}
            country={entry.country || ""}
            onLanguageChange={(newLanguage) => updateMultiLanguageField(fieldConfig.name, entry.id, { language: newLanguage })}
            onStreetChange={(value) => updateMultiLanguageField(fieldConfig.name, entry.id, { street: value })}
            onStreetNumberChange={(value) => updateMultiLanguageField(fieldConfig.name, entry.id, { streetNumber: value })}
            onCityChange={(value) => updateMultiLanguageField(fieldConfig.name, entry.id, { city: value })}
            onCountryChange={(value) => updateMultiLanguageField(fieldConfig.name, entry.id, { country: value })}
            onRemove={() => removeMultiLanguageEntry(fieldConfig.name, entry.id)}
            onAdd={() => addMultiLanguageEntry(fieldConfig.name)}
            showLabel={index === 0}
            showRemoveButton={fieldData.length > 1}
            showAddButton={index === fieldData.length - 1}
          />
        ));

      case "file":
        return (
          <FileInput
            key={fieldConfig.name}
            label={fieldConfig.label}
            helpText={fieldConfig.tooltip}
            contentType={fieldConfig.contentType}
            onChange={(fileName) => updateField(fieldConfig.name, fileName)}
            className="mb-4"
          />
        );

      case "list":
        return (
          <CollectionInput
            key={fieldConfig.name}
            label={fieldConfig.label}
            helpText={fieldConfig.tooltip}
            value={fieldData}
            onChange={(newValue) => updateField(fieldConfig.name, newValue)}
            collectionType="SubmodelElementList"
            elementTemplate={fieldConfig.elementTemplate}
            className="mb-4"
          />
        );

      case "collection":
        // Handle complex collections (not AddressInformation)
        if (fieldConfig.name === "AddressInformation" || fieldConfig.type === "address") {
          // Keep existing address handling
          return fieldData.map((entry, index) => (
            <AddressInformation
              key={entry.id}
              label={fieldConfig.label}
              helpText={fieldConfig.tooltip}
              language={entry.language}
              street={entry.street || ""}
              streetNumber={entry.streetNumber || ""}
              city={entry.city || ""}
              country={entry.country || ""}
              onLanguageChange={(newLanguage) => updateMultiLanguageField(fieldConfig.name, entry.id, { language: newLanguage })}
              onStreetChange={(value) => updateMultiLanguageField(fieldConfig.name, entry.id, { street: value })}
              onStreetNumberChange={(value) => updateMultiLanguageField(fieldConfig.name, entry.id, { streetNumber: value })}
              onCityChange={(value) => updateMultiLanguageField(fieldConfig.name, entry.id, { city: value })}
              onCountryChange={(value) => updateMultiLanguageField(fieldConfig.name, entry.id, { country: value })}
              onRemove={() => removeMultiLanguageEntry(fieldConfig.name, entry.id)}
              onAdd={() => addMultiLanguageEntry(fieldConfig.name)}
              showLabel={index === 0}
              showRemoveButton={fieldData.length > 1}
              showAddButton={index === fieldData.length - 1}
            />
          ));
        } else {
          // Handle complex collections with CollectionInput
          return (
            <CollectionInput
              key={fieldConfig.name}
              label={fieldConfig.label}
              helpText={fieldConfig.tooltip}
              value={fieldData}
              onChange={(newValue) => updateField(fieldConfig.name, newValue)}
              collectionType="SubmodelElementCollection"
              elementTemplate={fieldConfig.elementTemplate}
              className="mb-4"
            />
          );
        }

      case "collection_legacy":
        // Legacy fallback for collections without proper structure
        return (
          <div key={fieldConfig.name} className="mb-4">
            <div className="p-3 border border-secondary rounded bg-dark">
              <h6 className="text-warning mb-3">
                {fieldConfig.label}
                {fieldConfig.tooltip && (
                  <OverlayTrigger
                    placement="top"
                    overlay={<Tooltip>{fieldConfig.tooltip}</Tooltip>}
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
              </h6>
              <p className="text-muted small">
                This collection contains nested elements that are not currently supported in the form builder.
                <br />
                <small>Model Type: {fieldConfig.originalElement?.modelType}</small>
              </p>
            </div>
          </div>
        );

      default:
        return (
          <div key={fieldConfig.name} className="mb-4">
            <div className="p-3 border border-warning rounded bg-dark">
              <h6 className="text-warning mb-2">
                {fieldConfig.label} 
                <small className="text-muted ms-2">({fieldConfig.originalElement?.modelType || 'Unknown'})</small>
              </h6>
              <p className="text-muted small mb-0">
                This field type is not yet supported in the form builder.
              </p>
            </div>
          </div>
        );
    }
  };

  return (
    <div className="create-template-container">
      <Container className="py-4">
        {/* Progress bar */}
        <div className="d-flex mb-1">
          <div className="text-white step-progress-item step-progress-left">Select a Template</div>
          <div className="text-warning step-progress-item step-progress-center">Fill the details</div>
          <div className="text-white step-progress-item step-progress-right">All done</div>
        </div>

      <div className="d-flex justify-content-between align-items-center mb-4">
        <div
          style={{
            flex: 1,
            height: "4px",
            background: "green",
            marginRight: "4px",
            borderRadius: "2px",
          }}
        />
        <div
          style={{
            flex: 1,
            height: "4px",
            background: "gold",
            marginLeft: "4px",
            marginRight: "4px",
            borderRadius: "2px",
          }}
        />
        <div
          style={{
            flex: 1,
            height: "4px",
            background: "#ccc",
            marginLeft: "4px",
            borderRadius: "2px",
          }}
        />
      </div>

      {/* Required Fields Template */}
      <Card className="text-white mb-3 template-form-card">
        <Card.Body>
          <Card.Title className="mb-4">
            {formConfig.title}
          </Card.Title>
          
          {/* Dynamically rendered required fields */}
          {formConfig.requiredFields.map(fieldConfig => renderField(fieldConfig))}
          
          {formConfig.requiredFields.length === 0 && (
            <p className="text-muted">No required fields found in this template.</p>
          )}
        </Card.Body>
      </Card>

      {/* Advanced Fields */}
      <div className="mb-4">
        <Button 
          variant="link" 
          className="text-white d-flex align-items-center"
          onClick={() => setShowAdvancedFields(!showAdvancedFields)}
        >
          Advanced Fields
          <span className="ms-2">
            {showAdvancedFields ? (
              <ChevronDownIcon style={{ fill: "white", width: "16px", height: "16px" }} />
            ) : (
              <ChevronRightIcon style={{ fill: "white", width: "16px", height: "16px" }} />
            )}
          </span>
          {formConfig.advancedFields.length > 0 && (
            <small className="ms-2">({formConfig.advancedFields.length})</small>
          )}
        </Button>
      </div>

      {/* Advanced Fields Card */}
      {showAdvancedFields && (
        <Card className="text-white mb-3 template-form-card">
          <Card.Body>
            <Card.Title className="mb-4">
              Advanced Fields
            </Card.Title>
            
            {/* Dynamically rendered advanced fields */}
            {formConfig.advancedFields.map(fieldConfig => (
              <div key={fieldConfig.name} className="mb-3">
                {renderField(fieldConfig)}
              </div>
            ))}
            
            {formConfig.advancedFields.length === 0 && (
              <p className="text-muted">No advanced fields found in this template.</p>
            )}
          </Card.Body>
        </Card>
      )}

      {/* Action Buttons */}
      <div className="d-flex gap-3">
        <Button 
          onClick={handleBackNavigation}
          style={{
            backgroundColor: "#004277",
            border: "2px solid #0D598B",
            color: "white",
            display: "flex",
            alignItems: "center",
            gap: "6px"
          }}
        >
          <ChevronLeftIcon style={{ fill: "white", width: "16px", height: "16px" }} />
          Back
        </Button>
        <Button 
          variant="primary"
          onClick={handleSave}
          style={{
            display: "flex",
            alignItems: "center",
            gap: "6px"
          }}
        >
          <FloppyFillIcon style={{ fill: "white", width: "16px", height: "16px" }} />
          Save
        </Button>
      </div>
    </Container>
    </div>
  );
}
