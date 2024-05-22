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
  sourceSets {
    commonMain { dependencies { implementation(project(":redacted-compiler-plugin-annotations")) } }
    getByName("jvmTest") {
      dependencies {
        implementation(libs.junit)
        implementation(libs.truth)
      }
    }
  }
}

configurations.configureEach {
  resolutionStrategy.dependencySubstitution {
    substitute(module("dev.zacsweers.redacted:redacted-compiler-plugin-annotations"))
      .using(project(":redacted-compiler-plugin-annotations"))
    substitute(module("dev.zacsweers.redacted:redacted-compiler-plugin-annotations-jvm"))
      .using(project(":redacted-compiler-plugin-annotations"))
    substitute(module("dev.zacsweers.redacted:redacted-compiler-plugin"))
      .using(project(":redacted-compiler-plugin"))
  }
}
