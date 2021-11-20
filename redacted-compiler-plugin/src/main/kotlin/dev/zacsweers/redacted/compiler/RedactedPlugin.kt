package dev.zacsweers.redacted.compiler

import com.google.auto.service.AutoService
import org.jetbrains.kotlin.backend.common.extensions.IrGenerationExtension
import org.jetbrains.kotlin.cli.common.CLIConfigurationKeys
import org.jetbrains.kotlin.cli.common.messages.MessageCollector
import org.jetbrains.kotlin.com.intellij.mock.MockProject
import org.jetbrains.kotlin.compiler.plugin.ComponentRegistrar
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.name.FqName

@AutoService(ComponentRegistrar::class)
class RedactedComponentRegistrar : ComponentRegistrar {

  override fun registerProjectComponents(project: MockProject,
      configuration: CompilerConfiguration) {

    if (configuration[KEY_ENABLED] == false) return

    val messageCollector = configuration.get(CLIConfigurationKeys.MESSAGE_COLLECTOR_KEY,
        MessageCollector.NONE)
    val replacementString = checkNotNull(configuration[KEY_REPLACEMENT_STRING])
    val redactedAnnotation = checkNotNull(configuration[KEY_REDACTED_ANNOTATION])
    val fqRedactedAnnotation = FqName(redactedAnnotation)

    IrGenerationExtension.registerExtension(
        project,
        RedactedIrGenerationExtension(messageCollector, replacementString, fqRedactedAnnotation)
    )
  }
}
