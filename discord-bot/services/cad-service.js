const axios = require('axios');
const fs = require('fs');
const path = require('path');
const config = require('../config');
const filenameUtils = require('../utils/filename-utils');
const logger = require('../utils/logger')('cad-service');

class CADService {
  constructor() {
    this.apiBaseUrl = config.API_BASE_URL;
    this.clojureServiceUrl = config.CLOJURE_SERVICE_URL;
    
    logger.info('CAD service initialized', { 
      apiBaseUrl: this.apiBaseUrl, 
      clojureServiceUrl: this.clojureServiceUrl 
    });
    
    // Ensure temp directory exists
    this.tempDir = path.join(__dirname, '..', 'temp');
    if (!fs.existsSync(this.tempDir)) {
      fs.mkdirSync(this.tempDir, { recursive: true });
      logger.info('Created temp directory', { path: this.tempDir });
    }
    
    // Cache for model schemas
    this.schemaCache = {};
  }

  /**
   * Get all available model types
   * @returns {Promise<string[]>} List of model types
   */
  async getModelTypes() {
    logger.info('Fetching available model types');
    
    try {
      const url = `${this.apiBaseUrl}/models/types`;
      
      logger.debug(`Sending request to ${url}`);
      
      const response = await axios.get(url);
      
      logger.info('Model types fetched successfully', {
        status: response.status,
        hasData: !!response.data
      });
      
      // Extract the model types from response
      let modelTypes = [];
      if (Array.isArray(response.data)) {
        modelTypes = response.data;
      } else if (response.data.model_types && Array.isArray(response.data.model_types)) {
        modelTypes = response.data.model_types;
      } else {
        logger.warn("Unexpected response format for model types", { data: response.data });
        // Try to extract model types if possible
        if (typeof response.data === 'object') {
          modelTypes = Object.keys(response.data).filter(key => 
            Array.isArray(response.data[key]) || typeof response.data[key] === 'object');
        }
      }
      
      if (modelTypes.length === 0) {
        // Fallback to hardcoded models if API fails
        logger.warn("No model types returned, using fallback list");
        modelTypes = ["washer", "cylinder"];
      }
      
      logger.info(`Loaded ${modelTypes.length} model types`, { modelTypes });
      return modelTypes;
    } catch (error) {
      this.handleApiError(error, 'Failed to fetch model types', {
        url: `${this.apiBaseUrl}/models/types`
      });
      throw error;
    }
  }

  /**
   * Get schema for a specific model type
   * @param {string} modelType - Type of model
   * @returns {Promise<Object>} Schema for the model type
   */
  async getModelSchema(modelType) {
    // Check cache first
    if (this.schemaCache[modelType]) {
      logger.debug(`Using cached schema for ${modelType}`);
      return this.schemaCache[modelType];
    }
    
    logger.info(`Fetching schema for model type: ${modelType}`);
    
    try {
      const url = `${this.apiBaseUrl}/models/schema/${modelType}`;
      
      logger.debug(`Sending request to ${url}`);
      
      const response = await axios.get(url);
      
      logger.info(`Schema fetched successfully for ${modelType}`, {
        status: response.status,
        hasData: !!response.data
      });
      
      // Process the schema
      const schema = response.data;
      
      // Convert validation_rules to camelCase for frontend
      if (schema.validation_rules) {
        schema.validationRules = schema.validation_rules;
        delete schema.validation_rules;
      }
      
      // Convert param_names to camelCase for frontend
      if (schema.param_names) {
        schema.paramNames = schema.param_names;
        delete schema.param_names;
      }
      
      // Cache the schema
      this.schemaCache[modelType] = schema;
      
      return schema;
    } catch (error) {
      this.handleApiError(error, `Failed to fetch schema for ${modelType}`, {
        modelType,
        url: `${this.apiBaseUrl}/models/schema/${modelType}`
      });
      throw error;
    }
  }

  /**
   * Get all schemas at once
   * @returns {Promise<Object>} Map of model types to schemas
   */
  async getAllSchemas() {
    logger.info('Fetching all model schemas');
    
    try {
      const url = `${this.apiBaseUrl}/models/schemas`;
      
      logger.debug(`Sending request to ${url}`);
      
      const response = await axios.get(url);
      
      logger.info('All schemas fetched successfully', {
        status: response.status,
        hasData: !!response.data
      });
      
      // Get schemas from the response
      const schemas = response.data.schemas || {};
      
      // Process and cache each schema
      Object.entries(schemas).forEach(([modelType, schema]) => {
        // Convert validation-rules to camelCase
        if (schema.validation_rules) {
          schema.validationRules = schema.validation_rules;
          delete schema.validation_rules;
        }
        
        // Convert param-names to camelCase
        if (schema.param_names) {
          schema.paramNames = schema.param_names;
          delete schema.param_names;
        }
        
        // Cache the schema
        this.schemaCache[modelType] = schema;
      });
      
      logger.info(`Loaded and cached schemas for ${Object.keys(schemas).length} models`);
      
      return schemas;
    } catch (error) {
      this.handleApiError(error, 'Failed to fetch all schemas', {
        url: `${this.apiBaseUrl}/models/schemas`
      });
      throw error;
    }
  }

  /**
   * Generate a model with specified parameters
   * @param {string} modelType - Type of model to generate
   * @param {Object} params - Parameters for the model
   * @returns {Promise<Object>} Generation result with file name
   */
  async generateModel(modelType, params) {
    logger.info(`Generating ${modelType} model`, { params });
    
    try {
      const url = `${this.apiBaseUrl}/generate/${modelType}`;
      
      logger.debug(`Sending request to ${url}`, { params });
      
      const response = await axios.post(url, params);
      
      logger.info(`Model generation successful for ${modelType}`, {
        status: response.status,
        hasData: !!response.data
      });
      
      // Extract the filename from response
      let fileName = this.extractFileName(response.data, modelType, params);
      
      logger.info(`Generated model filename: ${fileName}`);
      
      return {
        ...response.data,
        fileName: fileName
      };
    } catch (error) {
      this.handleApiError(error, `Failed to generate ${modelType} model`, {
        modelType,
        params,
        url: `${this.apiBaseUrl}/generate/${modelType}`
      });
      throw error;
    }
  }

  /**
   * Render a model and get the image
   * @param {string} fileName - Filename of the model to render
   * @param {string} modelType - Type of the model
   * @returns {Promise<string>} Path to the rendered image
   */
  async renderModel(fileName, modelType) {
    try {
      // Ensure we're using just the base filename without extension
      const baseName = filenameUtils.baseFilename(fileName);
      const url = `${this.apiBaseUrl}/render/${baseName}?model_type=${modelType}&view=front`;
      
      logger.info(`Rendering model ${baseName}`, { modelType, url });
      
      const response = await axios.get(url, { responseType: 'arraybuffer' });
      
      logger.info(`Render successful for ${baseName}`, { status: response.status });
      
      // Save the image temporarily
      const tempImagePath = path.join(this.tempDir, `${baseName}_render.png`);
      fs.writeFileSync(tempImagePath, response.data);
      
      logger.debug(`Saved render to ${tempImagePath}`);
      
      return tempImagePath;
    } catch (error) {
      this.handleApiError(error, `Failed to render model: ${fileName}`, {
        fileName,
        modelType,
        url: `${this.apiBaseUrl}/render/${fileName}`
      });
      throw error;
    }
  }
  
  /**
   * Extract filename from the model generation response
   * @param {Object} data - Response data
   * @param {string} modelType - Type of model
   * @param {Object} params - Model parameters
   * @returns {string} Extracted or generated filename
   */
  extractFileName(data, modelType, params) {
    // Check all possible paths where the filename could be returned
    if (data.obj_path) {
      return filenameUtils.baseFilename(data.obj_path);
    } else if (data["obj-path"]) {
      return filenameUtils.baseFilename(data["obj-path"]);
    } else if (data.obj_result && data.obj_result.file) {
      return filenameUtils.baseFilename(data.obj_result.file);
    } else if (data["obj-result"] && data["obj-result"].file) {
      return filenameUtils.baseFilename(data["obj-result"].file);
    } else if (data.file_name) {
      return data.file_name;
    } else {
      // Generate a filename from the model type and parameters if not found
      logger.warn('No filename found in response, generating one', { data });
      return filenameUtils.generateModelFilename(modelType, params);
    }
  }
  
  /**
   * Handle API errors consistently
   * @param {Error} error - Caught error
   * @param {string} message - Error message prefix
   * @param {Object} context - Additional context
   * @throws {Error} Rethrows with better message
   */
  handleApiError(error, message, context = {}) {
    let errorMessage = message;
    let errorDetails = {};
    
    if (error.response) {
      // The request was made and the server responded with a non-2xx status
      errorDetails = {
        status: error.response.status,
        statusText: error.response.statusText,
        data: error.response.data
      };
      
      errorMessage += `: ${error.response.status} ${error.response.statusText}`;
      
      if (typeof error.response.data === 'string') {
        errorMessage += ` - ${error.response.data}`;
      } else if (error.response.data && error.response.data.message) {
        errorMessage += ` - ${error.response.data.message}`;
      }
    } else if (error.request) {
      // The request was made but no response was received
      errorDetails = {
        request: {
          method: error.request.method,
          path: error.request.path
        }
      };
      
      if (error.code === 'ECONNREFUSED') {
        errorMessage += `: Connection refused at ${error.address || error.host}:${error.port}`;
      } else {
        errorMessage += `: ${error.code || 'No response received'}`;
      }
    } else {
      // Something happened in setting up the request
      errorMessage += `: ${error.message}`;
    }
    
    logger.error(errorMessage, {
      ...context,
      error: {
        message: error.message,
        code: error.code,
        stack: error.stack
      },
      ...errorDetails
    });
    
    // Create a new error with the enhanced message
    const enhancedError = new Error(errorMessage);
    enhancedError.originalError = error;
    enhancedError.context = context;
    enhancedError.details = errorDetails;
    
    throw enhancedError;
  }
}

module.exports = new CADService();