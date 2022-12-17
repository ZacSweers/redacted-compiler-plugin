import com.vanniktech.maven.publish.MavenPublishBaseExtension
import com.vanniktech.maven.publish.SonatypeHost
import org.jetbrains.dokka.gradle.DokkaTask
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
  alias(libs.plugins.kotlin.jvm)
  `java-gradle-plugin`
  alias(libs.plugins.dokka)
  alias(libs.plugins.mavenPublish)
  alias(libs.plugins.spotless)
}

java { toolchain { languageVersion.set(JavaLanguageVersion.of(libs.versions.jdk.get().toInt())) } }

tasks.withType<JavaCompile>().configureEach {
  options.release.set(libs.versions.jvmTarget.get().removePrefix("1.").toInt())
}

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

tasks.withType<KotlinCompile>().configureEach {
  dependsOn(copyVersionTemplatesProvider)
  kotlinOptions { jvmTarget = libs.versions.jvmTarget.get() }
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

kotlin { explicitApi() }

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
    licenseHeaderFile("../spotless/spotless.kt")
    targetExclude("**/spotless.kt", "build/**")
  }
}

dependencies {
  compileOnly(libs.kotlin.gradlePlugin)
  compileOnly(libs.kotlin.gradlePlugin.api)
}

configure<MavenPublishBaseExtension> {
  publishToMavenCentral(SonatypeHost.DEFAULT)
  signAllPublications()
  pomFromGradleProperties()
}
