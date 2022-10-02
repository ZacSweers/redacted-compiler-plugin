plugins {
  kotlin("jvm")
  id("dev.zacsweers.redacted")
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>().configureEach {
  kotlinOptions {
    useK2 = true
    jvmTarget = "11"
    @Suppress("SuspiciousCollectionReassignment")
    freeCompilerArgs += "-Xstring-concat=${project.findProperty("string_concat")}"
  }
  compilerExecutionStrategy.set(
      org.jetbrains.kotlin.gradle.tasks.KotlinCompilerExecutionStrategy.IN_PROCESS)
}

dependencies {
  implementation(project(":redacted-compiler-plugin-annotations"))
  testImplementation(libs.junit)
  testImplementation(libs.truth)
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
