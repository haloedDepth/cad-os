"""Utilities for working with model filenames."""

import re
import hashlib
import base64
from typing import Dict, Any, Tuple, Optional

# Map of formats to file extensions
FORMAT_EXTENSIONS = {
    "obj": "obj",
    "stl": "stl",
    "step": "stp",
    "g": "g"
}

def get_extension(format_name: str) -> str:
    """Get the file extension for a given format."""
    return FORMAT_EXTENSIONS.get(format_name, format_name)

def base_filename(filename: str) -> str:
    """Extract the base filename without extension."""
    if "." in filename:
        return filename.rsplit(".", 1)[0]
    return filename

def with_extension(filename: str, format_name: str) -> str:
    """Add the specified extension to a filename."""
    base = base_filename(filename)
    ext = get_extension(format_name)
    return f"{base}.{ext}"

def extract_model_type(filename: str) -> str:
    """Extract the model type from a filename."""
    base = base_filename(filename)
    if "-" in base:
        return base.split("-", 1)[0]
    return base

def generate_hash_from_params(model_type: str, params: Dict[str, Any]) -> str:
    """
    Generate a short hash from model type and parameters for unique identification.
    
    Args:
        model_type: Type of the model (e.g., 'washer', 'cylinder')
        params: Dict of parameter values for the model
        
    Returns:
        A short hash string (10 characters)
    """
    # Sort parameters for consistent ordering
    sorted_params = sorted(params.items())
    
    # Create a string representation of model type and parameters
    param_parts = [f"{key}={value}" for key, value in sorted_params]
    param_str = f"{model_type}:{';'.join(param_parts)}"
    
    # Generate SHA-256 hash
    hash_obj = hashlib.sha256(param_str.encode('utf-8'))
    hash_bytes = hash_obj.digest()
    
    # Encode as base64 and take first 10 chars (for shorter filenames)
    hash_encoded = base64.b64encode(hash_bytes).decode('utf-8')
    # Replace problematic characters
    hash_encoded = hash_encoded.replace('/', '_').replace('+', '_').replace('=', '_')
    
    return hash_encoded[:10]

def encode_param_value(value: Any) -> str:
    """Encode a parameter value for use in a filename."""
    if value is None:
        return ""
    
    value_str = str(value)
    return value_str.replace(".", "_dot_").replace("/", "_slash_").replace("\\", "_backslash_")

def decode_param_value(value_str: str) -> Any:
    """Decode a parameter value from a filename."""
    if not value_str:
        return ""
    
    decoded = value_str.replace("_dot_", ".").replace("_slash_", "/").replace("_backslash_", "\\")
    
    # Try to convert to number if applicable
    try:
        if "." in decoded:
            return float(decoded)
        return int(decoded)
    except ValueError:
        return decoded

def generate_model_filename(model_type: str, params: Dict[str, Any]) -> str:
    """
    Generate a standard filename for a model using hash-based approach.
    
    Args:
        model_type: Type of the model (e.g., 'washer', 'cylinder')
        params: Dict of parameter values for the model
        
    Returns:
        A standardized filename without extension
    """
    # Filter out position parameters
    filtered_params = {k: v for k, v in params.items() 
                      if not (k.startswith("position") or k.startswith("position_"))}
    
    # Generate a hash based on model type and parameters
    param_hash = generate_hash_from_params(model_type, filtered_params)
    
    # Construct the shortened filename with hash
    return f"{model_type}-{param_hash}"

def parse_params_from_filename(filename: str) -> Dict[str, Any]:
    """
    Extract parameters from a legacy filename.
    
    This function is maintained for compatibility with old-format filenames
    that embedded parameters directly.
    """
    base = base_filename(filename)
    if "-" not in base:
        return {}
    
    params_part = base.split("-", 1)[1]
    param_pairs = params_part.split("_")
    
    params = {}
    for pair in param_pairs:
        if "=" in pair:
            key, value = pair.split("=", 1)
            params[key] = decode_param_value(value)
    
    return params