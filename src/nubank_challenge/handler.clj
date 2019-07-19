(ns nubank-challenge.handler
  (:require [compojure.api.sweet :refer :all]
            [ring.util.http-response :refer :all]
            [schema.core :as s]
            [nubank-challenge.service :as service]
            [compojure.route :as route]))

(s/defschema Robot
  {:x s/Int
   :y s/Int
   :dir s/Int
   (s/optional-key :id) s/Int})

(s/defschema Dinosaur
  {:x s/Int
   :y s/Int
   (s/optional-key :id) s/Int})

(s/defschema Simulation
  {:data {:robots [Robot]
          :dinosaurs [Dinosaur]}
   (s/optional-key :id) s/Int})

(s/defschema Board
  [[{:type (s/enum :ROBOT :DINOSAUR :EMPTY)
     (s/optional-key :dir) s/Int
     (s/optional-key :id) s/Int}]])

(defn my-api []
  (api
    {:swagger
     {:ui "/"
      :spec "/swagger.json"
      :data {:info {:title "Nubank Challenge"
                    :description "REST API for NuBank challenge about
                    robots and dinosaurs."}
             :tags [{:name "API", :description ""}]}}}
  
    (context "/api" []
      :tags ["api"]

      (POST "/simulations" []
        :return {:result Simulation}
        :summary "Creates a new simulation space"
        (service/handle-create-simulation))
      
      (POST "/simulations/:sid/robots" []
        :path-params [sid :- Long]
        :body [robot {:x Long, :y Long, :dir Long}]
        :return {:result Robot}
        :summary "Creates a new robot"
        (service/handle-create-robot (dec sid) robot))
        
      (POST "/simulations/:sid/dinosaurs" []
        :path-params [sid :- Long]
        :body [dinosaur {:x Long, :y Long}]
        :return {:result Dinosaur}
        :summary "Creates a new dinosaur"
        (service/handle-create-dinosaur (dec sid) dinosaur))
      
      (PUT "/simulations/:sid/robots/:rid" []
        :path-params [sid :- Long, rid :- Long]
        :query-params [action :- String]
        :return {:result Robot}
        :summary "Makes a robot do an action"
        (service/handle-robot-action (dec sid) (dec rid) action))
      
      (GET "/simulations/:sid" []
        :path-params [sid :- Long]
        :return {:result Board}
        :summary "Displays the state of a simulation space"
        (service/handle-get-simulation (dec sid))))))

(def app
  (routes
    (my-api)
    (route/resources "/client")
    (route/not-found "404 Not Found")))
