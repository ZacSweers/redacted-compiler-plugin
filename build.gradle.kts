import com.vanniktech.maven.publish.MavenPublishBaseExtension
import org.jetbrains.dokka.gradle.DokkaTask
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.dsl.KotlinProjectExtension
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

val javaTarget = libs.versions.jvmTarget.get().toInt()

subprojects {
  group = project.property("GROUP") as String
  version = project.property("VERSION_NAME") as String

  pluginManager.withPlugin("java") {
    configure<JavaPluginExtension> {
      toolchain { languageVersion.set(JavaLanguageVersion.of(libs.versions.jdk.get().toInt())) }
    }
    tasks.withType<JavaCompile>().configureEach { options.release.set(javaTarget) }
  }

  pluginManager.withPlugin("org.jetbrains.kotlin.jvm") {
    project.tasks.withType<KotlinCompile>().configureEach {
      compilerOptions {
        if (project.name != "sample") {
          jvmTarget.set(JvmTarget.fromTarget(libs.versions.jvmTarget.get()))
        }
        freeCompilerArgs.addAll("-progressive", "-Xjvm-default=all")
      }
    }
    if ("sample" !in project.path) {
      configure<KotlinProjectExtension> { explicitApi() }
    }
  }

  pluginManager.withPlugin("org.jetbrains.dokka") {
    tasks.named<DokkaTask>("dokkaHtml") {
      outputDirectory.set(rootProject.file("docs/0.x"))
      dokkaSourceSets.configureEach { skipDeprecated.set(true) }
    }
  }

  plugins.withId("com.vanniktech.maven.publish.base") {
    configure<MavenPublishBaseExtension> { publishToMavenCentral() }

    // configuration required to produce unique META-INF/*.kotlin_module file names
    tasks.withType<KotlinCompile>().configureEach {
      kotlinOptions { moduleName = project.property("POM_ARTIFACT_ID") as String }
    }
  }
}
