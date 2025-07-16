import React, { useState, useEffect } from "react";
import { Container, Row, Col, Button, Form, Card, OverlayTrigger, Tooltip } from "react-bootstrap";
import { Link, useNavigate, useLocation } from "react-router-dom";
import AddressInformation from "../components/form_inputs/AddressInformation";
import Prop from "../components/form_inputs/Prop";
import MLP from "../components/form_inputs/MLP";
import ChevronLeftIcon from "../assets/icons/chevron-left.svg?react";
import FloppyFillIcon from "../assets/icons/floppy-fill.svg?react";
import "../styles/createTemplate.css";

// Mock API response for Digital Nameplate
const mockDigitalNameplateConfig = {
  title: "Digital Nameplate For Industrial Equipment",
  fields: [
    {
      name: "uriOfTheProduct",
      type: "prop",
      label: "URI Of The Product",
      placeholder: "https://www.domain-abc.com/Model-Nr-1234/Serial-Nr-5678",
      tooltip: "unique global identification of the product instance using an universal resource identifier (URI)"
    },
    {
      name: "manufacturerName",
      type: "multiLanguage",
      label: "ManufacturerName",
      placeholder: "Example Company",
      tooltip: "legally valid designation of the natural or judicial person which is directly responsible for the design, production, packaging and labeling of a product in respect to its being brought into circulation"
    },
    {
      name: "manufacturerProductDesignation",
      type: "multiLanguage",
      label: "ManufacturerProductDesignation",
      placeholder: "ABC-123",
      tooltip: "short description of the product (short text), third or lowest level of a 3 level manufacturer specific product hierarchy"
    },
    {
      name: "orderCode",
      type: "prop",
      label: "OrderCodeOfManufacturer",
      placeholder: "FMABC1234",
      tooltip: "unique combination of numbers and letters issued by themanufacturer that is used to identify the device for ordering"
    },
    {
      name: "addressInformation",
      type: "address",
      label: "Address Information",
      tooltip: "Address information details"
    }
  ]
};

// Mock API response for Carbon Footprint
const mockCarbonFootprintConfig = {
  title: "Carbon Footprint",
  fields: [
    {
      name: "productCarbonFootprint",
      type: "prop",
      label: "Product Carbon Footprint",
      placeholder: "12.5",
      tooltip: "Total carbon footprint of the product in kg CO2 equivalent"
    },
    {
      name: "calculationMethod",
      type: "multiLanguage",
      label: "Calculation Method",
      placeholder: "ISO 14067:2018",
      tooltip: "Method or standard used for carbon footprint calculation"
    },
    {
      name: "assessmentScope",
      type: "multiLanguage",
      label: "Assessment Scope",
      placeholder: "Cradle to gate",
      tooltip: "Scope of the carbon footprint assessment (e.g., cradle to gate, cradle to grave)"
    },
    {
      name: "referenceUnit",
      type: "prop",
      label: "Reference Unit",
      placeholder: "kg",
      tooltip: "Unit of measurement for the carbon footprint calculation"
    },
    {
      name: "validityPeriod",
      type: "prop",
      label: "Validity Period",
      placeholder: "2024-2026",
      tooltip: "Time period for which the carbon footprint data is valid"
    },
    {
      name: "certificationBody",
      type: "multiLanguage",
      label: "Certification Body",
      placeholder: "TÜV SÜD",
      tooltip: "Organization that certified the carbon footprint assessment"
    }
  ]
};

// Function to get template configuration based on selected template
const getTemplateConfig = (selectedTemplate) => {
  if (!selectedTemplate) return mockDigitalNameplateConfig;
  
  switch (selectedTemplate.id) {
    case 'digital-nameplate-for-industrial-equipment':
      return mockDigitalNameplateConfig;
    case 'carbon-footprint':
      return mockCarbonFootprintConfig;
    default:
      return mockDigitalNameplateConfig;
  }
};

export default function CreateTemplate() {
  const navigate = useNavigate();
  const location = useLocation();
  
  // Get selected template from location state (passed from /templates)
  const selectedTemplate = location.state?.selectedTemplate;
  
  // Form configuration from API - dynamically set based on selected template
  const [formConfig, setFormConfig] = useState(() => getTemplateConfig(selectedTemplate));
  
  // Unified form state - dynamically initialized from API
  const [formData, setFormData] = useState({});

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
      
      // For now, use mock data based on selected template
      const templateConfig = getTemplateConfig(selectedTemplate);
      setFormConfig(templateConfig);
      console.log('Selected template:', selectedTemplate);
    }
  }, [selectedTemplate]);

  // Initialize form data based on API configuration
  useEffect(() => {
    const initializeFormData = () => {
      const initialData = {};
      
      formConfig.fields.forEach(field => {
        switch (field.type) {
          case "prop":
            initialData[field.name] = "";
            break;
          case "multiLanguage":
            initialData[field.name] = [{ id: 1, language: "English", value: "" }];
            break;
          case "address":
            initialData[field.name] = [{ 
              id: 1, 
              language: "English",
              street: "",
              streetNumber: "",
              city: "",
              country: ""
            }];
            break;
          default:
            initialData[field.name] = "";
        }
      });
      
      setFormData(initialData);
    };

    initializeFormData();
  }, [formConfig]);

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
    const originalFormData = location.state?.originalFormData;

    // Navigate back to /create with template data and original form data
    navigate('/create', { 
      state: { 
        templateData: templateData,
        originalFormData: originalFormData
      } 
    });
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
            placeholder={fieldConfig.placeholder}
            helpText={fieldConfig.tooltip}
            type="text"
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

      default:
        return null;
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

      {/* Digital Nameplate Template */}
      <Card className="text-white mb-3 template-form-card">
        <Card.Body>
          <Card.Title className="mb-4">{formConfig.title}</Card.Title>
          
          {/* Dynamically rendered fields based on API configuration */}
          {formConfig.fields.map(fieldConfig => renderField(fieldConfig))}
        </Card.Body>
      </Card>

      {/* Advanced Fields */}
      <div className="mb-4">
        <Button variant="link" className="text-white">
          Advanced Fields &gt;
        </Button>
      </div>

      {/* Action Buttons */}
      <div className="d-flex gap-3">
        <Button 
          as={Link} 
          to="/templates"
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
