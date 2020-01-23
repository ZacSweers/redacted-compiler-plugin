package dev.zacsweers.redacted.gradle

import com.google.auto.service.AutoService
import org.gradle.api.Project
import org.gradle.api.tasks.compile.AbstractCompile
import org.jetbrains.kotlin.gradle.dsl.KotlinCommonOptions
import org.jetbrains.kotlin.gradle.plugin.KotlinCompilation
import org.jetbrains.kotlin.gradle.plugin.KotlinGradleSubplugin
import org.jetbrains.kotlin.gradle.plugin.SubpluginArtifact
import org.jetbrains.kotlin.gradle.plugin.SubpluginOption

@AutoService(KotlinGradleSubplugin::class)
class RedactedGradleSubplugin : KotlinGradleSubplugin<AbstractCompile> {

  override fun isApplicable(project: Project, task: AbstractCompile): Boolean =
      project.plugins.hasPlugin(RedactedGradlePlugin::class.java)

  override fun getCompilerPluginId(): String = "redacted-compiler-plugin"

  override fun getPluginArtifact(): SubpluginArtifact =
      SubpluginArtifact(
          groupId = "dev.zacsweers.redacted",
          artifactId = "redacted-compiler-plugin",
          // TODO: What's the best way to keep this synced?
          version = "1.0.0-SNAPSHOT"
      )

  override fun apply(
      project: Project,
      kotlinCompile: AbstractCompile,
      javaCompile: AbstractCompile?,
      variantData: Any?,
      androidProjectHandler: Any?,
      kotlinCompilation: KotlinCompilation<KotlinCommonOptions>?
  ): List<SubpluginOption> {
    val extension = project.extensions.findByType(RedactedPluginExtension::class.java) ?: RedactedPluginExtension()
    val annotation = requireNotNull(extension.redactedAnnotation) {
      "Redacted annotation must be specified!"
    }

    return listOf(
        SubpluginOption(key = "enabled", value = extension.enabled.toString()),
        SubpluginOption(key = "verbose", value = extension.enabled.toString()),
        SubpluginOption(key = "replacementString", value = extension.replacementString),
        SubpluginOption(key = "redactedAnnotation", value = annotation)
    )
  }
}
