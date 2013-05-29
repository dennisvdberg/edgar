(ns edgar.core.signal.lagging
  (:require [edgar.core.analysis.lagging :as analysis]))


(defn join-averages
  "Create a list where i) tick-list ii) sma-list and iii) ema-list are overlaid"

  ([tick-window tick-list]

     (let [sma-list (analysis/simple-moving-average nil tick-window tick-list)
           ema-list (analysis/exponential-moving-average nil tick-window tick-list sma-list)]
       (moving-averages tick-window tick-list sma-list ema-list)))

  ([tick-window tick-list sma-list ema-list]
     (map (fn [titem sitem eitem]

            ;; 1. ensure that we have the :last-trade-time for simple and exponential items
            ;; 2. ensure that all 3 time items line up
            (if (and (and (not (nil? (:last-trade-time sitem)))
                          (not (nil? (:last-trade-time eitem))))
                     (= (:last-trade-time titem) (:last-trade-time sitem) (:last-trade-time eitem)))

              {:last-trade-time (:last-trade-time titem)
               :last-trade-price (read-string (:last-trade-price titem))
               :last-trade-price-average (:last-trade-price-average sitem)
               :last-trade-price-exponential (:last-trade-price-exponential eitem)}

              nil))

          tick-list
          sma-list
          ema-list)))


(defn moving-averages
  "Takes baseline time series, along with 2 other moving averages.

   Produces a list of signals where the 2nd moving average overlaps (abouve or below) the first.
   By default, this function will produce a Simple Moving Average and an Exponential Moving Average."

  ([tick-window tick-list]

     (let [sma-list (analysis/simple-moving-average nil tick-window tick-list)
           ema-list (analysis/exponential-moving-average nil tick-window tick-list sma-list)]
       (moving-averages tick-window tick-list sma-list ema-list)))

  ([tick-window tick-list sma-list ema-list]

     ;; create a list where i) tick-list ii) sma-list and iii) ema-list are overlaid
     (let [joined-list (join-averages tick-window tick-list sma-list ema-list)
           partitioned-join (partition 2 1 (remove nil? joined-list))
           start-list (into '() (repeat tick-window nil))]


       ;; find time points where ema-list (or second list) crosses over the sma-list (or 1st list)
       (reduce (fn [rslt ech]

                 (let [fst (first ech)
                       snd (second ech)

                       ;; in the first element, has the ema crossed abouve the sma from the second element
                       signal-up (and (< (:last-trade-price-exponential snd) (:last-trade-price-average snd))
                                      (> (:last-trade-price-exponential fst) (:last-trade-price-average snd)))

                       ;; in the first element, has the ema crossed below the sma from the second element
                       signal-down (and (> (:last-trade-price-exponential snd) (:last-trade-price-average snd))
                                        (< (:last-trade-price-exponential fst) (:last-trade-price-average snd)))

                       raw-data fst
                       ]

                   ;; return either i) :up signal, ii) :down signal or iii) nothing, with just the raw data
                   (if signal-up
                     (cons (assoc raw-data :signal :up) rslt)
                     (if signal-down
                       (cons (assoc raw-data :isgnal :down) rslt)
                       (cons raw-data rslt)))))
               start-list
               (reverse partitioned-join))
       )))
