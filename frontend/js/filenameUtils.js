/**
 * Utilities for handling model filenames
 */

// Map of formats to file extensions
const FORMAT_EXTENSIONS = {
  obj: "obj",
  stl: "stl",
  step: "stp",
  g: "g"
};

/**
 * Get the file extension for a format
 * @param {string} format - Format name
 * @returns {string} - File extension or the format itself if not found
 */
export function getExtension(format) {
  // Handle null or undefined format
  if (format === null || format === undefined) {
    return "";
  }
  return FORMAT_EXTENSIONS[format.toLowerCase()] || format;
}

/**
 * Extract base filename (without extension)
 * @param {string} filename - Filename with or without extension
 * @returns {string} - Base filename without extension or empty string if invalid
 */
export function baseFilename(filename) {
  if (!filename) return '';
  const lastDotIndex = filename.lastIndexOf('.');
  return lastDotIndex > -1 ? filename.substring(0, lastDotIndex) : filename;
}

/**
 * Add the specified extension to a filename
 * @param {string} filename - Base filename
 * @param {string} format - Format name
 * @returns {string} - Filename with extension or null if invalid input
 */
export function withExtension(filename, format) {
  if (!filename || !format) return null;
  const base = baseFilename(filename);
  const ext = getExtension(format);
  return `${base}.${ext}`;
}

/**
 * Extract model type from a filename
 * @param {string} filename - Filename to parse
 * @returns {string} - Model type or empty string if not found
 */
export function extractModelType(filename) {
  const base = baseFilename(filename);
  const parts = base.split('-');
  return parts.length > 0 ? parts[0] : '';
}

/**
 * Generate a hash from model parameters
 * @param {string} modelType - Type of model
 * @param {Object} params - Parameters for the model
 * @returns {string} - Short hash string
 */
export function generateHashFromParams(modelType, params) {
  // Sort parameters for consistent ordering
  const sortedEntries = Object.entries(params).sort(([a], [b]) => a.localeCompare(b));
  
  // Create a string representation of model type and parameters
  const paramParts = sortedEntries.map(([key, value]) => `${key}=${value}`);
  const paramString = `${modelType}:${paramParts.join(';')}`;
  
  // Use SubtleCrypto API if available, otherwise fallback to simpler hash
  if (window.crypto && window.crypto.subtle) {
    // This would be async in real implementation:
    // Instead, for simplicity we'll use a deterministic hash algorithm
    return simpleHash(paramString);
  } else {
    return simpleHash(paramString);
  }
}

/**
 * Simple hash function for parameter strings
 * @param {string} str - String to hash
 * @returns {string} - Hash string
 */
function simpleHash(str) {
  let hash = 0;
  if (str.length === 0) return 'hash000000';
  
  for (let i = 0; i < str.length; i++) {
    const char = str.charCodeAt(i);
    hash = ((hash << 5) - hash) + char;
    hash = hash & hash; // Convert to 32bit integer
  }
  
  // Convert to base36 and ensure it's 10 chars
  const hashStr = Math.abs(hash).toString(36);
  return hashStr.padStart(10, '0').substring(0, 10);
}

/**
 * Encode a parameter value for use in a filename
 * @param {any} value - Value to encode
 * @returns {string} - Encoded value
 */
export function encodeParamValue(value) {
  if (value === null || value === undefined) {
    return '';
  }
  const valueStr = String(value);
  return valueStr
    .replace(/\./g, '_dot_')
    .replace(/\//g, '_slash_')
    .replace(/\\/g, '_backslash_')
    .replace(/=/g, '_equals_')
    .replace(/,/g, '_comma_');
}

/**
 * Decode a parameter value from a filename
 * @param {string} valueStr - Encoded value string
 * @returns {any} - Decoded value
 */
export function decodeParamValue(valueStr) {
  if (!valueStr) return '';
  const decoded = valueStr
    .replace(/_dot_/g, '.')
    .replace(/_slash_/g, '/')
    .replace(/_backslash_/g, '\\')
    .replace(/_equals_/g, '=')
    .replace(/_comma_/g, ',');

  if (/^-?\d+(\.\d+)?$/.test(decoded)) {
    return Number(decoded);
  }
  return decoded;
}

/**
 * Generate a standardized filename for a model using hash-based approach
 * @param {string} modelType - Type of model
 * @param {Object} params - Parameters for the model
 * @returns {string} - Standardized filename without extension
 */
export function generateModelFilename(modelType, params) {
  if (!modelType || !params) return null;

  // Filter out position parameters
  const filteredParams = Object.fromEntries(
    Object.entries(params).filter(([key]) =>
      !key.startsWith('position') && !key.startsWith('position_')
    )
  );

  // Generate a hash based on model type and parameters
  const paramHash = generateHashFromParams(modelType, filteredParams);
  
  // Construct the shortened filename with hash
  return `${modelType}-${paramHash}`;
}

/**
 * Parse parameters from a legacy filename (for backward compatibility)
 * @param {string} filename - Filename to parse
 * @returns {Object} - Extracted parameters or an empty object if parsing fails
 */
export function parseParamsFromFilename(filename) {
  const base = baseFilename(filename);
  const parts = base.split('-');

  if (parts.length < 2) return {};

  const paramsStr = parts[1];
  const paramPairs = paramsStr.split('_');

  const params = {};
  paramPairs.forEach(pair => {
    if (pair.includes('=')) {
      const [key, value] = pair.split('=');
      if (key && value !== undefined) {
        params[key] = decodeParamValue(value);
      }
    }
  });

  return params;
}