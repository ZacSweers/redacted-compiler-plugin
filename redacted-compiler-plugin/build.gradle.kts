import com.diffplug.gradle.spotless.SpotlessTask

plugins {
  alias(libs.plugins.kotlin.jvm)
  alias(libs.plugins.dokka)
  `java-test-fixtures`
  alias(libs.plugins.mavenPublish)
  alias(libs.plugins.buildConfig)
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

kotlin { compilerOptions.freeCompilerArgs.add("-Xcontext-parameters") }

val redactedRuntime by configurations.dependencyScope("redactedRuntime") { isTransitive = false }

val redactedRuntimeClasspath =
  configurations.resolvable("redactedRuntimeClasspath") {
    isTransitive = false
    extendsFrom(redactedRuntime)
  }

dependencies {
  compileOnly(libs.kotlin.compiler)

  testFixturesApi(libs.kotlin.testJunit5)
  testFixturesApi(libs.kotlin.compilerTestFramework)
  testFixturesApi(libs.kotlin.compiler)

  redactedRuntime(project(":redacted-compiler-plugin-annotations"))

  // Dependencies required to run the internal test framework.
  testRuntimeOnly(libs.junit)
  testRuntimeOnly(libs.kotlin.reflect)
  testRuntimeOnly(libs.kotlin.test)
  testRuntimeOnly(libs.kotlin.scriptRuntime)
  testRuntimeOnly(libs.kotlin.annotationsJvm)
}

buildConfig {
  useKotlinOutput { internalVisibility = true }

  packageName("dev.zacsweers.redacted")
  buildConfigField("String", "KOTLIN_PLUGIN_ID", "\"dev.zacsweers.redacted.compiler\"")
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

tasks.withType<SpotlessTask>().configureEach { dependsOn(generateTests) }

fun Test.setLibraryProperty(propName: String, jarName: String) {
  val path =
    project.configurations.testRuntimeClasspath
      .get()
      .files
      .find { """$jarName-\d.*jar""".toRegex().matches(it.name) }
      ?.absolutePath ?: return
  systemProperty(propName, path)
}
