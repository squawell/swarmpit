(ns swarmpit.component.service.form-settings
  (:require [material.component :as comp]
            [material.component.form :as form]
            [swarmpit.component.state :as state]
            [swarmpit.component.handler :as handler]
            [swarmpit.component.service.form-ports :as ports]
            [swarmpit.routes :as routes]
            [rum.core :as rum]))

(enable-console-print!)

(def cursor [:form :settings])

(defonce isValid (atom false))

(defonce tags (atom []))

(defn public-tags-handler
  [repository]
  (handler/get
    (routes/path-for-backend :public-repository-tags)
    {:params     {:repository repository}
     :on-success (fn [response]
                   (reset! tags response))}))

(defn dockerhub-tags-handler
  [distribution repository]
  (handler/get
    (routes/path-for-backend :dockerhub-repository-tags {:id distribution})
    {:params     {:repository repository}
     :on-success (fn [response]
                   (reset! tags response))}))

(defn registry-tags-handler
  [distribution repository]
  (handler/get
    (routes/path-for-backend :registry-repository-tags {:id distribution})
    {:params     {:repository repository}
     :on-success (fn [response]
                   (reset! tags response))}))

(defn tags-handler
  [distributionType distribution repository]
  (case distributionType
    "dockerhub" (dockerhub-tags-handler distribution repository)
    "registry" (registry-tags-handler distribution repository)
    (public-tags-handler repository)))

(def form-mode-style
  {:display   "flex"
   :marginTop "14px"})

(def form-image-style
  {:color "rgb(117, 117, 117)"})

(defn- form-image [value]
  (form/comp
    "IMAGE"
    (comp/vtext-field
      {:name          "image"
       :key           "image"
       :required      true
       :disabled      true
       :underlineShow false
       :inputStyle    form-image-style
       :value         value})))

(defn- form-image-tag-with-port-load [value tags distribution]
  "Preload ports for services created via swarmpit"
  (form/comp
    "IMAGE TAG"
    (comp/autocomplete {:name          "imageTagAuto"
                        :key           "imageTagAuto"
                        :searchText    (:tag value)
                        :onUpdateInput (fn [v] (state/update-value [:repository :tag] v cursor))
                        :onNewRequest  (fn [_] (ports/load-suggestable-ports distribution value))
                        :dataSource    tags})))

(defn- form-image-tag [value tags]
  "For services created by docker cli there is no port preload"
  (form/comp
    "IMAGE TAG"
    (comp/autocomplete
      {:name          "image-tag"
       :key           "image-tag"
       :searchText    (:tag value)
       :onUpdateInput (fn [v] (state/update-value [:repository :tag] v cursor))
       :dataSource    tags})))

(defn- form-name [value update-form?]
  (form/comp
    "SERVICE NAME"
    (comp/vtext-field
      {:name     "service-name"
       :key      "service-name"
       :required true
       :disabled update-form?
       :value    value
       :onChange (fn [_ v]
                   (state/update-value [:serviceName] v cursor))})))

(defn- form-mode [value update-form?]
  (form/comp
    "MODE"
    (comp/radio-button-group
      {:name          "mode"
       :key           "mode"
       :style         form-mode-style
       :valueSelected value
       :onChange      (fn [_ v]
                        (state/update-value [:mode] v cursor))}
      (comp/radio-button
        {:name     "replicated-mode"
         :key      "replicated-mode"
         :disabled update-form?
         :label    "Replicated"
         :value    "replicated"})
      (comp/radio-button
        {:name     "global-mode"
         :key      "global-mode"
         :disabled update-form?
         :label    "Global"
         :value    "global"}))))

(defn- form-replicas [value]
  (form/comp
    "REPLICAS"
    (comp/vtext-field
      {:name     "replicas"
       :key      "replicas"
       :required true
       :type     "number"
       :min      0
       :value    value
       :onChange (fn [_ v]
                   (state/update-value [:replicas] (js/parseInt v) cursor))})))

(rum/defc form < rum/reactive [update-form?]
  (let [{:keys [distribution
                repository
                serviceName
                mode
                replicas]} (state/react cursor)]
    [:div.form-edit
     (form/form
       {:onValid   #(reset! isValid true)
        :onInvalid #(reset! isValid false)}
       (form-image (:name repository))
       (if update-form?
         (form-image-tag repository (rum/react tags))
         (form-image-tag-with-port-load repository (rum/react tags) distribution))
       (form-name serviceName update-form?)
       (form-mode mode update-form?)
       (when (= "replicated" mode)
         (form-replicas replicas)))]))