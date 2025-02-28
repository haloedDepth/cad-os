"""Utilities for working with model filenames."""

import re
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

def generate_model_filename(model_type: str, params: Dict[str, Any]) -> str:
    """Generate a standard filename for a model.
    Excludes position parameters as these are only relevant for assemblies.
    
    Args:
        model_type: Type of the model (e.g., 'washer', 'cylinder')
        params: Dict of parameter values for the model
        
    Returns:
        A standardized filename without extension
    """
    # Filter out position parameters
    filtered_params = {k: v for k, v in params.items() 
                      if not (k.startswith("position") or k.startswith("position_"))}
    
    # Sort parameters for consistent ordering
    sorted_params = sorted(filtered_params.items())
    
    # Format parameters as key=value
    param_strs = [f"{key}={value}" for key, value in sorted_params]
    
    # Join with underscores
    param_str = "_".join(param_strs)
    
    return f"{model_type}-{param_str}"

def parse_params_from_filename(filename: str) -> Dict[str, str]:
    """Extract parameters from a filename."""
    base = base_filename(filename)
    if "-" not in base:
        return {}
    
    params_part = base.split("-", 1)[1]
    param_pairs = params_part.split("_")
    
    params = {}
    for pair in param_pairs:
        if "=" in pair:
            key, value = pair.split("=", 1)
            params[key] = value
    
    return params