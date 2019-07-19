(ns nubank-challenge.service
  (:require [ring.util.http-response :refer :all]))

(defonce simulations (atom []))

(defn- valid?
  "Given a simulation id and two 1-indexed coordinates, it determines
  if that cell is valid, that is, if it is inside the board and if it
  is not occupied."
  [sid cx cy]
  (let [{:keys [robots dinosaurs]} (nth @simulations sid)
        not-same-cell (fn [{:keys [x y]}] (or (not= cx x) (not= cy y)))]
    (and (every? not-same-cell robots)
         (every? not-same-cell dinosaurs)
         (<= 1 cx 50) (<= 1 cy 50))))

(defn- create-simulation [prev]
  "Given the global state, it adds an empty simulation space to it."
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
          newboard (assoc-in board
                             [(dec x) (dec y)]
                             {:type "DINOSAUR"})]
      (recur (rest dinosaurs) newboard))))

(defn- place-robots
  "Given a list of robots and a board, it recursively inserts the robots
  into the required positions of the board."
  [robots board]
  (if (empty? robots)
    board
    (let [[{:keys [x y dir id] :as robot}] robots
          newboard (assoc-in board
                             [(dec x) (dec y)]
                             {:type "ROBOT" :dir dir :id id})]
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
  "Given a simulation id, a robot id, a turning value, and the previous
  global state, returns a new state with the respective robot turned
  by the turning value in clockwise direction."
  [sid rid turn-val prev]
  (update-in prev
             [sid :robots rid :dir]
             (fn [dir] (mod (+ dir turn-val) 4))))

(defn- robot-move-forward
  "Given a simulation id, a robot id, and the previous global state,
  it returns a new state with the position of the robot moved forward
  one cell, according to its facing direction.
  Note: this doesn't check if the new position is valid."
  [sid rid prev]
  (update-in prev
             [sid :robots rid]
             (fn [{:keys [x y dir] :as robot}]
                 (into robot (move-dir x y dir)))))

(defn- robot-move-backwards
  "Given a simulation id, a robot id, and the previous global state,
  it returns a new state with the position of the robot moved backwards
  one cell, according to its facing direction.
  Note: this doesn't check if the new position is valid."
  [sid rid prev]
  (update-in prev
             [sid :robots rid]
             (fn [{:keys [x y dir] :as robot}]
                 (into robot (move-dir x y (mod (+ dir 2) 4))))))

(defn- robot-attack
  "Given a simulation id, a robot id, and the global state, it returns
  the new global state after the robot's attack, which disappears all
  dinosaurs that are adjacent to it."
  [sid rid prev]
  (let [{rx :x ry :y} (get-in prev [sid :robots rid])]
    (update-in prev
               [sid :dinosaurs]
               (partial filter (fn [{:keys [x y]}]
                                   (let [dx (Math/abs (- rx x))
                                         dy (Math/abs (- ry y))]
                                         (> (+ dx dy) 1)))))))

(defn handle-create-simulation
  "Handles POST request to create a simulation space and returns the
  expected HTTP response."
  []
  (let [new-state (swap! simulations create-simulation)]
    (ok {:result {:id (count new-state) :data (last new-state)}})))
    
(defn handle-create-robot
  "Given a simulation id and a robot, it handles the creation of that
  robot in the respective simulation space and returns the expected HTTP
  response."
  [sid {:keys [x y dir] :as robot}]
  (if (and (<= 0 sid (dec (count @simulations)))
           (<= 1 x 50) (<= 1 y 50) (<= 0 dir 3))
    (let [{:keys [robots dinosaurs]} (nth @simulations sid)]
      (if (valid? sid x y) ; FIXME: Concurrency problem.
        (let [new-state (swap! simulations
                               (partial create-robot sid robot))]
          (ok {:result (last (get-in new-state [sid :robots]))}))
        (forbidden "Invalid position")))
    (bad-request "Invalid parameters")))

(defn handle-create-dinosaur
  "Given a simulation id and a dinosaur, it handles the creation of that
  dinosaur in the respective simulation space and returns the expected
  HTTP response."
  [sid {:keys [x y] :as dinosaur}]
  (if (and (<= 0 sid (dec (count @simulations)))
           (<= 1 x 50) (<= 1 y 50))
    (if (valid? sid x y)
      (let [new-state (swap! simulations
                             (partial create-dinosaur sid dinosaur))]
        (ok {:result (last (get-in new-state [sid :dinosaurs]))}))
      (forbidden "Invalid position"))
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
                                         (do (swap! simulations (partial robot-move-forward sid rid))
                                             (ok {:result (get-in @simulations [sid :robots rid])}))
                                         (forbidden "There is an entity in that position")))
                 "move-backwards" (let [newpos (move-dir x y (mod (+ dir 2) 4))
                                       nx (:x newpos)
                                       ny (:y newpos)]
                                       (if (valid? sid nx ny) ; FIX CONCURRENCY
                                         (do (swap! simulations (partial robot-move-backwards sid rid))
                                             (ok {:result (get-in @simulations [sid :robots rid])}))
                                         (forbidden "There is an entity in that position")))
                 "attack" (do (swap! simulations (partial robot-attack sid rid))
                              (ok {:result (get-in @simulations [sid :robots rid])})))
               (bad-request "Invalid parameters")))
      (bad-request "Invalid parameters")))

(defn handle-get-simulation
  "Given a simulation id it handles the GET request to obtain the
  current state of the board, returning the expected HTTP response."
  [sid]
  (if (<= 0 sid (dec (count @simulations)))
    (let [board (vec (repeat 50 (vec (repeat 50 {:type "EMPTY"}))))
          {:keys [robots dinosaurs]} (get @simulations sid)]
      (ok {:result (place-dinosaurs dinosaurs
                                    (place-robots robots board))}))
    (bad-request "Invalid parameter")))
