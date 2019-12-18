(ns task02.query
   (:require [clojure.core.match :refer [match]])
   (:require [clojure.edn :as edn])
   (:use [task02 helpers db]))

;; Функция выполняющая парсинг запроса переданного пользователем
;;
;; Синтаксис запроса:
;; SELECT table_name [WHERE column comp-op value] [ORDER BY column] [LIMIT N] [JOIN other_table ON left_column = right_column]
;;
;; - Имена колонок указываются в запросе как обычные имена, а не в виде keywords. В
;;   результате необходимо преобразовать в keywords
;; - Поддерживаемые операторы WHERE: =, !=, <, >, <=, >=
;; - Имя таблицы в JOIN указывается в виде строки - оно будет передано функции get-table для получения объекта
;; - Значение value может быть либо числом, либо строкой в одинарных кавычках ('test').
;;   Необходимо вернуть данные соответствующего типа, удалив одинарные кавычки для строки.
;;
;; - Ключевые слова --> case-insensitive
;;
;; Функция должна вернуть последовательность со следующей структурой:
;;  - имя таблицы в виде строки
;;  - остальные параметры которые будут переданы select
;;
;; Если запрос нельзя распарсить, то вернуть nil

;; Примеры вызова:
;; > (parse-select "select student")
;; ("student")
;; > (parse-select "select student where id = 10")
;; ("student" :where #<function>)
;; > (parse-select "select student where id = 10 limit 2")
;; ("student" :where #<function> :limit 2)
;; > (parse-select "select student where id = 10 order by id limit 2")
;; ("student" :where #<function> :order-by :id :limit 2)
;; > (parse-select "select student where id = 10 order by id limit 2 join subject on id = sid")
;; ("student" :where #<function> :order-by :id :limit 2 :joins [[:id "subject" :sid]])
;; > (parse-select "werfwefw")
;; nil
;; SELECT table_name [WHERE column comp-op value] [ORDER BY column] [LIMIT N] [JOIN other_table ON left_column = right_column]
;(:or ("order by" _) ("limit" _)


(defn make-where-function [column op value]
      #(~op (keyword column) (edn/read-string value)))

(defn parse-select [^String sel-string]
      (loop [parsed [] req (vec (.split (.toLowerCase sel-string) " "))]
         (match req
                [] parsed

                ["select" tbl & other]
                (recur (conj parsed tbl) other)

                ["limit" n & other]
                (recur (concat parsed [:limit (parse-int n)]) other)

                ["order" "by" column & other]
                (recur (concat parsed [:order-by (keyword column)]) other)

                ["where" column op value & other]
                (recur (concat parsed [:where (make-where-function column op value)]) other)

                ["join" otherTbl "on" left "=" right & _]
                (concat parsed [:joins [[(keyword left) otherTbl (keyword right)]]])

                :else nil)))


                  ;; Выполняет запрос переданный в строке.  Бросает исключение если не удалось распарсить запрос

                  ;; Примеры вызова:
                  ;; > (perform-query "select student")
                  ;; ({:id 1, :year 1998, :surname "Ivanov"} {:id 2, :year 1997, :surname "Petrov"} {:id 3, :year 1996, :surname "Sidorov"})
                  ;; > (perform-query "select student order by year")
                  ;; ({:id 3, :year 1996, :surname "Sidorov"} {:id 2, :year 1997, :surname "Petrov"} {:id 1, :year 1998, :surname "Ivanov"})
                  ;; > (perform-query "select student where id > 1")
                  ;; ({:id 2, :year 1997, :surname "Petrov"} {:id 3, :year 1996, :surname "Sidorov"})
                  ;; > (perform-query "not valid")
                  ;; exception...
(defn perform-query [^String sel-string]
  (if-let [query (parse-select sel-string)]
    (apply select (get-table (first query)) (rest query))
    (throw (IllegalArgumentException. (str "Can't parse query: " sel-string)))))
