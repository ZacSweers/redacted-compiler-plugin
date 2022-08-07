pluginManagement {
  repositories {
    google()
    mavenCentral()
    gradlePluginPortal()
  }
}

rootProject.name = "redacted-compiler-plugin"

include(":redacted-compiler-plugin")

include(":redacted-compiler-plugin-annotations")

include(":sample")
include(":sample-jvm")

includeBuild("redacted-compiler-plugin-gradle") {
  dependencySubstitution {
    substitute(module("dev.zacsweers.redacted:redacted-compiler-plugin-gradle")).using(project(":"))
  }
}

enableFeaturePreview("VERSION_CATALOGS")
