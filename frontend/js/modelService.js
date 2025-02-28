// API Service for model operations
import * as filenameUtils from './filenameUtils.js';
import * as logger from './logger.js';
import { handleError, ERROR_CATEGORY, withErrorHandling } from './errorHandler.js';

/**
 * Load all available model types
 * @returns {Promise<string[]>} Array of model type names
 */
export const loadModelTypes = withErrorHandling(async function() {
  logger.info("Loading model types");
  
  const response = await fetch('/api/models/types');
  if (!response.ok) {
    const errorText = await response.text();
    throw new Error(`Failed to load model types: ${errorText}`);
  }
  
  const data = await response.json();
  logger.debug("Raw model types response:", data);
  
  // Handle different response formats
  let modelTypes = [];
  if (Array.isArray(data)) {
    modelTypes = data;
  } else if (data.model_types && Array.isArray(data.model_types)) {
    modelTypes = data.model_types;
  } else {
    logger.warn("Unexpected response format for model types", data);
    // Try to extract model types if possible
    if (typeof data === 'object') {
      modelTypes = Object.keys(data).filter(key => 
        Array.isArray(data[key]) || typeof data[key] === 'object');
    }
  }
  
  if (modelTypes.length === 0) {
    // Fallback to hardcoded models if API fails
    logger.warn("No model types returned, using fallback list");
    modelTypes = ["washer", "cylinder"];
  }
  
  logger.info(`Loaded ${modelTypes.length} model types`, { modelTypes });
  return modelTypes;
}, ERROR_CATEGORY.API, { endpoint: '/api/models/types' });

/**
 * Load schema for a specific model type
 * @param {string} modelType - Type of model to load schema for
 * @returns {Promise<Object>} Schema for the model type
 */
export const loadModelSchema = withErrorHandling(async function(modelType) {
  logger.info(`Loading schema for model type: ${modelType}`);
  
  const response = await fetch(`/api/models/schema/${modelType}`);
  if (!response.ok) {
    const errorText = await response.text();
    throw new Error(`Failed to load schema for ${modelType}: ${errorText}`);
  }
  
  const schema = await response.json();
  logger.debug(`Schema loaded for ${modelType}`, schema);
  
  // Convert validation-rules to camelCase for frontend
  if (schema.validation_rules) {
    schema.validationRules = schema.validation_rules;
    delete schema.validation_rules;
  }
  
  // Convert param-names to camelCase for frontend
  if (schema.param_names) {
    schema.paramNames = schema.param_names;
    delete schema.param_names;
  }
  
  return schema;
}, ERROR_CATEGORY.API, { endpoint: '/api/models/schema/{modelType}' });

/**
 * Load all schemas at once
 * @returns {Promise<Object>} Map of model types to their schemas
 */
export const loadAllSchemas = withErrorHandling(async function() {
  logger.info("Loading all model schemas");
  
  const response = await fetch('/api/models/schemas');
  if (!response.ok) {
    const errorText = await response.text();
    throw new Error(`Failed to load all schemas: ${errorText}`);
  }
  
  const data = await response.json();
  
  // Get schemas from the response
  const schemas = data.schemas || {};
  logger.info(`Loaded schemas for ${Object.keys(schemas).length} models`, {
    modelTypes: Object.keys(schemas)
  });
  
  // Convert validation-rules to camelCase for all schemas
  Object.keys(schemas).forEach(modelType => {
    const schema = schemas[modelType];
    
    if (schema && schema["validation-rules"]) {
      schema.validationRules = schema["validation-rules"];
      delete schema["validation-rules"];
    }
    
    if (schema && schema["param-names"]) {
      schema.paramNames = schema["param-names"];
      delete schema["param-names"];
    }
  });
  
  return schemas;
}, ERROR_CATEGORY.API, { endpoint: '/api/models/schemas' });

/**
 * Generate a model with the given parameters
 * @param {string} modelType - Type of model to generate
 * @param {Object} params - Parameters for the model
 * @returns {Promise<string>} Filename of the generated model
 */
export const generateModel = withErrorHandling(async function(modelType, params) {
  logger.info(`Generating ${modelType} model`, { params });
  
  // Call the API to generate the model
  const response = await fetch(`/api/generate/${modelType}`, {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
    },
    body: JSON.stringify(params)
  });
  
  if (!response.ok) {
    const errorText = await response.text();
    
    // Try to parse as JSON to extract detailed error message
    try {
      const errorJson = JSON.parse(errorText);
      if (errorJson.detail) {
        throw new Error(errorJson.detail);
      } else if (errorJson.message) {
        throw new Error(errorJson.message);
      } else {
        throw new Error(`Failed to generate model: ${errorText}`);
      }
    } catch (parseError) {
      throw new Error(`Failed to generate model: ${errorText}`);
    }
  }
  
  const data = await response.json();
  logger.debug('Model generation response:', data);
  
  // Extract the base filename without extension from the response
  // Check all possible paths where the filename could be returned
  let fileName = null;
  
  if (data.obj_path) {
    fileName = filenameUtils.baseFilename(data.obj_path);
  } else if (data["obj-path"]) {
    fileName = filenameUtils.baseFilename(data["obj-path"]);
  } else if (data.obj_result && data.obj_result.file) {
    fileName = filenameUtils.baseFilename(data.obj_result.file);
  } else if (data["obj-result"] && data["obj-result"].file) {
    fileName = filenameUtils.baseFilename(data["obj-result"].file);
  } else if (data.file_name) {
    fileName = data.file_name;
  } else {
    // If no filename is found, generate one from the model type and parameters
    fileName = filenameUtils.generateModelFilename(modelType, params);
    logger.warn(`No filename found in response, generated: ${fileName}`, { data });
  }
  
  logger.info(`Model generated successfully: ${fileName}`);
  return fileName;
}, ERROR_CATEGORY.API, { endpoint: '/api/generate/{modelType}' });

/**
 * Download a model in the specified format
 * @param {string} fileName - Base filename of the model
 * @param {string} format - Format to download (obj, stl, step, g)
 */
export function downloadModel(fileName, format) {
  try {
    // Create a download link with the correctly formatted URL
    const downloadUrl = `/api/models/${fileName}/${format}`;
    logger.info(`Downloading model: ${fileName} in format: ${format}`, { url: downloadUrl });
    
    // Create and click a temporary link
    const link = document.createElement('a');
    link.href = downloadUrl;
    link.download = filenameUtils.withExtension(fileName, format);
    document.body.appendChild(link);
    link.click();
    document.body.removeChild(link);
  } catch (error) {
    handleError(error, ERROR_CATEGORY.UNKNOWN, { fileName, format });
  }
}