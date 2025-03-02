/**
 * Utilities for handling model filenames in the Discord bot
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
 * Generate a standardized filename for a model
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
  
  // Sort parameters for consistent ordering
  const sortedParams = Object.entries(filteredParams).sort(([a], [b]) => a.localeCompare(b));
  
  // Format parameters as key=value with encoded values
  const paramStrs = sortedParams.map(([key, value]) => `${key}=${encodeParamValue(value)}`);
  
  // Join with underscores
  const paramStr = paramStrs.join('_');
  
  return `${modelType}-${paramStr}`;
}

/**
 * Parse parameters from a filename
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
  extractModelType,
  encodeParamValue,
  decodeParamValue,
  generateModelFilename,
  parseParamsFromFilename
};