(ns koan-engine.util
  (:require [clojure.string :as s]))

(defn version<
  "< for Clojure's version map."
  [v1 v2]
  (let [[major-a major-b] (map :major [v1 v2])
        [minor-a minor-b] (map :minor [v1 v2])]
    (or (< major-a major-b)
        (and (== major-a major-b)
             (< minor-a minor-b)))))

(defn require-version [[req-major req-minor]]
  (when (version< {:major req-major, :minor req-minor}
                  *clojure-version*)
    (throw (Exception.
            (format "Clojure version %s.%s or higher required."
                    req-major req-minor)))))

(defmacro safe-assert
  "Assertion with support for a message argument in all Clojure
   versions. (Pre-1.3.0, `assert` didn't accept a second argument and
   threw an error.)"
  ([x] `(safe-assert ~x ""))
  ([x msg]
     (if (version< *clojure-version* {:major 1, :minor 3})
       `(assert ~x)
       `(assert ~x ~msg))))

(defmacro fancy-assert
  "Assertion with fancy error messaging."
  ([x] (fancy-assert x ""))
  ([x message]
     `(try (safe-assert ~x ~message)
           (catch Exception e#
             (throw (Exception. (str '~message "\n" '~x )
                                e#))))))

(defn read-project []
  (let [rdr (clojure.lang.LineNumberingPushbackReader.
             (java.io.FileReader. (java.io.File. "project.clj")))]
    (->> (read rdr)
         (drop 3)
         (apply hash-map))))

(defn parse-required-version []
  (let [{deps :dependencies} (read-project)
        version-string (->> deps
                            (map (fn [xs] (vec (take 2 xs))))
                            (into {})
                            ('org.clojure/clojure))]
    (map read-string (take 3 (s/split version-string #"[\.\-]")))))
