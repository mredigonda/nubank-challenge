(ns nubank-challenge.service
  (:require [ring.util.http-response :refer :all]))

(defonce simulations (atom []))

(defn- valid?
  "Given a simulation id and two 1-indexed coordinates, it determines
  if that cell is valid, that is, if it is inside the board and if it
  is not occupied."
  [sid cx cy]
  (let [{:keys [robots dinosaurs]} (nth @simulations sid)]
    (or (some (fn [{:keys [x y]}] (and (== cx x) (== cy y))) robots)
        (some (fn [{:keys [x y]}] (and (== cx x) (== cy y))) dinosaurs)
        (< cx 1) (> cx 50) (< cy 1) (> cy 50))))

(defn- create-simulation [prev]
  (conj prev {:robots [] :dinosaurs []}))

(defn- create-robot
  "Given a simulation id, a robot and the previous global state, it
  returns a new state in which the robot is added to the simulation.
  Note: this doesn't check if there is an entity occupying that
  position, nor if the robot values are valid."
  [sid robot prev]
  (update-in prev
             [sid :robots]
             #(conj % (assoc robot :id (inc (count %))))))

(defn- create-dinosaur
  "Given a simulation id, a dinosaur and the previous global state, it
  returns a new state in which the dinosaur is added to the simulation.
  Note: this doesn't check if there is an entity occupying that
  position, nor if the dinosaur values are valid."
  [sid dinosaur prev]
  (update-in prev
             [sid :dinosaurs]
             #(conj % (assoc dinosaur :id (inc (count %))))))

(defn- place-dinosaurs
  "Given a list of dinosaurs and a board, it recursively inserts the
  dinosaurs into the required positions of the board."
  [dinosaurs board]
  (if (empty? dinosaurs)
    board
    (let [[{:keys [x y] :as dinosaur}] dinosaurs
          newboard (assoc-in board [(dec x) (dec y)] {:type "DINOSAUR"})]
      (recur (rest dinosaurs) newboard))))

(defn- place-robots
  "Given a list of robots and a board, it recursively inserts the robots
  into the required positions of the board."
  [robots board]
  (if (empty? robots)
    board
    (let [[{:keys [x y dir id] :as robot}] robots
          newboard (assoc-in board [(dec x) (dec y)] {:type "ROBOT" :dir dir :id id})]
      (recur (rest robots) newboard))))

(defn- move-dir
  "Given x, y coordinates and a direction (where 0 is north, and it
  goes clockwise) it moves the coordinates in that direction and it
  returns the new position as a map."
  [x y dir]
  (let [dx (nth [-1 0 1 0] dir)
        dy (nth [0 1 0 -1] dir)]
    {:x (+ x dx) :y (+ y dy)}))

(defn- robot-turn
  "Takes a simulation id, a robot id, a turning value, and the previous
  global state, and returns a new state with the respective robot turned
  by the turning value in clockwise direction."
  [sid rid turn-val prev]
  (update-in prev
             [sid :robots rid :dir]
             (fn [dir] (mod (+ dir turn-val) 4))))

(defn- robot-move-forward [sid rid prev]
  (update-in prev
             [sid :robots rid]
             (fn [{:keys [x y dir] :as robot}]
                 (into robot (move-dir x y dir)))))

(defn- robot-move-backwards [sid rid prev]
  (update-in prev
             [sid :robots rid]
             (fn [{:keys [x y dir] :as robot}]
                 (into robot (move-dir x y (mod (+ dir 2) 4))))))

(defn- robot-attack [sid rid prev]
  (let [robot (get-in prev [sid :robots rid])]
        (update-in prev
                   [sid :dinosaurs]
                   (partial filter (fn [dinosaur] (let [dx (Math/abs (- (:x robot) (:x dinosaur)))
                                                        dy (Math/abs (- (:y robot) (:y dinosaur)))]
                                                        (> (+ dx dy) 1)))))))

(defn handle-create-simulation []
  (swap! simulations create-simulation)
  (ok {:result {:id (count @simulations) :data (last @simulations)}}))
    
(defn handle-create-robot [sid robot]
  (if (and (<= 0 sid (dec (count @simulations)))
           (<= 1 (:x robot) 50)
           (<= 1 (:y robot) 50)
           (<= 0 (:dir robot) 3))
    (let [simulation (nth @simulations sid)
          x (:x robot)
          y (:y robot)
          robots (:robots simulation)
          dinosaurs (:dinosaurs simulation)]
      (if (valid? sid x y) ; FIXME: Concurrency problem.
        (forbidden "There is another entity in this position")
        (let [new-state (swap! simulations (partial create-robot sid robot))]
             (ok {:result (last (get-in new-state [sid :robots]))}))))
    (bad-request "Invalid parameters")))

(defn handle-create-dinosaur [sid dinosaur]
  (if (and (<= 0 sid (dec (count @simulations)))
           (<= 1 (:x dinosaur) 50)
           (<= 1 (:y dinosaur) 50))
      (let [x (:x dinosaur)
            y (:y dinosaur)]
           (if (valid? sid x y)
               (forbidden "There is another entity in this position")
               (let [new-state (swap! simulations (partial create-dinosaur sid dinosaur))]
                    (ok {:result (last (get-in new-state [sid :dinosaurs]))}))))
      (bad-request "Invalid parameters")))

(defn handle-robot-action [sid rid action]
  (if (<= 0 sid (dec (count @simulations)))
      (let [simulation (nth @simulations sid)
            robot (get-in @simulations [sid :robots rid]) ; USE DESTRUCTURING!
            x (:x robot)
            y (:y robot)
            dir (:dir robot)]
           (if (<= 0 rid (dec (count (:robots simulation))))
               (case action
                 "turn-right" (do (swap! simulations (partial robot-turn sid rid 1))
                                  (ok {:result (get-in @simulations [sid :robots rid])}))
                 "turn-left" (do (swap! simulations (partial robot-turn sid rid -1))
                                 (ok {:result (get-in @simulations [sid :robots rid])}))
                 "move-forward" (let [newpos (move-dir x y dir)
                                      nx (:x newpos)
                                      ny (:y newpos)]
                                      (if (valid? sid nx ny) ; FIX CONCURRENCY
                                         (forbidden "There is an entity in that position")
                                         (do (swap! simulations (partial robot-move-forward sid rid))
                                             (ok {:result (get-in @simulations [sid :robots rid])}))))
                 "move-backwards" (let [newpos (move-dir x y (mod (+ dir 2) 4))
                                       nx (:x newpos)
                                       ny (:y newpos)]
                                       (if (valid? sid nx ny) ; FIX CONCURRENCY
                                         (forbidden "There is an entity in that position")
                                         (do (swap! simulations (partial robot-move-backwards sid rid))
                                             (ok {:result (get-in @simulations [sid :robots rid])}))))
                 "attack" (do (swap! simulations (partial robot-attack sid rid))
                              (ok {:result (get-in @simulations [sid :robots rid])})))
               (bad-request "Invalid parameters")))
      (bad-request "Invalid parameters")))

(defn handle-get-simulation [sid]
  (if (<= 0 sid (dec (count @simulations)))
      (let [board (vec (repeat 50 (vec (repeat 50 {:type "EMPTY"}))))
            robots (get-in @simulations [sid :robots])
            dinosaurs (get-in @simulations [sid :dinosaurs])]
           (ok {:result (place-dinosaurs dinosaurs (place-robots robots board))}))
      (bad-request "Invalid parameter")))
