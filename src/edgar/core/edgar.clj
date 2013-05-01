(ns edgar.core.edgar
  (:use [clojure.repl]
        [clojure.core.strint]
        [clojure.tools.namespace.repl]
        [datomic.api :only [q db] :as d])
  (:require [clojure.tools.logging :as log]
            [clojure.walk :as walk]
            [clojure.string :as cstring]
            [edgar.datomic :as edatomic]
            [edgar.ib.market :as market]
            [edgar.tee.datomic :as tdatomic])
  )


(defn load-historical-data [conn]

  ;; find entity.symbol (and entire entity) where price-difference is greatest
  (let [historical-entities (q '[:find ?p ?s ?c :where
                                 [?h :historical/price-difference ?p]
                                 [?h :historical/symbol ?s]
                                 [?h :historical/company ?c]
                                 ] (db conn))]
    (sort-by first historical-entities)
    )

  )


(defn feed-handler
  "Event structures will look like below:

  {type tickPrice, tickerId 0, price 403.87, canAutoExecute 1, field 1}
  {type tickPrice, tickerId 0, price 404.16, canAutoExecute 1, field 2}
  {type tickPrice, tickerId 0, price 404.01, canAutoExecute 0, field 4}
  {type tickPrice, tickerId 0, price 408.5, canAutoExecute 0, field 6}
  {type tickPrice, tickerId 0, price 403.28, canAutoExecute 0, field 7}
  {type tickPrice, tickerId 0, price 406.73, canAutoExecute 0, field 9}
  {type tickPrice, tickerId 0, price 406.56, canAutoExecute 0, field 14}"
  [options evt]


  (let [tick-list (:tick-list options)]

    (log/debug "edgar.core.edgar/feed-handler [" evt "] > tick-list size[" (count @tick-list) "] / [" (> (count @tick-list) 20) "] > options[" options "]")

    ;; handle tickPrice. Format will look like:
    ;; {type tickPrice, tickerId 0, timeStamp #<DateTime 2013-05-01T13:29:38.129-04:00>, price 412.14, canAutoExecute 0, field 4}
    (if (= "tickPrice" (options "type"))

      )


    ;; handle tickString. Format will look like:
    ;; {type tickString, tickerId 0, tickType 48, value 412.14;1;1367429375742;1196;410.39618025;true}
    (if (= "tickString" (options "type"))

      (let [values (cstring/split (options "value"))  ;; parsing RTVolume data
            ]
        )
      )


    ;; data structure that can contain the last 20 running ticks
    #_(dosync (alter tick-list
                   (fn [inp] (conj inp (walk/keywordize-keys evt)))))



    ;; TODO: at the end of our 20 tick window...
    ;;    -- only for RTVolume last ticks
    ;;    -- wrt a given tickerId

    ;; i. spit the data out to DB and
    ;; ii. and trim the list list back to 20
    #_(if (> (count @tick-list) 20)

      (do
        (tdatomic/tee-market (first @tick-list))
        (dosync (alter tick-list
                       (fn [inp] (into []
                                      (rest inp)))))))


    )

  ;; the recieving data structure, should allow me to apply a strategy to:
  ;; 1. plot bid / ask data on an x / y graph
  ;; 2. overlay SMA on the same graph
  ;; 3. overlay an EMA on the same graph

  ;; strategy should identify when trend lines cross-over

  ;; i. 20 tick structure & ii. strategy should allow me to extrude this to a clojurescript front-end

  )


(defn test-run []

  (let [client (:esocket (market/connect-to-market))
        conn (edatomic/database-connect)
        hdata (load-historical-data edatomic/conn)

        tick-list (ref [])]

    (market/subscribe-to-market (partial feed-handler {:tick-list tick-list}))
    (market/request-market-data client 0 (-> hdata last second) "233" false)
    ))

(defn fubar []
  (test-run))

#_(def historical-ids (q '[:find ?p ?s ?c :where
                         [?h :historical/price-difference ?p]
                         [?h :historical/symbol ?s]
                         [?h :historical/company ?c]
                         ] (db edatomic/conn)))
;;(def the-list (into [] historical-ids))
;;(def thing (sort-by first the-list))
;;(def allthings (doall (map println thing)))

;;(def historical-entities (map (fn [[hid]] (d/entity (db edatomic/conn) hid)) historical-ids))
;;(def historical-sorted (sort-by :historical/price-difference historical-entities))