(ns webtools.components.grid)

(defn  full-width-column
  [& content]
  `[:div.col-xs-12.col-sm-10.offset-sm-1
    ~@content])
