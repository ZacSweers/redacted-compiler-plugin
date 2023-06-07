import com.vanniktech.maven.publish.MavenPublishBaseExtension
import org.jetbrains.dokka.gradle.DokkaTask
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.dsl.KotlinVersion
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
  alias(libs.plugins.kotlin.jvm)
  `java-gradle-plugin`
  alias(libs.plugins.dokka)
  alias(libs.plugins.mavenPublish)
  alias(libs.plugins.spotless)
}

java { toolchain { languageVersion.set(libs.versions.jdk.map(JavaLanguageVersion::of)) } }

tasks.withType<JavaCompile>().configureEach {
  options.release.set(libs.versions.jvmTarget.map(String::toInt))
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
  compilerOptions {
    jvmTarget.set(libs.versions.jvmTarget.map(JvmTarget::fromTarget))

    // Lower version for Gradle compat
    languageVersion.set(KotlinVersion.KOTLIN_1_8)
    apiVersion.set(KotlinVersion.KOTLIN_1_8)
  }
}

tasks
    .matching { it.name == "sourcesJar" || it.name == "dokkaHtml" }
    .configureEach { dependsOn(copyVersionTemplatesProvider) }

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

configure<MavenPublishBaseExtension> { publishToMavenCentral(automaticRelease = true) }

// configuration required to produce unique META-INF/*.kotlin_module file names
tasks.withType<KotlinCompile>().configureEach {
  kotlinOptions { moduleName = project.property("POM_ARTIFACT_ID") as String }
}
