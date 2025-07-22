import React, { useState, useRef, useEffect } from "react";
import "../styles/createPage.css";
import helpIcon from "../assets/icons/help.png";
import aiAssistantIcon from "../assets/ai-chatbot-assistant.png";
import tagIcon from "../assets/icons/tags.svg";
import ChevronLeftIcon from "../assets/icons/chevron-left.svg?react";
import FloppyFillIcon from "../assets/icons/floppy-fill.svg?react";
import { Link, useNavigate, useLocation } from "react-router-dom";
import { useTranslation } from "react-i18next";
import { Card, Button } from "react-bootstrap";

function CreatePage() {
  const { t } = useTranslation();
  const navigate = useNavigate();
  const location = useLocation();

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
      console.log('Adding new template:', newTemplate);
      
      setSubmodelTemplates(prev => {
        console.log('Current templates before adding:', prev);
        const updated = [...prev, newTemplate];
        console.log('Updated templates:', updated);
        return updated;
      });
      
      // Restore the original form data if it exists
      if (location.state?.originalFormData) {
        setFormData(location.state.originalFormData);
      }
      
      // Clear the state to prevent re-adding on refresh
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
    navigate('/templates', { 
      state: { 
        fromCreate: true,
        formData: formData 
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
      // TODO: Replace with actual API endpoint
      console.log('Saving AASX model:', finalData);
      // const response = await fetch('/api/create-aasx', {
      //   method: 'POST',
      //   headers: { 'Content-Type': 'application/json' },
      //   body: JSON.stringify(finalData)
      // });
      // if (response.ok) {
      //   sessionStorage.removeItem('submodelTemplates'); // Clear saved templates
      //   navigate('/dashboard');
      // }
      
      // Clear the templates after successful save
      sessionStorage.removeItem('submodelTemplates');
      setSubmodelTemplates([]);
      
      // Navigate to createComplete page with model name
      navigate('/create/complete', { 
        state: { 
          modelName: formData.name || 'Untitled Model' 
        } 
      });
    } catch (error) {
      console.error('Error saving model:', error);
      alert('Error saving model');
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
                    className="text-white text-center"
                    style={{
                      width: "140px",
                      height: "180px",
                      padding: "1rem",
                      borderRadius: "5px",
                      border: "3px solid #0E4175",
                      background: "linear-gradient(180deg, #002C5A 0%, #002C59 50%, #002C5D 100%)"
                    }}
                  >
                    <Card.Body className="d-flex flex-column align-items-center justify-content-center p-0 h-100">
                      <Card.Img
                        variant="top"
                        src={tagIcon}
                        style={{ width: "50px", margin: "0 auto 0.5rem auto" }}
                        alt="Template Icon"
                      />
                      <Card.Text style={{ fontSize: "0.875rem", lineHeight: "1.2" }}>
                        {template.title}
                      </Card.Text>
                    </Card.Body>
                  </Card>
                  <button 
                    className="remove-submodel-card"
                    onClick={() => handleRemoveTemplate(index)}
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
                      justifyContent: "center"
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
