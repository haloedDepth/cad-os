import { initViewer, loadModel } from './viewer.js';
import { loadModelTypes, loadAllSchemas, loadModelSchema, generateModel, downloadModel } from './modelService.js';
import * as ui from './uiController.js';
import { validateParameters } from './validator.js';
import * as logger from './logger.js';
import { setupGlobalErrorHandlers, handleError, ERROR_CATEGORY } from './errorHandler.js';

// Initialize the application
async function initApp() {
  logger.info("Initializing application");
  
  // Setup global error handlers
  setupGlobalErrorHandlers();
  
  // Initialize UI components
  ui.initUI();
  
  // Initialize the 3D viewer
  initViewer();
  
  // Initialize download buttons
  initDownloadButtons();
  
  // Load all model schemas first (new approach)
  try {
    logger.info("Loading all model schemas");
    const allSchemas = await loadAllSchemas();
    ui.setAllModelSchemas(allSchemas);
  } catch (error) {
    logger.warn("Failed to load all schemas, will load individual schemas on demand", { error: error.message });
  }
  
  // Load model types
  try {
    const modelTypes = await loadModelTypes();
    ui.populateModelTypes(modelTypes);
  } catch (error) {
    handleError(error, ERROR_CATEGORY.API, { action: "loading model types" });
  }
  
  // Add event listeners
  document.getElementById('modelTypeSelect').addEventListener('change', handleModelTypeChange);
  document.getElementById('generateButton').addEventListener('click', handleGenerateModel);
  
  logger.info("Application initialized successfully");
}

// Initialize download buttons
function initDownloadButtons() {
  logger.debug("Setting up download buttons");
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
  
  logger.info(`Downloading model in ${format} format`, { fileName });
  ui.showStatus(`Preparing ${format.toUpperCase()} file for download...`, 'loading');
  
  downloadModel(fileName, format);
  
  ui.showStatus(`${format.toUpperCase()} file download initiated`, 'success');
}

// Handle model type change
async function handleModelTypeChange() {
  const selectedType = document.getElementById('modelTypeSelect').value;
  if (!selectedType) {
    logger.debug("No model type selected, resetting form");
    ui.resetParameterForm();
    return;
  }
  
  logger.info(`Model type changed to: ${selectedType}`);
  ui.setCurrentModelType(selectedType);
  
  // Try to use already loaded schema
  // If not available, load it from the server
  if (!ui.getCurrentModelSchema()) {
    try {
      logger.debug(`Loading schema for ${selectedType}`);
      const schema = await loadModelSchema(selectedType);
      ui.updateModelForm(schema);
    } catch (error) {
      handleError(error, ERROR_CATEGORY.API, { modelType: selectedType });
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
  
  logger.info(`Generating model of type: ${modelType}`);
  
  // Show loading state
  ui.showLoading(true);
  ui.showStatus('Validating parameters...', 'info');
  
  try {
    // First, validate parameters on client side
    const currentSchema = ui.getCurrentModelSchema();
    const currentParams = ui.getCurrentModelParams();
    logger.debug("Validating parameters before submission", { params: currentParams });
    
    // Re-run validation manually as a double-check
    const validationResult = validateParameters(currentParams, currentSchema);
    
    if (!validationResult.valid) {
      throw new Error(`Validation failed: ${validationResult.errors.join(', ')}`);
    }
    
    // Get processed parameters
    ui.showStatus('Generating model...', 'loading');
    const paramsToSend = ui.getPreparedModelParams();
    logger.debug("Sending parameters to API", { params: paramsToSend });
    
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
          logger.info(`Model loaded successfully: ${fileName}`);
        },
        (error) => {
          handleError(error, ERROR_CATEGORY.RENDERING, { fileName });
          
          // Retry loading after a delay if it was a 404
          if (error.message.includes('404') || error.message.includes('Not Found')) {
            logger.info("Model not found, retrying after delay", { fileName });
            ui.showStatus('Model not ready yet. Retrying...', 'loading');
            
            setTimeout(() => {
              loadModel(
                fileName,
                () => {
                  ui.showDownloadButtons(true);
                  ui.showStatus('Model loaded successfully!', 'success');
                  logger.info(`Model loaded successfully on retry: ${fileName}`);
                },
                (error) => {
                  handleError(
                    error, 
                    ERROR_CATEGORY.RENDERING, 
                    { fileName, retry: true }
                  );
                }
              );
            }, 2000);
          }
        }
      );
    }, 1000);
  } catch (error) {
    if (error.message.includes('Validation failed')) {
      handleError(error, ERROR_CATEGORY.VALIDATION);
    } else {
      handleError(error, ERROR_CATEGORY.API, { modelType });
    }
  } finally {
    // Reset loading state
    ui.showLoading(false);
  }
}

// Start the application when the page loads
document.addEventListener('DOMContentLoaded', initApp);