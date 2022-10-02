import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
  kotlin("jvm")
  id("dev.zacsweers.redacted")
}

tasks.withType<KotlinCompile>().configureEach {
  kotlinOptions {
    useK2 = project.findProperty("rcp.useK2")?.toString().toBoolean()
    jvmTarget = "11"
    @Suppress("SuspiciousCollectionReassignment")
    freeCompilerArgs += "-Xstring-concat=${project.findProperty("string_concat")}"
  }
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
