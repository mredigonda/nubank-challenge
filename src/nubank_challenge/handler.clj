(ns nubank-challenge.handler
  (:require [compojure.api.sweet :refer :all]
            [ring.util.http-response :refer :all]
            [schema.core :as s]
            [nubank-challenge.service :as service]))

(s/defschema Pizza
  {:name s/Str
   (s/optional-key :description) s/Str
   :size (s/enum :L :M :S)
   :origin {:country (s/enum :FI :PO)
            :city s/Str}})

(s/defschema Board
  [[{:type (s/enum :ROBOT :DINASAUR :EMPTY)
    :id s/Int}]])

(def app
  (api
    {:swagger
     {:ui "/"
      :spec "/swagger.json"
      :data {:info {:title "Nubank-challenge"
                    :description "Compojure Api example"}
             :tags [{:name "api", :description "some apis"}]}}}

    (context "/api" []
      :tags ["api"]

      (GET "/plus" []
        :return {:result Long}
        :query-params [x :- Long, y :- Long]
        :summary "adds two numbers together"
        (ok {:result (+ x y)}))

      (POST "/echo" []
        :return Pizza
        :body [pizza Pizza]
        :summary "echoes a Pizza"
        (ok pizza))

      (POST "/simulations" []
        :return {:result Long}
        :summary "Creates a new simulation space"
        (service/handle-create-simulation))
      
      (POST "/simulations/:sid/robots" []
        :path-params [sid :- Long]
        :body [robot {:x Long, :y Long, :dir Long}]
        :return {:result Long}
        :summary "Creates a new robot"
        (service/handle-create-robot sid robot))
        
      (POST "/simulations/:sid/dinosaurs" []
        :path-params [sid :- Long]
        :body [dinosaur {:x Long, :y Long}]
        :return {:result String}
        :summary "Creates a new dinosaur"
        (service/handle-create-dinosaur sid dinosaur))
      
      (PUT "/simulations/:sid/robots/:rid" []
        :path-params [sid :- Long, rid :- Long]
        :query-params [action :- String]
        :return {:result String}
        :summary "Makes a robot do an action"
        (ok {:result (str "fuck: " action)}))
      
      (GET "/simulations/:sid" []
        :path-params [sid :- Long]
        :return {:result Board}
        :summary "Displays the state of a simulation space"
        (service/handle-get-simulation sid)))))
        ;~ (ok {:result '("..R.." "R.D.." "R...." "....." "..DD.")})))))

