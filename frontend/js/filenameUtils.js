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
  //Added explicit check for null or undefined format
  if (format === null || format === undefined) {
    return "";
  }
  return FORMAT_EXTENSIONS[format.toLowerCase()] || format; // handle case-insensitive formats
}

/**
 * Extract base filename (without extension)
 * @param {string} filename - Filename with or without extension
 * @returns {string} - Base filename without extension or empty string if invalid
 */
export function baseFilename(filename) {
  if (!filename) return '';
  const lastDotIndex = filename.lastIndexOf('.');
  return lastDotIndex > -1 ? filename.substring(0, lastDotIndex) : filename; // Corrected comparison
}

/**
 * Add the specified extension to a filename
 * @param {string} filename - Base filename
 * @param {string} format - Format name
 * @returns {string} - Filename with extension or null if invalid input
 */
export function withExtension(filename, format) {
  if (!filename || !format) return null; //Added null check for both parameters
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
  return parts.length > 0 ? parts[0] : ''; // Handle cases with no '-'
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
    .replace(/=/g, '_equals_') // added to prevent conflicts with key=value pairs
    .replace(/,/g, '_comma_'); // added to prevent conflicts with CSV-like data
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
    .replace(/_equals_/g, '=') // added
    .replace(/_comma_/g, ','); // added

  if (/^-?\d+(\.\d+)?$/.test(decoded)) {
    return Number(decoded);
  }
  return decoded;
}

/**
 * Generate a standardized filename for a model
 * @param {string} modelType - Type of model
 * @param {Object} params - Parameters for the model
 * @returns {string} - Standardized filename without extension or null if invalid input
 */
export function generateModelFilename(modelType, params) {
  if (!modelType || !params) return null; // Added null checks

  const filteredParams = Object.fromEntries(
    Object.entries(params).filter(([key]) =>
      !key.startsWith('position') && !key.startsWith('position_')
    )
  );

  const sortedParams = Object.entries(filteredParams).sort(([a], [b]) => a.localeCompare(b));

  const paramStrs = sortedParams.map(([key, value]) => `${key}=${encodeParamValue(value)}`);
  const paramStr = paramStrs.join('_');

  return `${modelType}-${paramStr}`;
}


/**
 * Parse parameters from a filename
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
