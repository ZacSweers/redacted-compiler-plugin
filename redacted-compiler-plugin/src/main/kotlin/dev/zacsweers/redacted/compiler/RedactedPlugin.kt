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
import org.jetbrains.kotlin.cli.common.messages.MessageCollector
import org.jetbrains.kotlin.compiler.plugin.CompilerPluginRegistrar
import org.jetbrains.kotlin.compiler.plugin.ExperimentalCompilerApi
import org.jetbrains.kotlin.config.CommonConfigurationKeys
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.config.languageVersionSettings
import org.jetbrains.kotlin.fir.extensions.FirExtensionRegistrarAdapter
import org.jetbrains.kotlin.name.ClassId

@OptIn(ExperimentalCompilerApi::class)
@AutoService(CompilerPluginRegistrar::class)
public class RedactedComponentRegistrar : CompilerPluginRegistrar() {

  override val supportsK2: Boolean
    get() = true

  override fun ExtensionStorage.registerExtensions(configuration: CompilerConfiguration) {
    if (configuration[KEY_ENABLED] == false) return

    val messageCollector =
      configuration.get(CommonConfigurationKeys.MESSAGE_COLLECTOR_KEY, MessageCollector.NONE)
    val replacementString = checkNotNull(configuration[KEY_REPLACEMENT_STRING])
    val redactedAnnotation = checkNotNull(configuration[KEY_REDACTED_ANNOTATION])
    val unRedactedAnnotation = checkNotNull(configuration[KEY_UNREDACTED_ANNOTATION])
    val usesK2 = configuration.languageVersionSettings.languageVersion.usesK2
    val redactedAnnotationClassId = ClassId.fromString(redactedAnnotation)
    val fqRedactedAnnotation = redactedAnnotationClassId.asSingleFqName()
    val unRedactedAnnotationClassId = ClassId.fromString(unRedactedAnnotation)
    val fqUnRedactedAnnotation = unRedactedAnnotationClassId.asSingleFqName()

    if (usesK2) {
      FirExtensionRegistrarAdapter.registerExtension(
        FirRedactedExtensionRegistrar(redactedAnnotationClassId, unRedactedAnnotationClassId)
      )
    }
    IrGenerationExtension.registerExtension(
      RedactedIrGenerationExtension(
        messageCollector,
        replacementString,
        fqRedactedAnnotation,
        fqUnRedactedAnnotation,
      )
    )
  }
}
