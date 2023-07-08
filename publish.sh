#!/bin/bash

if [[ "$1" = "--local" ]]; then local=true; fi

if ! [[ ${local} ]]; then
  cd redacted-compiler-plugin-gradle || exit
  ./gradlew publish -x dokkaHtml
  cd ..
  ./gradlew publish -x dokkaHtml
else
  cd redacted-compiler-plugin-gradle || exit
  ./gradlew publishToMavenLocal -x dokkaHtml
  cd ..
  ./gradlew publishToMavenLocal -x dokkaHtml
fi