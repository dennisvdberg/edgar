(ns edgar.core.strategy.strategy)


(defn price-increase? [tick-list]

  (let [fst (first tick-list)
        snd (second tick-list)]

    (> (:last-trade-price fst) (:last-trade-price snd))))

(defn price-below-sma? [tick-list signals-ma]

  (let [p-tick (first tick-list)
        sma-tick (first signals-ma)]

    (< (:last-trade-price p-tick) (:last-trade-price-average sma-tick))))

(defn price-cross-abouve-sma? [tick-list sma-list]

  (let [ftick (first tick-list)
        ntick (second tick-list)

        fsma (first (filter (fn [inp] (= (:last-trade-time ftick)
                                        (:last-trade-time inp)))
                            sma-list))
        nsma (first (filter (fn [inp] (= (:last-trade-time ntick)
                                        (:last-trade-time inp)))
                            sma-list))]

    (and (< (:last-trade-price ntick) (:last-trade-price-average nsma))
         (> (:last-trade-price ftick) (:last-trade-price-average fsma)))))

(defn bollinger-price-below? [tick-list signals-bollinger]

  (let [p-tick (first tick-list)
        b-ticks (take 2 signals-bollinger)]

    (some (fn [inp]
            (<= (:last-trade-price p-tick)
                (:lower-band inp)))
          b-ticks)))

(defn bollinger-was-narrower? [signals-bollinger]

  (let [b1 (first signals-bollinger)
        b-first (assoc b1 :difference (- (:upper-band b1)
                                         (:lower-band b1)))

        b2 (take 2 (rest signals-bollinger))
        b-rest (map (fn [inp]
                      (assoc inp :difference (- (:upper-band inp)
                                                (:lower-band inp))))
                    b2)]

    (some (fn [inp]
            (> (:difference b-first)
               (:difference inp)))
          b-rest)))

(defn macd-crossover? [signals-macd]

  (let [fmacd (first signals-macd)
        nmacd (second signals-macd)]

    (and (< (:last-trade-macd nmacd) (:ema-signal nmacd))
         (> (:last-trade-macd fmacd ) (:ema-signal fmacd)))))

(defn macd-histogram-squeeze? [signals-macd]

  (let [h-first (first signals-macd)
        h-rest (take 2 (rest signals-macd))]

    (some (fn [inp]
            (> (:histogram h-first)
               (:histogram inp)))
          h-rest)))

(defn obv-increasing? [signals-obv]

  (let [fst (first signals-obv)
        snd (second signals-obv)]

    (> (:obv fst) (:obv snd))))

(defn stochastic-oversold? [signals-stochastic]

  (some (fn [inp]
          (<= 0.8 (:K inp)))
        signals-stochastic))

(defn stochastic-crossover? [signals-stochastic]

  (let [fstochastic (first signals-stochastic)
        nstochastic (second signals-stochastic)]

    (and (< (:K nstochastic) (:D nstochastic))
         (> (:K fstochastic ) (:D fstochastic)))))


(defn strategy-A
  "This strategy is a composition of the below signals. It works only for the first tick.

   A. Price increase
   B. Price below the SMA

   C. Price was just at or below the bollinger-band (w/in last 2 ticks)
   D. Bollinger-Band was narrower (w/in last 2 ticks)

   E. MACD Histogram squeeze

   F. OBV increasing

   G. Stochastic is oversold, or was (w/in last 2 ticks)"
  [tick-list signals-ma signals-bollinger signals-macd signals-stochastic signals-obv]

  (let [
        ;; A.
        price-increaseV (price-increase? tick-list)

        ;; B.
        price-below-smaV (price-below-sma? tick-list signals-ma)

        ;; C.
        bollinger-price-belowV (bollinger-price-below? tick-list signals-bollinger)

        ;; D.
        bollinger-was-narrowerV (bollinger-was-narrower? signals-bollinger)

        ;; E.
        macd-histogram-squeezeV (macd-histogram-squeeze? signals-macd)

        ;; F.
        obv-increasingV (obv-increasing? signals-obv)

        ;; G.
        stochastic-oversoldV (stochastic-oversold? signals-stochastic)]

    (if (and price-increaseV price-below-smaV bollinger-price-belowV bollinger-was-narrowerV macd-histogram-squeezeV obv-increasingV stochastic-oversoldV)

      ;; if all conditions are met, put an :up signal, with the reasons
      (concat (assoc (first tick-list) :strategies [{:signal :up
                                                     :why [:price-increase :price-below-sma :bollinger-price-below :bollinger-was-narrower :macd-histogram-squeeze :obv-increasing :stochastic-oversold]}])
              (rest tick-list)))))

(defn strategy-fill-A
  "Applies strategy-A filters, for the entire length of the tick-list"
  [tick-list signals-ma signals-bollinger signals-macd signals-stochastic signals-obv]

  )

(defn strategy-B
  "This strategy is a composition of the below signals. It works only for the first tick.

   A. Price crosses abouve SMA

   B. Bollinger-Band was narrower (w/in last 2 ticks)

   C. MACD crossover

   D. Stochastic crossover
   E. Stochastic is oversold or was (w/in last 2 ticks)

   F. OBV increasing"
  [tick-list signals-ma signals-bollinger signals-macd signals-stochastic signals-obv]

  (let [
        ;; A.
        price-cross-abouve-smaV (price-cross-abouve-sma? tick-list signals-ma)

        ;; B.
        bollinger-was-narrowerV (bollinger-was-narrower? signals-bollinger)

        ;; C.
        macd-crossoverV (macd-crossover? signals-macd)

        ;; D.
        stochastic-crossoverV (stochastic-crossover? signals-stochastic)

        ;; E.
        stochastic-oversoldV (stochastic-oversold? signals-stochastic)

        ;; F.
        obv-increasingV (obv-increasing? signals-obv)]

    (if (and price-cross-abouve-smaV bollinger-was-narrowerV macd-crossoverV stochastic-crossoverV stochastic-oversoldV obv-increasingV)

      (concat (assoc (first tick-list) :strategies [{:signal :up
                                                     :why [:price-cross-abouve-sma :bollinger-was-narrower :macd-crossover :stochastic-crossover :stochastic-oversold :obv-increasing]}])
              (rest tick-list)))))

(defn strategy-fill-B
  "Applies strategy-B filters, for the entire length of the tick-list"
  [tick-list signals-ma signals-bollinger signals-macd signals-stochastic signals-obv]

  )
