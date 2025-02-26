# CAD-OS: Extensible Parametric Modeling System

CAD-OS is an extensible parametric modeling system that integrates with BRL-CAD for generating 3D solid models. It features a dynamic model registry that allows adding new model types without modifying the core infrastructure.

## Overview

This system provides a web-based interface for creating parametric 3D models. Key features include:

- **Dynamic Model Registry**: Add new model types without modifying API routes, frontend, or gateway
- **Automatic UI Generation**: The interface dynamically adapts to each model's parameters
- **BRL-CAD Integration**: Leverages BRL-CAD's powerful solid modeling capabilities
- **3D Visualization**: Real-time 3D rendering of models in the browser
- **Three-Tier Architecture**: Separation of frontend, API gateway, and modeling service

## System Architecture

CAD-OS uses a three-tier architecture:

1. **Frontend**: HTML/JavaScript interface with dynamic form generation and 3D viewer
2. **API Gateway (Python)**: FastAPI service that handles HTTP requests and forwards to the Clojure service
3. **Modeling Service (Clojure)**: Core modeling logic that interfaces with BRL-CAD

### Model Registry

The core innovation is the model registry system:
- Models register themselves with the central registry
- Each model provides its schema (parameters) and creation function
- The API dynamically routes requests to the appropriate model handler
- The frontend auto-generates UI based on model schemas

## Project Structure

```
cad_os/
├── api/                  # FastAPI gateway
│   ├── main.py           # Python API gateway
│   └── requirements.txt  # Python dependencies
├── clojure_service/      # Clojure BRL-CAD integration
│   ├── deps.edn          # Clojure dependencies
│   ├── src/
│   │   └── cad_os/
│   │       ├── api.clj               # API routes and handlers
│   │       ├── commands.clj          # BRL-CAD command wrappers
│   │       ├── core.clj              # Core model creation logic
│   │       ├── obj.clj               # OBJ file conversion
│   │       ├── models/
│   │       │   ├── registry.clj      # Central model registry
│   │       │   ├── core.clj          # Shared model utilities
│   │       │   ├── washer.clj        # Washer model implementation
│   │       │   └── cylinder.clj      # Cylinder model implementation
├── frontend/             # Frontend assets
│   └── index.html        # Combined viewer and parameter panel
```

## Getting Started

### Prerequisites

- Clojure
- Python 3.8+
- BRL-CAD installed at "/usr/brlcad/rel-7.40.3/bin/mged"
- Node.js (optional, for development)

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

## Extending with New Models

CAD-OS is designed to be easily extended with new model types without modifying existing code. Here's how to add a new model:

### 1. Create a new model namespace

Create a new file in `clojure_service/src/cad_os/models/` (e.g., `box.clj`):

```clojure
(ns cad-os.models.box
  (:require [cad-os.models.core :as model-core]
            [cad-os.commands :as commands]
            [cad-os.models.registry :as registry]))

;; 1. Define the schema (parameters)
(defn schema
  "Return a schema description for box parameters"
  []
  {:name "Box"
   :description "A rectangular box with width, height, and depth"
   :parameters
   [{:name "width"
     :type "number"
     :description "Width of the box"
     :default 10.0
     :min 0.1}
    {:name "height"
     :type "number"
     :description "Height of the box"
     :default 10.0
     :min 0.1}
    {:name "depth"
     :type "number"
     :description "Depth of the box"
     :default 10.0
     :min 0.1}]})

;; 2. Parse and validate parameters
(defn parse-params
  "Parse and validate box parameters"
  [params]
  (model-core/parse-numeric-params 
   params 
   [{:name "width" :min 0.1}
    {:name "height" :min 0.1}
    {:name "depth" :min 0.1}]))

;; 3. Generate BRL-CAD commands for the model
(defn generate-commands
  "Generate commands to create a box model"
  [width height depth]
  [(commands/insert-right-circular-cylinder "box" 0 0 0 width 0 0 height)])

;; 4. Generate a file name based on parameters
(defn get-file-name
  "Generate a file name for a box based on its parameters"
  [params]
  (let [parsed (parse-params params)]
    (if (:valid parsed)
      (let [{:keys [width height depth]} (:params parsed)]
        (str "box_" width "_" height "_" depth))
      nil)))

;; 5. Create the model (main entry point)
(defn create
  "Create a box model from request parameters"
  [params]
  (try
    (let [parsed (parse-params params)]
      (if (:valid parsed)
        (let [{:keys [width height depth]} (:params parsed)
              file-name (get-file-name params)
              commands (generate-commands width height depth)]
          (model-core/create-model file-name "box" commands))
        {:status "error", :message (or (:message parsed) "Invalid parameters")}))
    (catch Exception e
      {:status "error", :message (str "Error creating box model: " (.getMessage e))})))

;; 6. Register the model with the registry
(registry/register-model
 "box"
 {:schema-fn schema
  :create-fn create})
```

### 2. Update the API to load your model

In `clojure_service/src/cad_os/api.clj`, add your model namespace to the requires:

```clojure
(ns cad-os.api
  (:require [compojure.core :refer [defroutes GET POST]]
            ;; ... other requires
            [cad-os.models.registry :as registry]
            ;; Add your model here
            [cad-os.models.box :as box-model]
            ;; ... other models
            )
  (:gen-class))
```

That's it! When you restart the Clojure service, your new model will be automatically available in the UI.

## API Endpoints

The system provides the following API endpoints:

- `GET /models/types` - Get list of available model types
- `GET /models/schema/:type` - Get schema for a specific model type
- `POST /generate/:type` - Generate a model of the specified type
- `GET /models/:filename` - Download a generated model file

## Troubleshooting

- If a model doesn't appear in the UI, check the Clojure service logs to ensure it was registered
- If parameter validation fails, the API will return a 400 error with details
- For detailed debugging, check both the Python gateway logs and Clojure service logs

## Future Enhancements

- Additional parametric models
- User authentication and model sharing
- Assembly modeling with multiple parts
- Direct editing of model parameters in the 3D viewer
- Export to additional CAD formats