#!/bin/bash

if [[ "$1" = "--snapshot" ]]; then snapshot=true; fi
if [[ "$1" = "--local" ]]; then local=true; fi

if ! [[ ${local} ]]; then
  ./gradlew -p redacted-compiler-plugin-gradle clean publish --no-daemon --no-parallel -x dokkaHtml
  ./gradlew clean publish --no-daemon --no-parallel -x dokkaHtml
  if ! [[ ${snapshot} ]]; then
    ./gradlew closeAndReleaseRepository
  fi
else
  ./gradlew -p redacted-compiler-plugin-gradle clean install --no-daemon --no-parallel -x dokkaHtml
  ./gradlew clean install --no-daemon --no-parallel -x dokkaHtml
fi