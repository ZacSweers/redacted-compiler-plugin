import com.vanniktech.maven.publish.MavenPublishBaseExtension
import com.vanniktech.maven.publish.SonatypeHost
import org.jetbrains.dokka.gradle.DokkaTask
import org.jetbrains.kotlin.gradle.targets.js.nodejs.NodeJsRootExtension
import org.jetbrains.kotlin.gradle.targets.js.nodejs.NodeJsRootPlugin
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

buildscript {
  dependencies {
    // Include our included build
    classpath("dev.zacsweers.redacted:redacted-compiler-plugin-gradle")
  }
}

plugins {
  alias(libs.plugins.kotlin.jvm) apply false
  alias(libs.plugins.dokka) apply false
  alias(libs.plugins.ksp) apply false
  alias(libs.plugins.mavenPublish) apply false
  alias(libs.plugins.spotless)
  alias(libs.plugins.binaryCompatibilityValidator)
}

plugins.withType<NodeJsRootPlugin>().configureEach {
  // 16+ required for Apple Silicon support
  // https://youtrack.jetbrains.com/issue/KT-49109#focus=Comments-27-5259190.0-0
  the<NodeJsRootExtension>().nodeVersion = "18.0.0"
}

apiValidation { ignoredProjects += listOf("sample", "sample-jvm") }

spotless {
  format("misc") {
    target("*.gradle", "*.md", ".gitignore")
    trimTrailingWhitespace()
    indentWithSpaces(2)
    endWithNewline()
  }
  kotlin {
    target("**/*.kt")
    ktfmt(libs.versions.ktfmt.get())
    trimTrailingWhitespace()
    endWithNewline()
    licenseHeaderFile("spotless/spotless.kt")
    targetExclude("**/spotless.kt", "**/build/**")
  }
  kotlinGradle {
    target("**/*.kts")
    ktfmt(libs.versions.ktfmt.get())
    trimTrailingWhitespace()
    endWithNewline()
  }
}

val javaTarget = libs.versions.jvmTarget.get().removePrefix("1.").toInt()

allprojects {
  group = project.property("GROUP") as String
  version = project.property("VERSION_NAME") as String

  repositories {
    google()
    mavenCentral()
  }

  pluginManager.withPlugin("java") {
    configure<JavaPluginExtension> {
      toolchain { languageVersion.set(JavaLanguageVersion.of(libs.versions.jdk.get().toInt())) }
    }
    tasks.withType<JavaCompile>().configureEach { options.release.set(javaTarget) }
  }

  pluginManager.withPlugin("org.jetbrains.kotlin.jvm") {
    project.tasks.withType<KotlinCompile>().configureEach {
      kotlinOptions {
        if (project.name != "sample") {
          jvmTarget = libs.versions.jvmTarget.get()
        }
        @Suppress("SuspiciousCollectionReassignment")
        freeCompilerArgs += listOf("-progressive", "-Xjvm-default=all")
      }
    }
  }

  pluginManager.withPlugin("org.jetbrains.dokka") {
    tasks.named<DokkaTask>("dokkaHtml") {
      outputDirectory.set(rootProject.file("docs/0.x"))
      dokkaSourceSets.configureEach { skipDeprecated.set(true) }
    }
  }

  plugins.withId("com.vanniktech.maven.publish.base") {
    configure<MavenPublishBaseExtension> {
      publishToMavenCentral(SonatypeHost.DEFAULT)
      signAllPublications()
      pomFromGradleProperties()
    }
  }
}
