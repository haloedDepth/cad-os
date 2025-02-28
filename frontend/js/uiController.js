// UI Controller - handles UI interactions and form management
import { validateParameters, prepareParameters, evaluateExpression } from './validator.js';
import * as filenameUtils from './filenameUtils.js';

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
}

export function setAllModelSchemas(schemas) {
  allModelSchemas = schemas || {};
  console.log('Loaded all schemas:', Object.keys(allModelSchemas));
}

export function populateModelTypes(modelTypes) {
  modelTypeSelect.innerHTML = '<option value="">-- Select a model type --</option>';
  
  modelTypes.forEach(type => {
    const option = document.createElement('option');
    option.value = type;
    option.textContent = type.charAt(0).toUpperCase() + type.slice(1);
    modelTypeSelect.appendChild(option);
  });
}

export function updateModelForm(schema) {
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
}

export function resetParameterForm() {
  modelTitle.textContent = 'Model Parameters';
  modelDescription.textContent = '';
  parametersContainer.innerHTML = '';
  generateButton.disabled = true;
  currentModelSchema = null;
  hideValidationErrors();
}

export function setCurrentModelType(type) {
  currentModelType = type;
  
  // If we already have the schema for this model type, use it
  if (allModelSchemas[type]) {
    updateModelForm(allModelSchemas[type]);
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
  currentModelFileName = fileName;
}

export function getCurrentModelFileName() {
  return currentModelFileName;
}

export function getPreparedModelParams() {
  if (!currentModelSchema) return {};
  
  const prepared = prepareParameters(currentModelParams, currentModelSchema);
  console.log("Prepared parameters for sending:", prepared);
  return prepared;
}

// Generate parameter input fields based on schema
function generateParameterFields(schema) {
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
      // Update current params
      currentModelParams[param.name] = e.target.value;
      
      // Run validation on change
      validateCurrentForm();
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
}

function validateCurrentForm() {
  if (!currentModelSchema) {
    hideValidationErrors();
    return true;
  }
  
  // Log for debugging
  console.log("Validating form with schema:", currentModelSchema);
  console.log("Current params:", currentModelParams);
  
  const validation = validateParameters(currentModelParams, currentModelSchema);
  console.log("Validation result:", validation);
  
  if (validation.valid) {
    hideValidationErrors();
    generateButton.disabled = false;
    return true;
  } else {
    showValidationErrors(validation.errors);
    generateButton.disabled = true;
    return false;
  }
}

function showValidationErrors(errors) {
  if (!validationContainer) return;
  
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
}

function hideValidationErrors() {
  if (validationContainer) {
    validationContainer.style.display = 'none';
    validationContainer.innerHTML = '';
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
}

// Show download buttons
export function showDownloadButtons(show) {
  downloadButtons.style.display = show ? 'flex' : 'none';
}

// Show status message
export function showStatus(message, type = 'info') {
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
}