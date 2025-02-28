// Client-side validator for model parameters
import * as logger from './logger.js';
import { handleError, ERROR_CATEGORY } from './errorHandler.js';

/**
 * Evaluates a validation expression against parameter values
 * 
 * @param {string} expr - The expression to evaluate (e.g. "inner-diameter < outer-diameter")
 * @param {Object} params - Parameter values to validate
 * @returns {boolean} - Whether the expression evaluates to true
 */
export function evaluateExpression(expr, params) {
  try {
    logger.debug(`Evaluating expression: "${expr}"`, { params });
    
    // We'll use direct property access instead of JavaScript identifiers
    // First, parse the expression to identify parameter names
    const paramRegex = /([a-zA-Z][a-zA-Z0-9\-_]*)/g;
    const paramNames = Array.from(new Set(expr.match(paramRegex) || []));
    
    // Replace parameter names with their values in the expression
    let jsExpr = expr;
    paramNames.forEach(paramName => {
      if (params[paramName] !== undefined) {
        const value = params[paramName];
        // Use a regex that only matches whole words
        const regex = new RegExp(`\\b${paramName}\\b`, 'g');
        
        // For numeric values, just insert them directly
        if (typeof value === 'number' || (typeof value === 'string' && !isNaN(parseFloat(value)))) {
          const numValue = typeof value === 'number' ? value : parseFloat(value);
          jsExpr = jsExpr.replace(regex, numValue);
        } else if (typeof value === 'string') {
          // For string values, wrap in quotes
          jsExpr = jsExpr.replace(regex, `"${value}"`);
        } else if (value === null) {
          jsExpr = jsExpr.replace(regex, 'null');
        } else if (value === undefined) {
          jsExpr = jsExpr.replace(regex, 'undefined');
        } else {
          // For other values, use JSON.stringify
          jsExpr = jsExpr.replace(regex, JSON.stringify(value));
        }
      }
    });
    
    logger.debug(`Transformed expression: "${jsExpr}"`);
    
    // Now evaluate the transformed expression
    const result = eval(jsExpr);
    logger.debug(`Expression "${expr}" evaluated to: ${result}`);
    
    return !!result; // Ensure boolean result
  } catch (error) {
    handleError(error, ERROR_CATEGORY.VALIDATION, {
      component: "evaluateExpression",
      action: "evaluating expression",
      expression: expr
    });
    return false;
  }
}

/**
 * Validates parameters against all validation rules in a schema
 * 
 * @param {Object} params - Parameter values to validate
 * @param {Object} schema - Schema containing validation rules
 * @returns {Object} - { valid: boolean, errors: string[] }
 */
export function validateParameters(params, schema) {
  try {
    // Ensure we have valid inputs
    if (!schema || !schema.parameters) {
      logger.error("Invalid schema provided to validateParameters", { schema });
      return { valid: false, errors: ["Invalid schema"] };
    }
    
    // Ensure numbers are parsed
    const processedParams = {};
    
    schema.parameters.forEach(param => {
      const name = param.name;
      const value = params[name];
      
      if (param.type === 'number' && typeof value === 'string') {
        processedParams[name] = parseFloat(value);
      } else {
        processedParams[name] = value;
      }
    });
    
    // Apply validation rules
    const errors = [];
    
    // Check for validationRules first, then fall back to validation-rules
    const validationRules = schema.validationRules || schema["validation-rules"] || [];
    
    logger.debug("Applying validation rules", { ruleCount: validationRules.length });
    
    if (Array.isArray(validationRules)) {
      validationRules.forEach(rule => {
        try {
          const isValid = evaluateExpression(rule.expr, processedParams);
          
          if (!isValid) {
            errors.push(rule.message || `Validation failed: ${rule.expr}`);
          }
        } catch (error) {
          handleError(error, ERROR_CATEGORY.VALIDATION, {
            component: "validateParameters",
            action: "evaluating rule",
            rule: rule.expr
          });
          errors.push(`Error in validation rule: ${rule.expr}`);
        }
      });
    }
    
    // Check for required fields based on parameter definitions
    schema.parameters.forEach(param => {
      const name = param.name;
      if (!param.default && (processedParams[name] === undefined || processedParams[name] === null || processedParams[name] === "")) {
        errors.push(`${name} is required`);
      }
    });
    
    const result = {
      valid: errors.length === 0,
      errors: errors
    };
    
    logger.debug("Validation result", result);
    
    return result;
  } catch (error) {
    handleError(error, ERROR_CATEGORY.VALIDATION, {
      component: "validateParameters",
      action: "validating parameters"
    });
    return { valid: false, errors: ["Error during validation"] };
  }
}

/**
 * Prepares parameters for sending to the server
 * 
 * @param {Object} params - Raw parameter values from the form
 * @param {Object} schema - Schema containing parameter types
 * @returns {Object} - Processed parameters with correct types
 */
export function prepareParameters(params, schema) {
  try {
    logger.debug("Preparing parameters for sending", { paramCount: Object.keys(params).length });
    
    const prepared = {};
    
    schema.parameters.forEach(param => {
      const name = param.name;
      const value = params[name];
      
      // Skip undefined or null values
      if (value === undefined || value === null || value === '') {
        // If there's a default value, use it
        if (param.default !== undefined) {
          prepared[name] = param.type === 'number' ? 
                          parseFloat(param.default) : param.default;
        }
        return;
      }
      
      // Convert string numbers to actual numbers
      if (param.type === 'number' && typeof value === 'string') {
        prepared[name] = parseFloat(value);
      } else {
        prepared[name] = value;
      }
    });
    
    return prepared;
  } catch (error) {
    handleError(error, ERROR_CATEGORY.VALIDATION, {
      component: "prepareParameters",
      action: "preparing parameters for server"
    });
    return params; // Return original params if preparation fails
  }
}