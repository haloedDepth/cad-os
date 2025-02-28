from fastapi import HTTPException, Request, status
from fastapi.responses import JSONResponse
from pydantic import BaseModel
from typing import Dict, Any, Optional, List, Type
from enum import Enum
import traceback

from .logger import get_logger

logger = get_logger("errors")

# Error categories
class ErrorCategory(str, Enum):
    CLIENT_ERROR = "CLIENT_ERROR"  # Client made a mistake (4xx)
    SERVER_ERROR = "SERVER_ERROR"  # Server failed (5xx)
    VALIDATION_ERROR = "VALIDATION_ERROR"  # Invalid input data
    EXTERNAL_SERVICE_ERROR = "EXTERNAL_SERVICE_ERROR"  # External API failure
    RESOURCE_ERROR = "RESOURCE_ERROR"  # Resource not found/unavailable
    UNKNOWN_ERROR = "UNKNOWN_ERROR"  # Catch-all

# Standard error response model
class ErrorResponse(BaseModel):
    status: str = "error"
    category: ErrorCategory
    message: str
    detail: Optional[Dict[str, Any]] = None
    code: Optional[str] = None

# Base exception class for the application
class AppError(Exception):
    def __init__(
        self, 
        message: str, 
        category: ErrorCategory = ErrorCategory.UNKNOWN_ERROR,
        status_code: int = status.HTTP_500_INTERNAL_SERVER_ERROR,
        detail: Optional[Dict[str, Any]] = None,
        code: Optional[str] = None
    ):
        self.message = message
        self.category = category
        self.status_code = status_code
        self.detail = detail
        self.code = code
        super().__init__(self.message)
    
    def to_response(self) -> JSONResponse:
        return JSONResponse(
            status_code=self.status_code,
            content=ErrorResponse(
                status="error",
                category=self.category,
                message=self.message,
                detail=self.detail,
                code=self.code
            ).dict()
        )

# Common error types
class ClientError(AppError):
    def __init__(
        self, 
        message: str, 
        status_code: int = status.HTTP_400_BAD_REQUEST,
        detail: Optional[Dict[str, Any]] = None,
        code: Optional[str] = None
    ):
        super().__init__(
            message=message,
            category=ErrorCategory.CLIENT_ERROR,
            status_code=status_code,
            detail=detail,
            code=code
        )

class ValidationError(AppError):
    def __init__(
        self, 
        message: str, 
        detail: Optional[Dict[str, Any]] = None,
        code: Optional[str] = None
    ):
        super().__init__(
            message=message,
            category=ErrorCategory.VALIDATION_ERROR,
            status_code=status.HTTP_422_UNPROCESSABLE_ENTITY,
            detail=detail,
            code=code
        )

class ResourceNotFoundError(AppError):
    def __init__(
        self, 
        message: str, 
        detail: Optional[Dict[str, Any]] = None,
        code: Optional[str] = None
    ):
        super().__init__(
            message=message,
            category=ErrorCategory.RESOURCE_ERROR,
            status_code=status.HTTP_404_NOT_FOUND,
            detail=detail,
            code=code
        )

class ExternalServiceError(AppError):
    def __init__(
        self, 
        message: str, 
        detail: Optional[Dict[str, Any]] = None,
        code: Optional[str] = None
    ):
        super().__init__(
            message=message,
            category=ErrorCategory.EXTERNAL_SERVICE_ERROR,
            status_code=status.HTTP_502_BAD_GATEWAY,
            detail=detail,
            code=code
        )

class ServerError(AppError):
    def __init__(
        self, 
        message: str, 
        detail: Optional[Dict[str, Any]] = None,
        code: Optional[str] = None
    ):
        super().__init__(
            message=message,
            category=ErrorCategory.SERVER_ERROR,
            status_code=status.HTTP_500_INTERNAL_SERVER_ERROR,
            detail=detail,
            code=code
        )

# Global exception handler for FastAPI
async def global_exception_handler(request: Request, exc: Exception):
    """Global exception handler for all unhandled exceptions"""
    
    # If it's our AppError, use its built-in to_response method
    if isinstance(exc, AppError):
        # Log the error
        logger.error(f"Application error: {exc.message}", 
                    extra={"category": exc.category, "detail": exc.detail})
        return exc.to_response()
    
    # If it's FastAPI's HTTPException, convert to our format
    if isinstance(exc, HTTPException):
        error = ClientError(
            message=str(exc.detail),
            status_code=exc.status_code,
            detail={"headers": dict(exc.headers)} if exc.headers else None
        )
        logger.error(f"HTTP exception: {exc.detail}", 
                    extra={"status_code": exc.status_code})
        return error.to_response()
    
    # Otherwise, it's an unhandled exception
    error_detail = {
        "exception_type": exc.__class__.__name__,
        "traceback": traceback.format_exc()
    }
    
    logger.exception(f"Unhandled exception: {str(exc)}")
    
    return ServerError(
        message="An unexpected error occurred",
        detail=error_detail
    ).to_response()

# Function to register the exception handlers with FastAPI
def setup_exception_handlers(app):
    app.exception_handler(Exception)(global_exception_handler)
    app.exception_handler(HTTPException)(global_exception_handler)
    app.exception_handler(AppError)(global_exception_handler)

# Make sure to export all error classes
__all__ = [
    'ErrorCategory',
    'ErrorResponse',
    'AppError',
    'ClientError',
    'ValidationError',
    'ResourceNotFoundError',
    'ExternalServiceError',
    'ServerError',
    'global_exception_handler',
    'setup_exception_handlers'
]