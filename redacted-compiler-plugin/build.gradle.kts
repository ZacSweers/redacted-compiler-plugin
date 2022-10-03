import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
  kotlin("jvm")
  id("org.jetbrains.dokka")
  id("com.vanniktech.maven.publish")
  id("com.google.devtools.ksp")
}

tasks.withType<KotlinCompile>().configureEach {
  kotlinOptions {
    @Suppress("SuspiciousCollectionReassignment")
    freeCompilerArgs += "-opt-in=kotlin.RequiresOptIn"
  }
}

dependencies {
  compileOnly(libs.kotlin.compilerEmbeddable)
  implementation(libs.autoService)
  ksp(libs.autoService.ksp)

  testImplementation(libs.kotlin.reflect)
  testImplementation(libs.kotlin.compilerEmbeddable)
  // Cover for https://github.com/tschuchortdev/kotlin-compile-testing/issues/274
  testImplementation("org.jetbrains.kotlin:kotlin-annotation-processing-embeddable:1.7.20-Beta")
  testImplementation(libs.kotlinCompileTesting)
  testImplementation(libs.junit)
  testImplementation(libs.truth)
}
