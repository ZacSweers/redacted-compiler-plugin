// Copyright (C) 2026 Zac Sweers
// SPDX-License-Identifier: Apache-2.0
import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
  kotlin("multiplatform")
  id("dev.zacsweers.redacted")
}

kotlin {
  jvm {
    compilations.configureEach {
      compileTaskProvider.configure {
        compilerOptions {
          jvmTarget.set(JvmTarget.JVM_11)
          freeCompilerArgs.add("-Xstring-concat=${project.findProperty("string_concat")}")
        }
      }
    }
  }
  js {
    nodejs { testTask { useMocha { timeout = "30s" } } }
    browser()
    binaries.executable()
  }

  @OptIn(ExperimentalWasmDsl::class)
  wasmJs {
    binaries.executable()
    browser {}
  }
  linuxX64()
  macosArm64()
  iosSimulatorArm64()

  sourceSets {
    commonMain { dependencies { implementation(project(":redacted-compiler-plugin-annotations")) } }
    commonTest {
      dependencies {
        implementation(libs.kotlin.test)
        implementation("io.ktor:ktor-utils:3.5.1") { because("For PlatformUtils use") }
      }
    }
  }
}

configurations.configureEach {
  resolutionStrategy.eachDependency {
    if (
      requested.group == "org.jetbrains.kotlinx" &&
        requested.name in setOf("kotlinx-io-core", "kotlinx-io-bytestring")
    ) {
      useVersion(libs.versions.kotlinx.io.get())
      because("kotlinx-io 0.9.0 emits Wasm JS that is not browser-module compatible")
    }
  }

  resolutionStrategy.dependencySubstitution {
    substitute(module("dev.zacsweers.redacted:redacted-compiler-plugin-annotations"))
      .using(project(":redacted-compiler-plugin-annotations"))
    substitute(module("dev.zacsweers.redacted:redacted-compiler-plugin-annotations-jvm"))
      .using(project(":redacted-compiler-plugin-annotations"))
    substitute(module("dev.zacsweers.redacted:redacted-compiler-plugin"))
      .using(project(":redacted-compiler-plugin"))
  }
}
