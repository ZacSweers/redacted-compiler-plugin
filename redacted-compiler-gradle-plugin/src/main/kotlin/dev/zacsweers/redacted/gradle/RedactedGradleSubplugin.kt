package dev.zacsweers.redacted.gradle

import org.gradle.api.Project
import org.gradle.api.provider.Provider
import org.jetbrains.kotlin.gradle.plugin.KotlinCompilation
import org.jetbrains.kotlin.gradle.plugin.KotlinCompilerPluginSupportPlugin
import org.jetbrains.kotlin.gradle.plugin.KotlinPlatformType
import org.jetbrains.kotlin.gradle.plugin.SubpluginArtifact
import org.jetbrains.kotlin.gradle.plugin.SubpluginOption

class RedactedGradleSubplugin : KotlinCompilerPluginSupportPlugin {

  override fun apply(target: Project) {
    target.extensions.create("redacted", RedactedPluginExtension::class.java)
  }

  override fun getCompilerPluginId(): String = "redacted-compiler-plugin"

  override fun getPluginArtifact(): SubpluginArtifact =
      SubpluginArtifact(
          groupId = "dev.zacsweers.redacted",
          artifactId = "redacted-compiler-plugin",
          version = VERSION
      )

  override fun isApplicable(kotlinCompilation: KotlinCompilation<*>): Boolean {
    return (kotlinCompilation.platformType == KotlinPlatformType.jvm || kotlinCompilation.platformType == KotlinPlatformType.androidJvm)
  }

  override fun applyToCompilation(
      kotlinCompilation: KotlinCompilation<*>
  ): Provider<List<SubpluginOption>> {
    val project = kotlinCompilation.target.project
    val extension = project.extensions.getByType(RedactedPluginExtension::class.java)
    val annotation = extension.redactedAnnotation

    // Default annotation is used, so add it as a dependency
    if (annotation.get() == DEFAULT_ANNOTATION) {
      project.dependencies.add("implementation",
          "dev.zacsweers.redacted:redacted-compiler-plugin-annotations:$VERSION")
    }

    val enabled = extension.enabled.get()

    return project.provider {
      listOf(
          SubpluginOption(key = "enabled", value = enabled.toString()),
          SubpluginOption(key = "replacementString", value = extension.replacementString.get()),
          SubpluginOption(key = "redactedAnnotation", value = annotation.get())
      )
    }
  }
}
