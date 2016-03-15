(ns bru-9.field.core)

(defprotocol PField
  "Defines elementary flow-field operations."
  (value-at [_ position] "Returns field value at given position in 3D space."))
