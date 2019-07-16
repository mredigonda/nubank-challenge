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
  (let [idx (dec sid)]
    (update-in prev [idx :robots] (fn [robots] (conj robots robot)))))

(defn- create-dinosaur [sid dinosaur prev]
  (let [idx (dec sid)]
    (update-in prev [idx :dinosaurs] (fn [dinosaurs] (conj dinosaurs dinosaur)))))

(defn- place-dinosaurs [dinosaurs board]
  (if (empty? dinosaurs)
      board
      (let [dinosaur (first dinosaurs)
            x        (:x dinosaur)
            y        (:y dinosaur)
            newboard (assoc-in board [(dec x) (dec y)] {:type "DINOSAUR" :id -1})]
            (recur (rest dinosaurs) newboard))))

(defn- place-robots
  ([robots board]
    (place-robots robots board 1))
  ([robots board id]
    (if (empty? robots)
        board
        (let [robot    (first robots)
              x        (:x robot)
              y        (:y robot)
              newboard (assoc-in board [(dec x) (dec y)] {:type "ROBOT" :id id})]
              (recur (rest robots) newboard (inc id))))))

(defn- robot-turn [sid rid turn-val prev]
  (update-in prev [(dec sid) :robots (dec rid) :dir]
             (fn [dir] (mod (+ dir turn-val) 4))))

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
      (if (occupied? sid x y) ; FIXME: Concurrency problem.
        (forbidden "There is another entity in this position")
        (do (swap! simulations (partial create-robot sid robot))
            (ok {:result (count (:robots (nth @simulations (dec sid))))}))))
    (bad-request "Invalid parameters")))

(defn handle-create-dinosaur [sid dinosaur]
  (if (and (>= sid 1)
           (<= sid (count @simulations))
           (>= (:x dinosaur) 1)
           (<= (:x dinosaur) 50)
           (>= (:y dinosaur) 1)
           (<= (:y dinosaur) 50))
      (let [x (:x dinosaur)
            y (:y dinosaur)]
           (if (occupied? sid x y)
               (forbidden "There is another entity in this position")
               (do (swap! simulations (partial create-dinosaur sid dinosaur))
                   (ok {:result "OK"}))))
      (bad-request "Invalid parameters")))

(defn handle-get-simulation [sid]
  (if (and (>= sid 1)
           (<= sid (count @simulations)))
      (let [board (into [] (repeat 50 (into [] (repeat 50 {:type "EMPTY" :id -1}))))
            robots (get-in @simulations [(dec sid) :robots])
            dinosaurs (get-in @simulations [(dec sid) :dinosaurs])]
           (ok {:result (place-dinosaurs dinosaurs (place-robots robots board))}))
      (bad-request "Invalid parameter")))

(defn handle-robot-action [sid rid action]
  (if (and (>= sid 1) (<= sid (count @simulations)))
      (let [simulation (nth @simulations (dec sid))]
           (if (and (>= rid 1) (<= rid (count (:robots simulation))))
               (case action
                 "turn-right" (swap! simulations (partial robot-turn sid rid 1))
                 "turn-left" (swap! simulations (partial robot-turn sid rid -1)))
                 ;~ "move-forward" ()
                 ;~ "move-backwards" ()
                 ;~ "attack" (robot-attack sid rid))
               (bad-request "Invalid parameters")))
      (bad-request "Invalid parameters")))
