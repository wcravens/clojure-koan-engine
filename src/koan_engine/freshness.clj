(ns koan-engine.freshness
  (:use [fresh.core :only [clj-files-in freshener]]
        [clojure.java.io :only [file]]
        [koan-engine.koans :only [among-paths?
                                  namaste
                                  next-koan-path
                                  ordered-koans
                                  ordered-koan-paths
                                  tests-pass?]])
  (:import [java.util.concurrent ScheduledThreadPoolExecutor TimeUnit]))

(defn files-to-keep-fresh [koan-root]
  (constantly
   (clj-files-in (file koan-root))))

(defn report-refresh [report]
  (when-let [refreshed-files (seq (:reloaded report))]
    (let [these-koans (filter
                       (among-paths? refreshed-files)
                       (ordered-koan-paths))]
      (when (every? tests-pass? these-koans)
        (if-let [next-koan-file (file (next-koan-path (last these-koans)))]
          (report-refresh {:reloaded [next-koan-file]})
          (namaste))))
    (println))
  :refreshed)

(defn refresh! [{:keys [koan-root]}]
  (freshener (files-to-keep-fresh koan-root)
             report-refresh))

(def scheduler (ScheduledThreadPoolExecutor. 1))

(defn setup-freshener [koan-map]
  (println "Starting auto-runner...")
  (.scheduleWithFixedDelay scheduler
                           (refresh! koan-map)
                           0 500 TimeUnit/MILLISECONDS)
  (.awaitTermination scheduler Long/MAX_VALUE TimeUnit/SECONDS))
