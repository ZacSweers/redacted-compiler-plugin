Releasing
=========

1. Change the version in `gradle.properties` to a non-SNAPSHOT version.
2. Update `CHANGELOG.md` for the impending release.
3. `git commit -am "Prepare for release X.Y.Z."` (where X.Y.Z is the new version)
4. `git tag -a X.Y.Z -m "Version X.Y.Z"` (where X.Y.Z is the new version)
5. `./gradlew publish --no-parallel -x dokkaHtml && ./gradlew closeAndReleaseRepository`
6. Update `gradle.properties` to the next SNAPSHOT version.
7. `git commit -am "Prepare next development version."`
8. `git push && git push --tags`
