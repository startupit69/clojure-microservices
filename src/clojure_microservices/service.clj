(ns clojure-microservices.service
  (:require [io.pedestal.http :as bootstrap]
            [io.pedestal.http.route :as route]
            [io.pedestal.http.body-params :as body-params]
            [io.pedestal.http.route.definition :refer [defroutes]]
            [monger.core :as mg]
            [monger.collection :as mc]
            [monger.json]
            [ring.util.response :as ring-resp]))

(defn about-page
  [request]
  (ring-resp/response (format "Clojure %s - served from %s"
                              (clojure-version)
                              (route/url-for ::about-page))))

;; MONGO_CONNECTION environment variable should be formed like this:
;; mongodb://username:password@staff.mongohq.com:port/dbname
(defn home-page
  [request]
    (let [uri (System/getenv "MONGO_CONNECTION")
          {:keys [conn db]} (mg/connect-via-uri uri)]
          (bootstrap/json-response
            (mc/find-maps db "books-catalogue") ))
)

(def mock-books-collection
   {
     :pro-mongodb-development
     {
       :name "Pro MongoDB Development"
       :author "Deepak Vohra"
       :year "2015"
       :publisher "APRESS"
       :isbn "978-1-4842-1598-2"
       :pages "301"
     }
     :data-structures-and-algorithms-with-javascript
     {
       :name "Data Structures and Algorithms with JavaScript"
       :author "Michael McMillan"
       :year "2014"
       :publisher "OReilly"
       :isbn "978-1-4493-6493-9"
       :pages "246"
     }
  }
)

(defn get-books
  [request]
  (let [uri (System/getenv "MONGO_CONNECTION")
        {:keys [conn db]} (mg/connect-via-uri uri)]
        (bootstrap/json-response
          (mc/find-maps db "books-catalogue") ))
)

(defn add-book
  [request]

    (let [incoming (:json-params request)
         connect-string (System/getenv "MONGO_CONNECTION")
         {:keys [conn db]} (mg/connect-via-uri connect-string)]
         (ring-resp/created
           "http://my-created-resource-url"
           (mc/insert-and-return db "books-catalogue" incoming)
         )
    )
)

(defn get-book-from-db
  [book-id]
  (let [uri (System/getenv "MONGO_CONNECTION")
        {:keys [conn db]} (mg/connect-via-uri uri)]
          (mc/find-maps db "books-catalogue" {:book-id book-id})
  )
)

(defn get-book
  [request]
  (bootstrap/json-response
    (get-book-from-db
      (get-in request [:path-params :book-id])
    )
  )
)


(defroutes routes
  ;; Defines "/" and "/about" routes with their associated :get handlers.
  ;; The interceptors defined after the verb map (e.g., {:get home-page}
  ;; apply to / and its children (/about).
  [[["/" {:get home-page}
     ^:interceptors [(body-params/body-params) bootstrap/html-body]
     ["/books" {:get get-books
                :post add-book}]
     ["/books/:book-id" {:get get-book}]
     ["/about" {:get about-page}]]]])

;; Consumed by clojure-microservices.server/create-server
;; See bootstrap/default-interceptors for additional options you can configure
(def service {:env :prod
              ;; You can bring your own non-default interceptors. Make
              ;; sure you include routing and set it up right for
              ;; dev-mode. If you do, many other keys for configuring
              ;; default interceptors will be ignored.
              ;; ::bootstrap/interceptors []
              ::bootstrap/routes routes

              ;; Uncomment next line to enable CORS support, add
              ;; string(s) specifying scheme, host and port for
              ;; allowed source(s):
              ;;
              ;; "http://localhost:8080"
              ;;
              ;;::bootstrap/allowed-origins ["scheme://host:port"]

              ;; Root for resource interceptor that is available by default.
              ::bootstrap/resource-path "/public"

              ;; Either :jetty, :immutant or :tomcat (see comments in project.clj)
              ::bootstrap/type :jetty
              ;;::bootstrap/host "localhost"
              ::bootstrap/port (Integer. (or (System/getenv "PORT") 5000))})
