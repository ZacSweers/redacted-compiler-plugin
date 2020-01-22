package io.sweers.redacted.compiler

import com.google.auto.service.AutoService
import org.jetbrains.kotlin.cli.common.CLIConfigurationKeys
import org.jetbrains.kotlin.cli.common.messages.MessageCollector
import org.jetbrains.kotlin.codegen.extensions.ExpressionCodegenExtension
import org.jetbrains.kotlin.com.intellij.mock.MockProject
import org.jetbrains.kotlin.com.intellij.openapi.extensions.Extensions
import org.jetbrains.kotlin.com.intellij.openapi.extensions.LoadingOrder
import org.jetbrains.kotlin.com.intellij.openapi.extensions.impl.ExtensionPointImpl
import org.jetbrains.kotlin.com.intellij.openapi.project.Project
import org.jetbrains.kotlin.compiler.plugin.ComponentRegistrar
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.extensions.ProjectExtensionDescriptor
import org.jetbrains.kotlin.resolve.extensions.SyntheticResolveExtension

@AutoService(ComponentRegistrar::class)
class TestComponentRegistrar : ComponentRegistrar {

  override fun registerProjectComponents(project: MockProject,
      configuration: CompilerConfiguration) {

    // see https://github.com/JetBrains/kotlin/blob/1.1.2/plugins/annotation-collector/src/org/jetbrains/kotlin/annotation/AnnotationCollectorPlugin.kt#L92
    val messageCollector = configuration.get(CLIConfigurationKeys.MESSAGE_COLLECTOR_KEY,
        MessageCollector.NONE)

    ExpressionCodegenExtension.registerExtensionAsFirst(project,
        RedactedCodegenExtension(messageCollector))

    SyntheticResolveExtension.registerExtensionAsFirst(project,
        RedactedSyntheticResolveExtension(messageCollector))
  }
}

fun <T> ProjectExtensionDescriptor<T>.registerExtensionAsFirst(project: Project, extension: T) {
  Extensions.getArea(project)
      .getExtensionPoint(extensionPointName)
      .let { it as ExtensionPointImpl }
      .registerExtension(extension, LoadingOrder.LAST)
}
