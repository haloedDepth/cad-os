from fastapi import FastAPI, HTTPException, Response
from fastapi.middleware.cors import CORSMiddleware
from fastapi.staticfiles import StaticFiles
from pydantic import BaseModel
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

# Model definitions
class WasherParams(BaseModel):
    outer_diameter: str
    inner_diameter: str
    thickness: str

# Clojure service URL
CLOJURE_SERVICE_URL = "http://localhost:3000"

@app.get("/api")
async def read_root():
    return {"message": "CAD-OS API Gateway is running"}

@app.post("/api/generate/washer")
async def generate_washer(params: WasherParams):
    """Generate a washer model with the given parameters"""
    try:
        print(f"Received washer parameters: {params}")
        
        async with httpx.AsyncClient() as client:
            response = await client.post(
                f"{CLOJURE_SERVICE_URL}/generate/washer",
                json={
                    "outer-diameter": params.outer_diameter,
                    "inner-diameter": params.inner_diameter,
                    "thickness": params.thickness
                }
            )
            
            if response.status_code != 200:
                try:
                    error_text = response.text
                    if callable(getattr(response, "text", None)):
                        error_text = await response.text()
                except Exception as e:
                    error_text = f"Error communicating with Clojure service: {str(e)}"
                
                print(f"Error response from Clojure service: {error_text}")
                raise HTTPException(
                    status_code=500, 
                    detail=f"Error generating model: {error_text}"
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
        
        async with httpx.AsyncClient() as client:
            response = await client.get(
                f"{CLOJURE_SERVICE_URL}/models/{request_filename}",
                follow_redirects=True
            )
            
            if response.status_code != 200:
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
        raise HTTPException(status_code=503, detail=f"Error communicating with CAD service: {str(e)}")

# Mount static files (frontend)
app.mount("/", StaticFiles(directory="../frontend", html=True), name="frontend")

if __name__ == "__main__":
    import uvicorn
    uvicorn.run(app, host="0.0.0.0", port=8000)