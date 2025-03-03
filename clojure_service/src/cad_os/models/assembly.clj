(ns cad-os.models.assembly
  (:require [cad-os.commands :as commands]
            [cad-os.models.registry :as registry]
            [clojure.string :as str]
            [cad-os.utils.logger :as logger]))

;; Initialize logger
(def log (logger/get-logger))

;; This function extracts all component and subcomponent names from commands
(defn extract-component-names
  "Extract all component names from a set of commands"
  [cmd-list]
  (let [name-patterns [#"^in\s+([^\s]+)"     ; captures name from "in name ..."
                       #"^r\s+([^\s]+)"      ; captures name from "r name ..."
                       #"\sr\s+([^\s]+)"     ; captures name from "... r name ..."
                       #"\su\s+([^\s]+)"     ; captures name from "... u name ..."
                       #"\s\-\s+([^\s]+)"    ; captures name from "... - name ..."
                       #"\s\+\s+([^\s]+)"    ; captures name from "... + name ..."
                       #"^cp\s+([^\s]+)"]]   ; captures name from "cp name ..."
    (set (mapcat (fn [cmd]
                   (when (string? cmd)
                     (mapcat (fn [pattern]
                               (map second (re-seq pattern cmd)))
                             name-patterns)))
                 cmd-list))))

(defn rename-command
  "Rename component references in a command string"
  [cmd name-map]
  (if-not (string? cmd)
    cmd
    (reduce (fn [cmd-str [old-name new-name]]
              ;; Replace all occurrences, but only when they are complete tokens
              ;; (preceded and followed by whitespace, start/end of string or specific operators)
              (let [pattern (re-pattern (str "(?<=^|\\s|\\-)\\b" old-name "\\b(?=$|\\s)"))]
                (str/replace cmd-str pattern new-name)))
            cmd
            name-map)))

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
  (let [;; Process each component
        processed-components
        (map (fn [component-spec]
               (let [type (:type component-spec)
                     params (:params component-spec)
                     component-name (or (:name component-spec)
                                        (str type "-" (str/join "-" (vals params))))

                    ;; Get the command generator for this component type
                     registry-entry (get @registry/model-registry type)
                     command-generator (:command-generator registry-entry)

                    ;; Generate the original commands
                     original-commands (command-generator params)

                    ;; Extract all component names used in the commands
                     component-names (extract-component-names original-commands)

                    ;; Create a mapping for all component names to include the parent name
                     name-map (reduce (fn [m name]
                                        (assoc m
                                               name
                                               (str component-name "-" name)))
                                      {}
                                      component-names)

                    ;; Add the main component name to the map
                     name-map (assoc name-map type component-name)

                    ;; Create renamed commands
                     renamed-commands (map #(rename-command % name-map) original-commands)]

                 {:name component-name
                  :commands renamed-commands}))
             components)

        ;; Extract all component commands
        all-component-commands (mapcat :commands processed-components)

        ;; Get component names for the union
        component-names (map :name processed-components)

        ;; Create a union command for all top-level components
        union-command (reduce (fn [cmd name]
                                (str cmd " u " name))
                              (str "r " assembly-name)
                              component-names)]

    ;; Return concatenated commands
    (concat all-component-commands [union-command])))