import React, { useState, useRef, useEffect, useContext } from "react";
import "../styles/createPage.css";
import helpIcon from "../assets/icons/help.png";
import aiAssistantIcon from "../assets/ai-chatbot-assistant.png";
import tagIcon from "../assets/icons/tags.svg";
import ChevronLeftIcon from "../assets/icons/chevron-left.svg?react";
import FloppyFillIcon from "../assets/icons/floppy-fill.svg?react";
import { Link, useNavigate, useLocation } from "react-router-dom";
import { useTranslation } from "react-i18next";
import { Card, Button } from "react-bootstrap";
import { KeycloakContext } from "../KeycloakContext";

function CreatePage() {
  const { t } = useTranslation();
  const navigate = useNavigate();
  const location = useLocation();
  const { keycloak, authenticated } = useContext(KeycloakContext);

  const [tooltipVisible, setTooltipVisible] = useState(null);
  const [tooltipStyle, setTooltipStyle] = useState({});
  const [showChat, setShowChat] = useState(false);
  
  // State for form data
  const [formData, setFormData] = useState({
    name: "",
    description: "",
    id: "",
    assetKind: "Instance",
    globalAssetId: "",
    specificAssetId: ""
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

  const helpRefs = {
    name: useRef(null),
    description: useRef(null),
    id: useRef(null),
    assetKind: useRef(null),
    globalAssetId: useRef(null),
    specificAssetId: useRef(null),
  };

  const showPopup = (key) => {
    const ref = helpRefs[key];
    if (ref.current) {
      const iconPos = ref.current.getBoundingClientRect();
      setTooltipStyle({
        left: iconPos.right + 15 + "px",
        top: window.scrollY + iconPos.top - 60 + "px",
        display: "block",
        position: "absolute",
        backgroundColor: "rgba(174, 174, 174, 1)",
        color: "black",
        padding: "12px",
        borderRadius: "6px",
        boxShadow: "0 0 10px rgba(0,0,0,0.2)",
        width: "260px",
        zIndex: 10
      });
      setTooltipVisible(key);
    }
  };

  const hidePopup = () => {
    setTooltipVisible(null);
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

  // Handle final save
  const handleSave = async () => {
    const finalData = {
      aasData: formData,
      submodelTemplates: submodelTemplates
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
      
      const response = await fetch('http://localhost:9090/submodels/new', {
        method: 'POST',
        headers: headers,
        body: JSON.stringify(finalData)
      });
      
      if (response.ok) {
        // Clear the templates after successful save
        sessionStorage.removeItem('submodelTemplates');
        setSubmodelTemplates([]);
        
        // Navigate to createComplete page with model name
        navigate('/create/complete', { 
          state: { 
            modelName: formData.name || 'Untitled Model' 
          } 
        });
      } else {
        // Handle error response
        const errorData = await response.json().catch(() => ({}));
        const errorMessage = errorData.message || `Error: ${response.status} ${response.statusText}`;
        console.error('Error saving model:', errorMessage);
        
        if (response.status === 401) {
          alert('Authentication required. Please sign in and try again.');
        } else if (response.status === 403) {
          alert('Access denied. You do not have permission to create models.');
        } else {
          alert(`Failed to save model: ${errorMessage}`);
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
        alert(`Connection error: Unable to reach the server.`);
      } else {
        alert('Network error: Unable to save model. Please check your connection and try again.');
      }
    }
  };

  // Handle form field changes
  const handleFieldChange = (field, value) => {
    setFormData(prev => ({ ...prev, [field]: value }));
  };

  const tooltipText = {
    name: ["Model Name", "This name will be used as the title for your model in the Dashboard."],
    description: ["Model Description", "Optional field for a short description of your model."],
    id: ["AAS Identifier", "Unique identifier for the Asset Administration Shell."],
    assetKind: ["Asset Kind", "Defines whether the asset is an Instance or a Type."],
    globalAssetId: ["Global Asset ID", "Globally unique identifier for the asset this AAS refers to."],
    specificAssetId: ["Specific Asset ID", "Optional additional identifier for the asset."]
  };

  return (
    <div className="create-page-wrapper">
      {/* Progress bar */}
      <div className="d-flex mb-1">
        <div className="text-warning step-progress-item step-progress-left">Fill the Details</div>
        <div className="text-white step-progress-item step-progress-right">All done</div>
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

      <div className="section boxed">
        <h3>General Information</h3>
        <div className="field-block">
          <div className="field-group half">
            <label htmlFor="name" className="help-wrapper">
              name
              <img
                src={helpIcon}
                alt="?"
                className="help-icon"
                ref={helpRefs.name}
                onMouseEnter={() => showPopup("name")}
                onMouseLeave={hidePopup}
              />
            </label>
            <input 
              id="name" 
              placeholder="ex. Office Building"
              value={formData.name}
              onChange={(e) => handleFieldChange('name', e.target.value)}
            />
          </div>
          <div className="field-group auto">
            <label htmlFor="description" className="help-wrapper">
              description
              <img
                src={helpIcon}
                alt="?"
                className="help-icon"
                ref={helpRefs.description}
                onMouseEnter={() => showPopup("description")}
                onMouseLeave={hidePopup}
              />
            </label>
            <input 
              id="description" 
              placeholder="ex. Optional Description"
              value={formData.description}
              onChange={(e) => handleFieldChange('description', e.target.value)}
            />
          </div>
        </div>
      </div>

      <div className="section boxed">
        <h3>AAS Identification</h3>
        <div className="field-block">
          <div className="field-group half">
            <label htmlFor="id" className="help-wrapper">
              id
              <img
                src={helpIcon}
                alt="?"
                className="help-icon"
                ref={helpRefs.id}
                onMouseEnter={() => showPopup("id")}
                onMouseLeave={hidePopup}
              />
            </label>
            <input 
              id="id" 
              placeholder="ex. urn:aas:example:aas:123456"
              value={formData.id}
              onChange={(e) => handleFieldChange('id', e.target.value)}
            />
          </div>
          <div className="field-group auto">
            <label htmlFor="assetKind" className="help-wrapper">
              assetKind
              <img
                src={helpIcon}
                alt="?"
                className="help-icon"
                ref={helpRefs.assetKind}
                onMouseEnter={() => showPopup("assetKind")}
                onMouseLeave={hidePopup}
              />
            </label>
            <select 
              id="assetKind"
              value={formData.assetKind}
              onChange={(e) => handleFieldChange('assetKind', e.target.value)}
            >
              <option>Instance</option>
              <option>Type</option>
            </select>
          </div>
        </div>
      </div>

      <div className="section boxed">
        <h3>Asset Identification</h3>
        <div className="field-block">
          <div className="field-group half">
            <label htmlFor="globalAssetId" className="help-wrapper">
              globalAssetId
              <img
                src={helpIcon}
                alt="?"
                className="help-icon"
                ref={helpRefs.globalAssetId}
                onMouseEnter={() => showPopup("globalAssetId")}
                onMouseLeave={hidePopup}
              />
            </label>
            <input 
              id="globalAssetId" 
              placeholder="ex. urn:aas:example:aas:123456"
              value={formData.globalAssetId}
              onChange={(e) => handleFieldChange('globalAssetId', e.target.value)}
            />
          </div>
          <div className="field-group auto">
            <label htmlFor="specificAssetId" className="help-wrapper">
              specificAssetId
              <img
                src={helpIcon}
                alt="?"
                className="help-icon"
                ref={helpRefs.specificAssetId}
                onMouseEnter={() => showPopup("specificAssetId")}
                onMouseLeave={hidePopup}
              />
            </label>
            <input 
              id="specificAssetId" 
              placeholder="name"
              value={formData.specificAssetId}
              onChange={(e) => handleFieldChange('specificAssetId', e.target.value)}
            />
          </div>
          <div className="field-group auto">
            <label>Value</label>
            <input placeholder="Value" />
          </div>
          <div className="field-group auto">
            <label>&nbsp;</label>
            <button className="add-btn">+ Add</button>
          </div>
        </div>
      </div>

      <div className="section boxed">
        <h3>Submodel Templates</h3>
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
                      <Card.Text style={{ fontSize: "0.875rem", lineHeight: "1.2", pointerEvents: "none" }}>
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
      </div>

      <div className="form-footer">
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

      {tooltipVisible && (
        <div id="mypopup" className="tip" style={tooltipStyle}>
          <h4 style={{ marginTop: 0 }}>{tooltipText[tooltipVisible][0]}</h4>
          <p style={{ marginBottom: 0 }}>{tooltipText[tooltipVisible][1]}</p>
        </div>
      )}
    </div>
  );
}

export default CreatePage;
