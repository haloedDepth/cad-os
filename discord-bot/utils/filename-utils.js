/**
 * Utilities for handling model filenames in the Discord bot
 */
const crypto = require('crypto');

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
 * @returns {string} - File extension
 */
function getExtension(format) {
  return FORMAT_EXTENSIONS[format] || format;
}

/**
 * Extract base filename (without extension)
 * @param {string} filename - Filename with or without extension
 * @returns {string} - Base filename without extension
 */
function baseFilename(filename) {
  if (!filename) return '';
  const lastDotIndex = filename.lastIndexOf('.');
  return lastDotIndex > 0 ? filename.substring(0, lastDotIndex) : filename;
}

/**
 * Add the specified extension to a filename
 * @param {string} filename - Base filename
 * @param {string} format - Format name
 * @returns {string} - Filename with extension
 */
function withExtension(filename, format) {
  const base = baseFilename(filename);
  const ext = getExtension(format);
  return `${base}.${ext}`;
}

/**
 * Add a suffix to a filename before the extension
 * @param {string} filename - Base filename
 * @param {string} suffix - Suffix to add
 * @returns {string} - Filename with suffix
 */
function withSuffix(filename, suffix) {
  const base = baseFilename(filename);
  return `${base}_${suffix}`;
}

/**
 * Add a suffix to a filename and ensure it has the specified extension
 * @param {string} filename - Base filename
 * @param {string} suffix - Suffix to add
 * @param {string} format - Format name
 * @returns {string} - Filename with suffix and extension
 */
function withSuffixAndExtension(filename, suffix, format) {
  const base = baseFilename(filename);
  const ext = getExtension(format);
  return `${base}_${suffix}.${ext}`;
}

/**
 * Extract model type from a filename
 * @param {string} filename - Filename to parse
 * @returns {string} - Model type
 */
function extractModelType(filename) {
  const base = baseFilename(filename);
  const parts = base.split('-');
  return parts[0] || '';
}

/**
 * Generate a hash from model parameters
 * @param {string} modelType - Type of model
 * @param {Object} params - Parameters for the model
 * @returns {string} - Short hash string
 */
function generateHashFromParams(modelType, params) {
  // Sort parameters for consistent ordering
  const sortedEntries = Object.entries(params).sort(([a], [b]) => a.localeCompare(b));
  
  // Create a string representation of model type and parameters
  const paramParts = sortedEntries.map(([key, value]) => `${key}=${value}`);
  const paramString = `${modelType}:${paramParts.join(';')}`;
  
  // Generate SHA-256 hash
  const hash = crypto.createHash('sha256').update(paramString).digest('base64');
  
  // Replace characters that might cause issues in filenames
  const safeHash = hash.replace(/[\/\+=]/g, '_');
  
  // Return first 10 characters
  return safeHash.substring(0, 10);
}

/**
 * Encode a parameter value for use in a filename
 * @param {any} value - Value to encode
 * @returns {string} - Encoded value
 */
function encodeParamValue(value) {
  if (value === null || value === undefined) {
    return '';
  }
  
  const valueStr = String(value);
  return valueStr
    .replace(/\./g, '_dot_')
    .replace(/\//g, '_slash_')
    .replace(/\\/g, '_backslash_');
}

/**
 * Decode a parameter value from a filename
 * @param {string} valueStr - Encoded value string
 * @returns {any} - Decoded value
 */
function decodeParamValue(valueStr) {
  if (!valueStr) return '';
  
  // Restore special character encodings
  const decoded = valueStr
    .replace(/_dot_/g, '.')
    .replace(/_slash_/g, '/')
    .replace(/_backslash_/g, '\\');
  
  // Try to convert to number if it looks like one
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
function generateModelFilename(modelType, params) {
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
 * @returns {Object} - Extracted parameters
 */
function parseParamsFromFilename(filename) {
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

module.exports = {
  getExtension,
  baseFilename,
  withExtension,
  withSuffix,
  withSuffixAndExtension,
  extractModelType,
  encodeParamValue,
  decodeParamValue,
  generateModelFilename,
  parseParamsFromFilename,
  generateHashFromParams
};