import com.vanniktech.maven.publish.JavadocJar
import com.vanniktech.maven.publish.KotlinMultiplatform
import com.vanniktech.maven.publish.MavenPublishBaseExtension
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension

plugins {
  kotlin("multiplatform")
  id("org.jetbrains.dokka")
  id("com.vanniktech.maven.publish.base")
}

/*
 * Here's the main hierarchy of variants. Any `expect` functions in one level of the tree are
 * `actual` functions in a (potentially indirect) child node.
 *
 * ```
 *   common
 *   |-- jvm
 *   |-- js
 *   '-- native
 *       |- unix
 *       |   |-- apple
 *       |   |   |-- iosArm64
 *       |   |   |-- iosX64
 *       |   |   |-- macosX64
 *       |   |   |-- tvosArm64
 *       |   |   |-- tvosX64
 *       |   |   |-- watchosArm32
 *       |   |   |-- watchosArm64
 *       |   |   '-- watchosX86
 *       |   '-- linux
 *       |       '-- linuxX64
 *       '-- mingw
 *           '-- mingwX64
 * ```
 *
 * Every child of `unix` also includes a source set that depends on the pointer size:
 *
 *  * `sizet32` for watchOS, including watchOS 64-bit architectures
 *  * `sizet64` for everything else
 */
val kmpNativeEnabled = System.getProperty("knative", "true").toBoolean()
val kmpJsEnabled = System.getProperty("kjs", "true").toBoolean()
kotlin {
  jvm()
  if (kmpJsEnabled) {
    js(BOTH) {
      compilations.all {
        kotlinOptions {
          moduleKind = "umd"
          sourceMap = true
          metaInfo = true
        }
      }
      nodejs {
        testTask {
          useMocha {
            timeout = "30s"
          }
        }
      }
      browser {
      }
    }
  }
  if (kmpNativeEnabled) {
    configureOrCreateNativePlatforms()
  }
}

fun KotlinMultiplatformExtension.configureOrCreateNativePlatforms() {
  iosX64()
  iosArm64()
  iosSimulatorArm64()
  tvosX64()
  tvosArm64()
  tvosSimulatorArm64()
  watchosArm32()
  watchosArm64()
  watchosX86()
  watchosX64()
  watchosSimulatorArm64()
  // Required to generate tests tasks: https://youtrack.jetbrains.com/issue/KT-26547
  linuxX64()
  macosX64()
  macosArm64()
  mingwX64()
}

val appleTargets = listOf(
  "iosArm64",
  "iosX64",
  "iosSimulatorArm64",
  "macosX64",
  "macosArm64",
  "tvosArm64",
  "tvosX64",
  "tvosSimulatorArm64",
  "watchosArm32",
  "watchosArm64",
  "watchosX86",
  "watchosX64",
  "watchosSimulatorArm64"
)

val mingwTargets = listOf(
  "mingwX64"
)

val linuxTargets = listOf(
  "linuxX64"
)

val nativeTargets = appleTargets + linuxTargets + mingwTargets

/** Note that size_t is 32-bit on legacy watchOS versions (ie. pointers are always 32-bit). */
val unixSizet32Targets = listOf(
  "watchosArm32",
  "watchosArm64",
  "watchosX86"
)

val unixSizet64Targets = listOf(
  "iosArm64",
  "iosX64",
  "iosSimulatorArm64",
  "linuxX64",
  "macosX64",
  "macosArm64",
  "tvosArm64",
  "tvosX64",
  "tvosSimulatorArm64",
  "watchosSimulatorArm64",
  "watchosX64"
)

configure<MavenPublishBaseExtension> {
  configure(
    KotlinMultiplatform(javadocJar = JavadocJar.Dokka("dokkaGfm"))
  )
}

// https://youtrack.jetbrains.com/issue/KT-46978
tasks.withType<ProcessResources>().all {
  when (name) {
    "jvmTestProcessResources", "jvmProcessResources" -> {
      duplicatesStrategy = DuplicatesStrategy.WARN
    }
  }
}