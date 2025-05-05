plugins {
  kotlin("jvm")
  id("org.jetbrains.dokka")
  id("com.vanniktech.maven.publish")
}

tasks.withType<Test>().configureEach {
  systemProperty("rdt.jvmTarget", libs.versions.jvmTarget.get())
}

kotlin { compilerOptions.freeCompilerArgs.add("-Xcontext-parameters") }

dependencies {
  compileOnly(libs.kotlin.compilerEmbeddable)
  compileOnly(libs.kotlin.stdlib)

  testImplementation(libs.kotlin.reflect)
  testImplementation(libs.kotlin.stdlib)
  testImplementation(libs.kotlin.compilerEmbeddable)
  // Cover for https://github.com/tschuchortdev/kotlin-compile-testing/issues/274
  testImplementation(libs.kotlin.aptEmbeddable)
  testImplementation(libs.kotlinCompileTesting)
  testImplementation(libs.junit)
  testImplementation(libs.truth)
}
