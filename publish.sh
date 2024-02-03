#!/bin/bash

if [[ "$1" = "--local" ]]; then local=true; fi

if ! [[ ${local} ]]; then
  cd redacted-compiler-plugin-gradle || exit
  ./gradlew publish -x dokkaHtml --no-configuration-cache
  cd ..
  ./gradlew publish -x dokkaHtml --no-configuration-cache
else
  cd redacted-compiler-plugin-gradle || exit
  ./gradlew publishToMavenLocal -x dokkaHtml
  cd ..
  ./gradlew publishToMavenLocal -x dokkaHtml
fi