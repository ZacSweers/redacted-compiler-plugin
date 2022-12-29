import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
  kotlin("jvm")
  id("org.jetbrains.dokka")
  id("com.vanniktech.maven.publish")
  id("com.google.devtools.ksp")
}

tasks.withType<KotlinCompile>().configureEach {
  compilerOptions { freeCompilerArgs.add("-opt-in=kotlin.RequiresOptIn") }
}

tasks.withType<Test>().configureEach {
  systemProperty("rdt.jvmTarget", libs.versions.jvmTarget.get())
}

dependencies {
  compileOnly(libs.kotlin.compilerEmbeddable)
  implementation(libs.autoService)
  ksp(libs.autoService.ksp)

  testImplementation(libs.kotlin.reflect)
  testImplementation(libs.kotlin.compilerEmbeddable)
  // Cover for https://github.com/tschuchortdev/kotlin-compile-testing/issues/274
  testImplementation(libs.kotlin.aptEmbeddable)
  testImplementation(libs.kotlinCompileTesting)
  testImplementation(libs.junit)
  testImplementation(libs.truth)
}
