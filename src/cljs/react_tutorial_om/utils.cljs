(ns react-tutorial-om.utils
  (:require [cljs.reader :as reader])
  (:import [goog.ui IdGenerator]))

(defn guid []
  (-> IdGenerator
      .getInstance
      .getNextUniqueId))
