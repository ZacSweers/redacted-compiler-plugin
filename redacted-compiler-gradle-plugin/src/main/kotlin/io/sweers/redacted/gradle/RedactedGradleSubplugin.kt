package io.sweers.redacted.gradle

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
          groupId = "io.sweers.redacted",
          artifactId = "redacted-compiler-plugin",
          version = "0.0.1" // TODO: What's the best way to keep this synced?
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

    return listOf(
        SubpluginOption(key = "enabled", value = extension.enabled.toString()),
        SubpluginOption(key = "replacementString", value = extension.replacementString)
    )
  }
}
