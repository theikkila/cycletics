(ns cycletics.core
  (:require [clojure.data.json :as json]
            [clojurewerkz.elastisch.rest  :as esr]
            [clojurewerkz.elastisch.rest.index :as esi]
            [clojurewerkz.elastisch.rest.document :as esd]
            [clj-time.core :as t]
            [clojure.core.async :as async :refer [go go-loop <!]]
            [clj-time.format :as f])
  (:gen-class))

; curl -H 'Content-Type: application/json' https://dev.hsl.fi/matka.hsl.fi/otp/routers/hsl/bike_rental |jq .
(def api-endpoint "https://dev.hsl.fi/matka.hsl.fi/otp/routers/hsl/bike_rental")


(defn station->essential-station [full-station]
  (let [bikes-available (:bikesAvailable full-station)
        slots-available (:spacesAvailable full-station)]
    {:bikes-available bikes-available
     :slots-available slots-available
     :slots (+ bikes-available slots-available)
     :x (:x full-station)
     :id (:id full-station)
     :y (:y full-station)
     :name (:name full-station)}))

;(station->essential-station test-station)

(defn now []
  (let [dformat (f/formatters :date-time)]
    (f/unparse dformat (t/now))))


(defn current-station-status []
  (let [api-data-text (slurp api-endpoint)
        full-data (json/read-str
                      api-data-text
                      :key-fn keyword)
        stations (:stations full-data)]
    {:timestamp (now)
     :stations (map station->essential-station stations)}))


(defn push-to-elasticsearch! [conn document]
  (esd/create conn "station-statuses" "snapshot" document))

(defn start-pushing! []
  (let [conn (esr/connect "http://elasticsearch:9200")]
    ; LOOP
    (loop [] ; go-loop
      (do
        (println (now))
        (push-to-elasticsearch! conn (current-station-status))
        (Thread/sleep 10000) ;(<! (async/timeout 10000))
        (recur)))))







(defn -main
  "I don't do a whole lot ... yet."
  [& args]
  (do
    (println "Start fetching...")
    (start-pushing!)))
