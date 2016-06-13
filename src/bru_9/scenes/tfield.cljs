(ns bru-9.scenes.tfield
  (:require [bru-9.debug :as debug]
            [bru-9.field.core :as f]
            [bru-9.field.linear :as fl]
            [bru-9.util :as u]
            [thi.ng.geom.vector :as v]
            [thi.ng.math.core :as m]
            [thi.ng.color.core :as c]))

;; Scene setup & main loop

(def config {:field-resolution 5
             :visualisation-step 0.4})

(defonce *state* (atom {}))

(defn generator [offset coords]
  (m/+ (v/randvec3) offset))

(defn setup [initial-context]
  (let [scene (:scene initial-context)
        fres (:field-resolution config)
        step (:visualisation-step config)
        offset (v/randvec3)
        field (fl/linear-field [fres fres fres] #(generator offset %))
        axis-range (range 0 (dec fres) step)]
    (reset! *state* {:scene scene
                     :field field})
    (doseq [x axis-range
            y axis-range
            z axis-range
            :let [coord [x y z]
                  coordv (v/vec3 x y z)]]
      (debug/line coordv
                  (m/+ coordv (f/value-at field coord))
                  (c/hsla (/ z (dec fres)) 1 0.5)))
    initial-context))

(defn reload [context]
  (let [scene (:scene context)]
    (u/clear-scene scene)
    (setup context)))

(defn animate [context]
  (let []
    context))