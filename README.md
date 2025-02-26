# CAD-OS: Extensible Parametric Modeling System

CAD-OS is an extensible parametric modeling system that integrates with BRL-CAD for generating 3D solid models. It features a dynamic model registry that allows adding new model types without modifying the core infrastructure, client-side validation, and real-time 3D visualization.

## Overview

CAD-OS provides a web-based interface for creating parametric 3D models with these key features:

- **Dynamic Model Registry**: Add new model types without modifying existing code
- **Client-side Validation**: Real-time parameter validation in the browser
- **Automatic UI Generation**: Interface dynamically adapts to model parameters
- **BRL-CAD Integration**: Leverages BRL-CAD's powerful solid modeling capabilities
- **3D Visualization**: Real-time model rendering and manipulation
- **Multi-format Export**: Download models in OBJ, STL, STEP, and native BRL-CAD formats

## System Architecture

CAD-OS uses a three-tier architecture:

1. **Frontend**: HTML/JavaScript UI with dynamic form generation and Three.js 3D viewer
2. **API Gateway (Python/FastAPI)**: Handles HTTP requests and proxies to the modeling service
3. **Modeling Service (Clojure)**: Core modeling logic that interfaces with BRL-CAD

### Key Components

- **Model Registry**: Central registry where models register their schemas and creation functions
- **Schema System**: Declarative parameter definitions with frontend validation
- **Command System**: Abstraction layer for BRL-CAD operations
- **Format Conversion**: Utilities to convert between different 3D file formats

## Installation

### Prerequisites

- **BRL-CAD**: Version 7.40.3 or later (installed at `/usr/brlcad/rel-7.40.3/bin/`)
- **Clojure**: Version 1.11.1 or later
- **Python**: Version 3.8 or later
- **Node.js**: For frontend development (optional)

### Setup

1. **Clone the repository**:
   ```bash
   git clone https://github.com/yourusername/cad-os.git
   cd cad-os
   ```

2. **Start the Clojure modeling service**:
   ```bash
   cd clojure_service
   clojure -M:run
   ```
   This will start the service on port 3000.

3. **Start the Python API gateway**:
   ```bash
   cd api
   pip install -r requirements.txt
   uvicorn main:app --reload --host 0.0.0.0 --port 8000
   ```
   This will start the gateway on port 8000.

4. **Access the application**:
   Open http://localhost:8000 in your web browser

## Using CAD-OS

### Creating Models

1. Select a model type from the dropdown menu (e.g., "Washer" or "Cylinder")
2. Fill in the parameter values in the form
   - The form will validate your inputs in real-time
   - Error messages will appear if parameters don't meet validation rules
3. Click "Generate Model" to create the 3D model
4. The model will be displayed in the 3D viewer
   - Use the mouse to rotate, pan, and zoom the model
   - The grid helps with size reference

### Downloading Models

After generating a model, use the download buttons in the top-right of the viewer to get the model in your preferred format:

- **OBJ**: Standard 3D format for most modeling software
- **STL**: For 3D printing and CAM applications
- **STEP**: For parametric CAD software
- **G**: Native BRL-CAD format for further editing

## Extending CAD-OS with New Models

One of CAD-OS's core features is its extensibility. You can add new model types without modifying any existing code. Here's how:

### 1. Create a Model Definition File

Create a new Clojure file in `clojure_service/src/cad_os/models/` (e.g., `cone.clj`):

```clojure
(ns cad-os.models.cone
  (:require [cad-os.commands :as commands]
            [cad-os.models.registry :as registry]))

;; Define schema - this is the single source of truth for the model
(def cone-schema
  {:name "Cone"
   :description "A cone with specified base radius, height, and top radius"
   :parameters
   [{:name "base-radius"
     :type "number"
     :description "Radius of the cone base"
     :default 5.0}
    {:name "height"
     :type "number"
     :description "Height of the cone"
     :default 10.0}
    {:name "top-radius"
     :type "number"
     :description "Radius of the cone top (0 for pointed cone)"
     :default 0.0}]
   
   ;; Validation rules - these run on the frontend
   :validation-rules
   [{:expr "base-radius > 0.1"
     :message "Base radius must be greater than 0.1"}
    {:expr "height > 0.1"
     :message "Height must be greater than 0.1"}
    {:expr "top-radius >= 0"
     :message "Top radius cannot be negative"}
    {:expr "base-radius > top-radius"
     :message "Base radius must be greater than top radius"}]})

;; Command generator function
(defn generate-cone-commands
  "Generate commands to create a cone model"
  [params]
  (let [base-radius (get params :base-radius)
        height (get params :height)
        top-radius (get params :top-radius)]
    
    [(commands/insert-truncated-right-circular-cone 
      "cone" 0 0 0 0 0 height base-radius top-radius)]))

;; Register the model - this makes it available in the UI
(registry/register-model
 "cone"
 cone-schema
 generate-cone-commands)
```

### 2. Update `api.clj` to Load Your Model

In `clojure_service/src/cad_os/api.clj`, add a require statement for your new model:

```clojure
(defn -main [& args]
  ;; Clear the registry before starting to avoid duplicates
  (println "Clearing model registry...")
  (reset! registry/model-registry {})

  ;; These requires will trigger the registration
  (println "Loading model namespaces...")
  (require 'cad-os.models.washer)
  (require 'cad-os.models.cylinder)
  (require 'cad-os.models.cone)  ;; Add your model here

  ;; Start the server
  (println "Starting CAD-OS API server on port 3000...")
  (println "Available model types:" (registry/get-model-types))
  (run-jetty app {:port 3000 :join? false})
  (println "Server started!"))
```

### 3. Restart the Clojure Service

Restart the Clojure service to register your new model:

```bash
cd clojure_service
clojure -M:run
```

Your new model will now be available in the UI dropdown!

### Key Components of a Model Definition

1. **Schema** (`cone-schema`):
   - **name**: Display name in the UI
   - **description**: Help text shown in the UI
   - **parameters**: List of parameters with types, defaults, etc.
   - **validation-rules**: Client-side validation expressions

2. **Command Generator** (`generate-cone-commands`):
   - Takes processed parameters
   - Returns a list of BRL-CAD commands

3. **Registration** (`registry/register-model`):
   - Registers the model with the central registry
   - Makes it available via the API and UI

### Available Commands

CAD-OS provides wrappers for BRL-CAD primitives in `commands.clj`. Common ones include:

- `insert-right-circular-cylinder`: Create a cylinder
- `insert-truncated-right-circular-cone`: Create a cone or truncated cone
- `insert-sphere`: Create a sphere
- `insert-torus`: Create a torus
- `union`: Combine shapes
- `subtraction`: Remove one shape from another
- `intersection`: Create intersection of shapes

See `clojure_service/src/cad_os/commands.clj` for the full list of available commands.

## API Reference

CAD-OS exposes these API endpoints:

- `GET /api/models/types`: Get list of available model types
- `GET /api/models/schemas`: Get all model schemas at once
- `GET /api/models/schema/:type`: Get schema for a specific model type
- `POST /api/generate/:type`: Generate a model with parameters
- `GET /api/models/:filename`: Download model in OBJ format
- `GET /api/models/:filename/:format`: Download model in specified format (obj, stl, step, g)

## Validation System

CAD-OS uses a frontend-only validation approach:

1. **Declaration**: Validation rules are declared in the model schema
2. **Transport**: Rules are sent to the frontend on startup
3. **Execution**: Frontend validates in real-time as users type
4. **Feedback**: Users get immediate visual feedback on validation errors

Validation expressions use standard operators and can reference any parameter by name (e.g., `inner-diameter < outer-diameter`).

## Troubleshooting

### Common Issues

- **Missing BRL-CAD**: Ensure BRL-CAD is installed at `/usr/brlcad/rel-7.40.3/bin/` or update the path in `core.clj`
- **Model Not Appearing**: Check Clojure service logs to verify your model was registered
- **Server Error on Generation**: Check the Clojure logs for errors in your command generator function
- **Validation Failures**: Check the browser console for validation expression errors

### Debugging Tips

- Frontend validation is logged to the browser console
- The Clojure service logs model creation steps
- The API gateway provides access logs

## Future Enhancements

- More primitive model types (box, torus, etc.)
- Composite models from multiple primitives
- Boolean operations in the UI
- User accounts and model saving
- Direct parameter manipulation in the 3D viewer
- Material and texture support

## License

[Your license information]
