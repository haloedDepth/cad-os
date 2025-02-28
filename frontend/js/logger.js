/**
 * Centralized logger for the frontend application
 */

// Log levels
export const LOG_LEVEL = {
    DEBUG: 'debug',
    INFO: 'info',
    WARNING: 'warn',
    ERROR: 'error'
  };
  
  // Determine current environment
  const isProduction = window.location.hostname !== 'localhost' && 
                      !window.location.hostname.includes('127.0.0.1');
  
  // Default minimum log level based on environment
  let minLogLevel = isProduction ? LOG_LEVEL.INFO : LOG_LEVEL.DEBUG;
  
  // Mapping of log levels to numeric values for comparison
  const LOG_LEVEL_VALUE = {
    [LOG_LEVEL.DEBUG]: 0,
    [LOG_LEVEL.INFO]: 1,
    [LOG_LEVEL.WARNING]: 2,
    [LOG_LEVEL.ERROR]: 3
  };
  
  /**
   * Set the minimum log level
   * @param {string} level - One of LOG_LEVEL values
   */
  export function setLogLevel(level) {
    if (LOG_LEVEL_VALUE[level] !== undefined) {
      minLogLevel = level;
    }
  }
  
  /**
   * Get the current log level
   * @returns {string} Current log level
   */
  export function getLogLevel() {
    return minLogLevel;
  }
  
  /**
   * Check if a log level should be displayed
   * @param {string} level - Log level to check
   * @returns {boolean} Whether the level should be displayed
   */
  function shouldLog(level) {
    return LOG_LEVEL_VALUE[level] >= LOG_LEVEL_VALUE[minLogLevel];
  }
  
  /**
   * Format a log message with context
   * @param {string} message - Main log message
   * @param {Object} context - Additional context data
   * @returns {string} Formatted log message
   */
  function formatMessage(message, context) {
    if (!context || Object.keys(context).length === 0) {
      return message;
    }
    
    try {
      const contextStr = JSON.stringify(context);
      return `${message} | Context: ${contextStr}`;
    } catch (e) {
      return `${message} | Context: [Object]`;
    }
  }
  
  /**
   * Log a debug message
   * @param {string} message - Log message
   * @param {Object} context - Additional context
   */
  export function debug(message, context = {}) {
    if (shouldLog(LOG_LEVEL.DEBUG)) {
      console.debug(formatMessage(message, context));
    }
  }
  
  /**
   * Log an info message
   * @param {string} message - Log message
   * @param {Object} context - Additional context
   */
  export function info(message, context = {}) {
    if (shouldLog(LOG_LEVEL.INFO)) {
      console.info(formatMessage(message, context));
    }
  }
  
  /**
   * Log a warning message
   * @param {string} message - Log message
   * @param {Object} context - Additional context
   */
  export function warn(message, context = {}) {
    if (shouldLog(LOG_LEVEL.WARNING)) {
      console.warn(formatMessage(message, context));
    }
  }
  
  /**
   * Log an error message
   * @param {string} message - Log message
   * @param {Error|null} error - Error object if available
   * @param {Object} context - Additional context
   */
  export function error(message, error = null, context = {}) {
    if (shouldLog(LOG_LEVEL.ERROR)) {
      const errorContext = { ...context };
      
      if (error) {
        errorContext.error = {
          message: error.message,
          name: error.name,
          stack: error.stack
        };
      }
      
      console.error(formatMessage(message, errorContext));
    }
  }
  
  // Add ability to send logs to server (can be implemented later)
  export function enableServerLogging(endpoint) {
    // To be implemented if needed
  }