package dev.zacsweers.redacted.compiler

import com.google.auto.service.AutoService
import org.jetbrains.annotations.TestOnly
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
class RedactedComponentRegistrar constructor() : ComponentRegistrar {

  private var testConfiguration: CompilerConfiguration? = null

  // No way to define options yet https://github.com/tschuchortdev/kotlin-compile-testing/issues/34
  @TestOnly
  internal constructor(
      redactedAnnotation: String,
      enabled: Boolean = true,
      replacementString: String = "██") : this() {
    testConfiguration = CompilerConfiguration().apply {
      put(KEY_ENABLED, enabled)
      put(KEY_REPLACEMENT_STRING, replacementString)
      put(KEY_REDACTED_ANNOTATION, redactedAnnotation)
    }
  }

  override fun registerProjectComponents(project: MockProject,
      configuration: CompilerConfiguration) {

    val actualConfiguration = testConfiguration ?: configuration
    if (actualConfiguration[KEY_ENABLED] == false) return

    val realMessageCollector = configuration.get(CLIConfigurationKeys.MESSAGE_COLLECTOR_KEY,
        MessageCollector.NONE)
    val messageCollector = testConfiguration?.get(CLIConfigurationKeys.MESSAGE_COLLECTOR_KEY,
        realMessageCollector) ?: realMessageCollector
    val replacementString = checkNotNull(actualConfiguration[KEY_REPLACEMENT_STRING])
    val redactedAnnotation = checkNotNull(actualConfiguration[KEY_REDACTED_ANNOTATION])
    val fqRedactedAnnotation = FqName(redactedAnnotation)
    ExpressionCodegenExtension.registerExtensionAsFirst(project,
        RedactedCodegenExtension(messageCollector, replacementString, fqRedactedAnnotation))

    SyntheticResolveExtension.registerExtensionAsFirst(project,
        RedactedSyntheticResolveExtension(fqRedactedAnnotation))
  }
}

fun <T> ProjectExtensionDescriptor<T>.registerExtensionAsFirst(project: Project, extension: T) {
  Extensions.getArea(project)
      .getExtensionPoint(extensionPointName)
      .let { it as ExtensionPointImpl }
      .registerExtension(extension, project)
}
