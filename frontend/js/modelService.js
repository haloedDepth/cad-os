// API Service for model operations

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
  
  export async function loadModelSchema(modelType) {
    try {
      const response = await fetch(`/api/models/schema/${modelType}`);
      if (!response.ok) {
        throw new Error(`Failed to load schema for model type: ${modelType}`);
      }
      
      const schema = await response.json();
      console.log('Loaded schema:', schema);
      return schema;
    } catch (error) {
      console.error('Error loading model schema:', error);
      throw error;
    }
  }
  
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
      
      // Check if we have an obj_path or "obj-path" property (both formats)
      const objPath = data.obj_path || data["obj-path"];
      
      if (objPath) {
        console.log(`OBJ file path: ${objPath}`);
        // Strip .obj extension if present
        const fileName = objPath.replace(/\.obj$/i, '');
        return fileName;
      } else {
        console.error('Response data:', data);
        throw new Error('No OBJ file path returned from server');
      }
    } catch (error) {
      console.error('Error generating model:', error);
      throw error;
    }
  }
  
  export function downloadModel(fileName, format) {
    // Format to file extension mapping
    const formatToExt = {
      'obj': 'obj',
      'stl': 'stl',
      'step': 'stp',
      'g': 'g'
    };
    
    // Create a download link
    const downloadUrl = `/api/models/${fileName}/${format}`;
    console.log(`Downloading from: ${downloadUrl}`);
    
    // Create and click a temporary link
    const link = document.createElement('a');
    link.href = downloadUrl;
    link.download = `${fileName}.${formatToExt[format]}`;
    document.body.appendChild(link);
    link.click();
    document.body.removeChild(link);
  }