(ns clojure-microservices.service
  (:require [io.pedestal.http :as bootstrap]
            [io.pedestal.http.route :as route]
            [io.pedestal.http.body-params :as body-params]
            [io.pedestal.http.route.definition :refer [defroutes]]
            [ring.util.response :as ring-resp]))

(defn about-page
  [request]
  (ring-resp/response (format "Clojure %s - served from %s"
                              (clojure-version)
                              (route/url-for ::about-page))))

(defn home-page
  [request]
  (prn request)
  (ring-resp/response "Hello World!"))

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
  (bootstrap/json-response mock-books-collection)
)

(defn add-book
  [request]
  (prn (:json-params request))
      (ring-resp/created "http://fake-201-url" "fake 201 in the body")
)

(defn get-book
  [request]
  (let [bookname (get-in request [:path-params :book-name])]
    (bootstrap/json-response ((keyword bookname) mock-books-collection))
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
     ["/books/:book-name" {:get get-book}]
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
