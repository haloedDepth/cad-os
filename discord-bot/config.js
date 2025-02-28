require('dotenv').config();
const logger = require('./utils/logger')('config');

// Load and validate required environment variables
const DISCORD_TOKEN = process.env.DISCORD_TOKEN;
const CLIENT_ID = process.env.CLIENT_ID;

// Ensure required variables are present
if (!DISCORD_TOKEN) {
  logger.error('Missing required environment variable: DISCORD_TOKEN');
  throw new Error('DISCORD_TOKEN is required in .env file');
}

if (!CLIENT_ID) {
  logger.error('Missing required environment variable: CLIENT_ID');
  throw new Error('CLIENT_ID is required in .env file');
}

// API URLs - explicitly use IPv4 (127.0.0.1) instead of localhost to avoid IPv6 issues
const API_BASE_URL = process.env.API_BASE_URL || 'http://127.0.0.1:8000/api';
const CLOJURE_SERVICE_URL = process.env.CLOJURE_SERVICE_URL || 'http://127.0.0.1:3000';

logger.info('Configuration loaded', {
  apiBaseUrl: API_BASE_URL,
  clojureServiceUrl: CLOJURE_SERVICE_URL,
  hasDiscordToken: !!DISCORD_TOKEN,
  hasClientId: !!CLIENT_ID
});

const config = {
  // Discord configuration
  DISCORD_TOKEN,
  CLIENT_ID,
  GUILD_ID: process.env.GUILD_ID,
  
  // CAD-OS API configuration
  API_BASE_URL,
  CLOJURE_SERVICE_URL,
  
  // Logging configuration
  LOG_LEVEL: process.env.LOG_LEVEL || 'info'
};

module.exports = config;