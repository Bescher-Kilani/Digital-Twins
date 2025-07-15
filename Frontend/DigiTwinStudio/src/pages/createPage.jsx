import React, { useState, useRef, useEffect } from "react";
import "../styles/createPage.css";
import helpIcon from "../assets/icons/help.png";
import aiAssistantIcon from "../assets/ai-chatbot-assistant.png";
import { Link, useNavigate } from "react-router-dom";
import { useTranslation } from "react-i18next";

function CreatePage() {
  const { t } = useTranslation();
  const navigate = useNavigate();

  const [tooltipVisible, setTooltipVisible] = useState(null);
  const [tooltipStyle, setTooltipStyle] = useState({});
  const [showChat, setShowChat] = useState(false);

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
      <div className="step-header">
        <span>Fill the Details</span>
        <div className="step-progress">
          <div className="dot active"></div>
          <div className="dot"></div>
        </div>
        <span>All done</span>
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
            <input id="name" placeholder="ex. Office Building" />
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
            <input id="description" placeholder="ex. Optional Description" />
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
            <input id="id" placeholder="ex. urn:aas:example:aas:123456" />
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
            <select id="assetKind">
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
            <input id="globalAssetId" placeholder="ex. urn:aas:example:aas:123456" />
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
            <input id="specificAssetId" placeholder="name" />
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
          <button className="add-submodel">+ Add Submodel</button>
        </div>
      </div>

      <div className="form-footer">
        <button className="back-btn">← Back</button>
        <button className="save-btn">💾 Save</button>
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
