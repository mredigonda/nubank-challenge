(ns nubank-challenge.service
  (:require [ring.util.http-response :refer :all]))

(defonce simulations (atom []))

(defn- occupied? [sid x y]
  (let [simulation (nth @simulations (dec sid))
        robots (:robots simulation)
        dinosaurs (:dinosaurs simulation)]
    (or (some (fn [cand] (and (== (:x cand) x) (== (:y cand) y))) robots)
        (some (fn [cand] (and (== (:x cand) x) (== (:y cand) y))) dinosaurs))))

(defn- create-simulation [prev]
  (conj prev {:robots [] :dinosaurs []}))

(defn- create-robot [sid robot prev]
  (let [idx        (dec sid)]
    (update-in prev [idx :robots] (fn [robots] (conj robots robot)))))

(defn handle-create-simulation []
  (swap! simulations create-simulation)
  (ok {:result (count @simulations)}))
    
(defn handle-create-robot [sid robot]
  (if (and (>= sid 1)
           (<= sid (count @simulations))
           (>= (:x robot) 1)
           (<= (:x robot) 50)
           (>= (:y robot) 1)
           (<= (:y robot) 50)
           (>= (:dir robot) 0)
           (<= (:dir robot) 3))
    (let [simulation (nth @simulations (dec sid))
          x (:x robot)
          y (:y robot)
          robots (:robots simulation)
          dinosaurs (:dinosaurs simulation)]
      (if (occupied? sid x y)
        (forbidden "There is another entity in this position")
        (do (swap! simulations (partial create-robot sid robot))
            (ok {:result (count (:robots (nth @simulations (dec sid))))}))))
    (bad-request "Invalid parameters")))
