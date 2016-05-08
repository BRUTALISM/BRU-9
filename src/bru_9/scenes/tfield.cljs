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
             :visualisation-step 0.25})

(defonce *state* (atom {}))

(defn setup [initial-context]
  (let [scene (:scene initial-context)
        fres (:field-resolution config)
        step (:visualisation-step config)
        field (fl/linear-field [fres fres fres] #(v/randvec3))
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

;; (for [x (range 0 4 0.5)
;;       y (range 0 4 0.5)
;;       z (range 0 4 0.5)
;;       :let [coord [x y z]
;;             coordv (v/vec3 x y z)]]
;;   (prn x))
