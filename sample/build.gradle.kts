plugins {
  kotlin("multiplatform")
  id("dev.zacsweers.redacted")
}

kotlin {
  jvm {
    compilations.configureEach {
      kotlinOptions {
        jvmTarget = "11"
        @Suppress("SuspiciousCollectionReassignment")
        freeCompilerArgs += "-Xstring-concat=${project.findProperty("string_concat")}"
      }
    }
  }
  sourceSets {
    commonMain { dependencies { implementation(project(":redacted-compiler-plugin-annotations")) } }
    val jvmTest by getting {
      dependencies {
        implementation("junit:junit:4.13.2")
        implementation("com.google.truth:truth:1.1.3")
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
