(ns nubank-challenge.core-test
  (:require [cheshire.core :as cheshire]
            [midje.sweet :refer :all]
            [nubank-challenge.handler :refer :all]
            [ring.mock.request :as mock]))

(defn parse-body [body]
  (cheshire/parse-string (slurp body) true))

(facts "NuBank Challenge Tests"

  ;; Basic actions tests

  (fact "Test create new simulation space"
    (let [response (app (-> (mock/request :post "/api/simulations")))
          body     (parse-body (:body response))]
      (:status response) => 200
      (:result body) => {:data {:robots [] :dinosaurs []} :id 1}))

  (fact "Test create new robot in a corner, facing west"
    (let [response (app (-> (mock/request :post "/api/simulations/1/robots")
                            (mock/json-body {:x 50 :y 50 :dir 3})))
          body     (parse-body (:body response))]
      (:status response) => 200
      (:result body) => {:x 50 :y 50 :dir 3 :id 1}))

  (fact "Test create new dinosaur, to the left of the robot"
    (let [response (app (-> (mock/request :post "/api/simulations/1/dinosaurs")
                            (mock/json-body {:x 50 :y 49})))
          body     (parse-body (:body response))]
      (:status response) => 200
      (:result body) => {:x 50 :y 49 :id 1})) ;; id is not really important for dinosaurs

  (fact "Test turning the robot to the left"
    (let [response (app (-> (mock/request :put "/api/simulations/1/robots/1?action=turn-left")))
          body     (parse-body (:body response))]
      (:status response) => 200
      (:result body) => {:x 50 :y 50 :dir 2 :id 1}))

  (fact "Test turning the robot to the right, he should be facing west again"
    (let [response (app (-> (mock/request :put "/api/simulations/1/robots/1?action=turn-right")))
          body     (parse-body (:body response))]
      (:status response) => 200
      (:result body) => {:x 50 :y 50 :dir 3 :id 1}))

  (fact "Test making the robot attack, thus disappearing the dinosaur next to him"
    (let [response (app (-> (mock/request :put "/api/simulations/1/robots/1?action=attack")))
          body     (parse-body (:body response))]
      (:status response) => 200
      (:result body) => {:x 50 :y 50 :dir 3 :id 1}))

  (fact "Test moving the robot to the now unoccupied position"
    (let [response (app (-> (mock/request :put "/api/simulations/1/robots/1?action=move-forward")))
          body     (parse-body (:body response))]
      (:status response) => 200
      (:result body) => {:x 50 :y 49 :dir 3 :id 1}))

  (fact "Test moving the robot backwards to go back to his previous position"
    (let [response (app (-> (mock/request :put "/api/simulations/1/robots/1?action=move-backwards")))
          body     (parse-body (:body response))]
      (:status response) => 200
      (:result body) => {:x 50 :y 50 :dir 3 :id 1}))

  ;; Some corner case tests

  (fact "Test that moving the robot backwards off the board is not an allowed operation"
    (let [response (app (-> (mock/request :put "/api/simulations/1/robots/1?action=move-backwards")))]
      (:status response) => 403))

  (fact "Test that direction cycles from 3 to 0 by turning the robot right"
    (let [response (app (-> (mock/request :put "/api/simulations/1/robots/1?action=turn-right")))
          body     (parse-body (:body response))]
      (:status response) => 200
      (:result body) => {:x 50 :y 50 :dir 0 :id 1}))

  (fact "Test that direction cycles from 0 to 3 by turning the robot left"
    (let [response (app (-> (mock/request :put "/api/simulations/1/robots/1?action=turn-left")))
          body     (parse-body (:body response))]
      (:status response) => 200
      (:result body) => {:x 50 :y 50 :dir 3 :id 1}))

  (fact "Test you are not allowed to create a robot in the same position as the already created robot"
    (let [response (app (-> (mock/request :post "/api/simulations/1/robots")
                            (mock/json-body {:x 50 :y 50 :dir 2})))]
      (:status response) => 403))

  (fact "Test you are not allowed to create a dinosaur in the same position as the already created robot"
    (let [response (app (-> (mock/request :post "/api/simulations/1/dinosaurs")
                            (mock/json-body {:x 50 :y 50})))]
      (:status response) => 403))

  ;; These tests will fail, uncomment them at your own risk (:

  (fact "Create new dinosaur, to the west of the robot"
    (let [response (app (-> (mock/request :post "/api/simulations/1/dinosaurs")
                            (mock/json-body {:x 50 :y 49})))
          body     (parse-body (:body response))]
      (:status response) => 200
      (get-in body [:result :x]) => 50
      (get-in body [:result :y]) => 49
      (get-in body [:result :id]) => -5)) ;; On purpose, this should fail
                                          ;; and show that the three dinosaurs
                                          ;; have the same id.

  (fact "Create new dinosaur, to the north of the robot"
    (let [response (app (-> (mock/request :post "/api/simulations/1/dinosaurs")
                            (mock/json-body {:x 49 :y 50})))
          body     (parse-body (:body response))]
      (:status response) => 200
      (get-in body [:result :x]) => 49
      (get-in body [:result :y]) => 50
      (get-in body [:result :id]) => -5)) ;; On purpose, this should fail
                                          ;; and show that the three dinosaurs
                                          ;; have the same id.

  (fact "Create new dinosaur, to the northwest of the robot"
    (let [response (app (-> (mock/request :post "/api/simulations/1/dinosaurs")
                            (mock/json-body {:x 49 :y 49})))
          body     (parse-body (:body response))]
      (:status response) => 200
      (get-in body [:result :x]) => 49
      (get-in body [:result :y]) => 49
      (get-in body [:result :id]) => -5)) ;; On purpose, this should fail
                                          ;; and show that the three dinosaurs
                                          ;; have the same id.

  (fact "Test making the robot attack, knocking out two out of three dinosaurs"
    (let [response (app (-> (mock/request :put "/api/simulations/1/robots/1?action=attack")))
          body     (parse-body (:body response))]
      (:status response) => 200
      (:result body) => {:x 50 :y 50 :dir 3 :id 1}))

  (fact "Test that the dinosaur to the northwest survived, by attempting to create a dinosaur in that position"
    (let [response (app (-> (mock/request :post "/api/simulations/1/dinosaurs")
                            (mock/json-body {:x 49 :y 49})))]
      (:status response) => 403)))
