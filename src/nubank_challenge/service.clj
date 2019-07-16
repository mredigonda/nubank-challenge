(ns nubank-challenge.service
  (:require [ring.util.http-response :refer :all]))

(defonce simulations (atom []))

(defn create-simulation [prev]
  (conj prev {:robots [] :dinosaurs []}))

(defn create-robot [sid robot prev]
  (let [simulation (nth prev (dec sid))]
    
    (update simulation (fn [] ()))))

(defn handle-create-simulation []
  (swap! simulations create-simulation)
  (ok {:result (count @simulations)}))

(defn handle-create-robot [sid robot]
  (swap! simulations (partial create-robot sid robot))
  (count (:robots (nth @simulations (dec sid)))))
