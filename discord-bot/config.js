require('dotenv').config();

module.exports = {
  // Discord configuration
  DISCORD_TOKEN: process.env.DISCORD_TOKEN,
  CLIENT_ID: process.env.CLIENT_ID,
  
  
  // CAD-OS API configuration
  API_BASE_URL: process.env.API_BASE_URL || 'http://localhost:8000/api',
  CLOJURE_SERVICE_URL: process.env.CLOJURE_SERVICE_URL || 'http://localhost:3000',
};