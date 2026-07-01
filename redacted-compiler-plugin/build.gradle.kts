// Copyright (C) 2026 Zac Sweers
// SPDX-License-Identifier: Apache-2.0
import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import org.gradle.api.attributes.java.TargetJvmVersion
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
  alias(libs.plugins.kotlin.jvm)
  alias(libs.plugins.dokka)
  `java-test-fixtures`
  alias(libs.plugins.mavenPublish)
  alias(libs.plugins.buildConfig)
  alias(libs.plugins.shadow) apply false
  idea
}

sourceSets {
  test {
    java.setSrcDirs(listOf("test-gen/java"))
    kotlin.setSrcDirs(listOf("test/kotlin"))
    resources.setSrcDirs(listOf("testData"))
  }
}

idea { module.generatedSourceDirs.add(projectDir.resolve("test-gen/java")) }

java {
  sourceCompatibility = JavaVersion.VERSION_21
  targetCompatibility = JavaVersion.VERSION_21
}

kotlin {
  compilerOptions {
    jvmTarget.set(JvmTarget.JVM_21)
    optIn.add("org.jetbrains.kotlin.compiler.plugin.ExperimentalCompilerApi")
  }
}

tasks.withType<KotlinCompile>().configureEach {
  compilerOptions { jvmTarget.set(JvmTarget.JVM_21) }
}

tasks.withType<JavaCompile>().configureEach { options.release.set(21) }

val redactedRuntime by configurations.dependencyScope("redactedRuntime") { isTransitive = false }

val redactedRuntimeClasspath =
  configurations.resolvable("redactedRuntimeClasspath") {
    isTransitive = false
    extendsFrom(redactedRuntime)
  }

val embedded = configurations.dependencyScope("embedded")

val embeddedClasspath = configurations.resolvable("embeddedClasspath") { extendsFrom(embedded) }

val testKotlinVersion = providers.gradleProperty("kotlinVersion").orElse(libs.versions.kotlin)

configurations.named("compileOnly").configure { extendsFrom(embedded) }

configurations.named("testFixturesCompileOnly").configure { extendsFrom(embedded) }

configurations.named("testImplementation").configure { extendsFrom(embedded) }

listOf(
    "compileClasspath",
    "embeddedClasspath",
    "testCompileClasspath",
    "testFixturesCompileClasspath",
    "testRuntimeClasspath",
  )
  .forEach { configurationName ->
    configurations.named(configurationName) {
      attributes.attribute(TargetJvmVersion.TARGET_JVM_VERSION_ATTRIBUTE, 21)
    }
  }

dependencies {
  compileOnly(libs.kotlin.compiler)

  add(embedded.name, libs.metro.compilerCompat.k2420Beta1)

  testKotlin("testFixturesApi", "kotlin-test-junit5")
  testKotlin("testFixturesApi", "kotlin-compiler-internal-test-framework")
  testKotlin("testFixturesApi", "kotlin-compiler")

  redactedRuntime(project(":redacted-compiler-plugin-annotations"))

  // Dependencies required to run the internal test framework.
  testRuntimeOnly(libs.junit)
  testKotlin("testRuntimeOnly", "kotlin-reflect")
  testKotlin("testRuntimeOnly", "kotlin-test")
  testKotlin("testRuntimeOnly", "kotlin-script-runtime")
  testKotlin("testRuntimeOnly", "kotlin-annotations-jvm")
}

buildConfig {
  useKotlinOutput { internalVisibility = true }

  packageName("dev.zacsweers.redacted")
  buildConfigField("String", "KOTLIN_PLUGIN_ID", "\"dev.zacsweers.redacted.compiler\"")
}

tasks.jar.configure { enabled = false }

val shadowJar =
  tasks.register("shadowJar", ShadowJar::class.java) {
    from(java.sourceSets.main.map { it.output })
    configurations.add(embeddedClasspath)

    duplicatesStrategy = DuplicatesStrategy.INCLUDE
    mergeServiceFiles()

    exclude("module-info.class")
    exclude("META-INF/versions/*/module-info.class")

    dependencies {
      exclude(dependency("org.jetbrains:.*"))
      exclude(dependency("org.intellij:.*"))
      exclude(dependency("org.jetbrains.kotlin:.*"))
    }

    relocate(
      "dev.zacsweers.metro.compiler.compat",
      "dev.zacsweers.redacted.shaded.dev.zacsweers.metro.compiler.compat",
    )
  }

for (configurationName in arrayOf("apiElements", "runtimeElements")) {
  configurations.named(configurationName) { artifacts.removeIf { true } }
  artifacts.add(configurationName, shadowJar)
}

tasks.test {
  dependsOn(redactedRuntimeClasspath)
  val redactedRuntimeClasspath = redactedRuntimeClasspath.map { it.asPath }

  useJUnitPlatform()
  workingDir = rootDir

  systemProperty("rcp.jvmTarget", libs.versions.jvmTarget.get())

  doFirst { systemProperty("redactedRuntime.classpath", redactedRuntimeClasspath.get()) }

  // Properties required to run the internal test framework.
  setLibraryProperty("org.jetbrains.kotlin.test.kotlin-stdlib", "kotlin-stdlib")
  setLibraryProperty("org.jetbrains.kotlin.test.kotlin-stdlib-jdk8", "kotlin-stdlib-jdk8")
  setLibraryProperty("org.jetbrains.kotlin.test.kotlin-reflect", "kotlin-reflect")
  setLibraryProperty("org.jetbrains.kotlin.test.kotlin-test", "kotlin-test")
  setLibraryProperty("org.jetbrains.kotlin.test.kotlin-script-runtime", "kotlin-script-runtime")
  setLibraryProperty("org.jetbrains.kotlin.test.kotlin-annotations-jvm", "kotlin-annotations-jvm")

  systemProperty("idea.ignore.disabled.plugins", "true")
  systemProperty("idea.home.path", rootDir)
}

val generateTests =
  tasks.register<JavaExec>("generateTests") {
    inputs
      .dir(layout.projectDirectory.dir("testData"))
      .withPropertyName("testData")
      .withPathSensitivity(PathSensitivity.RELATIVE)
    outputs.dir(layout.projectDirectory.dir("test-gen")).withPropertyName("generatedTests")

    classpath = sourceSets.testFixtures.get().runtimeClasspath
    mainClass.set("dev.zacsweers.redacted.compiler.GenerateTestsKt")
    workingDir = rootDir
  }

tasks.compileTestKotlin { dependsOn(generateTests) }

fun Test.setLibraryProperty(propName: String, jarName: String) {
  val path =
    project.configurations.testRuntimeClasspath
      .get()
      .files
      .find { """$jarName-\d.*jar""".toRegex().matches(it.name) }
      ?.absolutePath ?: return
  systemProperty(propName, path)
}

fun DependencyHandler.testKotlin(configurationName: String, moduleName: String) {
  addProvider(configurationName, testKotlinVersion.map { "org.jetbrains.kotlin:$moduleName:$it" })
}
