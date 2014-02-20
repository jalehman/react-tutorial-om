(ns react-tutorial-om.utils
  )

(defmacro logm
  "Log as a macro to give the caller's line number in web console"
  ([s]
     `(.log js/console (print-str ~s)))
  ([s & rest]
     `(.log js/console (print-str (cons ~s ~rest)))))

(defmacro make-table-cols
  "Build a table row with collumns "
  [el attrs cols]
  {:pre [(contains? #{'dom/td 'dom/th} el)]}
  `(dom/tr attrs
           ~@(for [col cols]
               `(~el nil ~col))))
(comment
  (.log js/console (str (cons ~s ~rest)))
  (fn [& args]
    (.apply js/console.log js/console (into-array args))))
