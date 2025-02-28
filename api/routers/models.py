from fastapi import APIRouter, HTTPException, Response, Body
from typing import Dict, Any
import logging
from services import clojure_service

router = APIRouter(
    prefix="/api",
    tags=["models"],
)

logger = logging.getLogger(__name__)

@router.get("/models/types")
async def get_model_types():
    """Get list of available model types"""
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
        logger.error(f"Error getting all schemas: {str(e)}")
        raise HTTPException(status_code=500, detail=f"Error fetching schemas: {str(e)}")

@router.get("/models/schema/{model_type}")
async def get_model_schema(model_type: str):
    """Get schema for a specific model type"""
    schema = await clojure_service.get_model_schema(model_type)
    
    if not schema:
        raise HTTPException(
            status_code=404, 
            detail=f"Unknown model type: {model_type}"
        )
    
    return schema

@router.post("/generate/{model_type}")
async def generate_model(model_type: str, params: Dict[str, Any] = Body(...)):
    """Generate a model with the given parameters"""
    result = await clojure_service.generate_model(model_type, params)
    
    if result["status"] != 200:
        # Instead of raising an HTTP 500, pass through the original error
        # This allows the frontend to see the actual error message
        return Response(
            content=result.get("error", "Unknown error"),
            status_code=400 if result["status"] < 500 else result["status"],
            media_type="application/json"
        )
    
    return result["data"]

@router.get("/models/{filename}")
async def get_model(filename: str):
    """Retrieve a model file by filename"""
    result = await clojure_service.get_model_file(filename)
    
    if result["status"] != 200:
        raise HTTPException(
            status_code=result["status"], 
            detail=f"Model not found: {filename}"
        )
    
    return Response(
        content=result["content"],
        media_type="application/octet-stream",
        headers={"Content-Disposition": f"attachment; filename={filename}.obj"}
    )

@router.get("/models/{filename}/{format}")
async def get_model_with_format(filename: str, format: str):
    """Retrieve a model file by filename in the specified format"""
    # Validate format
    valid_formats = ["obj", "stl", "step", "g"]
    if format not in valid_formats:
        raise HTTPException(
            status_code=400, 
            detail=f"Invalid format: {format}. Valid formats are: {', '.join(valid_formats)}"
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
        raise HTTPException(
            status_code=result["status"], 
            detail=f"Model not found: {filename} in format {format}"
        )
    
    return Response(
        content=result["content"],
        media_type="application/octet-stream",
        headers={"Content-Disposition": f"attachment; filename={filename}.{format_to_ext[format]}"}
    )

@router.get("/render/{filename}")
async def render_model(filename: str, view: str = "front", model_type: str = None):
    """Render a model image and return it"""
    logger.info(f"Request to render model: {filename}, view: {view}, type: {model_type}")
    
    try:
        # Call the clojure service to render the model
        result = await clojure_service.render_model(filename, model_type, view)
        
        if result["status"] != 200:
            raise HTTPException(
                status_code=result["status"], 
                detail=result.get("error", f"Failed to render model: {filename}")
            )
        
        # Return the image
        return Response(
            content=result["content"],
            media_type="image/png",
            headers={"Content-Disposition": f"attachment; filename={result['filename']}"}
        )
    except Exception as e:
        logger.error(f"Error rendering model {filename}: {str(e)}")
        raise HTTPException(status_code=500, detail=f"Error rendering model: {str(e)}")
