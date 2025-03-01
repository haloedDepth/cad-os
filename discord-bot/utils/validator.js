const logger = require('./logger')('validator');

/**
 * Validate parameters against a schema
 * @param {Object} params - Parameters to validate
 * @param {Object} schema - Model schema
 * @returns {Object} - { valid: boolean, errors: string[] }
 */
function validateParameters(params, schema) {
  logger.debug('Validating parameters against schema', { 
    paramCount: Object.keys(params).length,
    schemaName: schema.name
  });
  
  const errors = [];
  
  // Check required parameters
  if (schema.parameters && Array.isArray(schema.parameters)) {
    schema.parameters.forEach(param => {
      const name = param.name;
      // Skip hidden parameters
      if (param.hidden) return;
      
      // Check if required (no default value)
      if (!param.default && (params[name] === undefined || params[name] === null)) {
        errors.push(`${name} is required`);
      }
    });
  }
  
  // Check validation rules
  const validationRules = schema.validationRules || schema.validation_rules || [];
  if (validationRules.length > 0) {
    logger.debug('Checking validation rules', { ruleCount: validationRules.length });
    
    validationRules.forEach(rule => {
      try {
        const isValid = evaluateExpression(rule.expr, params);
        if (!isValid) {
          errors.push(rule.message || `Validation failed: ${rule.expr}`);
        }
      } catch (error) {
        logger.error(`Error evaluating rule: ${rule.expr}`, { error: error.message });
        errors.push(`Error in validation rule: ${rule.expr}`);
      }
    });
  }
  
  const result = {
    valid: errors.length === 0,
    errors: errors
  };
  
  logger.debug('Validation result', result);
  return result;
}

/**
 * Evaluate a validation expression
 * @param {string} expr - Expression to evaluate (e.g., "inner-diameter < outer-diameter")
 * @param {Object} params - Parameters to use in evaluation
 * @returns {boolean} - Whether the expression is valid
 */
function evaluateExpression(expr, params) {
  logger.debug(`Evaluating expression: ${expr}`);
  
  try {
    // Replace parameter names with their values
    let jsExpr = expr;
    
    // Find all parameter names in the expression
    const paramRegex = /([a-zA-Z][a-zA-Z0-9\-_]*)/g;
    const paramNames = [...new Set(expr.match(paramRegex) || [])];
    
    // Replace parameter names with their values
    paramNames.forEach(name => {
      if (params[name] !== undefined) {
        // Create a pattern that matches whole words only
        const pattern = new RegExp(`\\b${name}\\b`, 'g');
        
        // Replace with the parameter value
        if (typeof params[name] === 'number') {
          jsExpr = jsExpr.replace(pattern, params[name]);
        } else if (typeof params[name] === 'string' && !isNaN(parseFloat(params[name]))) {
          jsExpr = jsExpr.replace(pattern, parseFloat(params[name]));
        } else {
          jsExpr = jsExpr.replace(pattern, JSON.stringify(params[name]));
        }
      }
    });
    
    logger.debug(`Transformed expression: ${jsExpr}`);
    
    // Evaluate the expression
    const result = eval(jsExpr);
    
    logger.debug(`Expression evaluation result: ${result}`);
    return !!result;
  } catch (error) {
    logger.error(`Error evaluating expression: ${expr}`, { error: error.message });
    throw error;
  }
}

/**
 * Format parameter values for display
 * @param {Object} params - Parameter values
 * @param {Object} schema - Model schema
 * @returns {string} - Formatted parameter list
 */
function formatParameterList(params, schema) {
  const lines = [];
  
  if (schema && schema.parameters) {
    // Use schema to determine parameter display order
    schema.parameters.forEach(paramSpec => {
      const name = paramSpec.name;
      if (params[name] !== undefined && !paramSpec.hidden) {
        const displayName = name.replace(/-/g, ' ').replace(/\b\w/g, c => c.toUpperCase());
        lines.push(`${displayName}: ${params[name]}`);
      }
    });
  } else {
    // Without schema, just list all parameters
    Object.entries(params).forEach(([name, value]) => {
      const displayName = name.replace(/-/g, ' ').replace(/\b\w/g, c => c.toUpperCase());
      lines.push(`${displayName}: ${value}`);
    });
  }
  
  return lines.join('\n');
}

module.exports = {
  validateParameters,
  evaluateExpression,
  formatParameterList
};