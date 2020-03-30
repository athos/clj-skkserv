# clj-skkserv
[![GitHub Actions](https://github.com/athos/clj-skkserv/workflows/build/badge.svg)](https://github.com/athos/clj-skkserv/actions?query=workflow%3Abuild)

An skkserv server framework in Clojure, heavily inspired by Ring architecture

## Usage

```clojure
(ns example.core
  (:require [clj-skkserv.core :as skkserv]
            [clj-skkserv.middleware :refer [wrap-conversion-only]]))

(def handler
  (-> (fn [type content]
        (case content
          "rich" ["Rich Hickey"]
          "alex" ["Alex Miller"]
          nil))
      wrap-conversion-only))

;; start server
(def server (skkserv/start-server handler {:port 1178 :join? false}))

;; stop server
(.close server)
```

Or just run:

```sh
$ clj -m clj-skkserv.main --handler example.core/handler --port 1178
```

## License

Copyright © 2019 Shogo Ohta

This program and the accompanying materials are made available under the
terms of the Eclipse Public License 2.0 which is available at
http://www.eclipse.org/legal/epl-2.0.

This Source Code may also be made available under the following Secondary
Licenses when the conditions for such availability set forth in the Eclipse
Public License, v. 2.0 are satisfied: GNU General Public License as published by
the Free Software Foundation, either version 2 of the License, or (at your
option) any later version, with the GNU Classpath Exception which is available
at https://www.gnu.org/software/classpath/license.html.
