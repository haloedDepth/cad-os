const axios = require('axios');
const FormData = require('form-data');
const fs = require('fs');
const path = require('path');
const config = require('../config');

class CADService {
  constructor() {
    this.apiBaseUrl = config.API_BASE_URL;
    this.clojureServiceUrl = config.CLOJURE_SERVICE_URL;
    
    // Ensure temp directory exists
    this.tempDir = path.join(__dirname, '..', 'temp');
    if (!fs.existsSync(this.tempDir)) {
      fs.mkdirSync(this.tempDir, { recursive: true });
    }
  }

  async generateModel(modelType, params) {
    try {
      console.log(`Generating ${modelType} with params:`, params);
      
      const response = await axios.post(
        `${this.apiBaseUrl}/generate/${modelType}`,
        params
      );
      
      console.log('Model generation response:', response.data);
      return response.data;
    } catch (error) {
      console.error('Error generating model:', error.response?.data || error.message);
      throw new Error(`Failed to generate ${modelType} model: ${error.response?.data?.message || error.message}`);
    }
  }

  async renderModel(fileName, modelType) {
    try {
      // Since the render functionality is not directly exposed in the API,
      // we'll use the existing BRL-CAD render command (rt) directly via a
      // custom endpoint we would add to the FastAPI service
      
      // For demonstration, we'll show how this would work if the endpoint existed
      console.log(`Requesting image render for ${fileName}, model type: ${modelType}`);
      
      // This is a theoretical endpoint that would need to be implemented in the FastAPI service
      const response = await axios.get(
        `${this.apiBaseUrl}/render/${fileName}?model_type=${modelType}&view=front`,
        { responseType: 'arraybuffer' }
      );
      
      // Save the image temporarily
      const tempImagePath = path.join(this.tempDir, `${fileName}_render.png`);
      fs.writeFileSync(tempImagePath, response.data);
      
      return tempImagePath;
    } catch (error) {
      console.error('Error rendering model image:', error.response?.data || error.message);
      
      // For now, since the rendering endpoint doesn't exist, we'll throw a specific error
      throw new Error('Rendering functionality is not yet implemented in the API.');
    }
  }
  
  // A workaround method to directly interact with the Clojure service for rendering
  // This would require us to add routes to the Clojure service
  async directRenderModel(fileName, modelType) {
    try {
      console.log(`Requesting direct render for ${fileName}, model type: ${modelType}`);
      
      // This is a theoretical endpoint that would need to be added to the Clojure service
      const response = await axios.get(
        `${this.clojureServiceUrl}/direct-render/${fileName}?model_type=${modelType}&view=front&size=800`,
        { responseType: 'arraybuffer' }
      );
      
      // Save the image temporarily
      const tempImagePath = path.join(this.tempDir, `${fileName}_direct_render.png`);
      fs.writeFileSync(tempImagePath, response.data);
      
      return tempImagePath;
    } catch (error) {
      console.error('Error with direct rendering:', error.response?.data || error.message);
      throw new Error('Direct rendering is not implemented in the Clojure service.');
    }
  }
}

module.exports = new CADService();