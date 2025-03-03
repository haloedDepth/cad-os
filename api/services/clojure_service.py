import httpx
import os
from typing import Dict, Any, List, Optional

from config import CLOJURE_SERVICE_URL, DEFAULT_MODEL_TYPES
from utils.logger import get_logger
from utils.errors import ExternalServiceError
from utils import filename_utils

logger = get_logger("clojure_service")

async def get_model_types():
    """Get list of available model types from Clojure service"""
    try:
        logger.info("Requesting model types from Clojure service")
        async with httpx.AsyncClient() as client:
            response = await client.get(f"{CLOJURE_SERVICE_URL}/models/types")
            
            if response.status_code != 200:
                logger.warning(f"Clojure service responded with status {response.status_code}: {response.text}")
                # Return fallback types instead of raising an error
                return {"model_types": DEFAULT_MODEL_TYPES}
            
            try:
                data = response.json()
                logger.info(f"Received model types: {data}")
                
                # If data is already in the right format, return it
                if "model_types" in data:
                    return data
                
                # Otherwise wrap it in the expected format
                return {"model_types": data.get("model_types", DEFAULT_MODEL_TYPES)}
            except Exception as e:
                logger.error(f"Error parsing response as JSON", exc_info=True)
                # Return fallback types
                return {"model_types": DEFAULT_MODEL_TYPES}
    except httpx.RequestError as e:
        logger.error(f"Request error connecting to Clojure service", exc_info=True)
        # Return fallback types instead of raising an error
        return {"model_types": DEFAULT_MODEL_TYPES}

async def get_model_schema(model_type: str):
    """Get schema for a specific model type from Clojure service"""
    try:
        logger.info(f"Requesting schema for model type: {model_type}")
        async with httpx.AsyncClient() as client:
            response = await client.get(f"{CLOJURE_SERVICE_URL}/models/schema/{model_type}")
            
            if response.status_code == 200:
                schema = response.json()
                logger.debug(f"Received schema for {model_type}: {schema}")
                return schema
            else:
                logger.warning(f"Failed to get schema for {model_type}: {response.status_code} - {response.text}")
                return None
    except httpx.RequestError as e:
        logger.error(f"Request error getting schema for {model_type}", exc_info=True)
        return None

async def get_all_schemas():
    """Get all model schemas at once from Clojure service"""
    try:
        logger.info("Requesting all model schemas from Clojure service")
        async with httpx.AsyncClient() as client:
            response = await client.get(f"{CLOJURE_SERVICE_URL}/models/schemas")
            
            if response.status_code != 200:
                logger.warning(f"Failed to get all schemas: {response.status_code} - {response.text}")
                # Return empty schemas
                return {"schemas": {}}
            
            try:
                data = response.json()
                logger.info(f"Received schemas for {len(data.get('schemas', {}))} models")
                return data
            except Exception as e:
                logger.error(f"Error parsing schemas response as JSON", exc_info=True)
                # Return empty schemas
                return {"schemas": {}}
    except httpx.RequestError as e:
        logger.error(f"Request error getting all schemas", exc_info=True)
        # Return empty schemas
        return {"schemas": {}}

async def generate_model(model_type: str, params: Dict[str, Any]):
    """Generate a model with the given parameters"""
    logger.info(f"Generating {model_type} model with parameters: {params}")
    try:
        # Convert parameters to the format expected by the Clojure service
        # Replace underscores with hyphens in parameter names for Clojure convention
        converted_params = {}
        for key, value in params.items():
            # Convert keys with underscores to hyphenated format for Clojure
            new_key = key.replace("_", "-")
            converted_params[new_key] = value
            
        logger.debug(f"Converted parameters: {converted_params}")
        
        # Use an increased timeout for complex model generation (30 seconds)
        async with httpx.AsyncClient(timeout=30.0) as client:
            response = await client.post(
                f"{CLOJURE_SERVICE_URL}/generate/{model_type}",
                json=converted_params
            )
            
            response_status = response.status_code
            if response_status == 200:
                result = response.json()
                logger.info(f"Successfully generated {model_type} model")
                logger.debug(f"Generation result: {result}")
                
                # Make sure we're passing back all necessary information
                if "obj_result" in result and "file" in result["obj_result"]:
                    obj_path = result["obj_result"]["file"]
                    # Simply pass the full path without manipulating extensions
                    result["obj_path"] = obj_path
                
                return {"status": response_status, "data": result}
            else:
                try:
                    error_text = response.text
                except Exception as e:
                    error_text = f"Error communicating with Clojure service: {str(e)}"
                
                logger.error(f"Error generating model: {error_text}")
                return {"status": response_status, "error": error_text}
    except httpx.RequestError as e:
        error_msg = f"Error communicating with CAD service: {str(e)}"
        logger.error(error_msg, exc_info=True)
        return {"status": 503, "error": error_msg}

async def get_model_file(filename: str, format: str = None):
    """Retrieve a model file by filename and optional format"""
    try:
        base_name = filename_utils.base_filename(filename)
        
        # Determine URL based on whether format is provided
        if format:
            url = f"{CLOJURE_SERVICE_URL}/models/{base_name}/{format}"
        else:
            # Use obj format by default
            url = f"{CLOJURE_SERVICE_URL}/models/{base_name}/obj"
        
        logger.info(f"Requesting model file: {url}")
        async with httpx.AsyncClient() as client:
            response = await client.get(url, follow_redirects=True)
            
            if response.status_code == 200:
                logger.info(f"Successfully retrieved model file: {base_name}")
                return {"status": 200, "content": response.content, "filename": base_name}
            else:
                logger.warning(f"Failed to retrieve model file: {base_name}, status: {response.status_code}")
                return {"status": response.status_code, "error": "Model not found"}
    except httpx.RequestError as e:
        error_msg = f"Error communicating with CAD service: {str(e)}"
        logger.error(error_msg, exc_info=True)
        return {"status": 503, "error": error_msg}

async def render_model(filename: str, model_type: str = None, view: str = "front"):
    """Render a model and return the image"""
    try:
        base_name = filename_utils.base_filename(filename)
        logger.info(f"Requesting render for model: {base_name}, type: {model_type}, view: {view}")
        
        # If model_type is not provided, extract it from the filename
        if not model_type:
            model_type = filename_utils.extract_model_type(base_name)
            logger.info(f"Using extracted model type: {model_type}")
        
        # Create a temporary directory for the output if it doesn't exist
        output_dir = "render_output"
        if not os.path.exists(output_dir):
            os.makedirs(output_dir)
            
        # Determine the output image path
        image_name = f"{base_name}_{view}.png"
        image_path = os.path.join(output_dir, image_name)
        
        # Build the URL for the Clojure service render endpoint
        url = f"{CLOJURE_SERVICE_URL}/render/{base_name}"
        if view:
            url += f"/{view}"
        
        params = {}
        if model_type:
            params["model_type"] = model_type
            
        # Add rendering parameters
        params["size"] = 800  # Image size
        params["white_background"] = True
        
        logger.debug(f"Render URL: {url}, params: {params}")
        
        async with httpx.AsyncClient(timeout=30.0) as client:  # Longer timeout for rendering
            response = await client.get(url, params=params, follow_redirects=True)
            
            if response.status_code == 200:
                # Save the image temporarily
                with open(image_path, 'wb') as f:
                    f.write(response.content)
                
                logger.info(f"Successfully rendered model: {base_name}")
                return {
                    "status": 200,
                    "content": response.content,
                    "filename": image_name
                }
            else:
                logger.warning(f"Failed to render model: {base_name}, status: {response.status_code}")
                return {
                    "status": response.status_code,
                    "error": f"Failed to render model: {response.text}"
                }
    except httpx.RequestError as e:
        error_msg = f"Error communicating with CAD service: {str(e)}"
        logger.error(error_msg, exc_info=True)
        return {
            "status": 503,
            "error": error_msg
        }
    except Exception as e:
        error_msg = f"Unexpected error rendering model: {str(e)}"
        logger.exception(error_msg)
        return {
            "status": 500,
            "error": error_msg
        }