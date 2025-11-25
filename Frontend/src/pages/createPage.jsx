import React, { useState, useEffect, useContext } from "react";
import { Card, Button, Toast, ToastContainer, Container, Row, Col, OverlayTrigger, Tooltip } from "react-bootstrap";
import { useNavigate, useLocation, useParams } from "react-router-dom";
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
import { authenticatedFetch } from "../utils/tokenManager";
import "../styles/createPage.css";

function CreatePage() {
  const { t } = useTranslation();
  const navigate = useNavigate();
  const location = useLocation();
  const { modelId } = useParams();
  const { keycloak, authenticated } = useContext(KeycloakContext);
  const API_URL = import.meta.env.VITE_BACKEND_URL || 'http://localhost:9090';

  // State to store the effective modelId (from URL params or preserved from navigation)
  const [effectiveModelId, setEffectiveModelId] = useState(() => {
    // Initialize with modelId from URL if available, or from preserved state
    return modelId || location.state?.preserveModelId;
  });

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

  // State to track loading of existing models
  const [loadingModel, setLoadingModel] = useState(false);

  // Helper function to fix structural issues in submodel elements
  const fixSubmodelStructure = (submodel) => {
    // Create a deep copy to avoid modifying the original
    const fixedSubmodel = JSON.parse(JSON.stringify(submodel));
    
    console.log('Fixing submodel structure for:', submodel.idShort);
    
    // Recursively fix submodel elements
    const fixElements = (elements) => {
      if (!elements || !Array.isArray(elements)) return elements;
      
      return elements.map(element => {
        const fixedElement = { ...element };
        
        // Fix multi-language properties that are stored as single objects instead of arrays
        if (element.value && typeof element.value === 'object' && !Array.isArray(element.value)) {
          if (element.value.language && (element.value.text !== undefined || element.value.text !== null)) {
            // This should be a multi-language property
            console.log(`🔧 Fixing MLP structure for ${element.idShort}: converting object to array`);
            console.log('   Original value:', element.value);
            fixedElement.modelType = 'MultiLanguageProperty';
            fixedElement.value = [element.value]; // Convert to array
            console.log('   Fixed value:', fixedElement.value);
          }
        }
        
        // Fix nested elements recursively
        if (element.value && Array.isArray(element.value)) {
          fixedElement.value = fixElements(element.value);
        }
        
        return fixedElement;
      });
    };
    
    // Fix the submodel elements
    if (fixedSubmodel.submodelElements) {
      fixedSubmodel.submodelElements = fixElements(fixedSubmodel.submodelElements);
    }
    
    console.log('Structure fixing completed for:', fixedSubmodel.idShort);
    return fixedSubmodel;
  };

  // Function to load existing model data
  const loadExistingModel = async (id) => {
    if (!id || !authenticated) return;
    
    try {
      setLoadingModel(true);
      const response = await authenticatedFetch(`${API_URL}/models/${id}`, {
        method: 'GET'
      }, keycloak);

      if (!response.ok) {
        throw new Error(`Failed to load model: ${response.status} ${response.statusText}`);
      }

      const modelData = await response.json();
      console.log('Loaded model data:', modelData);
      console.log('🔍 LOADED DEBUG: ManufacturerProductType in loaded data:', 
        modelData.submodels?.[0]?.submodelElements?.find(e => e.idShort === 'ManufacturerProductType')?.value);
      console.log('🔍 LOADED DEBUG: Raw ManufacturerProductType element:', 
        modelData.submodels?.[0]?.submodelElements?.find(e => e.idShort === 'ManufacturerProductType'));

      // Transform the loaded data back to the form format
      if (modelData.aas) {
        setFormData({
          assetInformation: {
            assetKind: modelData.aas.assetInformation?.assetKind || "Instance",
            assetType: modelData.aas.assetInformation?.assetType || "testType",
            defaultThumbnail: modelData.aas.assetInformation?.defaultThumbnail || null,
            globalAssetId: modelData.aas.assetInformation?.globalAssetId || "",
            specificAssetIds: modelData.aas.assetInformation?.specificAssetIds || [{ name: "", value: "" }]
          },
          derivedFrom: modelData.aas.derivedFrom || null,
          submodels: modelData.aas.submodels || [],
          embeddedDataSpecifications: modelData.aas.embeddedDataSpecifications || [],
          extensions: modelData.aas.extensions || [],
          administration: modelData.aas.administration || null,
          id: modelData.aas.id || "",
          category: modelData.aas.category || null,
          description: modelData.aas.description && modelData.aas.description.length > 0 
            ? modelData.aas.description 
            : [{ language: "en", text: "" }],
          displayName: modelData.aas.displayName || [],
          idShort: modelData.aas.idShort || ""
        });
      }

      // Handle submodels - convert back to template format if needed
      if (modelData.submodels && modelData.submodels.length > 0) {
        const convertedTemplates = modelData.submodels.map((submodel, index) => {
          // First, fix any structural issues in the submodel before processing
          const fixedSubmodel = fixSubmodelStructure(submodel);
          
          // Extract user data from submodel elements
          const extractedData = {};
          
          if (fixedSubmodel.submodelElements) {
            fixedSubmodel.submodelElements.forEach(element => {
              extractUserDataFromElement(element, extractedData);
            });
          }
          
          console.log('Extracted data for submodel:', fixedSubmodel.idShort, extractedData);
          console.log('Keys in extracted data:', Object.keys(extractedData));
          console.log('Number of extracted properties:', Object.keys(extractedData).length);
          console.log('🔍 EXTRACTED ManufacturerProductType:', extractedData.ManufacturerProductType);
          console.log('🔍 EXTRACTED ManufacturerProductRoot:', extractedData.ManufacturerProductRoot);
          
          // Create a clean template (remove user values from the template)
          const cleanTemplate = createCleanTemplate(fixedSubmodel);
          
          // Create a proper template structure
          const templateStructure = {
            selectedTemplate: {
              templateData: {
                json: cleanTemplate // Use clean template without user values - this should have proper modelType
              }
            },
            templateData: {
              json: cleanTemplate // Use clean template for parsing too, not the filled one
            },
            data: extractedData, // This should contain the extracted user values
            id: fixedSubmodel.id || `loaded_submodel_${index}`,
            title: fixedSubmodel.idShort || fixedSubmodel.displayName?.[0]?.text || `Submodel ${index + 1}` // Add proper title
          };
          
          console.log('Final template structure created:', templateStructure);
          console.log('Template structure data property:', templateStructure.data);
          
          return templateStructure;
        });
        
        // CRITICAL FIX: Preserve existing templates that have preservedFormData
        setSubmodelTemplates(prevTemplates => {
          console.log('🔄 Merging loaded templates with existing preserved form data');
          console.log('Previous templates count:', prevTemplates.length);
          console.log('New converted templates count:', convertedTemplates.length);
          
          return convertedTemplates.map((newTemplate, index) => {
            const existingTemplate = prevTemplates[index];
            
            // If existing template has preservedFormData, keep it instead of overwriting
            if (existingTemplate?.preservedFormData) {
              console.log(`✅ Preserving form data for template ${index} (${newTemplate.title})`);
              console.log('Preserved form data keys:', Object.keys(existingTemplate.preservedFormData));
              console.log('ManufacturerProductType in preserved data:', existingTemplate.preservedFormData.ManufacturerProductType);
              console.log('ManufacturerProductType in new extracted data:', newTemplate.data.ManufacturerProductType);
              
              return {
                ...newTemplate,
                data: existingTemplate.preservedFormData,  // Use preserved data instead of extracted
                preservedFormData: existingTemplate.preservedFormData  // Keep the preserved data
              };
            } else {
              console.log(`📥 Using newly extracted data for template ${index} (${newTemplate.title})`);
              console.log('ManufacturerProductType in extracted data:', newTemplate.data.ManufacturerProductType);
              return newTemplate;
            }
          });
        });
      }

      showToast(t("createPage.modelLoaded"), 'success');
    } catch (error) {
      console.error('Error loading model:', error);
      showToast(t("createPage.loadModelError") || 'Failed to load model', 'danger');
    } finally {
      setLoadingModel(false);
    }
  };

  // Helper function to extract user data from submodel elements
  const extractUserDataFromElement = (element, data, path = "") => {
    if (!element || !element.idShort) return;
    
    const currentPath = path ? `${path}.${element.idShort}` : element.idShort;
    
    // Determine element type with better detection logic
    let elementType = element.modelType;
    if (!elementType) {
      // Check for multi-language property first (array with language/text structure)
      if (Array.isArray(element.value) && element.value.length > 0 && 
          element.value[0].language && (element.value[0].text !== undefined || element.value[0].text !== null)) {
        elementType = 'MultiLanguageProperty';
      }
      // Check for submodel element collection (array with idShort elements)
      else if (Array.isArray(element.value) && element.value.length > 0 && element.value[0].idShort) {
        elementType = 'SubmodelElementCollection';
      }
      // Check for File type
      else if (element.contentType) {
        elementType = 'File';
      }
      // Check for Property (has valueType or simple value)
      else if (element.valueType || (typeof element.value === 'string' || typeof element.value === 'number')) {
        elementType = 'Property';
      }
      // If value is an object but not array, might be incorrectly structured data
      else if (element.value && typeof element.value === 'object' && !Array.isArray(element.value)) {
        // Try to detect if this should be a multi-language property
        if (element.value.language && element.value.text !== undefined) {
          elementType = 'MultiLanguageProperty';
          // Fix the structure to be an array
          element.value = [element.value];
        }
      }
    }
    
    console.log(`Processing element: ${element.idShort}, type: ${elementType}, value:`, element.value);
    
    switch (elementType) {
      case 'Property': {
        // Always extract property values, even if empty, to maintain field structure
        const propertyValue = element.value !== undefined && element.value !== null ? element.value : "";
        data[element.idShort] = propertyValue;
        if (path) {
          data[currentPath] = propertyValue;
        }
        console.log(`Extracted Property ${element.idShort}:`, propertyValue);
        break;
      }
        
      case 'MultiLanguageProperty':
        if (element.value && Array.isArray(element.value) && element.value.length > 0) {
          const mlpValue = element.value.map(langValue => ({
            language: langValue.language === 'en' ? 'English' : 
                     langValue.language === 'de' ? 'German' : 
                     langValue.language,
            value: langValue.text ? langValue.text.replace(/^"|"$/g, '') : '' // Remove quotes
          }));
          data[element.idShort] = mlpValue;
          if (path) {
            data[currentPath] = mlpValue;
          }
          console.log(`Extracted MultiLanguageProperty ${element.idShort}:`, mlpValue);
        }
        // Handle case where MLP was incorrectly stored as single object
        else if (element.value && typeof element.value === 'object' && element.value.language) {
          const mlpValue = [{
            language: element.value.language === 'en' ? 'English' : 
                     element.value.language === 'de' ? 'German' : 
                     element.value.language,
            value: element.value.text ? element.value.text.replace(/^"|"$/g, '') : ''
          }];
          data[element.idShort] = mlpValue;
          if (path) {
            data[currentPath] = mlpValue;
          }
          console.log(`Extracted corrected MultiLanguageProperty ${element.idShort}:`, mlpValue);
        }
        break;
        
      case 'SubmodelElementCollection': {
        // Check for AddressInformation by name (case insensitive) or by structure
        const isAddressInfo = element.idShort && (
          element.idShort.toLowerCase().includes('address') ||
          element.idShort === 'AddressInformation' ||
          (element.value && Array.isArray(element.value) && 
           element.value.some(child => 
             child.idShort && (child.idShort === 'Street' || child.idShort === 'CityTown' || 
                             child.idShort === 'HouseNumber' || child.idShort === 'NationalCode')
           ))
        );
        
        if (isAddressInfo && element.value && Array.isArray(element.value)) {
          // Special handling for AddressInformation
          const addressData = {};
          element.value.forEach(addressElement => {
            switch (addressElement.idShort) {
              case 'Street':
                addressData.street = addressElement.value || '';
                break;
              case 'HouseNumber':
                addressData.streetNumber = addressElement.value || '';
                break;
              case 'CityTown':
                addressData.city = addressElement.value || '';
                break;
              case 'NationalCode':
                addressData.country = addressElement.value || '';
                break;
            }
          });
          data[element.idShort] = [addressData];
          if (path) {
            data[currentPath] = [addressData];
          }
          console.log(`Extracted AddressInformation:`, addressData);
        } else if (element.value && Array.isArray(element.value)) {
          // Recursively process collection elements
          element.value.forEach(childElement => {
            extractUserDataFromElement(childElement, data, currentPath);
          });
        }
        break;
      }
        
      case 'SubmodelElementList':
        if (element.value && Array.isArray(element.value)) {
          const listData = [];
          element.value.forEach((listItem, itemIndex) => {
            if (listItem.value && Array.isArray(listItem.value)) {
              const itemData = {};
              listItem.value.forEach(listElement => {
                extractUserDataFromElement(listElement, itemData);
              });
              listData.push({ data: { [itemIndex]: itemData } });
            } else if (listItem.modelType === 'Property' && listItem.value) {
              listData.push(listItem.value);
            }
          });
          
          if (listData.length > 0) {
            data[element.idShort] = listData;
            if (path) {
              data[currentPath] = listData;
            }
            console.log(`Extracted SubmodelElementList ${element.idShort}:`, listData);
          }
        }
        break;
        
      case 'File':
        if (element.value !== undefined && element.value !== null && element.value !== "") {
          data[element.idShort] = element.value;
          if (path) {
            data[currentPath] = element.value;
          }
          console.log(`Extracted File ${element.idShort}:`, element.value);
        }
        break;
        
      default: {
        // For other element types or unrecognized types, try to extract based on value structure
        if (element.value !== undefined && element.value !== null) {
          if (typeof element.value === 'string' && element.value !== '') {
            // Treat as simple property
            data[element.idShort] = element.value;
            if (path) {
              data[currentPath] = element.value;
            }
            console.log(`Extracted unknown element as property ${element.idShort}:`, element.value);
          } else if (Array.isArray(element.value)) {
            // Check if it's a multi-language property or collection
            if (element.value.length > 0 && element.value[0].language && (element.value[0].text !== undefined || element.value[0].text !== null)) {
              // Multi-language property
              const mlpValue = element.value.map(langValue => ({
                language: langValue.language === 'en' ? 'English' : 
                         langValue.language === 'de' ? 'German' : 
                         langValue.language,
                value: langValue.text ? langValue.text.replace(/^"|"$/g, '') : ''
              }));
              data[element.idShort] = mlpValue;
              if (path) {
                data[currentPath] = mlpValue;
              }
              console.log(`Extracted unknown element as MLP ${element.idShort}:`, mlpValue);
            } else if (element.value.length > 0 && element.value[0].idShort) {
              // Collection - recursively process
              element.value.forEach(childElement => {
                extractUserDataFromElement(childElement, data, currentPath);
              });
            }
          } else if (typeof element.value === 'object') {
            // Handle incorrectly structured multi-language properties
            if (element.value.language && (element.value.text !== undefined || element.value.text !== null)) {
              const mlpValue = [{
                language: element.value.language === 'en' ? 'English' : 
                         element.value.language === 'de' ? 'German' : 
                         element.value.language,
                value: element.value.text ? element.value.text.replace(/^"|"$/g, '') : ''
              }];
              data[element.idShort] = mlpValue;
              if (path) {
                data[currentPath] = mlpValue;
              }
              console.log(`Extracted corrected object as MLP ${element.idShort}:`, mlpValue);
            } else {
              // Try to extract as string representation if it's an object
              try {
                const stringValue = JSON.stringify(element.value);
                if (stringValue !== '{}' && stringValue !== '[]') {
                  console.warn(`Element ${element.idShort} has object value that couldn't be properly classified:`, element.value);
                  // Don't extract [object Object] - skip this field
                }
              } catch (e) {
                console.warn(`Error processing element ${element.idShort}:`, e);
              }
            }
          }
        }
        break;
      }
    }
  };

  // Helper function to create a clean template (remove user values)
  const createCleanTemplate = (filledSubmodel) => {
    // Create a deep copy of the submodel
    const cleanTemplate = JSON.parse(JSON.stringify(filledSubmodel));
    
    // Recursively clean the submodel elements
    const cleanSubmodelElements = (elements) => {
      if (!elements || !Array.isArray(elements)) return elements;
      
      return elements.map(element => {
        const cleanElement = { ...element };
        
        // Ensure modelType is preserved - if missing, try to infer it
        if (!cleanElement.modelType) {
          if (element.value && Array.isArray(element.value) && element.value.length > 0) {
            if (element.value[0].language && (element.value[0].text !== undefined || element.value[0].text !== null)) {
              cleanElement.modelType = 'MultiLanguageProperty';
            } else if (element.value[0].idShort) {
              cleanElement.modelType = 'SubmodelElementCollection';
            }
          } else if (element.contentType) {
            cleanElement.modelType = 'File';
          } else if (element.valueType || typeof element.value === 'string' || typeof element.value === 'number') {
            cleanElement.modelType = 'Property';
          }
        }
        
        switch (cleanElement.modelType) {
          case 'Property':
            cleanElement.value = '';
            break;
            
          case 'MultiLanguageProperty':
            cleanElement.value = [];
            break;
            
          case 'SubmodelElementCollection': {
            // Check for address information using the same logic as extraction
            const isAddressInfo = element.idShort && (
              element.idShort.toLowerCase().includes('address') ||
              element.idShort === 'AddressInformation' ||
              (element.value && Array.isArray(element.value) && 
               element.value.some(child => 
                 child.idShort && (child.idShort === 'Street' || child.idShort === 'CityTown' || 
                                 child.idShort === 'HouseNumber' || child.idShort === 'NationalCode')
               ))
            );
            
            if (isAddressInfo) {
              // Reset address information to empty structure
              cleanElement.value = [
                {
                  "modelType": "Property", 
                  "idShort": "Street",
                  "value": "",
                  "valueType": "xs:string"
                },
                {
                  "modelType": "Property",
                  "idShort": "HouseNumber", 
                  "value": "",
                  "valueType": "xs:string"
                },
                {
                  "modelType": "Property",
                  "idShort": "CityTown",
                  "value": "",
                  "valueType": "xs:string"
                },
                {
                  "modelType": "Property",
                  "idShort": "NationalCode",
                  "value": "",
                  "valueType": "xs:string"
                }
              ];
            } else if (element.value && Array.isArray(element.value)) {
              cleanElement.value = cleanSubmodelElements(element.value);
            }
            break;
          }
            
          case 'SubmodelElementList':
            if (element.value && Array.isArray(element.value) && element.value.length > 0) {
              // Keep the first item as template, but clean its values
              const templateItem = JSON.parse(JSON.stringify(element.value[0]));
              if (templateItem.value && Array.isArray(templateItem.value)) {
                templateItem.value = cleanSubmodelElements(templateItem.value);
              }
              cleanElement.value = [templateItem];
            }
            break;
            
          case 'File':
            cleanElement.value = '';
            break;
            
          default:
            // For other element types, try to clean nested elements if they exist
            if (element.value && Array.isArray(element.value)) {
              cleanElement.value = cleanSubmodelElements(element.value);
            }
            break;
        }
        
        return cleanElement;
      });
    };
    
    // Clean the submodel elements
    if (cleanTemplate.submodelElements) {
      cleanTemplate.submodelElements = cleanSubmodelElements(cleanTemplate.submodelElements);
    }
    
    return cleanTemplate;
  };

  // Effect to update effectiveModelId when modelId changes or when preserved modelId is available
  useEffect(() => {
    if (modelId) {
      // URL has modelId, use it
      setEffectiveModelId(modelId);
    } else if (location.state?.preserveModelId) {
      // No modelId in URL, but we have a preserved one - always use it
      setEffectiveModelId(location.state.preserveModelId);
    }
    // If neither exists, effectiveModelId remains as-is (could be undefined for new models)
  }, [modelId, location.state?.preserveModelId, effectiveModelId]);

  // Effect to load model data when effectiveModelId is present
  useEffect(() => {
    if (effectiveModelId && authenticated && !loadingModel) {
      loadExistingModel(effectiveModelId);
    }
  }, [effectiveModelId, authenticated]); // eslint-disable-line react-hooks/exhaustive-deps

  // Persist submodel templates to sessionStorage whenever they change
  useEffect(() => {
    sessionStorage.setItem('submodelTemplates', JSON.stringify(submodelTemplates));
  }, [submodelTemplates]);

  // Handle returning template data from /templates/create
  useEffect(() => {
    if (location.state?.templateData) {
      const newTemplate = location.state.templateData;
      
      // Fix the structure if it got nested during the save process
      let fixedTemplate = { ...newTemplate };
      
      // Check if selectedTemplate has nested templateData and fix it
      if (fixedTemplate.selectedTemplate?.templateData?.templateData) {
        fixedTemplate.selectedTemplate = {
          templateData: fixedTemplate.selectedTemplate.templateData.templateData
        };
      }
      
      // Also fix templateData if it's nested
      if (fixedTemplate.templateData?.templateData) {
        fixedTemplate.templateData = fixedTemplate.templateData.templateData;
      }
      
      // CRITICAL FIX: When template comes back from createTemplate.jsx, 
      // make sure the template.data contains the new form data, not the old extracted data
      if (fixedTemplate.data) {
        console.log('📋 Updating template with new form data from createTemplate.jsx');
        console.log('New form data keys:', Object.keys(fixedTemplate.data));
        console.log('ManufacturerProductType in new form data:', fixedTemplate.data.ManufacturerProductType);
      }
      
      // Check if this is an edit operation
      if (location.state?.editingTemplateIndex !== undefined) {
        const editIndex = location.state.editingTemplateIndex;
        console.log('Updating existing template at index:', editIndex);
        
        setSubmodelTemplates(prev => {
          const updated = [...prev];
          // CRITICAL: Replace the template's data field with the new form data
          // AND preserve the form data explicitly as preservedFormData
          updated[editIndex] = {
            ...fixedTemplate,
            preservedFormData: fixedTemplate.data  // Store the new form data as preservedFormData
          };
          return updated;
        });
      } else {
        // Adding new template
        setSubmodelTemplates(prev => {
          const updated = [...prev, fixedTemplate];
          return updated;
        });
      }
      
      // Restore the original form data if it exists
      if (location.state?.originalFormData) {
        setFormData(location.state.originalFormData);
      }
      
      // Clear the state to prevent re-adding on refresh, but preserve modelId in the URL if we have one
      const preservedModelId = location.state?.preserveModelId || effectiveModelId;
      
      if (preservedModelId && !modelId) {
        // Navigate back to the proper URL with modelId to maintain URL consistency
        setEffectiveModelId(preservedModelId); // Ensure state is updated immediately
        navigate(`/create/${preservedModelId}`, { replace: true, state: {} });
      } else {
        navigate(location.pathname, { replace: true, state: {} });
      }
    }
    
    // Handle form data restoration from back navigation
    if (location.state?.restoredFormData) {
      setFormData(location.state.restoredFormData);
      
      // Only restore submodel templates if they are explicitly provided
      // Otherwise, let the sessionStorage initialization handle templates
      if (location.state?.restoredSubmodelTemplates !== undefined) {
        console.log('Restoring submodel templates from back navigation:', location.state.restoredSubmodelTemplates);
        setSubmodelTemplates(location.state.restoredSubmodelTemplates);
      } else {
        console.log('No restored templates provided, keeping current templates from sessionStorage');
      }
      
      // Clear the state to prevent re-restoration on refresh, but preserve modelId in the URL if we have one
      const preservedModelId = location.state?.preserveModelId || effectiveModelId;
      console.log('Attempting to preserve modelId after form restoration:', preservedModelId, 'from location.state?.preserveModelId:', location.state?.preserveModelId, 'or effectiveModelId:', effectiveModelId);
      
      if (preservedModelId && !modelId) {
        // Navigate back to the proper URL with modelId to maintain URL consistency
        console.log('Restoring URL with preserved modelId after form restoration:', preservedModelId);
        setEffectiveModelId(preservedModelId); // Ensure state is updated immediately
        navigate(`/create/${preservedModelId}`, { replace: true, state: {} });
      } else {
        navigate(location.pathname, { replace: true, state: {} });
      }
    }
  }, [location.state, navigate, location.pathname, modelId, effectiveModelId, setEffectiveModelId]);

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
        formData: formData,
        preserveModelId: effectiveModelId // Preserve the effectiveModelId for navigation back
      } 
    });
  };

  // Handle editing existing submodel template
  const handleEditTemplate = (templateIndex) => {
    const templateToEdit = submodelTemplates[templateIndex];
    
    // Determine which template data to use - prefer templateData if available, otherwise use selectedTemplate
    const templateDataToUse = templateToEdit.templateData || templateToEdit.selectedTemplate;
    
    // Create the correct selectedTemplate structure for getTemplateConfig
    // getTemplateConfig expects: selectedTemplate.templateData.json.submodelElements
    const selectedTemplateForConfig = {
      templateData: templateDataToUse // This should have the {json: {...}} structure
    };
    
    console.log('Selected template for config:', selectedTemplateForConfig);
    
    // Navigate to template creation page with existing template data for editing
    navigate('/templates/create', {
      state: {
        fromCreate: true,
        formData: formData,
        currentSubmodelTemplates: submodelTemplates, // Include current templates
        editingTemplate: {
          index: templateIndex,
          data: templateToEdit // Pass the full template including data
        },
        selectedTemplate: selectedTemplateForConfig, // Use corrected structure
        userData: templateToEdit.data, // Explicitly pass the user data
        preserveModelId: effectiveModelId // Preserve the effectiveModelId for navigation back
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
      
      elements.forEach((element, elementIndex) => {
        const idShort = element.idShort;
        const fullPath = currentPath ? `${currentPath}.${idShort}` : idShort;
        
        console.log(`Processing element ${elementIndex}: ${idShort}, modelType: ${element.modelType}`);
        
        // Ensure modelType is always set
        if (!element.modelType) {
          console.warn(`Missing modelType for element ${idShort}, attempting to infer...`);
          if (element.valueType || typeof element.value === 'string' || typeof element.value === 'number') {
            element.modelType = 'Property';
          } else if (Array.isArray(element.value) && element.value.length > 0 && element.value[0].language) {
            element.modelType = 'MultiLanguageProperty';
          } else if (element.contentType) {
            element.modelType = 'File';
          } else if (Array.isArray(element.value) && element.value.length > 0 && element.value[0].idShort) {
            element.modelType = 'SubmodelElementCollection';
          } else if (element.typeValueListElement === 'SUBMODEL_ELEMENT_COLLECTION') {
            // Check for typeValueListElement which indicates this is a collection
            element.modelType = 'SubmodelElementCollection';
          } else if (element.typeValueListElement === 'SUBMODEL_ELEMENT_LIST') {
            element.modelType = 'SubmodelElementList';
          } else if (element.orderRelevant !== undefined && Array.isArray(element.value)) {
            // Another indicator of SubmodelElementCollection - has orderRelevant property
            element.modelType = 'SubmodelElementCollection';
          } else if (Array.isArray(element.value)) {
            // Default to SubmodelElementCollection for arrays without specific indicators
            element.modelType = 'SubmodelElementCollection';
          } else {
            // Default fallback
            element.modelType = 'Property';
          }
          console.log(`Inferred modelType for ${idShort}: ${element.modelType}`);
        }
        
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
          console.log(`  Found data for ${idShort}:`, value);
          
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
          console.log(`  No data found for ${idShort}, keeping template default`);
          
          // Ensure we have valid default values for required fields
          switch (element.modelType) {
            case 'Property':
              if (element.value === undefined || element.value === null) {
                element.value = '';
              }
              break;
            case 'MultiLanguageProperty':
              if (!element.value || !Array.isArray(element.value)) {
                element.value = [];
              }
              break;
            case 'File':
              if (element.value === undefined || element.value === null) {
                element.value = '';
              }
              break;
            case 'SubmodelElementCollection':
            case 'SubmodelElementList':
              // For collections and lists, we still need to recursively process nested elements
              // to ensure they all have proper modelType, even if we don't have user data for this specific element
              if (element.value && Array.isArray(element.value)) {
                updateSubmodelElements(element.value, data, fullPath);
              }
              break;
          }
        }
        
        console.log(`Final element ${elementIndex} (${idShort}):`, {
          modelType: element.modelType,
          value: element.value,
          valueType: element.valueType,
          idShort: element.idShort
        });
        
        // Recursively ensure all nested elements also have modelType set
        if (element.value && Array.isArray(element.value)) {
          element.value.forEach((nestedElement) => {
            // Process both elements with idShort and unnamed elements (like some list items)
            if (typeof nestedElement === 'object') {
              if (!nestedElement.modelType) {
                const elementName = nestedElement.idShort || 'unnamed';
                console.warn(`Missing modelType for nested element ${elementName}, attempting to infer...`);
                
                if (nestedElement.valueType || typeof nestedElement.value === 'string' || typeof nestedElement.value === 'number') {
                  nestedElement.modelType = 'Property';
                } else if (Array.isArray(nestedElement.value) && nestedElement.value.length > 0 && nestedElement.value[0].language) {
                  nestedElement.modelType = 'MultiLanguageProperty';
                } else if (nestedElement.contentType) {
                  nestedElement.modelType = 'File';
                } else if (Array.isArray(nestedElement.value) && nestedElement.value.length > 0 && nestedElement.value[0].idShort) {
                  nestedElement.modelType = 'SubmodelElementCollection';
                } else if (nestedElement.typeValueListElement === 'SUBMODEL_ELEMENT_COLLECTION') {
                  nestedElement.modelType = 'SubmodelElementCollection';
                } else if (nestedElement.typeValueListElement === 'SUBMODEL_ELEMENT_LIST') {
                  nestedElement.modelType = 'SubmodelElementList';
                } else if (nestedElement.orderRelevant !== undefined && Array.isArray(nestedElement.value)) {
                  nestedElement.modelType = 'SubmodelElementCollection';
                } else if (Array.isArray(nestedElement.value)) {
                  // Special case: check if element is missing idShort but has nested structure
                  if (!nestedElement.idShort && nestedElement.value.length > 0) {
                    nestedElement.modelType = 'SubmodelElementList';
                  } else {
                    nestedElement.modelType = 'SubmodelElementCollection';
                  }
                } else {
                  nestedElement.modelType = 'Property';
                }
                console.log(`Inferred modelType for nested element ${elementName}: ${nestedElement.modelType}`);
              }
              
              // Recursively process deeper nested elements
              if (nestedElement.value && Array.isArray(nestedElement.value)) {
                const nestedPath = nestedElement.idShort ? `${fullPath}.${nestedElement.idShort}` : fullPath;
                updateSubmodelElements(nestedElement.value, data, nestedPath);
              }
            }
          });
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
      // CRITICAL FIX: Use preservedFormData if available (from createTemplate.jsx), 
      // otherwise fall back to template.data (for templates that haven't been edited)
      const dataToUse = template.preservedFormData || template.data;
      
      return mergeDataIntoTemplate(template, dataToUse);
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
      // Log the HTTP request details for debugging
      console.log('=== HTTP REQUEST DEBUG ===');
      console.log('Request URL:', effectiveModelId 
        ? `${API_URL}/models/${effectiveModelId}/save`
        : `${API_URL}/models/new`);
      console.log('Request Method:', effectiveModelId ? 'PUT' : 'POST');
      console.log('Request Body - ManufacturerProductType value:', 
        finalData.submodels?.[0]?.submodelElements?.find(e => e.idShort === 'ManufacturerProductType')?.value);
      console.log('Full Request Payload:', JSON.stringify(finalData, null, 2));
      console.log('========================');
      
      let response;
      let responseData;
      
      // Check if user is authenticated first
      const hasToken = sessionStorage.getItem('access_token') || 
                      localStorage.getItem('authToken') || 
                      (keycloak && keycloak.token);
      
      if (authenticated && hasToken) {
        // User is authenticated - use authenticated endpoint ONLY
        if (effectiveModelId) {
          // Update existing model
          response = await authenticatedFetch(`${API_URL}/models/${effectiveModelId}/save`, {
            method: 'PUT',
            headers: {
              'Content-Type': 'application/json'
            },
            body: JSON.stringify(finalData)
          }, keycloak);
        } else {
          // Create new model
          response = await authenticatedFetch(`${API_URL}/models/new`, {
            method: 'POST',
            headers: {
              'Content-Type': 'application/json'
            },
            body: JSON.stringify(finalData)
          }, keycloak);
        }
        
        if (response.ok) {
          responseData = await response.json();
          console.log('=== HTTP RESPONSE DEBUG ===');
          console.log('Response Status:', response.status);
          console.log('Response ManufacturerProductType value:', 
            responseData.submodels?.[0]?.submodelElements?.find(e => e.idShort === 'ManufacturerProductType')?.value);
          console.log('=========================');
        } else {
          throw new Error(`Authenticated request failed: ${response.status} ${response.statusText}`);
        }
      } else {
        // User is not authenticated - use guest endpoint ONLY
        console.log('User is not authenticated, using guest endpoint');
        response = await fetch(`${API_URL}/guest/models/new`, {
          method: 'POST',
          headers: {
            'Content-Type': 'application/json'
          },
          body: JSON.stringify(finalData)
        });
        
        if (response.ok) {
          responseData = await response.json();
          console.log('Model created successfully (guest):', responseData);
        } else {
          throw new Error(`Guest request failed: ${response.status} ${response.statusText}`);
        }
      }
      
      // Extract id and idShort from response
      const savedModelId = responseData.id || effectiveModelId;
      const modelIdShort = responseData.aas?.idShort;
      
      // Clear the templates after successful save
      sessionStorage.removeItem('submodelTemplates');
      setSubmodelTemplates([]);
      
      // Show success message
      const successMessage = effectiveModelId ? t("createPage.modelUpdated") : t("createPage.modelSaved");
      showToast(successMessage, 'success');
      
      // Navigate to createComplete page with model data
      navigate('/create/complete', { 
        state: { 
          modelName: formData.idShort || 'Untitled Model',
          modelId: savedModelId,
          modelIdShort: modelIdShort,
          isUpdate: !!effectiveModelId
        } 
      });
      
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

  const handleBack = () => {
     navigate(authenticated ? "/dashboard" : "/");
  }

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
      
      {/* Loading indicator for existing model */}
      {loadingModel && (
        <div className="alert alert-info mb-4">
          <div className="d-flex align-items-center">
            <div className="spinner-border spinner-border-sm me-2" role="status">
              <span className="visually-hidden">Loading...</span>
            </div>
            {t("createPage.loadingModel") || "Loading existing model..."}
          </div>
        </div>
      )}

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
            {t("create.generalInfo")}
          </Card.Title>

          <Row>
            <Col sm={6}>
              <div className="mb-3">
                <label className="form-label text-white" style={{ display: "flex", alignItems: "center", gap: "6px" }}>
                  {t("create.name")}
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
                      {t("create.description")}
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
                          + {t("create.buttons.add")}
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
                          {t("create.buttons.remove")}
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
              {t("create.aasIdentification")}
            </Card.Title>

            <Row>
              <Col sm={6}>
                <Prop
                  key="general-id"
                  label={t("create.id")}
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
                  label={t("create.assetKind")}
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
              {t("create.assetIdentification")}
            </Card.Title>

            <Row>
              <Col sm={6}>
                <Prop
                  key="general-globalAssetId"
                  label={t("create.globalAssetId")}
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
                    label={index === 0 ? t("create.specificAssetId") : ""}
                    placeholder1={t("create.name")}
                    placeholder2={t("create.value")}
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
              {t("create.submodelTemplates")}
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
                    {t("create.addSubmodel")}
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
              onClick={handleBack}
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
