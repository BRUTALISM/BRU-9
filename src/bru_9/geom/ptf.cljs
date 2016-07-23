(ns bru-9.geom.ptf
  (:require [thi.ng.geom.ptf :as ptf]
            [thi.ng.geom.attribs :as attr]
            [thi.ng.geom.core :as g]
            [thi.ng.geom.basicmesh :as bm]))

(defn sweep-profile
  [profiles attribs opts [points _ norms bnorms]]
  (let [{:keys [close? loop?] :or {close? true}} opts
        looped-profiles (cycle profiles)
        frames (map vector points norms bnorms)
        tx (fn [[p n b] prof] (mapv #(ptf/sweep-point p n b %) prof))
        frame0 (tx (first frames) (first profiles))
        nprof (count (first profiles))
        nprof1 (inc nprof)
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
            [faces curr (inc i) (+ fid nprof1)]))]
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