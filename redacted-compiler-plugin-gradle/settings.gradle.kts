// Copyright (C) 2026 Zac Sweers
// SPDX-License-Identifier: Apache-2.0
pluginManagement {
  repositories {
    google()
    mavenCentral()
    gradlePluginPortal()
  }
}

dependencyResolutionManagement {
  versionCatalogs { maybeCreate("libs").apply { from(files("../gradle/libs.versions.toml")) } }
  repositories { mavenCentral() }
}
