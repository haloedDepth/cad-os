from fastapi import APIRouter, Response, Body
from typing import Dict, Any
import traceback

from services import clojure_service
from utils import filename_utils
from utils.logger import get_logger
from utils.errors import (
    ResourceNotFoundError, 
    ExternalServiceError, 
    ClientError,
    ServerError
)

router = APIRouter(
    prefix="/api",
    tags=["models"],
)

logger = get_logger("models_router")

@router.get("/models/types")
async def get_model_types():
    """Get list of available model types"""
    logger.info("Request for model types received")
    return await clojure_service.get_model_types()

@router.get("/models/schemas")
async def get_all_schemas():
    """Get all model schemas at once"""
    logger.info("Request for all schemas received")
    try:
        response = await clojure_service.get_all_schemas()
        logger.info(f"Returning schemas for {len(response.get('schemas', {}))} models")
        return response
    except Exception as e:
        logger.error(f"Error getting all schemas", exc_info=True)
        raise ExternalServiceError(
            message="Error fetching model schemas",
            detail={"error": str(e)}
        )

@router.get("/models/schema/{model_type}")
async def get_model_schema(model_type: str):
    """Get schema for a specific model type"""
    logger.info(f"Request for schema of model type: {model_type}")
    schema = await clojure_service.get_model_schema(model_type)
    
    if not schema:
        logger.warning(f"Schema not found for model type: {model_type}")
        raise ResourceNotFoundError(
            message=f"Unknown model type: {model_type}",
            detail={"model_type": model_type}
        )
    
    return schema

@router.post("/generate/{model_type}")
async def generate_model(model_type: str, params: Dict[str, Any] = Body(...)):
    """Generate a model with the given parameters"""
    logger.info(f"Generating model of type: {model_type} with parameters: {params}")
    
    result = await clojure_service.generate_model(model_type, params)
    
    if result["status"] != 200:
        error_msg = result.get("error", "Unknown error")
        logger.error(f"Error generating model: {error_msg}")
        
        # Determine if client or server error based on status code
        if result["status"] < 500:
            raise ClientError(
                message=error_msg,
                status_code=result["status"],
                detail={"model_type": model_type, "params": params}
            )
        else:
            raise ExternalServiceError(
                message=error_msg,
                detail={"model_type": model_type, "params": params}
            )
    
    logger.info(f"Model generated successfully: {model_type}")
    return result["data"]

@router.get("/models/{filename}")
async def get_model(filename: str):
    """Retrieve a model file by filename (defaults to OBJ format)"""
    logger.info(f"Request for model file: {filename}")
    result = await clojure_service.get_model_file(filename)
    
    if result["status"] != 200:
        logger.warning(f"Model file not found: {filename}")
        raise ResourceNotFoundError(
            message=f"Model not found: {filename}",
            detail={"filename": filename}
        )
    
    # Use obj as the default extension
    base_name = filename_utils.base_filename(filename)
    file_name = f"{base_name}.obj"
    
    return Response(
        content=result["content"],
        media_type="application/octet-stream",
        headers={"Content-Disposition": f"attachment; filename={file_name}"}
    )

@router.get("/models/{filename}/{format}")
async def get_model_with_format(filename: str, format: str):
    """Retrieve a model file by filename in the specified format"""
    logger.info(f"Request for model: {filename} in format: {format}")
    
    # Validate format
    valid_formats = ["obj", "stl", "step", "g"]
    if format not in valid_formats:
        logger.warning(f"Invalid format requested: {format}")
        raise ClientError(
            message=f"Invalid format: {format}. Valid formats are: {', '.join(valid_formats)}",
            detail={"filename": filename, "format": format, "valid_formats": valid_formats}
        )
    
    # Map format to file extension
    format_to_ext = {
        "obj": "obj",
        "stl": "stl", 
        "step": "stp",
        "g": "g"
    }
    
    result = await clojure_service.get_model_file(filename, format)
    
    if result["status"] != 200:
        logger.warning(f"Model file not found: {filename} in format {format}")
        raise ResourceNotFoundError(
            message=f"Model not found: {filename} in format {format}",
            detail={"filename": filename, "format": format}
        )
    
    base_name = filename_utils.base_filename(filename)
    file_name = f"{base_name}.{format_to_ext[format]}"
    
    return Response(
        content=result["content"],
        media_type="application/octet-stream",
        headers={"Content-Disposition": f"attachment; filename={file_name}"}
    )

@router.get("/render/{filename}")
async def render_model(filename: str, view: str = "front", model_type: str = None):
    """Render a model image and return it"""
    logger.info(f"Request to render model: {filename}, view: {view}, type: {model_type}")
    
    try:
        # Call the clojure service to render the model
        result = await clojure_service.render_model(filename, model_type, view)
        
        if result["status"] != 200:
            error_msg = result.get("error", f"Failed to render model: {filename}")
            logger.error(f"Error rendering model: {error_msg}")
            raise ExternalServiceError(
                message=error_msg,
                detail={"filename": filename, "view": view, "model_type": model_type}
            )
        
        # Return the image
        return Response(
            content=result["content"],
            media_type="image/png",
            headers={"Content-Disposition": f"attachment; filename={result['filename']}"}
        )
    except Exception as e:
        if not isinstance(e, ExternalServiceError):
            logger.exception(f"Unexpected error rendering model {filename}")
            raise ServerError(
                message=f"Error rendering model: {str(e)}",
                detail={"filename": filename, "view": view, "model_type": model_type}
            )
        raise