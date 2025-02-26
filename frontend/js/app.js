import { initViewer, loadModel } from './viewer.js';
import { loadModelTypes, loadModelSchema, generateModel, downloadModel } from './modelService.js';
import * as ui from './uiController.js';

// Initialize the application
async function initApp() {
  // Initialize UI components
  ui.initUI();
  
  // Initialize the 3D viewer
  initViewer();
  
  // Initialize download buttons
  initDownloadButtons();
  
  // Load model types
  try {
    const modelTypes = await loadModelTypes();
    ui.populateModelTypes(modelTypes);
  } catch (error) {
    ui.showStatus('Error loading model types: ' + error.message, 'error');
  }
  
  // Add event listeners
  document.getElementById('modelTypeSelect').addEventListener('change', handleModelTypeChange);
  document.getElementById('generateButton').addEventListener('click', handleGenerateModel);
}

// Initialize download buttons
function initDownloadButtons() {
  // Set up event listeners for download buttons
  document.getElementById('downloadObj').addEventListener('click', () => handleDownloadModel('obj'));
  document.getElementById('downloadStl').addEventListener('click', () => handleDownloadModel('stl'));
  document.getElementById('downloadStep').addEventListener('click', () => handleDownloadModel('step'));
  document.getElementById('downloadG').addEventListener('click', () => handleDownloadModel('g'));
}

// Download model in specified format
function handleDownloadModel(format) {
  const fileName = ui.getCurrentModelFileName();
  
  if (!fileName) {
    ui.showStatus('No model loaded to download', 'error');
    return;
  }
  
  ui.showStatus(`Preparing ${format.toUpperCase()} file for download...`, 'loading');
  
  downloadModel(fileName, format);
  
  ui.showStatus(`${format.toUpperCase()} file download initiated`, 'success');
}

// Handle model type change
async function handleModelTypeChange() {
  const selectedType = document.getElementById('modelTypeSelect').value;
  if (!selectedType) {
    ui.resetParameterForm();
    return;
  }
  
  ui.setCurrentModelType(selectedType);
  
  try {
    const schema = await loadModelSchema(selectedType);
    ui.updateModelForm(schema);
  } catch (error) {
    ui.showStatus('Error loading model schema: ' + error.message, 'error');
  }
}

// Handle generate model button click
async function handleGenerateModel() {
  const modelType = ui.getCurrentModelType();
  if (!modelType) {
    ui.showStatus('Please select a model type', 'error');
    return;
  }
  
  // Show loading state
  ui.showLoading(true);
  ui.showStatus('', 'info');
  
  try {
    // Prepare params with correct types
    const paramsRaw = ui.getCurrentModelParams();
    const paramsToSend = {};
    
    const currentSchema = ui.getCurrentModelSchema();
    if (currentSchema && currentSchema.parameters) {
      currentSchema.parameters.forEach(param => {
        const paramValue = paramsRaw[param.name];
        if (param.type === 'number') {
          paramsToSend[param.name] = parseFloat(paramValue);
        } else {
          paramsToSend[param.name] = paramValue;
        }
      });
    }
    
    // Generate the model
    const fileName = await generateModel(modelType, paramsToSend);
    
    // Show loading status
    ui.showStatus('Model generated. Loading...', 'loading');
    
    // Store the filename
    ui.setCurrentModelFileName(fileName);
    
    // Wait a bit before loading to let the API service have time to access the file
    setTimeout(() => {
      loadModel(
        fileName,
        () => {
          ui.showDownloadButtons(true);
          ui.showStatus('Model loaded successfully!', 'success');
        },
        (error) => {
          ui.showStatus(`Error loading model: ${error.message}`, 'error');
          
          // Retry loading after a delay if it was a 404
          if (error.message.includes('404') || error.message.includes('Not Found')) {
            ui.showStatus('Model not ready yet. Retrying...', 'loading');
            setTimeout(() => {
              loadModel(
                fileName,
                () => {
                  ui.showDownloadButtons(true);
                  ui.showStatus('Model loaded successfully!', 'success');
                },
                (error) => {
                  ui.showStatus(`Error loading model after retry: ${error.message}`, 'error');
                }
              );
            }, 2000);
          }
        }
      );
    }, 1000);
  } catch (error) {
    ui.showStatus(`Error: ${error.message}`, 'error');
  } finally {
    // Reset loading state
    ui.showLoading(false);
  }
}

// Start the application when the page loads
document.addEventListener('DOMContentLoaded', initApp);