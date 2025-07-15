import React, { useState, useRef, useEffect } from "react";
import "../styles/createPage.css";
import helpIcon from "../assets/icons/help.png";
import aiAssistantIcon from "../assets/ai-chatbot-assistant.png";
import { Link, useNavigate } from "react-router-dom";
import { useTranslation } from "react-i18next";

function CreatePage() {
  const { t } = useTranslation();
  const navigate = useNavigate();

  const [tooltipVisible, setTooltipVisible] = useState(false);
  const [tooltipStyle, setTooltipStyle] = useState({});
  const [showChat, setShowChat] = useState(false);
  const helpRef = useRef(null);

  const showPopup = () => {
    if (helpRef.current) {
      const iconPos = helpRef.current.getBoundingClientRect();
      setTooltipStyle({
        left: (iconPos.right + 20) + "px",
        top: (window.scrollY + iconPos.top - 60) + "px",
        display: "block",
        position: "absolute",
        backgroundColor: "white",
        color: "black",
        padding: "12px",
        borderRadius: "6px",
        boxShadow: "0 0 10px rgba(0,0,0,0.2)",
        width: "260px",
        zIndex: 10
      });
      setTooltipVisible(true);
    }
  };

  const hidePopup = () => {
    setTooltipVisible(false);
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
                id="myicon"
                src={helpIcon}
                alt="?"
                className="help-icon"
                ref={helpRef}
                onMouseEnter={showPopup}
                onMouseLeave={hidePopup}
              />
            </label>
            <input id="name" placeholder="ex. Office Building" />
          </div>
          <div className="field-group auto">
            <label htmlFor="description">description</label>
            <input id="description" placeholder="ex. Optional Description" />
          </div>
        </div>
      </div>

      {tooltipVisible && (
        <div id="mypopup" className="tooltip" style={tooltipStyle}>
          <h4 style={{ marginTop: 0 }}>Model Name</h4>
          <p style={{ marginBottom: 0 }}>This name will be used as the title for your model in the Dashboard.</p>
        </div>
      )}

      <div className="section boxed">
        <h3>AAS Identification</h3>
        <div className="field-block">
          <div className="field-group half">
            <label htmlFor="id">id</label>
            <input id="id" placeholder="ex. urn:aas:example:aas:123456" />
          </div>
          <div className="field-group auto">
            <label htmlFor="assetKind">
              assetKind
              <img src={helpIcon} alt="?" className="help-icon" />
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
            <label htmlFor="globalAssetId">globalAssetId</label>
            <input id="globalAssetId" placeholder="ex. urn:aas:example:aas:123456" />
          </div>
          <div className="field-group auto">
            <label htmlFor="specificAssetId">specificAssetId</label>
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
    </div>
  );
}

export default CreatePage;
