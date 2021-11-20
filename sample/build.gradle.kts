import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
  kotlin("jvm")
  id("dev.zacsweers.redacted")
}

tasks.withType<KotlinCompile>().configureEach {
  kotlinOptions {
    jvmTarget = "11"
    freeCompilerArgs += "-Xstring-concat=${project.findProperty("string_concat")}"
  }
}

configurations.configureEach {
  resolutionStrategy.dependencySubstitution {
    substitute(module("dev.zacsweers.redacted:redacted-compiler-plugin-annotations")).using(project(":redacted-compiler-plugin-annotations"))
    substitute(module("dev.zacsweers.redacted:redacted-compiler-plugin")).using(project(":redacted-compiler-plugin"))
  }
}

dependencies {
  testImplementation("junit:junit:4.13.2")
  testImplementation("com.google.truth:truth:1.1.3")
}
