import { initViewer, loadModel } from './viewer.js';
import { loadModelTypes, loadAllSchemas, loadModelSchema, generateModel, downloadModel } from './modelService.js';
import * as ui from './uiController.js';
import { validateParameters } from './validator.js';
import * as filenameUtils from './filenameUtils.js';

// Initialize the application
async function initApp() {
  // Initialize UI components
  ui.initUI();
  
  // Initialize the 3D viewer
  initViewer();
  
  // Initialize download buttons
  initDownloadButtons();
  
  // Load all model schemas first (new approach)
  try {
    console.log("Loading all model schemas...");
    const allSchemas = await loadAllSchemas();
    ui.setAllModelSchemas(allSchemas);
  } catch (error) {
    console.error("Failed to load all schemas:", error);
    // We'll fall back to loading individual schemas when types are selected
  }
  
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
  
  // Try to use already loaded schema
  // If not available, load it from the server
  if (!ui.getCurrentModelSchema()) {
    try {
      const schema = await loadModelSchema(selectedType);
      ui.updateModelForm(schema);
    } catch (error) {
      ui.showStatus('Error loading model schema: ' + error.message, 'error');
    }
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
  ui.showStatus('Validating parameters...', 'info');
  
  try {
    // First, validate parameters on client side
    const currentSchema = ui.getCurrentModelSchema();
    const currentParams = ui.getCurrentModelParams();
    console.log("Validating before submission:", currentParams, currentSchema);
    
    // Re-run validation manually as a double-check
    const validationResult = validateParameters(currentParams, currentSchema);
    console.log("Pre-submission validation result:", validationResult);
    
    if (!validationResult.valid) {
      throw new Error(`Validation failed: ${validationResult.errors.join(', ')}`);
    }
    
    // Get processed parameters
    ui.showStatus('Generating model...', 'loading');
    const paramsToSend = ui.getPreparedModelParams();
    console.log("Sending parameters:", paramsToSend);
    
    // Generate the model
    const fileName = await generateModel(modelType, paramsToSend);
    
    // Store the filename
    ui.setCurrentModelFileName(fileName);
    
    // Show loading status
    ui.showStatus('Model generated. Loading...', 'loading');
    
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
    console.error("Model generation error:", error);
    ui.showStatus(`Error: ${error.message}`, 'error');
  } finally {
    // Reset loading state
    ui.showLoading(false);
  }
}

// Start the application when the page loads
document.addEventListener('DOMContentLoaded', initApp);