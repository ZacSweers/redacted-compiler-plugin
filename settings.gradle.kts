pluginManagement {
  repositories {
    google()
    mavenCentral()
    gradlePluginPortal()
  }
  plugins { id("com.gradle.develocity") version "4.3.1" }
}

dependencyResolutionManagement {
  repositories {
    google()
    mavenCentral()
  }
}

plugins { id("com.gradle.develocity") }

rootProject.name = "redacted-compiler-plugin"

include(
  ":redacted-compiler-plugin",
  ":redacted-compiler-plugin-annotations",
  ":sample",
  ":sample-jvm",
)

includeBuild("redacted-compiler-plugin-gradle") {
  dependencySubstitution {
    substitute(module("dev.zacsweers.redacted:redacted-compiler-plugin-gradle")).using(project(":"))
  }
}

develocity {
  buildScan {
    termsOfUseUrl = "https://gradle.com/terms-of-service"
    termsOfUseAgree = "yes"

    tag(if (System.getenv("CI").isNullOrBlank()) "Local" else "CI")

    obfuscation {
      username { "Redacted" }
      hostname { "Redacted" }
      ipAddresses { addresses -> addresses.map { "0.0.0.0" } }
    }
  }
}
