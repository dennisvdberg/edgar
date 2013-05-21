(ns edgar
  (:use [jayq.core :only [$ css inner]])
  (:use-macros [jayq.macros :only [let-ajax let-deferred]])
  (:require [jayq.core :as jq]))


;; Scrolling with Lionbars
(.lionbars ($ ".tab-content"))


(defn render-main-stock-graph [tlist]

  (-> ($ "#main-stock-graph")
      (.highcharts "StockChart" (clj->js
                                 {:rangeSelector {:selected 1},
                                  :title {:text "Test Stock Data"},
                                  :series [{:name "Test Stock Data",
                                            :data tlist
                                            :marker {:enabled true, :radius 3},
                                            :shadow true,
                                            :tooltip {:valueDecimals 2}}]})
                   )))

(def tick-list (clj->js [[1368215573010 203.98] [1368215576331 203.99] [1368215576857 203.99] [1368215577765 203.99] [1368215578769 204.0] [1368215579272 204.01] [1368215579517 204.02] [1368215581769 204.02] [1368215583602 204.01] [1368215585650 204.02] [1368215586060 204.02] [1368215587029 204.01] [1368215588318 204.02] [1368215589335 204.01] [1368215589536 204.01] [1368215589846 204.0] [1368215591079 203.99] [1368215591789 203.99] [1368215592104 203.98] [1368215592615 203.98] [1368215592758 203.99] [1368215594039 203.97] [1368215597119 203.98] [1368215597632 203.97] [1368215599396 203.97] [1368215603876 203.96] [1368215606059 203.96] [1368215610316 203.95] [1368215610634 203.95] [1368215610813 203.93] [1368215612886 203.95] [1368215615858 203.94] [1368215618621 203.94] [1368215619138 203.96] [1368215623846 203.94] [1368215632669 203.94] [1368215634709 203.92] [1368215636587 203.93] [1368215636952 203.94] [1368215638328 203.93]]))

(render-main-stock-graph tick-list)


(defn populate-multiselect []

  (let-deferred
   [filtered-input ($/ajax "/list-filtered-input")]

   (let [multiselect ($ ".multiselect")]

     (reduce (fn [rslt inp]

               (let [option-value (second inp)
                     option-label (nth inp 2)
                     price-difference (.toFixed (first inp) 2)]

                 (-> multiselect
                     (.append (str "<option value='" option-value "'>" option-label " (" price-difference ")</option>")))))
             nil
             (into-array filtered-input))

     (-> ($ ".multiselect")
         (.multiselect (clj->js {:enableFiltering true
                                 :onChange (fn [element checked]

                                             (.log js/console (str "element[" element "] / checked[" checked "]"))
                                             ($/ajax "/get-historical-data"
                                                     (clj->js {:complete (fn [jqXHR status]
                                                                           )}))
                                             )})))))

  )

(populate-multiselect)
