(ns edgar.tee.datomic
  (:use [datomic.api :only [q db] :as d])
  (:require [clojure.tools.logging :as log]
            [edgar.tee.tee :as tns]
            [edgar.datomic :as edatomic]))

(defn tee
 "Process the list of entities. First, flatten out the :event-list, and merge it into the entity
 [{:id 0,
   :symbol DDD,
   :company 3D Systems Corporation,
   :price-difference 1.3100000000000023,
   :event-list [{high 35.11,
                 tickerId 0,
                 WAP 34.491,
                 open 35.07,
                 date 20130426,
                 count 3403,
                 low 33.8,
                 hasGaps false,
                 close 34.53,
                 field historicalData,
                 volume 8667,
                 type historicalData}]}]"
 [bucket]


 (log/debug "tee.datomic/tee > bucket[" bucket "]")


 ;; collect all data into a transaction list, then persist
 (let [final-tx (reduce (fn [rslt ech]


                          (log/debug "... EACH reduce > rslt[" rslt "] / ech[" ech "] / first example[" (-> ech :event-list first (get "high"))
                                     "]")
                          (conj rslt
                                {:db/id (d/tempid :db.part/user)
                                 :historical/symbol (:symbol ech)
                                 :historical/company (:company ech)
                                 :historical/price-difference (:price-difference ech)

                                 :historical/high (-> ech :event-list first (get "high"))
                                 :historical/low (-> ech :event-list first (get "low"))
                                 :historical/WAP (-> ech :event-list first (get "WAP"))

                                 :historical/open (-> ech :event-list first (get "open"))
                                 :historical/close (-> ech :event-list first (get "close"))

                                 :historical/date (-> ech :event-list first (get "date"))
                                 :historical/count (-> ech :event-list first (get "count"))
                                 :historical/hasGaps (-> ech :event-list first (get "hasGaps"))
                                 :historical/volume (-> ech :event-list first (get "volume"))
                                 })
                          )
                        []
                        bucket)
       ]

   (log/debug "tee.datomic/tee > final-tx[" final-tx "]")
   (d/transact edatomic/conn final-tx)
   )
 )
