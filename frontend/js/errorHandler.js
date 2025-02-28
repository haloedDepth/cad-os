/**
 * Centralized error handling for the frontend application
 */

import * as logger from './logger.js';
import * as ui from './uiController.js';

// Define error categories
export const ERROR_CATEGORY = {
  NETWORK: 'network',
  API: 'api',
  VALIDATION: 'validation',
  PARSING: 'parsing',
  RENDERING: 'rendering',
  UNKNOWN: 'unknown'
};

/**
 * Handle an error with proper logging and UI feedback
 * @param {Error|string} error - Error object or message
 * @param {string} category - Error category from ERROR_CATEGORY
 * @param {Object} context - Additional context for logging
 * @param {Function} callback - Optional callback after error is handled
 */
export function handleError(error, category = ERROR_CATEGORY.UNKNOWN, context = {}, callback = null) {
  const errorMessage = typeof error === 'string' ? error : error.message;
  
  // Log the error with context
  logger.error(`${category.toUpperCase()} ERROR: ${errorMessage}`, error, context);
  
  // Prepare user-friendly message based on error category
  let userMessage = errorMessage;
  
  switch (category) {
    case ERROR_CATEGORY.NETWORK:
      userMessage = `Network error: ${getNetworkErrorMessage(error)}`;
      break;
    case ERROR_CATEGORY.API:
      userMessage = `API error: ${getApiErrorMessage(error)}`;
      break;
    case ERROR_CATEGORY.VALIDATION:
      userMessage = `Invalid input: ${errorMessage}`;
      break;
    case ERROR_CATEGORY.PARSING:
      userMessage = `Error processing data: ${errorMessage}`;
      break;
    case ERROR_CATEGORY.RENDERING:
      userMessage = `Error displaying model: ${errorMessage}`;
      break;
    default:
      userMessage = `An error occurred: ${errorMessage}`;
  }
  
  // Display error to user
  ui.showStatus(userMessage, 'error');
  
  // Execute optional callback
  if (callback && typeof callback === 'function') {
    callback(error);
  }
  
  return userMessage;
}

/**
 * Get user-friendly message for network errors
 * @param {Error} error - Network error
 * @returns {string} User-friendly error message
 */
function getNetworkErrorMessage(error) {
  if (!error) return 'Could not connect to server';
  
  if (error.message && error.message.includes('Failed to fetch')) {
    return 'Could not connect to server. Please check your internet connection.';
  }
  
  if (error.message && error.message.includes('NetworkError')) {
    return 'Network error. Please check your connection and try again.';
  }
  
  if (error.message && error.message.includes('timeout')) {
    return 'Request timed out. Please try again.';
  }
  
  return error.message || 'Connection error';
}

/**
 * Get user-friendly message for API errors
 * @param {Error|Object} error - API error
 * @returns {string} User-friendly error message
 */
function getApiErrorMessage(error) {
  if (!error) return 'Server error';
  
  // Handle HTTP status codes
  if (error.status || error.statusCode) {
    const status = error.status || error.statusCode;
    
    if (status === 404) {
      return 'The requested resource was not found.';
    }
    
    if (status === 400) {
      return error.message || 'Invalid request parameters.';
    }
    
    if (status === 401) {
      return 'Authentication required.';
    }
    
    if (status === 403) {
      return 'You don\'t have permission to access this resource.';
    }
    
    if (status >= 500) {
      return 'Server error. Please try again later.';
    }
  }
  
  // Try to extract message from various API response formats
  if (error.message) {
    return error.message;
  }
  
  if (error.error) {
    return typeof error.error === 'string' ? error.error : JSON.stringify(error.error);
  }
  
  if (error.detail) {
    return typeof error.detail === 'string' ? error.detail : JSON.stringify(error.detail);
  }
  
  return 'Unknown server error';
}

/**
 * Wrap an async function with error handling
 * @param {Function} fn - Async function to wrap
 * @param {string} category - Error category from ERROR_CATEGORY
 * @param {Object} context - Additional context for logging
 * @returns {Function} Wrapped function with error handling
 */
export function withErrorHandling(fn, category = ERROR_CATEGORY.UNKNOWN, context = {}) {
  return async function(...args) {
    try {
      return await fn(...args);
    } catch (error) {
      handleError(error, category, { ...context, args });
      throw error; // Re-throw to allow further handling if needed
    }
  };
}

// Setup global error handlers
export function setupGlobalErrorHandlers() {
  // Handle unhandled promise rejections
  window.addEventListener('unhandledrejection', (event) => {
    handleError(
      event.reason || 'Unhandled Promise Rejection',
      ERROR_CATEGORY.UNKNOWN,
      { event }
    );
  });
  
  // Handle uncaught exceptions
  window.addEventListener('error', (event) => {
    handleError(
      event.error || event.message || 'Uncaught Error',
      ERROR_CATEGORY.UNKNOWN,
      { 
        fileName: event.filename,
        lineNo: event.lineno,
        colNo: event.colno,
        event 
      }
    );
    
    // Prevent default browser error handling
    event.preventDefault();
  });
  
  logger.info('Global error handlers set up');
}