// Copyright (C) 2021 Zac Sweers
// SPDX-License-Identifier: Apache-2.0
package dev.zacsweers.redacted.compiler

import dev.zacsweers.metro.compiler.compat.CompatContext
import dev.zacsweers.redacted.BuildConfig.KOTLIN_PLUGIN_ID
import dev.zacsweers.redacted.compiler.fir.RedactedFirExtensionRegistrar
import org.jetbrains.kotlin.compiler.plugin.CompilerPluginRegistrar
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.name.ClassId

public class RedactedCompilerPluginRegistrar : CompilerPluginRegistrar() {

  override val pluginId: String = KOTLIN_PLUGIN_ID

  override val supportsK2: Boolean = true

  override fun ExtensionStorage.registerExtensions(configuration: CompilerConfiguration) {
    if (configuration[KEY_ENABLED] == false) return

    val compatContext =
      try {
        CompatContext.create()
      } catch (t: Throwable) {
        System.err.println(
          "[Redacted] Skipping enabling Redacted extensions, unable to create CompatContext"
        )
        t.printStackTrace()
        return
      }

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

    with(compatContext) {
      registerFirExtensionCompat(
        RedactedFirExtensionRegistrar(redactedAnnotations, unRedactedAnnotations)
      )
      registerIrExtensionCompat(
        RedactedIrGenerationExtension(
          replacementString,
          redactedAnnotations,
          unRedactedAnnotations,
          compatContext,
        )
      )
    }
  }
}
