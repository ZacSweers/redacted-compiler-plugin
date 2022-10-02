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
package dev.zacsweers.redacted.compiler

import com.google.auto.service.AutoService
import dev.zacsweers.redacted.compiler.fir.FirRedactedExtensionRegistrar
import org.jetbrains.kotlin.backend.common.extensions.IrGenerationExtension
import org.jetbrains.kotlin.cli.common.CLIConfigurationKeys
import org.jetbrains.kotlin.cli.common.messages.MessageCollector
import org.jetbrains.kotlin.com.intellij.mock.MockProject
import org.jetbrains.kotlin.compiler.plugin.ComponentRegistrar
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.fir.extensions.FirExtensionRegistrarAdapter
import org.jetbrains.kotlin.name.ClassId

// TODO switch to CompilerPluginRegistrar in 1.8 https://youtrack.jetbrains.com/issue/KT-52665
@AutoService(ComponentRegistrar::class)
class RedactedComponentRegistrar : ComponentRegistrar {

  override val supportsK2: Boolean
    get() = true

  override fun registerProjectComponents(
      project: MockProject,
      configuration: CompilerConfiguration
  ) {

    if (configuration[KEY_ENABLED] == false) return

    val messageCollector =
        configuration.get(CLIConfigurationKeys.MESSAGE_COLLECTOR_KEY, MessageCollector.NONE)
    val replacementString = checkNotNull(configuration[KEY_REPLACEMENT_STRING])
    val redactedAnnotation = checkNotNull(configuration[KEY_REDACTED_ANNOTATION])
    val redactedAnnotationClassId = ClassId.fromString(redactedAnnotation)
    val fqRedactedAnnotation = redactedAnnotationClassId.asSingleFqName()

    IrGenerationExtension.registerExtension(
        project,
        RedactedIrGenerationExtension(messageCollector, replacementString, fqRedactedAnnotation))

    FirExtensionRegistrarAdapter.registerExtension(
        project, FirRedactedExtensionRegistrar(redactedAnnotationClassId))
  }
}
