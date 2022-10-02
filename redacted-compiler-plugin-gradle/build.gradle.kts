import org.jetbrains.dokka.gradle.DokkaTask

plugins {
  id("org.jetbrains.kotlin.jvm") version "1.7.20"
  id("java-gradle-plugin")
  id("org.jetbrains.dokka") version "1.7.10"
  id("com.vanniktech.maven.publish") version "0.19.0"
  id("com.diffplug.spotless") version "6.11.0"
}

java { toolchain { languageVersion.set(JavaLanguageVersion.of(17)) } }

tasks.withType<JavaCompile>().configureEach { options.release.set(8) }

// region Version.kt template for setting the project version in the build
sourceSets { main { java.srcDir("$buildDir/generated/sources/version-templates/kotlin/main") } }

val copyVersionTemplatesProvider =
    tasks.register<Copy>("copyVersionTemplates") {
      inputs.property("version", project.property("VERSION_NAME"))
      from(project.layout.projectDirectory.dir("version-templates"))
      into(project.layout.buildDirectory.dir("generated/sources/version-templates/kotlin/main"))
      expand(mapOf("projectVersion" to "${project.property("VERSION_NAME")}"))
      filteringCharset = "UTF-8"
    }
// endregion

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>().configureEach {
  dependsOn(copyVersionTemplatesProvider)
  kotlinOptions { jvmTarget = "1.8" }
}

gradlePlugin {
  plugins {
    create("redactedPlugin") {
      id = "dev.zacsweers.redacted"
      implementationClass = "dev.zacsweers.redacted.gradle.RedactedGradleSubplugin"
    }
  }
}

tasks.named<DokkaTask>("dokkaHtml") {
  outputDirectory.set(rootProject.file("../docs/0.x"))
  dokkaSourceSets.configureEach { skipDeprecated.set(true) }
}

repositories { mavenCentral() }

spotless {
  format("misc") {
    target("*.gradle", "*.md", ".gitignore")
    trimTrailingWhitespace()
    indentWithSpaces(2)
    endWithNewline()
  }
  kotlin {
    target("**/*.kt")
    ktfmt("0.41")
    trimTrailingWhitespace()
    endWithNewline()
    licenseHeaderFile("../spotless/spotless.kt")
    targetExclude("**/spotless.kt", "build/**")
  }
}

dependencies {
  compileOnly(libs.kotlin.gradlePlugin)
  compileOnly(libs.kotlin.gradlePlugin.api)
}
