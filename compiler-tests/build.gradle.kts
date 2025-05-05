// Copyright (C) 2025 Zac Sweers
// SPDX-License-Identifier: Apache-2.0
plugins {
  alias(libs.plugins.kotlin.jvm)
  java
}

val redactedRuntimeClasspath: Configuration by configurations.creating { isTransitive = false }

dependencies {
  testImplementation(project(":redacted-compiler-plugin"))

  testImplementation(libs.kotlin.testJunit5)
  testImplementation(libs.kotlin.compilerTestFramework)
  testImplementation(libs.kotlin.compiler)

  redactedRuntimeClasspath(project(":redacted-compiler-plugin-annotations"))

  // Dependencies required to run the internal test framework.
  testRuntimeOnly(libs.kotlin.reflect)
  testRuntimeOnly(libs.kotlin.test)
  testRuntimeOnly(libs.kotlin.scriptRuntime)
  testRuntimeOnly(libs.kotlin.annotationsJvm)
}

tasks.register<JavaExec>("generateTests") {
  inputs
    .dir(layout.projectDirectory.dir("src/test/data"))
    .withPropertyName("testData")
    .withPathSensitivity(PathSensitivity.RELATIVE)
  outputs.dir(layout.projectDirectory.dir("src/test/java")).withPropertyName("generatedTests")

  classpath = sourceSets.test.get().runtimeClasspath
  mainClass.set("dev.zacsweers.redacted.compiler.GenerateTestsKt")
  workingDir = rootDir
}

tasks.withType<Test> {
  dependsOn(redactedRuntimeClasspath)
  inputs
    .dir(layout.projectDirectory.dir("src/test/data"))
    .withPropertyName("testData")
    .withPathSensitivity(PathSensitivity.RELATIVE)

  workingDir = rootDir

  useJUnitPlatform()

  systemProperty("rdt.jvmTarget", libs.versions.jvmTarget.get())
  systemProperty("redactedRuntime.classpath", redactedRuntimeClasspath.asPath)

  // Properties required to run the internal test framework.
  systemProperty("idea.ignore.disabled.plugins", "true")
  systemProperty("idea.home.path", rootDir)
}
