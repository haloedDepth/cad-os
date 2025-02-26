// UI Controller - handles UI interactions and form management

// Current model state
let currentModelType = '';
let currentModelSchema = null;
let currentModelParams = {};
let currentModelFileName = null;

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
}

export function resetParameterForm() {
  modelTitle.textContent = 'Model Parameters';
  modelDescription.textContent = '';
  parametersContainer.innerHTML = '';
  generateButton.disabled = true;
  currentModelSchema = null;
}

export function setCurrentModelType(type) {
  currentModelType = type;
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

// Generate parameter input fields based on schema
function generateParameterFields(schema) {
  parametersContainer.innerHTML = '';
  currentModelParams = {};
  
  if (!schema.parameters || !Array.isArray(schema.parameters)) {
    return;
  }
  
  schema.parameters.forEach(param => {
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
      if (param.min !== undefined) input.min = param.min;
      if (param.max !== undefined) input.max = param.max;
    }
    
    // Set current param value
    currentModelParams[param.name] = param.default || '';
    
    // Add event listener
    input.addEventListener('change', (e) => {
      currentModelParams[param.name] = e.target.value;
    });
    
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
    generateButton.disabled = false;
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
  
  statusContainer.style.display = 'block';
}