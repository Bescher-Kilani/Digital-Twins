import React, { useState, useEffect, useContext } from "react";
import { Card, Button, Toast, ToastContainer, Container, Row, Col, OverlayTrigger, Tooltip } from "react-bootstrap";
import { useNavigate, useLocation } from "react-router-dom";
import Prop from "../components/form_inputs/Prop";
import AssetKind from "../components/form_inputs/AssetKind";
import SpecificAssetId from "../components/form_inputs/SpecificAssetId";
import aiAssistantIcon from "../assets/ai-chatbot-assistant.png";
import tagIcon from "../assets/icons/tags.svg";
import ChevronLeftIcon from "../assets/icons/chevron-left.svg?react";
import FloppyFillIcon from "../assets/icons/floppy-fill.svg?react";
import QuestionCircleIcon from "../assets/icons/question-circle.svg?react";
import { useTranslation } from "react-i18next";
import { KeycloakContext } from "../KeycloakContext";
import "../styles/createPage.css";

function CreatePage() {
  const { t } = useTranslation();
  const navigate = useNavigate();
  const location = useLocation();
  const { keycloak, authenticated } = useContext(KeycloakContext);

  const [showChat, setShowChat] = useState(false);
  
  // Toast state for error notifications
  const [toasts, setToasts] = useState([]);
  
  // State for form data
  const [formData, setFormData] = useState({
    assetInformation: {
      assetKind: "Instance",
      assetType: "testType",
      defaultThumbnail: null,
      globalAssetId: "",
      specificAssetIds: [{ name: "", value: "" }]
    },
    derivedFrom: null,
    submodels: [],
    embeddedDataSpecifications: [],
    extensions: [],
    administration: null,
    id: "",
    category: null,
    description: [{ language: "en", text: "" }],
    displayName: [],
    idShort: ""
  });
  
  // State for submodel templates - initialize from sessionStorage
  const [submodelTemplates, setSubmodelTemplates] = useState(() => {
    const saved = sessionStorage.getItem('submodelTemplates');
    return saved ? JSON.parse(saved) : [];
  });

  // Persist submodel templates to sessionStorage whenever they change
  useEffect(() => {
    sessionStorage.setItem('submodelTemplates', JSON.stringify(submodelTemplates));
  }, [submodelTemplates]);

  // Handle returning template data from /templates/create
  useEffect(() => {
    if (location.state?.templateData) {
      const newTemplate = location.state.templateData;
      console.log('Template data received:', newTemplate);
      
      // Check if this is an edit operation
      if (location.state?.editingTemplateIndex !== undefined) {
        const editIndex = location.state.editingTemplateIndex;
        console.log('Updating existing template at index:', editIndex);
        
        setSubmodelTemplates(prev => {
          const updated = [...prev];
          updated[editIndex] = newTemplate;
          console.log('Updated templates after edit:', updated);
          return updated;
        });
      } else {
        // Adding new template
        console.log('Adding new template:', newTemplate);
        
        setSubmodelTemplates(prev => {
          console.log('Current templates before adding:', prev);
          const updated = [...prev, newTemplate];
          console.log('Updated templates:', updated);
          return updated;
        });
      }
      
      // Restore the original form data if it exists
      if (location.state?.originalFormData) {
        setFormData(location.state.originalFormData);
      }
      
      // Clear the state to prevent re-adding on refresh
      navigate(location.pathname, { replace: true, state: {} });
    }
    
    // Handle form data restoration from back navigation
    if (location.state?.restoredFormData) {
      console.log('Restoring form data from back navigation:', location.state.restoredFormData);
      setFormData(location.state.restoredFormData);
      
      // Only restore submodel templates if they are explicitly provided
      // Otherwise, let the sessionStorage initialization handle templates
      if (location.state?.restoredSubmodelTemplates !== undefined) {
        console.log('Restoring submodel templates from back navigation:', location.state.restoredSubmodelTemplates);
        setSubmodelTemplates(location.state.restoredSubmodelTemplates);
      } else {
        console.log('No restored templates provided, keeping current templates from sessionStorage');
      }
      
      // Clear the state to prevent re-restoration on refresh
      navigate(location.pathname, { replace: true, state: {} });
    }
  }, [location.state, navigate, location.pathname]);

  // Function to show toast notifications
  const showToast = (message, variant = 'danger') => {
    const id = Date.now();
    const newToast = {
      id,
      message,
      variant,
      show: true
    };
    setToasts(prev => [...prev, newToast]);
    
    // Auto-hide toast after 5 seconds
    setTimeout(() => {
      setToasts(prev => prev.filter(toast => toast.id !== id));
    }, 5000);
  };

  // Function to manually close a toast
  const closeToast = (id) => {
    setToasts(prev => prev.filter(toast => toast.id !== id));
  };

  // Handle adding submodel
  const handleAddSubmodel = () => {
    // Pass current form data to maintain context
    // Templates are preserved by sessionStorage, so no need to pass them
    navigate('/templates', { 
      state: { 
        fromCreate: true,
        formData: formData
      } 
    });
  };

  // Handle editing existing submodel template
  const handleEditTemplate = (templateIndex) => {
    const templateToEdit = submodelTemplates[templateIndex];
    
    // Navigate to template creation page with existing template data for editing
    navigate('/templates/create', {
      state: {
        fromCreate: true,
        formData: formData,
        currentSubmodelTemplates: submodelTemplates, // Include current templates
        editingTemplate: {
          index: templateIndex,
          data: templateToEdit
        },
        selectedTemplate: templateToEdit.selectedTemplate
      }
    });
  };

  // Handle removing submodel template
  const handleRemoveTemplate = (templateIndex) => {
    setSubmodelTemplates(prev => prev.filter((_, index) => index !== templateIndex));
  };

  // Function to merge user input data into template structure
  const mergeDataIntoTemplate = (template, userData) => {
    // Create a deep copy of the template JSON structure
    const mergedTemplate = JSON.parse(JSON.stringify(template.selectedTemplate.templateData.json));
    
    // Function to recursively update values in submodel elements
    const updateSubmodelElements = (elements, data, currentPath = "") => {
      if (!elements || !Array.isArray(elements)) return;
      
      elements.forEach(element => {
        const idShort = element.idShort;
        const fullPath = currentPath ? `${currentPath}.${idShort}` : idShort;
        
        // Check if user has provided data for this field using nested path first, then simple idShort
        let value = undefined;
        let hasData = false;
        
        if (data && Object.prototype.hasOwnProperty.call(data, fullPath)) {
          value = data[fullPath];
          hasData = true;
        } else if (data && Object.prototype.hasOwnProperty.call(data, idShort)) {
          value = data[idShort];
          hasData = true;
        }
        
        if (hasData) {
          // Handle different element types
          switch (element.modelType) {
            case 'Property':
              // Always update the value, even if it's empty string
              element.value = typeof value === 'string' ? value : '';
              break;
              
            case 'MultiLanguageProperty':
              if (Array.isArray(value) && value.length > 0) {
                element.value = value.map(item => ({
                  language: item.language === 'English' ? 'en' : 
                           item.language === 'German' ? 'de' : 
                           item.language.toLowerCase().substring(0, 2),
                  text: `"${item.value || ''}"`
                }));
              } else {
                // Clear the value if user provided empty array or no data
                element.value = [];
              }
              break;
              
            case 'SubmodelElementCollection':
              // Handle special case for AddressInformation
              if (element.idShort === 'AddressInformation' && Array.isArray(value) && value.length > 0) {
                const addressData = value[0]; // Take the first entry
                // Create address submodel elements with only the 4 fields from the form
                element.value = [
                  {
                    "modelType": "Property", 
                    "idShort": "Street",
                    "value": addressData.street || "",
                    "valueType": "xs:string"
                  },
                  {
                    "modelType": "Property",
                    "idShort": "HouseNumber", 
                    "value": addressData.streetNumber || "",
                    "valueType": "xs:string"
                  },
                  {
                    "modelType": "Property",
                    "idShort": "CityTown",
                    "value": addressData.city || "",
                    "valueType": "xs:string"
                  },
                  {
                    "modelType": "Property",
                    "idShort": "NationalCode",
                    "value": addressData.country || "",
                    "valueType": "xs:string"
                  }
                ];
              } else {
                // For other collections, recursively update their value elements
                if (element.value && Array.isArray(element.value)) {
                  updateSubmodelElements(element.value, data, fullPath);
                }
              }
              break;
              
            case 'SubmodelElementList':
              if (Array.isArray(value) && value.length > 0) {
                // Handle complex lists like ProductCarbonFootprints
                element.value = value.map(item => {
                  // Create a copy of the template element structure
                  const listElement = JSON.parse(JSON.stringify(element.value[0]));
                  
                  // Handle the nested data structure
                  if (item.data) {
                    // For each key in the item data (could be "undefined" or other keys)
                    Object.keys(item.data).forEach(key => {
                      const itemData = item.data[key];
                      if (itemData && typeof itemData === 'object') {
                        // Recursively update the list element with the item data
                        if (listElement.value && Array.isArray(listElement.value)) {
                          updateSubmodelElements(listElement.value, itemData, fullPath);
                        }
                      }
                    });
                  }
                  
                  return listElement;
                });
              } else if (typeof value === 'string' && value.trim() !== '') {
                // Handle simple string values for SubmodelElementLists
                // Create a single list item with the provided value
                if (element.value && element.value.length > 0 && element.value[0]) {
                  const templateItem = element.value[0];
                  const listItem = JSON.parse(JSON.stringify(templateItem));
                  
                  // Set the value on the list item
                  if (listItem.modelType === 'Property') {
                    listItem.value = value;
                  }
                  
                  element.value = [listItem];
                } else {
                  // Fallback: create a basic property if no template exists
                  element.value = [{
                    "modelType": "Property",
                    "idShort": element.idShort.replace(/s$/, ''), // Remove trailing 's' if present
                    "value": value,
                    "valueType": "xs:string"
                  }];
                }
              } else {
                // For complex lists, recurse into their nested elements
                if (element.value && Array.isArray(element.value)) {
                  updateSubmodelElements(element.value, data, fullPath);
                }
              }
              break;
              
            case 'File':
              // Always update the value, even if it's empty string
              element.value = typeof value === 'string' ? value : '';
              break;
          }
        } else {
          // Recursively handle nested elements even if no direct data match
          if (element.value && Array.isArray(element.value)) {
            updateSubmodelElements(element.value, data, fullPath);
          }
        }
      });
    };
    
    // Update the submodel elements with user data
    if (mergedTemplate.submodelElements && userData) {
      updateSubmodelElements(mergedTemplate.submodelElements, userData);
    }
    
    return mergedTemplate;
  };

  // Handle final save
  const handleSave = async () => {
    // Transform submodel templates into the new format
    const transformedSubmodels = submodelTemplates.map(template => {
      return mergeDataIntoTemplate(template, template.data);
    });
    
    // Extract submodel references for AAS
    const submodelReferences = submodelTemplates.map(template => ({
      keys: [
        {
          type: "Submodel",
          value: template.selectedTemplate.templateData.json.id
        }
      ],
      type: "ModelReference"
    }));
    
    // Transform AAS data to match the new format
    const transformedAAS = {
      assetInformation: {
        assetKind: formData.assetInformation.assetKind,
        assetType: formData.assetInformation.assetType,
        defaultThumbnail: formData.assetInformation.defaultThumbnail,
        globalAssetId: formData.assetInformation.globalAssetId,
        specificAssetIds: formData.assetInformation.specificAssetIds.filter(
          item => item.name.trim() !== "" || item.value.trim() !== ""
        )
      },
      derivedFrom: formData.derivedFrom,
      submodels: submodelReferences,
      embeddedDataSpecifications: formData.embeddedDataSpecifications,
      extensions: formData.extensions,
      administration: formData.administration,
      id: formData.id,
      category: formData.category,
      description: formData.description.filter(desc => desc.text.trim() !== ""),
      displayName: formData.displayName,
      idShort: formData.idShort
    };
    
    const finalData = {
      aas: transformedAAS,
      submodels: transformedSubmodels
    };
    
    try {
      // Prepare headers
      const headers = {
        'Content-Type': 'application/json'
      };
      
      // Add Authorization header if user is logged in
      let token = null;
      
      // Try multiple sources for the token
      token = sessionStorage.getItem('access_token') || 
              localStorage.getItem('authToken') || 
              (keycloak && keycloak.token) ||
              null;
      
      console.log('Authentication status:', authenticated);
      console.log('Keycloak object:', keycloak);
      console.log('SessionStorage access_token:', sessionStorage.getItem('access_token') ? 'Present' : 'Missing');
      console.log('Using token:', token ? `Present (${token.substring(0, 20)}...)` : 'No token');
      
      if (token) {
        headers['Authorization'] = `Bearer ${token}`;
      }
      
      console.log('Saving AASX model:', finalData);
      console.log('Request headers:', headers);
      
      // Use authenticated endpoint if user is authenticated, otherwise use guest endpoint
      const endpoint = token ? 'http://localhost:9090/models/new' : 'http://localhost:9090/guest/models/new';
      console.log('Using endpoint:', endpoint);
      
      const response = await fetch(endpoint, {
        method: 'POST',
        headers: headers,
        body: JSON.stringify(finalData)
      });
      
      if (response.ok) {
        // Get the response data
        const responseData = await response.json();
        console.log('Model created successfully:', responseData);
        
        // Extract id and idShort from response
        const modelId = responseData.id;
        const modelIdShort = responseData.aas?.idShort;
        
        // Clear the templates after successful save
        sessionStorage.removeItem('submodelTemplates');
        setSubmodelTemplates([]);
        
        // Navigate to createComplete page with model data
        navigate('/create/complete', { 
          state: { 
            modelName: formData.idShort || 'Untitled Model',
            modelId: modelId,
            modelIdShort: modelIdShort
          } 
        });
      } else {
        // Handle error response
        const errorData = await response.json().catch(() => ({}));
        const errorMessage = errorData.message || `Error: ${response.status} ${response.statusText}`;
        console.error('Error saving model:', errorMessage);
        
        if (response.status === 401) {
          showToast('Authentication required. Please sign in and try again.', 'warning');
        } else if (response.status === 403) {
          showToast('Access denied. You do not have permission to create models.', 'danger');
        } else {
          showToast(`Failed to save model: ${errorMessage}`, 'danger');
        }
      }
    } catch (error) {
      console.error('Network error saving model:', error);
      console.error('Error details:', {
        message: error.message,
        name: error.name,
        stack: error.stack
      });
      
      // Check if it's a CORS or network error
      if (error.message.includes('Load failed') || error.message.includes('CORS') || error.message.includes('Network request failed')) {
        showToast('Connection error: Unable to reach the server.', 'danger');
      } else {
        showToast('Network error: Unable to save model. Please check your connection and try again.', 'danger');
      }
    }
  };

  // Handle form field changes
  const updateField = (field, value) => {
    setFormData(prev => ({ ...prev, [field]: value }));
  };

  const updateDescriptionField = (index, field, value) => {
    setFormData(prev => {
      const updated = [...prev.description];
      updated[index][field] = value;
      return { ...prev, description: updated };
    });
  };

  const addDescriptionLanguage = () => {
    setFormData(prev => ({
      ...prev,
      description: [...prev.description, { language: "de", text: "" }]
    }));
  };

  const removeDescriptionLanguage = (index) => {
    setFormData(prev => {
      const updated = prev.description.filter((_, i) => i !== index);
      return { ...prev, description: updated };
    });
  };

  const updateSpecificAssetIdField = (index, field, value) => {
    setFormData(prev => {
      const updated = [...prev.assetInformation.specificAssetIds];
      updated[index][field] = value;
      return { 
        ...prev, 
        assetInformation: {
          ...prev.assetInformation,
          specificAssetIds: updated
        }
      };
    });
  };

  const addSpecificAssetId = () => {
    setFormData(prev => ({
      ...prev,
      assetInformation: {
        ...prev.assetInformation,
        specificAssetIds: [...prev.assetInformation.specificAssetIds, { name: "", value: "" }]
      }
    }));
    console.log('FormData: ', formData);
  };

  const removeSpecificAssetId = (index) => {
    setFormData(prev => {
      const updated = prev.assetInformation.specificAssetIds.filter((_, i) => i !== index);
      return { 
        ...prev, 
        assetInformation: {
          ...prev.assetInformation,
          specificAssetIds: updated
        }
      };
    });
    console.log('FormData: ', formData);
  };

  return (
    <div className="create-page-container"> 
      <Container className="py-4">
      {/* Progress bar */}
      <div className="d-flex mb-1">
        <div className="text-warning step-progress-item step-progress-left">{t("create.progress.details")}</div>
        <div className="text-white step-progress-item step-progress-right">{t("create.progress.allDone")}</div>
      </div>

      <div className="d-flex justify-content-between align-items-center mb-4">
        <div
          style={{
            flex: 1,
            height: "4px",
            background: "gold",
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

      <Card className="text-white mb-3 form-card">
        <Card.Body>
          <Card.Title className="mb-4">
            General Information
          </Card.Title>

          <Row>
            <Col sm={6}>
              <div className="mb-3">
                <label className="form-label text-white" style={{ display: "flex", alignItems: "center", gap: "6px" }}>
                  name
                  {t("create.tooltips.name") && (
                    <OverlayTrigger
                      placement="top"
                      overlay={<Tooltip id="tooltip-name">{t("create.tooltips.name")}</Tooltip>}
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
                </label>
                <input
                  type="text"
                  className="form-control"
                  placeholder="ex. Office Building"
                  value={formData.idShort}
                  onChange={(e) => updateField("idShort", e.target.value)}
                  style={{
                    backgroundColor: "#1a1a1a",
                    border: "1px solid #444",
                    color: "white",
                    height: "38px"
                  }}
                />
              </div>
            </Col>
            <Col sm={6}>
              {formData.description.map((desc, index) => (
                <div key={`description-${index}`} className="mb-3">
                  {index === 0 && (
                    <label className="form-label text-white" style={{ display: "flex", alignItems: "center", gap: "6px" }}>
                      description
                      {t("create.tooltips.description") && (
                        <OverlayTrigger
                          placement="top"
                          overlay={<Tooltip id="tooltip-description">{t("create.tooltips.description")}</Tooltip>}
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
                    </label>
                  )}
                  <div className="d-flex align-items-center gap-2">
                    <div style={{ width: "25%" }}>
                      <select
                        className="form-control white-caret"
                        value={desc.language}
                        onChange={(e) => updateDescriptionField(index, "language", e.target.value)}
                        style={{
                          backgroundColor: "#1a1a1a",
                          border: "1px solid #444",
                          color: "white",
                          height: "38px",
                          backgroundImage: "url(\"data:image/svg+xml,%3csvg xmlns='http://www.w3.org/2000/svg' viewBox='0 0 16 16'%3e%3cpath fill='none' stroke='%23ffffff' stroke-linecap='round' stroke-linejoin='round' stroke-width='2' d='m1 6 7 7 7-7'/%3e%3c/svg%3e\")",
                          backgroundRepeat: "no-repeat",
                          backgroundPosition: "right 0.75rem center",
                          backgroundSize: "16px 12px",
                          paddingRight: "2.5rem"
                        }}
                      >
                        <option value="en">English</option>
                        <option value="de">German</option>
                        <option value="fr">French</option>
                        <option value="es">Spanish</option>
                      </select>
                    </div>
                    <div style={{ flex: 1 }}>
                      <input
                        type="text"
                        className="form-control"
                        placeholder="ex. Optional Description"
                        value={desc.text}
                        onChange={(e) => updateDescriptionField(index, "text", e.target.value)}
                        style={{
                          backgroundColor: "#1a1a1a",
                          border: "1px solid #444",
                          color: "white",
                          height: "38px"
                        }}
                      />
                    </div>
                    <div style={{ width: "auto", display: "flex", gap: "5px" }}>
                      {index === 0 && (
                        <Button
                          onClick={addDescriptionLanguage}
                          style={{
                            backgroundColor: "#003368",
                            border: "2px solid #1A4D82",
                            color: "white",
                            height: "38px",
                            padding: "0 12px",
                            fontSize: "14px"
                          }}
                        >
                          + Add
                        </Button>
                      )}
                      {index > 0 && (
                        <Button 
                          variant="outline-secondary" 
                          onClick={() => removeDescriptionLanguage(index)}
                          style={{
                            height: "38px",
                            padding: "0 12px",
                            fontSize: "14px"
                          }}
                        >
                          Remove
                        </Button>
                      )}
                    </div>
                  </div>
                </div>
              ))}
            </Col>
          </Row>
            
        </Card.Body>
      </Card>

      <Card className="text-white mb-3 form-card">
        <Card.Body>
            <Card.Title className="mb-4">
              AAS Identification
            </Card.Title>

            <Row>
              <Col sm={6}>
                <Prop
                  key="general-id"
                  label="id"
                  placeholder="ex. urn:aas:example:aas:123456"
                  helpText={t("create.tooltips.id")}
                  type="text"
                  value={formData.id}
                  onChange={(e) => updateField("id", e.target.value)}
                  className="mb-4"
                />
              </Col>
              <Col sm={6}>
                <AssetKind
                  key="general-assetKind"
                  label="assetKind"
                  helpText={t("create.tooltips.assetKind")}
                  showLabel={true}
                  value={formData.assetInformation.assetKind}
                  onChange={(e) => updateField("assetInformation", { 
                    ...formData.assetInformation, 
                    assetKind: e 
                  })}
                />
              </Col>
            </Row>
            
        </Card.Body>
      </Card>

      <Card className="text-white mb-3 form-card">
        <Card.Body>
            <Card.Title className="mb-4">
              Asset Identification
            </Card.Title>

            <Row>
              <Col sm={6}>
                <Prop
                  key="general-globalAssetId"
                  label="globalAssetId"
                  placeholder="ex. urn:aas:example:aas:123456"
                  helpText={t("create.tooltips.globalAssetId")}
                  type="text"
                  value={formData.assetInformation.globalAssetId}
                  onChange={(e) => updateField("assetInformation", {
                    ...formData.assetInformation,
                    globalAssetId: e.target.value
                  })}
                  className="mb-4"
                />
              </Col>
              <Col sm={6}>
                {formData.assetInformation.specificAssetIds.map((item, index) => (
                  <SpecificAssetId
                    key={`specificAssetId-${index}`}
                    label={index === 0 ? "specificAssetId" : ""}
                    placeholder1="Name"
                    placeholder2="Value"
                    helpText={t("create.tooltips.specificAssetId")}
                    value1={item.name}
                    value2={item.value}
                    onChange1={(e) => updateSpecificAssetIdField(index, "name", e.target.value)}
                    onChange2={(e) => updateSpecificAssetIdField(index, "value", e.target.value)}
                    onAdd={index === 0 ? addSpecificAssetId : undefined}
                    onRemove={() => removeSpecificAssetId(index)}
                    showAddButton={index === 0}
                    showRemoveButton={index > 0}
                    showLabel={index === 0}
                  />
                ))}
              </Col>
            </Row>
            
        </Card.Body>
      </Card>
  

      <Card className="text-white mb-5 form-card">
        <Card.Body>
          <Card.Title className="mb-2">
              Submodel Templates
            </Card.Title>
        <div className="field-block">
          <div className="d-flex flex-wrap gap-3">
            {/* Add Submodel Button Card */}
            <div className="position-relative">
              <Card
                className="text-white text-center"
                style={{
                  width: "140px",
                  height: "180px",
                  padding: "1rem",
                  borderRadius: "5px",
                  cursor: "pointer",
                  border: "3px solid #0E4175",
                  background: "linear-gradient(180deg, #03386C 0%, #02376B 50%, #01366A 100%)"
                }}
                onClick={handleAddSubmodel}
              >
                <Card.Body className="d-flex flex-column align-items-center justify-content-center p-0 h-100">
                  <div style={{ 
                    fontSize: "3rem", 
                    fontWeight: "bold",
                    marginBottom: "0.5rem"
                  }}>
                    +
                  </div>
                  <Card.Text style={{ fontSize: "0.875rem", lineHeight: "1.2" }}>
                    Add Submodel
                  </Card.Text>
                </Card.Body>
              </Card>
            </div>
            
            {/* Display added submodel templates */}
            {submodelTemplates.map((template, index) => (
                <div key={index} className="position-relative">
                  <Card
                    className="text-white text-center template-card-editable"
                    style={{
                      width: "140px",
                      height: "180px",
                      padding: "1rem",
                      borderRadius: "5px",
                      border: "3px solid #0E4175",
                      background: "linear-gradient(180deg, #002C5A 0%, #002C59 50%, #002C5D 100%)",
                      cursor: "pointer",
                      transition: "all 0.2s ease-in-out"
                    }}
                    onClick={() => handleEditTemplate(index)}
                    onMouseEnter={(e) => {
                      e.currentTarget.style.transform = "scale(1.05)";
                      e.currentTarget.style.boxShadow = "0 4px 12px rgba(0,0,0,0.3)";
                    }}
                    onMouseLeave={(e) => {
                      e.currentTarget.style.transform = "scale(1)";
                      e.currentTarget.style.boxShadow = "none";
                    }}
                  >
                    <Card.Body className="d-flex flex-column align-items-center justify-content-center p-0 h-100">
                      <Card.Img
                        variant="top"
                        src={tagIcon}
                        style={{ width: "50px", margin: "0 auto 0.5rem auto", pointerEvents: "none" }}
                        alt="Template Icon"
                      />
                      <Card.Text style={{ 
                        fontSize: "0.875rem", 
                        lineHeight: "1.1", 
                        pointerEvents: "none",
                        textAlign: "center",
                        wordWrap: "break-word",
                        overflowWrap: "break-word",
                        hyphens: "auto",
                        maxWidth: "100%",
                        padding: "0 4px"
                      }}>
                        {template.title}
                      </Card.Text>
                    </Card.Body>
                  </Card>
                  <button 
                    className="remove-submodel-card"
                    onClick={(e) => {
                      e.stopPropagation(); // Prevent triggering the card click
                      handleRemoveTemplate(index);
                    }}
                    style={{
                      position: "absolute",
                      top: "-8px",
                      right: "-8px",
                      background: "#dc3545",
                      color: "white",
                      border: "none",
                      borderRadius: "50%",
                      width: "24px",
                      height: "24px",
                      fontSize: "12px",
                      cursor: "pointer",
                      display: "flex",
                      alignItems: "center",
                      justifyContent: "center",
                      zIndex: 10
                    }}
                  >
                    ✕
                  </button>
                </div>
              ))}
            </div>
        </div>
        </Card.Body>
      </Card>

        <Row className="mb-4 justify-content-start">
          <Col xs="auto">
            <Button
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
              {t("create.buttons.back")}
            </Button>
          </Col>

          <Col xs="auto">
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
              {t("create.buttons.save")}
            </Button>
          </Col>
        </Row>

      <div className="ai-button" onClick={() => setShowChat(!showChat)}>
        <img src={aiAssistantIcon} alt="Assistant" />
      </div>

      {showChat && (
        <div className="chat-window">
          <div className="chat-header">DigiTwin-Assistant</div>
          <div className="chat-body">
            <p>
              Hello, I am DigiTwin-Assistant.
              <br />
              What can I help you with?
            </p>
          </div>
          <input className="chat-input" placeholder="Write your message here." />
        </div>
      )}

      {/* Toast notifications positioned at top right */}
      <ToastContainer 
        position="top-end" 
        className="p-3" 
        style={{ 
          position: 'fixed', 
          top: '20px', 
          right: '20px', 
          zIndex: 9999 
        }}
      >
        {toasts.map((toast) => (
          <Toast
            key={toast.id}
            show={toast.show}
            onClose={() => closeToast(toast.id)}
            bg={toast.variant}
            text={toast.variant === 'warning' ? 'dark' : 'white'}
            autohide
            delay={5000}
          >
            <Toast.Header>
              <strong className="me-auto">
                {toast.variant === 'danger' ? 'Error' : 
                 toast.variant === 'warning' ? 'Warning' : 
                 toast.variant === 'success' ? 'Success' : 'Notification'}
              </strong>
            </Toast.Header>
            <Toast.Body>
              {toast.message}
            </Toast.Body>
          </Toast>
        ))}
      </ToastContainer>
      </Container>
    </div>
  );
}

export default CreatePage;
