import os

# Clojure service URL
CLOJURE_SERVICE_URL = os.getenv("CLOJURE_SERVICE_URL", "http://localhost:3000")

# Default fallback model types if the Clojure service is unavailable
DEFAULT_MODEL_TYPES = ["washer", "cylinder"]