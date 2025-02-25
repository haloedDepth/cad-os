# CAD-OS MVP

A parametric modeling system that integrates with BRL-CAD.

## Project Structure

```
cad_os/
├── api/                  # FastAPI gateway
│   ├── main.py
│   └── requirements.txt
├── clojure_service/      # Clojure BRL-CAD integration
│   ├── deps.edn
│   ├── src/
│   │   └── cad_os/
│   │       ├── core.clj      # Core logic
│   │       ├── commands.clj  # BRL-CAD commands
│   │       ├── api.clj       # API wrapper
│   │       └── obj.clj       # Stub for OBJ conversion
├── frontend/             # Frontend with Alpine.js and Tailwind
│   └── index.html        # Combined viewer and parameter panel
```

## Getting Started

### Prerequisites

- Clojure
- Python 3.8+
- BRL-CAD installed at "/usr/brlcad/rel-7.40.3/bin/mged"

### Running the Services

1. Start the Clojure service:
   ```
   cd clojure_service
   clojure -M:run
   ```

2. Start the FastAPI gateway:
   ```
   cd api
   pip install -r requirements.txt
   uvicorn main:app --reload --host 0.0.0.0 --port 8000
   ```

3. Access the application at http://localhost:8000

## Features

- Parametric washer model generation
- 3D model viewer with Three.js
- Integration with BRL-CAD for solid modeling

## Development

The OBJ conversion functionality is currently a stub. To implement the actual conversion:

1. Edit the `clojure_service/src/cad_os/obj.clj` file to add your conversion logic
2. The application will use this to convert .g files to .obj files for display in the browser

## Future Enhancements

- Additional parametric models
- User authentication
- Model sharing
- More advanced visualization features