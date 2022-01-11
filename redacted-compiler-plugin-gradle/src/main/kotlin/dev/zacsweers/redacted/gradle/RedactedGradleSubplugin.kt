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
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.plugin.KotlinCompilation
import org.jetbrains.kotlin.gradle.plugin.KotlinCompilerPluginSupportPlugin
import org.jetbrains.kotlin.gradle.plugin.SubpluginArtifact
import org.jetbrains.kotlin.gradle.plugin.SubpluginOption
import org.jetbrains.kotlin.gradle.plugin.sources.DefaultKotlinSourceSet

class RedactedGradleSubplugin : KotlinCompilerPluginSupportPlugin {

  override fun apply(target: Project) {
    target.extensions.create("redacted", RedactedPluginExtension::class.java)
  }

  override fun getCompilerPluginId(): String = "redacted-compiler-plugin"

  override fun getPluginArtifact(): SubpluginArtifact =
      SubpluginArtifact(
          groupId = "dev.zacsweers.redacted",
          artifactId = "redacted-compiler-plugin",
          version = VERSION)

  override fun isApplicable(kotlinCompilation: KotlinCompilation<*>): Boolean = true

  override fun applyToCompilation(
      kotlinCompilation: KotlinCompilation<*>
  ): Provider<List<SubpluginOption>> {
    val project = kotlinCompilation.target.project
    val extension = project.extensions.getByType(RedactedPluginExtension::class.java)
    val annotation = extension.redactedAnnotation

    // Default annotation is used, so add it as a dependency
    // Note only multiplatform, jvm/android, and js are supported. Anyone else is on their own.
    if (annotation.get() == DEFAULT_ANNOTATION) {
      when {
        project.plugins.hasPlugin("org.jetbrains.kotlin.multiplatform") -> {
          val sourceSets =
              project.extensions.getByType(KotlinMultiplatformExtension::class.java).sourceSets
          val sourceSet = (sourceSets.getByName("commonMain") as DefaultKotlinSourceSet)
          project
              .configurations
              .getByName(sourceSet.apiConfigurationName)
              .dependencies
              .add(
                  project.dependencies.create(
                      "dev.zacsweers.redacted:redacted-compiler-plugin-annotations:$VERSION"))
        }
        else -> {
          project
              .configurations
              .getByName("implementation")
              .dependencies
              .add(
                  project.dependencies.create(
                      "dev.zacsweers.redacted:redacted-compiler-plugin-annotations:$VERSION"))
        }
      }
    }

    val enabled = extension.enabled.get()

    return project.provider {
      listOf(
          SubpluginOption(key = "enabled", value = enabled.toString()),
          SubpluginOption(key = "replacementString", value = extension.replacementString.get()),
          SubpluginOption(key = "redactedAnnotation", value = annotation.get()))
    }
  }
}
