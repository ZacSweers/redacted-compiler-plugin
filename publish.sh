#!/bin/bash

if [[ "$1" = "--local" ]]; then local=true; fi

if ! [[ ${local} ]]; then
  cd redacted-compiler-plugin-gradle || exit
  ./gradlew publish --no-configuration-cache
  cd ..
  ./gradlew publish --no-configuration-cache
else
  cd redacted-compiler-plugin-gradle || exit
  ./gradlew publishToMavenLocal
  cd ..
  ./gradlew publishToMavenLocal
fi