const axios = require('axios');
const FormData = require('form-data');
const fs = require('fs');
const path = require('path');
const config = require('../config');

class CADService {
  constructor() {
    this.apiBaseUrl = config.API_BASE_URL;
    this.clojureServiceUrl = config.CLOJURE_SERVICE_URL;
    
    console.log(`CADService initialized with API base URL: ${this.apiBaseUrl}`);
    console.log(`Clojure service URL: ${this.clojureServiceUrl}`);
    
    // Ensure temp directory exists
    this.tempDir = path.join(__dirname, '..', 'temp');
    if (!fs.existsSync(this.tempDir)) {
      fs.mkdirSync(this.tempDir, { recursive: true });
      console.log(`Created temp directory: ${this.tempDir}`);
    } else {
      console.log(`Using existing temp directory: ${this.tempDir}`);
    }
  }

  // Test connectivity to both IPv4 and IPv6 versions of a host
  async testConnectivity() {
    const urls = {
      'IPv4 API': 'http://127.0.0.1:8000/api',
      'IPv6 API': 'http://[::1]:8000/api',
      'IPv4 Clojure': 'http://127.0.0.1:3000',
      'IPv6 Clojure': 'http://[::1]:3000'
    };

    console.log('Testing connectivity to different IP versions...');
    
    for (const [name, url] of Object.entries(urls)) {
      try {
        console.log(`Testing ${name} at ${url}...`);
        const response = await axios.get(url, { timeout: 3000 });
        console.log(`✅ ${name} is reachable! Status: ${response.status}`);
      } catch (error) {
        console.log(`❌ ${name} is NOT reachable: ${error.message}`);
      }
    }
  }

  async generateModel(modelType, params) {
    try {
      const url = `${this.apiBaseUrl}/generate/${modelType}`;
      console.log(`Generating ${modelType} with params:`, params);
      console.log(`Making POST request to: ${url}`);
      
      // Test connectivity to both IPv4 and IPv6
      await this.testConnectivity();
      
      // Test if the API is reachable
      try {
        console.log('Testing API health check...');
        const healthCheckUrl = `${this.apiBaseUrl.replace(/\/api$/, '')}/api`;
        console.log(`Health check URL: ${healthCheckUrl}`);
        const healthCheck = await axios.get(healthCheckUrl);
        console.log('API health check response:', healthCheck.status, healthCheck.statusText);
      } catch (healthError) {
        console.log('API health check failed:', healthError.message);
        console.log('Error details:', healthError.code || 'No error code');
        if (healthError.response) {
          console.log('Response data:', healthError.response.data);
        } else {
          console.log('No response from API health check - API server may not be running');
        }
      }
      
      const response = await axios.post(url, params);
      
      console.log('Model generation successful. Response status:', response.status);
      console.log('Response data:', response.data);
      return response.data;
    } catch (error) {
      console.error('Error details:', {
        code: error.code || 'No error code',
        message: error.message,
        url: `${this.apiBaseUrl}/generate/${modelType}`,
        params: JSON.stringify(params)
      });
      
      if (error.response) {
        console.error('Response status:', error.response.status);
        console.error('Response data:', error.response.data);
      } else {
        console.error('No response object - likely a connectivity issue');
        console.error('Make sure the FastAPI server is running at:', this.apiBaseUrl);
      }
      
      throw new Error(`Failed to generate ${modelType} model: ${error.message}`);
    }
  }

  async renderModel(fileName, modelType) {
    try {
      const url = `${this.apiBaseUrl}/render/${fileName}?model_type=${modelType}&view=front`;
      console.log(`Requesting image render for ${fileName}, model type: ${modelType}`);
      console.log(`Making GET request to: ${url}`);
      
      const response = await axios.get(url, { responseType: 'arraybuffer' });
      
      console.log('Render successful. Response status:', response.status);
      
      // Save the image temporarily
      const tempImagePath = path.join(this.tempDir, `${fileName}_render.png`);
      fs.writeFileSync(tempImagePath, response.data);
      console.log(`Saved render to: ${tempImagePath}`);
      
      return tempImagePath;
    } catch (error) {
      console.error('Error rendering model image:', {
        code: error.code || 'No error code',
        message: error.message,
        url: `${this.apiBaseUrl}/render/${fileName}?model_type=${modelType}&view=front`
      });
      
      if (error.response) {
        console.error('Response status:', error.response.status);
        console.error('Response type:', typeof error.response.data);
      } else {
        console.error('No response object - likely a connectivity issue');
        console.error('Make sure the FastAPI server is running at:', this.apiBaseUrl);
      }
      
      // For now, since the rendering endpoint doesn't exist, we'll throw a specific error
      throw new Error('Rendering functionality is not yet implemented in the API.');
    }
  }
  
  // A workaround method to directly interact with the Clojure service for rendering
  async directRenderModel(fileName, modelType) {
    try {
      const url = `${this.clojureServiceUrl}/direct-render/${fileName}?model_type=${modelType}&view=front&size=800`;
      console.log(`Requesting direct render for ${fileName}, model type: ${modelType}`);
      console.log(`Making GET request to: ${url}`);
      
      const response = await axios.get(url, { responseType: 'arraybuffer' });
      
      console.log('Direct render successful. Response status:', response.status);
      
      // Save the image temporarily
      const tempImagePath = path.join(this.tempDir, `${fileName}_direct_render.png`);
      fs.writeFileSync(tempImagePath, response.data);
      console.log(`Saved direct render to: ${tempImagePath}`);
      
      return tempImagePath;
    } catch (error) {
      console.error('Error with direct rendering:', {
        code: error.code || 'No error code',
        message: error.message,
        url: `${this.clojureServiceUrl}/direct-render/${fileName}?model_type=${modelType}&view=front&size=800`
      });
      
      if (error.response) {
        console.error('Response status:', error.response.status);
      } else {
        console.error('No response object - likely a connectivity issue');
        console.error('Make sure the Clojure service is running at:', this.clojureServiceUrl);
      }
      
      throw new Error('Direct rendering is not implemented in the Clojure service.');
    }
  }
}

module.exports = new CADService();