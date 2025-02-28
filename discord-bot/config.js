require('dotenv').config();

// Debug logging for config values
console.log('Loading configuration...');
console.log('Discord token exists:', !!process.env.DISCORD_TOKEN);
console.log('Client ID exists:', !!process.env.CLIENT_ID);

// API URLs - explicitly use IPv4 (127.0.0.1) instead of localhost to avoid IPv6 issues
const apiBaseUrl = process.env.API_BASE_URL || 'http://127.0.0.1:8000/api';
const clojureServiceUrl = process.env.CLOJURE_SERVICE_URL || 'http://127.0.0.1:3000';

console.log('API Base URL:', apiBaseUrl, '(from env:', !!process.env.API_BASE_URL, ')');
console.log('Clojure Service URL:', clojureServiceUrl, '(from env:', !!process.env.CLOJURE_SERVICE_URL, ')');

const config = {
  // Discord configuration
  DISCORD_TOKEN: process.env.DISCORD_TOKEN,
  CLIENT_ID: process.env.CLIENT_ID,
  
  // CAD-OS API configuration
  API_BASE_URL: apiBaseUrl,
  CLOJURE_SERVICE_URL: clojureServiceUrl,
};

console.log('Configuration loaded');

module.exports = config;