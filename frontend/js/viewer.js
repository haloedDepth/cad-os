import * as THREE from 'three';
import { OrbitControls } from 'three/addons/controls/OrbitControls.js';
import { OBJLoader } from 'three/addons/loaders/OBJLoader.js';
import * as filenameUtils from './filenameUtils.js';
import * as logger from './logger.js';
import { handleError, ERROR_CATEGORY } from './errorHandler.js';

// Three.js variables
let renderer, scene, camera, controls, currentModel;
let viewerElement;

export function initViewer() {
  try {
    logger.info("Initializing 3D viewer");
    
    viewerElement = document.getElementById('viewer');
    
    // Create renderer
    renderer = new THREE.WebGLRenderer({ antialias: true });
    renderer.setSize(
      viewerElement.clientWidth,
      viewerElement.clientHeight
    );
    viewerElement.appendChild(renderer.domElement);
    
    // Create scene
    scene = new THREE.Scene();
    scene.background = new THREE.Color(0xf0f0f0);
    
    // Create camera
    camera = new THREE.PerspectiveCamera(
      45,
      viewerElement.clientWidth / viewerElement.clientHeight,
      1,
      10000
    );
    camera.position.set(0, 20, 100);
    
    // Set up OrbitControls
    controls = new OrbitControls(camera, renderer.domElement);
    controls.update();
    
    // Add lights
    const ambientLight = new THREE.AmbientLight(0xffffff, 0.7);
    scene.add(ambientLight);
    
    const directionalLight = new THREE.DirectionalLight(0xffffff, 0.5);
    directionalLight.position.set(1, 1, 1);
    scene.add(directionalLight);
    
    // Add grid helper
    const gridHelper = new THREE.GridHelper(100, 10);
    scene.add(gridHelper);
    
    // Animation loop
    function animate() {
      requestAnimationFrame(animate);
      controls.update();
      renderer.render(scene, camera);
    }
    animate();
    
    // Handle window resize
    window.addEventListener('resize', handleResize);
    
    logger.info("3D viewer initialized successfully");
  } catch (error) {
    handleError(error, ERROR_CATEGORY.RENDERING, {
      component: "initViewer",
      action: "initializing viewer"
    });
  }
}

export function handleResize() {
  try {
    if (camera && renderer && viewerElement) {
      camera.aspect = viewerElement.clientWidth / viewerElement.clientHeight;
      camera.updateProjectionMatrix();
      renderer.setSize(
        viewerElement.clientWidth,
        viewerElement.clientHeight
      );
      logger.debug("Viewer resized");
    }
  } catch (error) {
    handleError(error, ERROR_CATEGORY.RENDERING, {
      component: "handleResize",
      action: "resizing viewer"
    });
  }
}

export function loadModel(fileName, onSuccess, onError) {
  try {
    logger.info(`Loading model: ${fileName}`);
    
    // Remove current model if exists
    if (currentModel) {
      scene.remove(currentModel);
      logger.debug("Removed previous model from scene");
    }
    
    // Ensure we're using the base filename and then add obj extension
    const baseName = filenameUtils.baseFilename(fileName);
    const objFilePath = `/api/models/${baseName}/obj`;
    
    logger.debug(`Loading OBJ from: ${objFilePath}`);
    const loader = new OBJLoader();
    
    loader.load(
      objFilePath,
      (obj) => {
        try {
          logger.info(`OBJ loaded successfully: ${fileName}`);
          
          // Center the object
          const box = new THREE.Box3().setFromObject(obj);
          const center = new THREE.Vector3();
          box.getCenter(center);
          
          obj.position.sub(center);
          
          // Apply default material if needed
          obj.traverse((child) => {
            if (child.isMesh) {
              logger.debug(`Processing mesh: ${child.name || 'unnamed'}`);
              if (!child.material) {
                logger.debug("Applying default material to mesh");
                child.material = new THREE.MeshStandardMaterial({
                  color: 0x3498db,
                  metalness: 0.2,
                  roughness: 0.8
                });
              }
            }
          });
          
          currentModel = obj;
          scene.add(obj);
          
          // Adjust camera to fit the object
          const boxSize = box.getSize(new THREE.Vector3());
          const maxDim = Math.max(boxSize.x, boxSize.y, boxSize.z);
          const distance = maxDim * 2;
          camera.position.set(distance, distance, distance);
          camera.lookAt(0, 0, 0);
          controls.update();
          
          logger.info("Model added to scene and camera adjusted");
          
          if (onSuccess) onSuccess();
        } catch (error) {
          handleError(error, ERROR_CATEGORY.RENDERING, {
            component: "loadModel",
            action: "processing loaded OBJ",
            fileName
          });
          if (onError) onError(error);
        }
      },
      (xhr) => {
        logger.debug(`Loading progress: ${(xhr.loaded / xhr.total) * 100}%`);
      },
      (error) => {
        handleError(error, ERROR_CATEGORY.RENDERING, {
          component: "loadModel",
          action: "loading OBJ file",
          fileName,
          objFilePath
        });
        if (onError) onError(error);
      }
    );
  } catch (error) {
    handleError(error, ERROR_CATEGORY.RENDERING, {
      component: "loadModel",
      action: "setting up model loading",
      fileName
    });
    if (onError) onError(error);
  }
}