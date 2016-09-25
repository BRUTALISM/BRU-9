(ns bru-9.geom.ptf
  (:require-macros [thi.ng.math.macros :as mm])
  (:require [thi.ng.geom.ptf :as ptf]
            [thi.ng.geom.attribs :as attr]
            [thi.ng.geom.core :as g]
            [thi.ng.geom.basicmesh :as bm]
            [thi.ng.geom.vector :as v]))

(defn sweep-point
  "Takes a path point, a PTF normal & binormal and a profile point.
  Returns profile point projected on path (point)."
  [p t n b [qx qy qz]]
  (v/vec3
    (mm/madd qx (:x n) qy (:x b) qz (:x t) (:x p))
    (mm/madd qx (:y n) qy (:y b) qz (:y t) (:y p))
    (mm/madd qx (:z n) qy (:z b) qz (:z t) (:z p))))

(defn sweep-profile
  [profiles attribs opts [points tangents norms bnorms]]
  (let [{:keys [close? loop?] :or {close? true}} opts
        looped-profiles (cycle profiles)
        frames (map vector points tangents norms bnorms)
        tx (fn [[p t n b] prof] (mapv #(sweep-point p t n b %) prof))
        frame0 (tx (first frames) (first profiles))
        nprof (count (first profiles))
        numf (dec (count points))
        attr-state {:du (/ 1.0 nprof) :dv (/ 1.0 numf)}
        frames (if loop?
                 (concat (next frames) [(first frames)])
                 (next frames))
        sweep
        (fn [[faces prev i fid] [frame profile]]
          (let [curr (tx frame profile)
                curr (if close? (conj curr (first curr)) curr)
                atts (assoc attr-state :v (double (/ i numf)))
                faces (->> (interleave
                             (partition 2 1 prev)
                             (partition 2 1 curr))
                           (partition 2)
                           (map-indexed
                             (fn [j [a b]]
                               (attr/generate-face-attribs
                                 [(nth a 0) (nth a 1) (nth b 1) (nth b 0)]
                                 (+ fid j)
                                 attribs
                                 (assoc atts :u (double (/ j nprof))))))
                           (concat faces))]
            [faces curr (inc i) (+ fid nprof)]))]
    (first (reduce sweep
                   [nil (if close? (conj frame0 (first frame0)) frame0) 0 0]
                   (map vector frames looped-profiles)))))

(defn sweep-mesh
  ([points profiles]
   (sweep-mesh points profiles nil))
  ([points profiles {:keys [mesh attribs align?] :as opts}]
   (let [frames (ptf/compute-frames points)
         frames (if align? (ptf/align-frames frames) frames)]
     (->> frames
          (sweep-profile profiles attribs opts)
          (g/into (or mesh (bm/basic-mesh)))))))
