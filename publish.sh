#!/bin/bash

if [[ "$1" = "--local" ]]; then local=true; fi

if ! [[ ${local} ]]; then
  ./gradlew -p redacted-compiler-plugin-gradle publish -x dokkaHtml
  ./gradlew publish -x dokkaHtml
else
  ./gradlew -p redacted-compiler-plugin-gradle publishToMavenLocal -x dokkaHtml
  ./gradlew publishToMavenLocal -x dokkaHtml
fi