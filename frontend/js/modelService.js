// API Service for model operations
import * as filenameUtils from './filenameUtils.js';

/**
 * Load all available model types
 * @returns {Promise<string[]>} Array of model type names
 */
export async function loadModelTypes() {
  try {
    console.log("Loading model types...");
    const response = await fetch('/api/models/types');
    if (!response.ok) {
      throw new Error('Failed to load model types');
    }
    
    const data = await response.json();
    console.log("Raw response from /api/models/types:", data);
    
    // Handle different response formats
    let modelTypes = [];
    if (Array.isArray(data)) {
      modelTypes = data;
    } else if (data.model_types && Array.isArray(data.model_types)) {
      modelTypes = data.model_types;
    } else {
      console.warn("Unexpected response format:", data);
      // Try to extract model types if possible
      if (typeof data === 'object') {
        modelTypes = Object.keys(data).filter(key => 
          Array.isArray(data[key]) || typeof data[key] === 'object');
      }
    }
    
    console.log("Processed model types:", modelTypes);
    
    if (modelTypes.length === 0) {
      // Fallback to hardcoded models if API fails
      console.warn("No model types returned, using fallback list");
      modelTypes = ["washer", "cylinder"];
    }
    
    return modelTypes;
  } catch (error) {
    console.error('Error loading model types:', error);
    // Fallback to hardcoded models if API fails
    console.warn("Using fallback model types due to error");
    return ["washer", "cylinder"];
  }
}

/**
 * Load schema for a specific model type
 * @param {string} modelType - Type of model to load schema for
 * @returns {Promise<Object>} Schema for the model type
 */
export async function loadModelSchema(modelType) {
  try {
    const response = await fetch(`/api/models/schema/${modelType}`);
    if (!response.ok) {
      throw new Error(`Failed to load schema for model type: ${modelType}`);
    }
    
    const schema = await response.json();
    console.log('Loaded schema:', schema);
    
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
  } catch (error) {
    console.error('Error loading model schema:', error);
    throw error;
  }
}

/**
 * Load all schemas at once
 * @returns {Promise<Object>} Map of model types to their schemas
 */
export async function loadAllSchemas() {
  try {
    console.log("Loading all model schemas...");
    const response = await fetch('/api/models/schemas');
    if (!response.ok) {
      console.error(`Failed to load schemas: ${response.status} ${response.statusText}`);
      throw new Error('Failed to load model schemas');
    }
    
    const data = await response.json();
    console.log("Schema response data:", data);
    
    // Get schemas from the response
    const schemas = data.schemas || {};
    console.log("Extracted schemas:", schemas);
    console.log("Loaded schemas for models:", Object.keys(schemas));
    
    // Convert validation-rules to camelCase for all schemas
    Object.keys(schemas).forEach(modelType => {
      const schema = schemas[modelType];
      console.log(`Processing schema for ${modelType}:`, schema);
      
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
  } catch (error) {
    console.error('Error loading all schemas:', error);
    
    // Fallback: load schemas individually
    try {
      console.log("Falling back to loading schemas individually");
      const modelTypes = await loadModelTypes();
      const schemas = {};
      
      for (const modelType of modelTypes) {
        try {
          schemas[modelType] = await loadModelSchema(modelType);
          console.log(`Loaded schema for ${modelType}:`, schemas[modelType]);
        } catch (schemaError) {
          console.error(`Failed to load schema for ${modelType}:`, schemaError);
        }
      }
      
      return schemas;
    } catch (fallbackError) {
      console.error('Fallback schema loading failed:', fallbackError);
      throw error; // Throw the original error
    }
  }
}

/**
 * Generate a model with the given parameters
 * @param {string} modelType - Type of model to generate
 * @param {Object} params - Parameters for the model
 * @returns {Promise<string>} Filename of the generated model
 */
export async function generateModel(modelType, params) {
  try {
    console.log(`Generating ${modelType} model with parameters:`, params);
    
    // Call the API to generate the model
    const response = await fetch(`/api/generate/${modelType}`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
      },
      body: JSON.stringify(params)
    });
    
    if (!response.ok) {
      const errorData = await response.text();
      console.error('Error response:', errorData);
      
      // Try to parse as JSON to extract detailed error message
      try {
        const errorJson = JSON.parse(errorData);
        if (errorJson.detail) {
          throw new Error(errorJson.detail);
        } else if (errorJson.message) {
          throw new Error(errorJson.message);
        } else {
          throw new Error(`Failed to generate model: ${errorData}`);
        }
      } catch (parseError) {
        throw new Error(`Failed to generate model: ${errorData}`);
      }
    }
    
    const data = await response.json();
    console.log('Model generation response:', data);
    
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
      // This shouldn't happen with the new implementation but we'll keep it as a fallback
      fileName = filenameUtils.generateModelFilename(modelType, params);
    }
    
    console.log(`Extracted filename: ${fileName}`);
    return fileName;
  } catch (error) {
    console.error('Error generating model:', error);
    throw error;
  }
}

/**
 * Download a model in the specified format
 * @param {string} fileName - Base filename of the model
 * @param {string} format - Format to download (obj, stl, step, g)
 */
export function downloadModel(fileName, format) {
  // Create a download link with the correctly formatted URL
  const downloadUrl = `/api/models/${fileName}/${format}`;
  console.log(`Downloading from: ${downloadUrl}`);
  
  // Create and click a temporary link
  const link = document.createElement('a');
  link.href = downloadUrl;
  link.download = filenameUtils.withExtension(fileName, format);
  document.body.appendChild(link);
  link.click();
  document.body.removeChild(link);
}