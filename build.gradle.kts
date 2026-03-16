import com.vanniktech.maven.publish.MavenPublishBaseExtension
import kotlinx.validation.ExperimentalBCVApi
import org.jetbrains.dokka.gradle.DokkaExtension
import org.jetbrains.dokka.gradle.engine.parameters.VisibilityModifier
import org.jetbrains.kotlin.gradle.dsl.JvmDefaultMode
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.dsl.KotlinJvmCompilerOptions
import org.jetbrains.kotlin.gradle.dsl.KotlinProjectExtension
import org.jetbrains.kotlin.gradle.plugin.KotlinBasePlugin
import org.jetbrains.kotlin.gradle.tasks.KotlinCompilationTask
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

buildscript {
  dependencies {
    // Include our included build
    classpath("dev.zacsweers.redacted:redacted-compiler-plugin-gradle")
  }
}

plugins {
  alias(libs.plugins.kotlin.jvm) apply false
  alias(libs.plugins.kotlin.multiplatform) apply false
  alias(libs.plugins.dokka)
  alias(libs.plugins.mavenPublish) apply false
  alias(libs.plugins.spotless)
  alias(libs.plugins.binaryCompatibilityValidator)
  alias(libs.plugins.buildConfig) apply false
}

apiValidation {
  ignoredProjects += listOf("sample", "sample-jvm")
  @OptIn(ExperimentalBCVApi::class)
  klib {
    // This is only really possible to run on macOS
    //    strictValidation = true
    enabled = true
  }
}

dokka {
  dokkaPublications.html {
    outputDirectory.set(rootDir.resolve("docs/api/1.x"))
    includes.from(project.layout.projectDirectory.file("README.md"))
  }
}

spotless {
  format("misc") {
    target("*.gradle", "*.md", ".gitignore")
    trimTrailingWhitespace()
    leadingTabsToSpaces(2)
    endWithNewline()
  }
  kotlin {
    target("**/src/**/*.kt")
    ktfmt(libs.versions.ktfmt.get()).googleStyle()
    trimTrailingWhitespace()
    endWithNewline()
    licenseHeaderFile("spotless/spotless.kt")
    targetExclude("**/spotless.kt", "**/build/**", "**/compiler-tests/src/test/data/**")
    // TODO temporary until ktfmt supports context params
    suppressLintsFor {
      step = "ktfmt"
      shortCode = "ktfmt"
      path =
        "redacted-compiler-plugin/src/main/kotlin/dev/zacsweers/redacted/compiler/fir/RedactedFirExtensionRegistrar.kt"
    }
  }
  kotlinGradle {
    target("**/*.kts", "*.kts")
    ktfmt(libs.versions.ktfmt.get()).googleStyle()
    trimTrailingWhitespace()
    endWithNewline()
  }
}

subprojects {
  group = project.property("GROUP") as String
  version = project.property("VERSION_NAME") as String

  pluginManager.withPlugin("java") {
    configure<JavaPluginExtension> {
      toolchain { languageVersion.set(libs.versions.jdk.map(JavaLanguageVersion::of)) }
    }
    tasks.withType<JavaCompile>().configureEach {
      options.release.set(libs.versions.jvmTarget.map(String::toInt))
    }
  }

  plugins.withType<KotlinBasePlugin> {
    project.tasks.withType<KotlinCompilationTask<*>>().configureEach {
      compilerOptions {
        progressiveMode.set(true)
        if (this is KotlinJvmCompilerOptions) {
          if (project.name != "sample") {
            jvmTarget.set(libs.versions.jvmTarget.map(JvmTarget::fromTarget))
          }
          jvmDefault.set(JvmDefaultMode.NO_COMPATIBILITY)
        }
      }
    }
    if ("sample" !in project.path) {
      configure<KotlinProjectExtension> { explicitApi() }
    }
  }

  pluginManager.withPlugin("org.jetbrains.dokka") {
    configure<DokkaExtension> {
      basePublicationsDirectory.set(layout.buildDirectory.dir("dokkaDir"))
      dokkaSourceSets.configureEach {
        skipDeprecated.set(true)
        documentedVisibilities.add(VisibilityModifier.Public)

        sourceLink {
          localDirectory.set(layout.projectDirectory.dir("src"))
          val relPath = rootProject.projectDir.toPath().relativize(projectDir.toPath())
          remoteUrl(
            providers.gradleProperty("POM_SCM_URL").map { scmUrl ->
              "$scmUrl/tree/main/$relPath/src"
            }
          )
          remoteLineSuffix.set("#L")
        }
      }
    }
  }

  plugins.withId("com.vanniktech.maven.publish") {
    configure<MavenPublishBaseExtension> { publishToMavenCentral(automaticRelease = true) }

    // configuration required to produce unique META-INF/*.kotlin_module file names
    tasks.withType<KotlinCompile>().configureEach {
      compilerOptions { moduleName.set(project.property("POM_ARTIFACT_ID") as String) }
    }
  }
}

dependencies { dokka(project(":redacted-compiler-plugin-annotations")) }
