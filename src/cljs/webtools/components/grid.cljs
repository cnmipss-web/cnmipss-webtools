(ns webtools.components.grid)

(defn row
  "Bootstrap row"
  [& content]
  `[:div.row
    ~@content])

(defn full-width-column
  "Bootstrap column xs-12 sm-10 offset-sm-1"
  [& content]
  `[:div.col-xs-12.col-lg-10.offset-lg-1
    ~@content])
