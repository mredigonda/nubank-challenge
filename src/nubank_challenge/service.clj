(ns nubank-challenge.service
  (:require [ring.util.http-response :refer :all]))

(defonce simulations (atom []))

(defn create-simulation [prev]
  (conj prev {:robots [] :dinosaurs []}))

(defn create-robot [sid robot prev]
  (let [idx        (dec sid)]
    ;~ (do (println sid) (println robot) (println prev)
        ;~ (println (update-in prev [idx :robots] (fn [robots] (conj robots robot))))
        ;~ prev)))
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
      (if (or (some (fn [cand] (and (== (:x cand) (:x robot)) (== (:y cand) (:y robot)))) robots) 
              (some (fn [cand] (and (== (:x cand) (:x robot)) (== (:y cand) (:y robot)))) dinosaurs))
        (forbidden "There is another entity in this position")
        (do (swap! simulations (partial create-robot sid robot))
            (ok {:result (count (:robots (nth @simulations (dec sid))))}))))
    (bad-request "Invalid parameters")))
