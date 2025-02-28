from fastapi import FastAPI
from fastapi.middleware.cors import CORSMiddleware
from fastapi.staticfiles import StaticFiles
from fastapi.responses import RedirectResponse

from routers import models
from utils.logger import get_logger
from utils.errors import setup_exception_handlers

# Configure logging
logger = get_logger("main")

app = FastAPI(title="CAD-OS API Gateway")

# Enable CORS
app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],  # For development, in production limit this
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

# Setup error handlers
setup_exception_handlers(app)

# Include routers
app.include_router(models.router)

@app.get("/")
async def root():
    logger.info("Redirecting root request to index.html")
    return RedirectResponse(url="/index.html") 

@app.get("/api")
async def read_root():
    logger.info("API health check called")
    return {"message": "CAD-OS API Gateway is running"}

# Mount static files (frontend)
app.mount("/", StaticFiles(directory="../frontend", html=True), name="frontend")

if __name__ == "__main__":
    import uvicorn
    logger.info("Starting CAD-OS API Gateway")
    uvicorn.run(app, host="0.0.0.0", port=8000)