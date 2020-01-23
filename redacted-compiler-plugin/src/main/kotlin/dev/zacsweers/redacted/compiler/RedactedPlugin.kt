package dev.zacsweers.redacted.compiler

import com.google.auto.service.AutoService
import org.jetbrains.kotlin.cli.common.CLIConfigurationKeys
import org.jetbrains.kotlin.cli.common.messages.MessageCollector
import org.jetbrains.kotlin.codegen.extensions.ExpressionCodegenExtension
import org.jetbrains.kotlin.com.intellij.mock.MockProject
import org.jetbrains.kotlin.com.intellij.openapi.extensions.Extensions
import org.jetbrains.kotlin.com.intellij.openapi.extensions.impl.ExtensionPointImpl
import org.jetbrains.kotlin.com.intellij.openapi.project.Project
import org.jetbrains.kotlin.compiler.plugin.ComponentRegistrar
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.extensions.ProjectExtensionDescriptor
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.resolve.extensions.SyntheticResolveExtension

@AutoService(ComponentRegistrar::class)
class TestComponentRegistrar : ComponentRegistrar {

  override fun registerProjectComponents(project: MockProject,
      configuration: CompilerConfiguration) {

    if (configuration[KEY_ENABLED] == false) return

    // see https://github.com/JetBrains/kotlin/blob/1.1.2/plugins/annotation-collector/src/org/jetbrains/kotlin/annotation/AnnotationCollectorPlugin.kt#L92
    val messageCollector = configuration.get(CLIConfigurationKeys.MESSAGE_COLLECTOR_KEY,
        MessageCollector.NONE)

    val replacementString = checkNotNull(configuration[KEY_REPLACEMENT_STRING])
    val redactedAnnotation = checkNotNull(configuration[KEY_REDACTED_ANNOTATION])
    val fqRedactedAnnotation = FqName(redactedAnnotation)
    ExpressionCodegenExtension.registerExtensionAsFirst(project,
        RedactedCodegenExtension(messageCollector, replacementString, fqRedactedAnnotation))

    SyntheticResolveExtension.registerExtensionAsFirst(project, RedactedSyntheticResolveExtension(fqRedactedAnnotation))
  }
}

fun <T> ProjectExtensionDescriptor<T>.registerExtensionAsFirst(project: Project, extension: T) {
  Extensions.getArea(project)
      .getExtensionPoint(extensionPointName)
      .let { it as ExtensionPointImpl }
      .registerExtension(extension, project)
}
