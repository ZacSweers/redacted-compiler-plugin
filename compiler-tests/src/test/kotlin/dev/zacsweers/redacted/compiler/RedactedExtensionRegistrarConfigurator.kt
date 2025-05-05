// Copyright (C) 2025 Zac Sweers
// SPDX-License-Identifier: Apache-2.0
package dev.zacsweers.redacted.compiler

import dev.zacsweers.redacted.compiler.fir.RedactedFirExtensionRegistrar
import org.jetbrains.kotlin.backend.common.extensions.IrGenerationExtension
import org.jetbrains.kotlin.compiler.plugin.CompilerPluginRegistrar
import org.jetbrains.kotlin.compiler.plugin.ExperimentalCompilerApi
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.fir.extensions.FirExtensionRegistrarAdapter
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.test.builders.TestConfigurationBuilder
import org.jetbrains.kotlin.test.model.TestModule
import org.jetbrains.kotlin.test.services.EnvironmentConfigurator
import org.jetbrains.kotlin.test.services.TestServices

fun TestConfigurationBuilder.configurePlugin() {
  useConfigurators(
    ::RedactedExtensionRegistrarConfigurator,
    ::RedactedRuntimeEnvironmentConfigurator,
  )

  useDirectives(RedactedDirectives)

  useCustomRuntimeClasspathProviders(::RedactedRuntimeClassPathProvider)

  useSourcePreprocessor(::RedactedDefaultImportPreprocessor)
}

class RedactedExtensionRegistrarConfigurator(testServices: TestServices) :
  EnvironmentConfigurator(testServices) {
  @OptIn(ExperimentalCompilerApi::class)
  override fun CompilerPluginRegistrar.ExtensionStorage.registerCompilerExtensions(
    module: TestModule,
    configuration: CompilerConfiguration,
  ) {
    val redactedAnnotations =
      module.directives[RedactedDirectives.REDACTED_ANNOTATIONS].mapTo(mutableSetOf()) {
        ClassId.fromString(it)
      } + ClassId.fromString("dev/zacsweers/redacted/annotations/Redacted")
    val unredactedAnnotations =
      module.directives[RedactedDirectives.UNREDACTED_ANNOTATIONS].mapTo(mutableSetOf()) {
        ClassId.fromString(it)
      } + ClassId.fromString("dev/zacsweers/redacted/annotations/Unredacted")
    val replacementString =
      module.directives[RedactedDirectives.REPLACEMENT_STRING].firstOrNull() ?: "██"

    FirExtensionRegistrarAdapter.registerExtension(
      RedactedFirExtensionRegistrar(redactedAnnotations, unredactedAnnotations)
    )
    IrGenerationExtension.registerExtension(
      RedactedIrGenerationExtension(replacementString, redactedAnnotations, unredactedAnnotations)
    )
  }
}
