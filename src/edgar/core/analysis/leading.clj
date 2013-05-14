(ns edgar.core.analysis.leading
  (:require [edgar.core.analysis.lagging :as lagging]))


(defn macd
  "The MACD 'oscillator' or 'indicator' is a collection of three signals (or computed data-series), calculated from historical price data. These three signal lines are:

    i) the MACD line: difference between the 12 and 26 days EMAs
      MACD = EMA[stockPrices,12] – EMA[stockPrices,26]

    ii) the signal line (or average line): 9 EMA of the MACD line
      signal = EMA[MACD,9]

    iii) and the difference (or divergence): difference between the blue and red lines
      histogram = MACD – signal

    Options are:
      :macd-window-fast (default is 12)
      :macd-window-slow (default is 26)
      :signal-window (default is 9)"

  ([options tick-window tick-list]
     (macd options tick-window tick-list (lagging/simple-moving-average nil tick-window tick-list)))

  ([options tick-window tick-list sma-list]

     ;; compute the MACD line
     (let [
           {macd-fast :macd-window-fast
            macd-slow :macd-window-slow
            signal-window :signal-window
            :or {macd-fast 12
                 macd-slow 26
                 signal-window 9}} options

           ;; 1. compute 12 EMA
           ema-short (lagging/exponential-moving-average nil macd-fast tick-list sma-list)


           ;; 2. compute 26 EMA
           ema-long (lagging/exponential-moving-average nil macd-slow tick-list sma-list)


           ;; 3. for each tick, compute difference between 12 and 26 EMA
           ;; EMA lists will have a structure like:
           #_({:last-trade-price 203.98,
                :last-trade-time 1368215573010,
                :last-trade-price-exponential 204.00119130504845}
               )

           macd (map (fn [e1 e2]

                       (if (and (-> e1 nil? not)
                                (-> e2 nil? not))

                         {:last-trade-price (:last-trade-price e1)
                          :last-trade-time (:last-trade-time e1)
                          :last-trade-macd (- (:last-trade-price-exponential e1) (:last-trade-price-exponential e2))
                          }))
                     ema-short
                     ema-long)

           ;; Compute 9 EMA of the MACD
           ema-signal (lagging/exponential-moving-average {:input :last-trade-macd :output :ema-signal :etal [:last-trade-price :last-trade-time]} signal-window nil macd)

           ]

       ;; compute the difference, or divergence
       (let [macd-list (into '() (repeat signal-window nil))]

         (cons
          (map (fn [e-macd e-ema]

                 (if (and (-> e-macd nil? not)
                          (-> e-ema nil? not))

                   {:last-trade-price (:last-trade-price e-macd)
                    :last-trade-time (:last-trade-time e-macd)
                    :histogram (- (:last-trade-macd e-macd) (:ema-signal e-ema))}
                   ))
               macd
               ema-signal
               )
          macd-list)
         )
       )
     )
)

(defn stochastic-oscillator
  "The stochastic oscillator is a momentum indicator. According to George C. Lane (the inventor), it 'doesn't follow price, it doesn't follow volume or anything like that. It follows the speed or the momentum of price. As a rule, the momentum changes direction before price'. A 3-line Stochastics will give an anticipatory signal in %K, a signal in the turnaround of %D at or before a bottom, and a confirmation of the turnaround in %D-Slow. Smoothing the indicator over 3 periods is standard. Returns a list, equal in length to the tick-list, but only with slots filled, where preceding tick-list allows.

     i) last-price:
       the last closing price

     ii) %K:
       (last-price - low-price / high-price - low-price) * 100

     iii) %D:
       3-period exponential moving average of %K

     iv)  %D-Slow
       a 3-period exponential moving average of %D

     v) low-price:
       the lowest price over the last N periods

     vi) high-price:
       the highest price over the last N periods


   The inputs to this function are:

     tick-window: the length of Stochastic, or number of ticks under observation (defaults to 14)
     trigger-window: the smoothing line (defaults to 3)
     trigger-line: (defaults to 3)
     tick-list: the input time series (in last trade price)"

  [tick-window trigger-window trigger-line tick-list]


  (let [stochastic-list (into '() (repeat tick-window nil))]


    (reduce (fn [rslt ech]

              (let [last-price (:last-trade-price (first ech))

                    last-price-list (map #(if (string? (:last-trade-price %))
                                            (read-string (:last-trade-price %))
                                            (:last-trade-price %)) ech)

                    highest-price (apply max last-price-list)
                    lowest-price (apply min last-price-list)

                    ;; calculate %K
                    %K (/ (- last-price lowest-price) (- highest-price lowest-price))


                    ;;xxx (println "... last-price-list[" last-price-list "] / high[" highest-price "] low[" lowest-price "] PRICE[" last-price "] ...k[" %K "]")

                    ;; calculate %D

                    ]

                (cons {:last-price last-price
                       :highest-price highest-price
                       :lowest-price lowest-price
                       :K %K} rslt)))
            stochastic-list
            (partition tick-window 1 tick-list))
    )
)
