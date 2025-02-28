import logging
import sys
import os
from datetime import datetime

# Constants for log levels
DEBUG = logging.DEBUG
INFO = logging.INFO
WARNING = logging.WARNING
ERROR = logging.ERROR
CRITICAL = logging.CRITICAL

# Create a custom logger class that adds context information
class ContextLogger:
    def __init__(self, name, level=logging.INFO):
        self.logger = logging.getLogger(name)
        self.logger.setLevel(level)
        self.setup_handlers()
        
    def setup_handlers(self):
        # Clear any existing handlers
        if self.logger.handlers:
            self.logger.handlers.clear()
            
        # Console handler
        console_handler = logging.StreamHandler(sys.stdout)
        console_handler.setLevel(logging.INFO)
        console_format = logging.Formatter(
            '%(asctime)s - %(name)s - %(levelname)s - %(message)s'
        )
        console_handler.setFormatter(console_format)
        self.logger.addHandler(console_handler)
        
        # File handler
        log_dir = os.environ.get("LOG_DIR", "logs")
        os.makedirs(log_dir, exist_ok=True)
        
        log_file = os.path.join(log_dir, f"app_{datetime.now().strftime('%Y%m%d')}.log")
        file_handler = logging.FileHandler(log_file)
        file_handler.setLevel(logging.DEBUG)
        file_format = logging.Formatter(
            '%(asctime)s - %(name)s - %(levelname)s - %(message)s'
        )
        file_handler.setFormatter(file_format)
        self.logger.addHandler(file_handler)
    
    def debug(self, message, *args, **kwargs):
        self.logger.debug(message, *args, **kwargs)
        
    def info(self, message, *args, **kwargs):
        self.logger.info(message, *args, **kwargs)
        
    def warning(self, message, *args, **kwargs):
        self.logger.warning(message, *args, **kwargs)
        
    def error(self, message, *args, **kwargs):
        self.logger.error(message, *args, **kwargs)
        
    def critical(self, message, *args, **kwargs):
        self.logger.critical(message, *args, **kwargs)
        
    def exception(self, message, *args, exc_info=True, **kwargs):
        self.logger.exception(message, *args, exc_info=exc_info, **kwargs)

# Helper function to get a logger instance for a module
def get_logger(name):
    return ContextLogger(name)