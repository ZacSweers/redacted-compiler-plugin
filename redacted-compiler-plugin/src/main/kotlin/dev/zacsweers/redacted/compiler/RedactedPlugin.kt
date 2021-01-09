package dev.zacsweers.redacted.compiler

import com.google.auto.service.AutoService
import dev.zacsweers.redacted.compiler.ir.RedactedIrGenerationExtension
import org.jetbrains.kotlin.backend.common.extensions.IrGenerationExtension
import org.jetbrains.kotlin.cli.common.CLIConfigurationKeys
import org.jetbrains.kotlin.cli.common.messages.MessageCollector
import org.jetbrains.kotlin.codegen.extensions.ExpressionCodegenExtension
import org.jetbrains.kotlin.com.intellij.mock.MockProject
import org.jetbrains.kotlin.com.intellij.openapi.extensions.impl.ExtensionPointImpl
import org.jetbrains.kotlin.com.intellij.openapi.project.Project
import org.jetbrains.kotlin.compiler.plugin.ComponentRegistrar
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.config.JVMConfigurationKeys
import org.jetbrains.kotlin.extensions.ProjectExtensionDescriptor
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.resolve.extensions.SyntheticResolveExtension

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
    ExpressionCodegenExtension.registerExtensionAsFirst(project,
        RedactedCodegenExtension(messageCollector, replacementString, fqRedactedAnnotation))

    SyntheticResolveExtension.registerExtensionAsFirst(project,
        RedactedSyntheticResolveExtension(fqRedactedAnnotation))

    if (configuration.getBoolean(JVMConfigurationKeys.IR)) {
      IrGenerationExtension.registerExtension(
          project,
          RedactedIrGenerationExtension(messageCollector, replacementString, fqRedactedAnnotation)
      )
    }
  }
}

internal fun <T : Any> ProjectExtensionDescriptor<T>.registerExtensionAsFirst(
    project: Project,
    extension: T
) {
  project.extensionArea
      .getExtensionPoint(extensionPointName)
      .let { it as ExtensionPointImpl }
      .registerExtension(extension, project)
}
