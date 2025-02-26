import httpx
from config import CLOJURE_SERVICE_URL, DEFAULT_MODEL_TYPES
import logging

logger = logging.getLogger(__name__)

async def get_model_types():
    """Get list of available model types from Clojure service"""
    try:
        logger.info("Requesting model types from Clojure service")
        async with httpx.AsyncClient() as client:
            response = await client.get(f"{CLOJURE_SERVICE_URL}/models/types")
            
            if response.status_code != 200:
                logger.warning(f"Error from Clojure service: {response.status_code} - {response.text}")
                # Return fallback types instead of raising an error
                return {"model_types": DEFAULT_MODEL_TYPES}
            
            try:
                data = response.json()
                logger.info(f"Received model types from Clojure: {data}")
                
                # If data is already in the right format, return it
                if "model_types" in data:
                    return data
                
                # Otherwise wrap it in the expected format
                return {"model_types": data.get("model_types", DEFAULT_MODEL_TYPES)}
            except Exception as e:
                logger.error(f"Error parsing response as JSON: {e}")
                # Return fallback types
                return {"model_types": DEFAULT_MODEL_TYPES}
    except httpx.RequestError as e:
        logger.error(f"Request error: {e}")
        # Return fallback types instead of raising an error
        return {"model_types": DEFAULT_MODEL_TYPES}

async def get_model_schema(model_type: str):
    """Get schema for a specific model type from Clojure service"""
    try:
        logger.info(f"Requesting schema for model type: {model_type}")
        async with httpx.AsyncClient() as client:
            response = await client.get(f"{CLOJURE_SERVICE_URL}/models/schema/{model_type}")
            
            if response.status_code == 200:
                return response.json()
            else:
                logger.warning(f"Error from Clojure service: {response.status_code} - {response.text}")
                return None
    except httpx.RequestError as e:
        logger.error(f"Request error: {e}")
        return None

async def generate_model(model_type: str, params: dict):
    """Generate a model with the given parameters"""
    try:
        logger.info(f"Received parameters for {model_type}: {params}")
        
        # Convert parameters to the format expected by the Clojure service
        # Replace underscores with hyphens in parameter names for Clojure convention
        converted_params = {}
        for key, value in params.items():
            # Convert keys with underscores to hyphenated format for Clojure
            new_key = key.replace("_", "-")
            converted_params[new_key] = value
            
        logger.info(f"Converted parameters: {converted_params}")
        
        async with httpx.AsyncClient() as client:
            response = await client.post(
                f"{CLOJURE_SERVICE_URL}/generate/{model_type}",
                json=converted_params
            )
            
            response_status = response.status_code
            if response_status == 200:
                result = response.json()
                logger.info(f"Success response from Clojure service: {result}")
                
                # Make sure we're passing back all necessary information
                if "obj_result" in result and "file" in result["obj_result"]:
                    result["obj_path"] = result["obj_result"]["file"]
                
                return {"status": response_status, "data": result}
            else:
                try:
                    error_text = response.text
                    if callable(getattr(response, "text", None)):
                        error_text = await response.text()
                except Exception as e:
                    error_text = f"Error communicating with Clojure service: {str(e)}"
                
                logger.warning(f"Error response from Clojure service: {error_text}")
                return {"status": response_status, "error": error_text}
    except httpx.RequestError as e:
        logger.error(f"Request error communicating with CAD service: {str(e)}")
        return {"status": 503, "error": f"Error communicating with CAD service: {str(e)}"}

async def get_model_file(filename: str, format: str = None):
    """Retrieve a model file by filename and optional format"""
    try:
        # Determine URL based on whether format is provided
        if format:
            url = f"{CLOJURE_SERVICE_URL}/models/{filename}/{format}"
        else:
            # Ensure we're requesting with .obj extension for the default case
            request_filename = filename if filename.endswith(".obj") else f"{filename}.obj"
            url = f"{CLOJURE_SERVICE_URL}/models/{request_filename}"
        
        logger.info(f"Requesting model file: {url}")
        async with httpx.AsyncClient() as client:
            response = await client.get(url, follow_redirects=True)
            
            if response.status_code == 200:
                return {"status": 200, "content": response.content, "filename": filename}
            else:
                logger.warning(f"Error from Clojure service: {response.status_code} - {response.text}")
                return {"status": response.status_code, "error": "Model not found"}
    except httpx.RequestError as e:
        logger.error(f"Request error: {e}")
        return {"status": 503, "error": f"Error communicating with CAD service: {str(e)}"}

async def get_all_schemas():
    """Get all model schemas at once from Clojure service"""
    try:
        logger.info("Requesting all model schemas from Clojure service")
        async with httpx.AsyncClient() as client:
            response = await client.get(f"{CLOJURE_SERVICE_URL}/models/schemas")
            
            if response.status_code != 200:
                logger.warning(f"Error from Clojure service: {response.status_code} - {response.text}")
                # Return empty schemas
                return {"schemas": {}}
            
            try:
                data = response.json()
                logger.info(f"Received schemas for {len(data.get('schemas', {}))} models")
                return data
            except Exception as e:
                logger.error(f"Error parsing response as JSON: {e}")
                # Return empty schemas
                return {"schemas": {}}
    except httpx.RequestError as e:
        logger.error(f"Request error: {e}")
        # Return empty schemas
        return {"schemas": {}}