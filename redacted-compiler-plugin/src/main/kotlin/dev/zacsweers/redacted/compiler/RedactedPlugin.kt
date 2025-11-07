// Copyright (C) 2021 Zac Sweers
// SPDX-License-Identifier: Apache-2.0
package dev.zacsweers.redacted.compiler

import dev.zacsweers.redacted.compiler.fir.RedactedFirExtensionRegistrar
import org.jetbrains.kotlin.backend.common.extensions.IrGenerationExtension
import org.jetbrains.kotlin.compiler.plugin.CompilerPluginRegistrar
import org.jetbrains.kotlin.compiler.plugin.ExperimentalCompilerApi
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.fir.extensions.FirExtensionRegistrarAdapter
import org.jetbrains.kotlin.name.ClassId

@OptIn(ExperimentalCompilerApi::class)
public class RedactedComponentRegistrar : CompilerPluginRegistrar() {

  override val pluginId: String = "dev.zacsweers.redacted.compiler"

  override val supportsK2: Boolean = true

  override fun ExtensionStorage.registerExtensions(configuration: CompilerConfiguration) {
    if (configuration[KEY_ENABLED] == false) return

    val replacementString = checkNotNull(configuration[KEY_REPLACEMENT_STRING])
    val redactedAnnotations =
      checkNotNull(configuration[KEY_REDACTED_ANNOTATIONS]).splitToSequence(":").mapTo(
        LinkedHashSet()
      ) {
        ClassId.fromString(it)
      }
    val unRedactedAnnotations =
      checkNotNull(configuration[KEY_UNREDACTED_ANNOTATION]).splitToSequence(":").mapTo(
        LinkedHashSet()
      ) {
        ClassId.fromString(it)
      }

    FirExtensionRegistrarAdapter.registerExtension(
      RedactedFirExtensionRegistrar(redactedAnnotations, unRedactedAnnotations)
    )
    IrGenerationExtension.registerExtension(
      RedactedIrGenerationExtension(replacementString, redactedAnnotations, unRedactedAnnotations)
    )
  }
}
