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
  js(IR) {
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
  macosX64()
  macosArm64()
  iosSimulatorArm64()
  iosX64()

  sourceSets {
    commonMain { dependencies { implementation(project(":redacted-compiler-plugin-annotations")) } }
    commonTest {
      dependencies {
        implementation(libs.kotlin.test)
        implementation("io.ktor:ktor-utils:3.3.0") { because("For PlatformUtils use") }
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
