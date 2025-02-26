from fastapi import FastAPI, HTTPException, Response, Body
from fastapi.middleware.cors import CORSMiddleware
from fastapi.staticfiles import StaticFiles
from typing import Dict, Any
import httpx
import os

app = FastAPI(title="CAD-OS API Gateway")

# Enable CORS
app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],  # For development, in production limit this
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

# Clojure service URL
CLOJURE_SERVICE_URL = "http://localhost:3000"

@app.get("/api")
async def read_root():
    return {"message": "CAD-OS API Gateway is running"}

@app.get("/api/models/types")
async def get_model_types():
    """Get list of available model types"""
    try:
        print("Requesting model types from Clojure service")
        async with httpx.AsyncClient() as client:
            response = await client.get(f"{CLOJURE_SERVICE_URL}/models/types")
            
            if response.status_code != 200:
                print(f"Error from Clojure service: {response.status_code} - {response.text}")
                # Return fallback types instead of raising an error
                return {"model_types": ["washer", "cylinder"]}
            
            try:
                data = response.json()
                print(f"Received model types from Clojure: {data}")
                
                # If data is already in the right format, return it
                if "model_types" in data:
                    return data
                
                # Otherwise wrap it in the expected format
                return {"model_types": data.get("model_types", ["washer", "cylinder"])}
            except Exception as e:
                print(f"Error parsing response as JSON: {e}")
                # Return fallback types
                return {"model_types": ["washer", "cylinder"]}
    except httpx.RequestError as e:
        print(f"Request error: {e}")
        # Return fallback types instead of raising an error
        return {"model_types": ["washer", "cylinder"]}

@app.get("/api/models/schema/{model_type}")
async def get_model_schema(model_type: str):
    """Get schema for a specific model type"""
    try:
        print(f"Requesting schema for model type: {model_type}")
        async with httpx.AsyncClient() as client:
            response = await client.get(f"{CLOJURE_SERVICE_URL}/models/schema/{model_type}")
            
            if response.status_code != 200:
                print(f"Error from Clojure service: {response.status_code} - {response.text}")
                raise HTTPException(
                    status_code=404, 
                    detail=f"Unknown model type: {model_type}"
                )
            
            return response.json()
    except httpx.RequestError as e:
        print(f"Request error: {e}")
        raise HTTPException(status_code=503, detail=f"Error communicating with CAD service: {str(e)}")

@app.post("/api/generate/{model_type}")
async def generate_model(model_type: str, params: Dict[str, Any] = Body(...)):
    """Generate a model with the given parameters"""
    try:
        print(f"Received parameters for {model_type}: {params}")
        
        # Convert parameters to the format expected by the Clojure service
        # Replace underscores with hyphens in parameter names for Clojure convention
        converted_params = {}
        for key, value in params.items():
            # Convert keys with underscores to hyphenated format for Clojure
            new_key = key.replace("_", "-")
            converted_params[new_key] = value
            
        print(f"Converted parameters: {converted_params}")
        
        async with httpx.AsyncClient() as client:
            response = await client.post(
                f"{CLOJURE_SERVICE_URL}/generate/{model_type}",
                json=converted_params
            )
            
            if response.status_code != 200:
                try:
                    error_text = response.text
                    if callable(getattr(response, "text", None)):
                        error_text = await response.text()
                except Exception as e:
                    error_text = f"Error communicating with Clojure service: {str(e)}"
                
                print(f"Error response from Clojure service: {error_text}")
                
                # Instead of raising an HTTP 500, pass through the original error
                # This allows the frontend to see the actual error message
                return Response(
                    content=error_text, 
                    status_code=400,
                    media_type="application/json"
                )
            
            result = response.json()
            print(f"Success response from Clojure service: {result}")
            
            # Make sure we're passing back all necessary information
            if "obj_result" in result and "file" in result["obj_result"]:
                result["obj_path"] = result["obj_result"]["file"]
            
            return result
    except httpx.RequestError as e:
        print(f"Request error communicating with CAD service: {str(e)}")
        raise HTTPException(status_code=503, detail=f"Error communicating with CAD service: {str(e)}")

@app.get("/api/models/{filename}")
async def get_model(filename: str):
    """Retrieve a model file by filename"""
    try:
        # Ensure we're requesting with .obj extension
        request_filename = filename if filename.endswith(".obj") else f"{filename}.obj"
        
        print(f"Requesting model file: {request_filename}")
        async with httpx.AsyncClient() as client:
            response = await client.get(
                f"{CLOJURE_SERVICE_URL}/models/{request_filename}",
                follow_redirects=True
            )
            
            if response.status_code != 200:
                print(f"Error from Clojure service: {response.status_code} - {response.text}")
                raise HTTPException(
                    status_code=response.status_code, 
                    detail=f"Model not found: {request_filename}"
                )
                
            return Response(
                content=response.content,
                media_type="application/octet-stream",
                headers={"Content-Disposition": f"attachment; filename={request_filename}"}
            )
    except httpx.RequestError as e:
        print(f"Request error: {e}")
        raise HTTPException(status_code=503, detail=f"Error communicating with CAD service: {str(e)}")

@app.get("/api/models/{filename}/{format}")
async def get_model_with_format(filename: str, format: str):
    """Retrieve a model file by filename in the specified format"""
    try:
        # Validate format
        valid_formats = ["obj", "stl", "step", "g"]
        if format not in valid_formats:
            raise HTTPException(
                status_code=400, 
                detail=f"Invalid format: {format}. Valid formats are: {', '.join(valid_formats)}"
            )
            
        # Use the filename as-is, without modification
        # This ensures we pass the exact filename format to the Clojure service
        base_filename = filename
        
        # Map format to file extension
        format_to_ext = {
            "obj": "obj",
            "stl": "stl",
            "step": "stp",
            "g": "g"
        }
        
        print(f"Requesting model file: {base_filename} in format: {format}")
        async with httpx.AsyncClient() as client:
            response = await client.get(
                f"{CLOJURE_SERVICE_URL}/models/{base_filename}/{format}",
                follow_redirects=True
            )
            
            if response.status_code != 200:
                print(f"Error from Clojure service: {response.status_code} - {response.text}")
                raise HTTPException(
                    status_code=response.status_code, 
                    detail=f"Model not found: {base_filename} in format {format}"
                )
                
            return Response(
                content=response.content,
                media_type="application/octet-stream",
                headers={"Content-Disposition": f"attachment; filename={base_filename}.{format_to_ext[format]}"}
            )
    except httpx.RequestError as e:
        print(f"Request error: {e}")
        raise HTTPException(status_code=503, detail=f"Error communicating with CAD service: {str(e)}")

# Legacy endpoint for backward compatibility
@app.post("/api/generate/washer")
async def generate_washer(params: Dict[str, Any] = Body(...)):
    """Legacy endpoint for washer generation"""
    return await generate_model("washer", params)

# Mount static files (frontend)
app.mount("/", StaticFiles(directory="../frontend", html=True), name="frontend")

if __name__ == "__main__":
    import uvicorn
    uvicorn.run(app, host="0.0.0.0", port=8000)