(ns cycletics.core
  (:require [clojure.data.json :as json]
            [clojurewerkz.elastisch.rest  :as esr]
            [clojurewerkz.elastisch.rest.index :as esi]
            [clojurewerkz.elastisch.rest.document :as esd]
            [clj-time.core :as t]
            [clojure.core.async :as async :refer [go go-loop <!]]
            [clj-time.format :as f]
            [clojure.pprint :as pp])
  (:gen-class))

; curl -H 'Content-Type: application/json' https://dev.hsl.fi/matka.hsl.fi/otp/routers/hsl/bike_rental |jq .
(def api-endpoint "http://api.digitransit.fi/routing/v1/routers/hsl/bike_rental")


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
    (map station->essential-station stations)))



(defn add-timestamp-mapper [timestamp]
  (fn [m]
    (assoc m :timestamp timestamp)))

(defn push-to-elasticsearch! [conn document]
  (esd/create conn "station-statuses-single" "station" document))

(defn start-pushing! []
  (let [conn (esr/connect "http://essi:9200")
        push! (partial push-to-elasticsearch! conn)]
    ; LOOP
    (loop [] ; go-loop
      (do
        (println (now))
        (dorun (map push! (pmap (add-timestamp-mapper (now)) (current-station-status))))
        (Thread/sleep 60000) ;(<! (async/timeout 10000))
        (recur)))))



;; OLD RECORDS

; (defn handle-snapshot [push! snapshot]
;   (let [ts (:timestamp snapshot)
;         stations (:stations snapshot)
;         tsmapper (add-timestamp-mapper ts)
;         stations-with-timestamp (pmap tsmapper stations)]
;     ;(pp/pprint stations-with-timestamp)
;     (count (map push! stations-with-timestamp))))
;
; (let [conn (esr/connect "http://localhost:9200")
;       push! (partial push-to-elasticsearch! conn)
;       records (esd/search conn "station-statuses" "snapshot" :size 10000)
;       snapshots (pmap :_source (-> records :hits :hits))
;       push-snapshot! (partial handle-snapshot push!)]
;   (pp/pprint (apply + (pmap push-snapshot! snapshots))))

(defn -main
  "I don't do a whole lot ... yet."
  [& args]
  (do
    (println "Start fetching...")
    (start-pushing!)))
