from fastapi import FastAPI
from fastapi.middleware.cors import CORSMiddleware
from fastapi.staticfiles import StaticFiles
import logging

from routers import models

# Configure logging
logging.basicConfig(
    level=logging.INFO,
    format="%(asctime)s - %(name)s - %(levelname)s - %(message)s",
)

app = FastAPI(title="CAD-OS API Gateway")

# Enable CORS
app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],  # For development, in production limit this
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

# Include routers
app.include_router(models.router)

@app.get("/api")
async def read_root():
    return {"message": "CAD-OS API Gateway is running"}

# Mount static files (frontend)
app.mount("/", StaticFiles(directory="../frontend", html=True), name="frontend")

if __name__ == "__main__":
    import uvicorn
    uvicorn.run(app, host="0.0.0.0", port=8000)