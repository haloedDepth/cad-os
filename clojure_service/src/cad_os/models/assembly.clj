(ns cad-os.models.assembly
  (:require [cad-os.commands :as commands]
            [cad-os.models.registry :as registry]
            [clojure.string :as str]))

;; This is a template for creating assembly models programmatically
;; NOT intended to be exposed through the UI

(defn create-assembly
  "Create an assembly from multiple components
   
   components should be a sequence of maps with the following keys:
   - :type - the model type
   - :params - the parameters for the model
   - :name - the name to use for this component in the assembly (optional)
   
   Example:
   (create-assembly \"bolt-assembly\"
                   [{:type \"washer\"
                     :name \"head-washer\"
                     :params {:outer-diameter 10
                              :inner-diameter 5
                              :thickness 1
                              :position-x 0
                              :position-y 0
                              :position-z 0}}
                    {:type \"cylinder\"
                     :name \"shaft\"
                     :params {:radius 2.5
                              :height 20
                              :position-x 0
                              :position-y 0
                              :position-z 1}}])"
  [assembly-name components]
  (let [;; Generate commands for each component
        all-component-commands
        (mapcat (fn [component-spec]
                  (let [type (:type component-spec)
                        params (:params component-spec)
                        component-name (or (:name component-spec)
                                           (str type "-" (str/join "-" (vals params))))

                        ;; Get the command generator for this component type
                        registry-entry (get @registry/model-registry type)
                        command-generator (:command-generator registry-entry)]

                    ;; Replace the default object name with our assembly-specific name
                    ;; This is a simplified approach - would need enhancement for complex models
                    (map (fn [cmd]
                           (if (string? cmd)
                             (str/replace cmd
                                          (re-pattern (str "^in " type))
                                          (str "in " component-name))
                             cmd))
                         (command-generator params))))
                components)

        ;; Create a union command that includes all components
        component-names (map (fn [c] (or (:name c) (:type c))) components)
        union-command (reduce (fn [cmd name]
                                (str cmd " u " name))
                              (str "r " assembly-name)
                              component-names)]

    ;; Concatenate all commands
    (concat all-component-commands [union-command])))


