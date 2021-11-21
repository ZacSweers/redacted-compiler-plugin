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

includeBuild("redacted-compiler-plugin-gradle") {
  dependencySubstitution {
    substitute(module("dev.zacsweers.redacted:redacted-compiler-plugin-gradle")).using(project(":"))
  }
}
