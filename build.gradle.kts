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
  kotlin("jvm") version "1.7.0-RC2" apply false
  id("org.jetbrains.dokka") version "1.6.20" apply false
  id("org.jetbrains.kotlinx.binary-compatibility-validator") version "0.10.0"
  id("com.google.devtools.ksp") version "1.7.0-RC-1.0.5" apply false
  id("com.vanniktech.maven.publish") version "0.19.0" apply false
  id("com.diffplug.spotless") version "6.6.1"
}

plugins.withType<NodeJsRootPlugin>().configureEach {
  // 16+ required for Apple Silicon support
  // https://youtrack.jetbrains.com/issue/KT-49109#focus=Comments-27-5259190.0-0
  the<NodeJsRootExtension>().nodeVersion = "17.0.0"
}

apiValidation { ignoredProjects += listOf("sample") }

spotless {
  format("misc") {
    target("*.gradle", "*.md", ".gitignore")
    trimTrailingWhitespace()
    indentWithSpaces(2)
    endWithNewline()
  }
  kotlin {
    target("**/*.kt")
    ktfmt("0.37")
    trimTrailingWhitespace()
    endWithNewline()
    licenseHeaderFile("spotless/spotless.kt")
    targetExclude("**/spotless.kt", "**/build/**")
  }
  kotlinGradle {
    target("**/*.kts")
    ktfmt("0.37")
    trimTrailingWhitespace()
    endWithNewline()
  }
}

allprojects {
  group = project.property("GROUP") as String
  version = project.property("VERSION_NAME") as String

  repositories {
    google()
    mavenCentral()
  }

  pluginManager.withPlugin("java") {
    configure<JavaPluginExtension> { toolchain { languageVersion.set(JavaLanguageVersion.of(17)) } }
    tasks.withType<JavaCompile>().configureEach { options.release.set(8) }
  }

  pluginManager.withPlugin("org.jetbrains.kotlin.jvm") {
    project.tasks.withType<KotlinCompile>().configureEach {
      kotlinOptions {
        if (project.name != "sample") {
          jvmTarget = "1.8"
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
