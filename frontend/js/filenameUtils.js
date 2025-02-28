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
 * @returns {string} - File extension
 */
export function getExtension(format) {
  return FORMAT_EXTENSIONS[format] || format;
}

/**
 * Extract base filename (without extension)
 * @param {string} filename - Filename with or without extension
 * @returns {string} - Base filename without extension
 */
export function baseFilename(filename) {
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
export function withExtension(filename, format) {
  const base = baseFilename(filename);
  const ext = getExtension(format);
  return `${base}.${ext}`;
}

/**
 * Extract model type from a filename
 * @param {string} filename - Filename to parse
 * @returns {string} - Model type
 */
export function extractModelType(filename) {
  const base = baseFilename(filename);
  const parts = base.split('-');
  return parts[0] || '';
}

/**
 * Generate a standardized filename for a model
 * Excludes position parameters as these are only relevant for assemblies.
 * @param {string} modelType - Type of model
 * @param {Object} params - Parameters for the model
 * @returns {string} - Standardized filename without extension
 */
export function generateModelFilename(modelType, params) {
  // Filter out position parameters
  const filteredParams = Object.fromEntries(
    Object.entries(params).filter(([key]) => 
      !key.startsWith('position') && !key.startsWith('position_')
    )
  );
  
  // Sort parameters for consistent ordering
  const sortedParams = Object.entries(filteredParams).sort(([a], [b]) => a.localeCompare(b));
  
  // Format parameters as key=value
  const paramStrs = sortedParams.map(([key, value]) => `${key}=${value}`);
  
  // Join with underscores
  const paramStr = paramStrs.join('_');
  
  return `${modelType}-${paramStr}`;
}

/**
 * Parse parameters from a filename
 * @param {string} filename - Filename to parse
 * @returns {Object} - Extracted parameters
 */
export function parseParamsFromFilename(filename) {
  const base = baseFilename(filename);
  const parts = base.split('-');
  
  if (parts.length < 2) return {};
  
  const paramsStr = parts[1];
  const paramPairs = paramsStr.split('_');
  
  const params = {};
  paramPairs.forEach(pair => {
    const [key, value] = pair.split('=');
    if (key && value !== undefined) {
      params[key] = value;
    }
  });
  
  return params;
}