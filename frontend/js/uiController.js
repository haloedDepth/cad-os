// UI Controller - handles UI interactions and form management
import { validateParameters, prepareParameters } from './validator.js';
import * as filenameUtils from './filenameUtils.js';
import * as logger from './logger.js';
import { handleError, ERROR_CATEGORY } from './errorHandler.js';

// Current model state
let currentModelType = '';
let currentModelSchema = null;
let currentModelParams = {};
let currentModelFileName = null;
let allModelSchemas = {};

// DOM Elements
let modelTypeSelect;
let modelTitle;
let modelDescription;
let parametersContainer;
let generateButton;
let buttonText;
let loader;
let statusContainer;
let statusMessage;
let downloadButtons;
let validationContainer;

export function initUI() {
  logger.info("Initializing UI components");
  
  try {
    // Initialize DOM elements
    modelTypeSelect = document.getElementById('modelTypeSelect');
    modelTitle = document.getElementById('modelTitle');
    modelDescription = document.getElementById('modelDescription');
    parametersContainer = document.getElementById('parametersContainer');
    generateButton = document.getElementById('generateButton');
    buttonText = document.getElementById('buttonText');
    loader = document.getElementById('loader');
    statusContainer = document.getElementById('statusContainer');
    statusMessage = document.getElementById('statusMessage');
    downloadButtons = document.getElementById('downloadButtons');
    
    // Create validation container if it doesn't exist
    if (!document.getElementById('validationContainer')) {
      validationContainer = document.createElement('div');
      validationContainer.id = 'validationContainer';
      validationContainer.className = 'mt-4 p-3 rounded bg-red-100 text-red-700';
      validationContainer.style.display = 'none';
      
      // Insert after parameters container
      parametersContainer.parentNode.insertBefore(
        validationContainer, 
        parametersContainer.nextSibling
      );
    } else {
      validationContainer = document.getElementById('validationContainer');
    }
    
    logger.debug("UI components initialized");
  } catch (error) {
    handleError(error, ERROR_CATEGORY.UNKNOWN, {
      component: "initUI",
      action: "initializing UI components"
    });
  }
}

export function setAllModelSchemas(schemas) {
  try {
    allModelSchemas = schemas || {};
    logger.info(`Loaded all schemas`, { 
      count: Object.keys(allModelSchemas).length,
      types: Object.keys(allModelSchemas)
    });
  } catch (error) {
    handleError(error, ERROR_CATEGORY.UNKNOWN, {
      component: "setAllModelSchemas",
      action: "setting schemas"
    });
  }
}

export function populateModelTypes(modelTypes) {
  try {
    logger.info("Populating model type dropdown", { count: modelTypes.length });
    
    modelTypeSelect.innerHTML = '<option value="">-- Select a model type --</option>';
    
    modelTypes.forEach(type => {
      const option = document.createElement('option');
      option.value = type;
      option.textContent = type.charAt(0).toUpperCase() + type.slice(1);
      modelTypeSelect.appendChild(option);
    });
  } catch (error) {
    handleError(error, ERROR_CATEGORY.UNKNOWN, {
      component: "populateModelTypes",
      action: "populating dropdown",
      modelTypes
    });
  }
}

export function updateModelForm(schema) {
  try {
    logger.info(`Updating model form for schema`, { name: schema.name });
    
    // Update current model state
    currentModelSchema = schema;
    
    // Update UI
    modelTitle.textContent = schema.name || 'Model Parameters';
    modelDescription.textContent = schema.description || '';
    
    // Generate parameter fields
    generateParameterFields(schema);
    
    // Enable generate button
    generateButton.disabled = false;
    
    // Clear any existing validation messages
    hideValidationErrors();
  } catch (error) {
    handleError(error, ERROR_CATEGORY.UNKNOWN, {
      component: "updateModelForm",
      action: "updating form",
      schema: schema?.name
    });
  }
}

export function resetParameterForm() {
  try {
    logger.debug("Resetting parameter form");
    
    modelTitle.textContent = 'Model Parameters';
    modelDescription.textContent = '';
    parametersContainer.innerHTML = '';
    generateButton.disabled = true;
    currentModelSchema = null;
    hideValidationErrors();
  } catch (error) {
    handleError(error, ERROR_CATEGORY.UNKNOWN, {
      component: "resetParameterForm",
      action: "resetting form"
    });
  }
}

export function setCurrentModelType(type) {
  try {
    logger.info(`Setting current model type to ${type}`);
    currentModelType = type;
    
    // If we already have the schema for this model type, use it
    if (allModelSchemas[type]) {
      updateModelForm(allModelSchemas[type]);
    }
  } catch (error) {
    handleError(error, ERROR_CATEGORY.UNKNOWN, {
      component: "setCurrentModelType",
      action: "setting model type",
      type
    });
  }
}

export function getCurrentModelType() {
  return currentModelType;
}

export function getCurrentModelParams() {
  return currentModelParams;
}

export function getCurrentModelSchema() {
  return currentModelSchema;
}

export function setCurrentModelFileName(fileName) {
  try {
    logger.info(`Setting current model filename: ${fileName}`);
    currentModelFileName = fileName;
  } catch (error) {
    handleError(error, ERROR_CATEGORY.UNKNOWN, {
      component: "setCurrentModelFileName",
      action: "setting filename",
      fileName
    });
  }
}

export function getCurrentModelFileName() {
  return currentModelFileName;
}

export function getPreparedModelParams() {
  try {
    if (!currentModelSchema) return {};
    
    const prepared = prepareParameters(currentModelParams, currentModelSchema);
    logger.debug("Prepared parameters for sending", { params: prepared });
    return prepared;
  } catch (error) {
    handleError(error, ERROR_CATEGORY.VALIDATION, {
      component: "getPreparedModelParams",
      action: "preparing parameters",
      params: currentModelParams
    });
    return {};
  }
}

// Generate parameter input fields based on schema
function generateParameterFields(schema) {
  try {
    logger.debug("Generating parameter fields", { 
      schemaName: schema.name,
      paramCount: schema.parameters?.length || 0 
    });
    
    parametersContainer.innerHTML = '';
    currentModelParams = {};
    
    if (!schema.parameters || !Array.isArray(schema.parameters)) {
      return;
    }
    
    schema.parameters.forEach(param => {
      // Skip hidden parameters in the UI
      if (param.hidden) {
        // Still set default value in params
        currentModelParams[param.name] = param.default || '';
        return;
      }
        
      const inputId = `param_${param.name}`;
      const div = document.createElement('div');
      
      // Create label
      const label = document.createElement('label');
      label.className = 'block text-sm font-medium text-gray-700';
      label.htmlFor = inputId;
      label.textContent = param.name
        .split('-')
        .map(word => word.charAt(0).toUpperCase() + word.slice(1))
        .join(' ');
      
      // Create input
      const input = document.createElement('input');
      input.id = inputId;
      input.name = param.name;
      input.type = param.type === 'number' ? 'number' : 'text';
      input.className = 'mt-1 block w-full rounded-md border-gray-300 shadow-sm focus:border-blue-500 focus:ring-blue-500 p-2 border';
      input.value = param.default || '';
      
      if (param.type === 'number') {
        input.step = 'any'; // For number inputs
      }
      
      // Set current param value
      currentModelParams[param.name] = param.default || '';
      
      // Add event listener for live validation
      input.addEventListener('input', debounce((e) => {
        try {
          // Update current params
          currentModelParams[param.name] = e.target.value;
          
          logger.debug(`Parameter ${param.name} updated`, { value: e.target.value });
          
          // Run validation on change
          validateCurrentForm();
        } catch (error) {
          handleError(error, ERROR_CATEGORY.VALIDATION, {
            component: "inputChangeHandler",
            action: "handling input change",
            param: param.name,
            value: e.target.value
          });
        }
      }, 300));
      
      // Add description if available
      if (param.description) {
        input.title = param.description;
        const desc = document.createElement('p');
        desc.className = 'mt-1 text-xs text-gray-500';
        desc.textContent = param.description;
        div.appendChild(label);
        div.appendChild(input);
        div.appendChild(desc);
      } else {
        div.appendChild(label);
        div.appendChild(input);
      }
      
      parametersContainer.appendChild(div);
    });
    
    // Initial validation
    validateCurrentForm();
  } catch (error) {
    handleError(error, ERROR_CATEGORY.UNKNOWN, {
      component: "generateParameterFields",
      action: "generating form fields",
      schema: schema?.name
    });
  }
}

function validateCurrentForm() {
  try {
    if (!currentModelSchema) {
      hideValidationErrors();
      return true;
    }
    
    logger.debug("Validating form");
    
    const validation = validateParameters(currentModelParams, currentModelSchema);
    
    if (validation.valid) {
      hideValidationErrors();
      generateButton.disabled = false;
      return true;
    } else {
      showValidationErrors(validation.errors);
      generateButton.disabled = true;
      return false;
    }
  } catch (error) {
    handleError(error, ERROR_CATEGORY.VALIDATION, {
      component: "validateCurrentForm",
      action: "validating form"
    });
    return false;
  }
}

function showValidationErrors(errors) {
  try {
    if (!validationContainer) return;
    
    logger.debug("Showing validation errors", { errors });
    
    validationContainer.innerHTML = '';
    
    // Create a list of errors
    const ul = document.createElement('ul');
    ul.className = 'pl-5 list-disc';
    
    errors.forEach(error => {
      const li = document.createElement('li');
      li.textContent = error;
      ul.appendChild(li);
    });
    
    validationContainer.appendChild(ul);
    validationContainer.style.display = 'block';
  } catch (error) {
    handleError(error, ERROR_CATEGORY.UNKNOWN, {
      component: "showValidationErrors",
      action: "showing validation errors"
    });
  }
}

function hideValidationErrors() {
  try {
    if (validationContainer) {
      validationContainer.style.display = 'none';
      validationContainer.innerHTML = '';
    }
  } catch (error) {
    handleError(error, ERROR_CATEGORY.UNKNOWN, {
      component: "hideValidationErrors",
      action: "hiding validation errors"
    });
  }
}

// Utility function for debouncing inputs
function debounce(func, wait) {
  let timeout;
  return function(...args) {
    const context = this;
    clearTimeout(timeout);
    timeout = setTimeout(() => func.apply(context, args), wait);
  };
}

// Show UI loading state
export function showLoading(isLoading) {
  try {
    logger.debug(`Setting loading state: ${isLoading}`);
    
    if (isLoading) {
      buttonText.style.display = 'none';
      loader.style.display = 'flex';
      generateButton.disabled = true;
    } else {
      buttonText.style.display = 'inline';
      loader.style.display = 'none';
      
      // Only re-enable if validation passes
      generateButton.disabled = !validateCurrentForm();
    }
  } catch (error) {
    handleError(error, ERROR_CATEGORY.UNKNOWN, {
      component: "showLoading",
      action: "updating loading state",
      isLoading
    });
  }
}

// Show download buttons
export function showDownloadButtons(show) {
  try {
    logger.debug(`Setting download buttons visibility: ${show}`);
    downloadButtons.style.display = show ? 'flex' : 'none';
  } catch (error) {
    handleError(error, ERROR_CATEGORY.UNKNOWN, {
      component: "showDownloadButtons",
      action: "updating button visibility",
      show
    });
  }
}

// Show status message
export function showStatus(message, type = 'info') {
  try {
    logger.debug(`Showing status message: "${message}" (${type})`);
    
    statusMessage.textContent = message;
    
    // Set appropriate styling based on message type
    statusContainer.className = 'mt-4 p-3 rounded';
    
    switch (type) {
      case 'success':
        statusContainer.className += ' bg-green-100 text-green-700';
        break;
      case 'error':
        statusContainer.className += ' bg-red-100 text-red-700';
        break;
      case 'loading':
        statusContainer.className += ' bg-yellow-100 text-yellow-700';
        break;
      default:
        statusContainer.className += ' bg-blue-100 text-blue-700';
    }
    
    statusContainer.style.display = message ? 'block' : 'none';
  } catch (error) {
    // Use console directly here to avoid potential infinite loop
    console.error("Error showing status:", error);
  }
}