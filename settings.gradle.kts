// Copyright (C) 2026 Zac Sweers
// SPDX-License-Identifier: Apache-2.0
pluginManagement {
  repositories {
    google()
    mavenCentral()
    maven("https://central.sonatype.com/repository/maven-snapshots/") {
      name = "central-portal-snapshots"
      mavenContent { snapshotsOnly() }
    }
    gradlePluginPortal()
  }
  plugins { id("com.gradle.develocity") version "4.5.0" }
}

dependencyResolutionManagement {
  repositories {
    google()
    mavenCentral()
    maven("https://central.sonatype.com/repository/maven-snapshots/") {
      name = "central-portal-snapshots"
      mavenContent { snapshotsOnly() }
    }
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
