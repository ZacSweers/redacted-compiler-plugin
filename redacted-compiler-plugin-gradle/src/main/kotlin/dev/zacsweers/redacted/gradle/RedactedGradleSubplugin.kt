/*
 * Copyright (C) 2021 Zac Sweers
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package dev.zacsweers.redacted.gradle

import org.gradle.api.Project
import org.gradle.api.provider.Provider
import org.jetbrains.kotlin.gradle.plugin.KotlinCompilation
import org.jetbrains.kotlin.gradle.plugin.KotlinCompilerPluginSupportPlugin
import org.jetbrains.kotlin.gradle.plugin.SubpluginArtifact
import org.jetbrains.kotlin.gradle.plugin.SubpluginOption

public class RedactedGradleSubplugin : KotlinCompilerPluginSupportPlugin {

  override fun apply(target: Project) {
    target.extensions.create("redacted", RedactedPluginExtension::class.java)
  }

  override fun getCompilerPluginId(): String = "dev.zacsweers.redacted.compiler"

  override fun getPluginArtifact(): SubpluginArtifact =
    SubpluginArtifact(
      groupId = "dev.zacsweers.redacted",
      artifactId = "redacted-compiler-plugin",
      version = VERSION,
    )

  override fun isApplicable(kotlinCompilation: KotlinCompilation<*>): Boolean = true

  override fun applyToCompilation(
    kotlinCompilation: KotlinCompilation<*>
  ): Provider<List<SubpluginOption>> {
    val project = kotlinCompilation.target.project
    val extension = project.extensions.getByType(RedactedPluginExtension::class.java)
    @Suppress("DEPRECATION")
    val annotations =
      extension.redactedAnnotations.zip(extension.redactedAnnotation, Set<String>::plus)
    @Suppress("DEPRECATION")
    val unredactedAnnotations =
      extension.unredactedAnnotations.zip(extension.unredactedAnnotation, Set<String>::plus)

    // Default annotation is used, so add it as a dependency
    // Note only multiplatform, jvm/android, and js are supported. Anyone else is on their own.
    val useDefaults =
      annotations.getOrElse(DEFAULT_ANNOTATION_SET) == DEFAULT_ANNOTATION_SET ||
        unredactedAnnotations.getOrElse(DEFAULT_UNREDACTED_ANNOTATION_SET) ==
          DEFAULT_UNREDACTED_ANNOTATION_SET
    if (useDefaults) {
      project.dependencies.add(
        kotlinCompilation.implementationConfigurationName,
        "dev.zacsweers.redacted:redacted-compiler-plugin-annotations:$VERSION",
      )
    }

    val enabled = extension.enabled.get()

    return project.provider {
      listOf(
        SubpluginOption(key = "enabled", value = enabled.toString()),
        SubpluginOption(key = "replacementString", value = extension.replacementString.get()),
        SubpluginOption(key = "redactedAnnotations", value = annotations.get().joinToString(",")),
        SubpluginOption(
          key = "unredactedAnnotations",
          value = unredactedAnnotations.get().joinToString(","),
        ),
      )
    }
  }
}
