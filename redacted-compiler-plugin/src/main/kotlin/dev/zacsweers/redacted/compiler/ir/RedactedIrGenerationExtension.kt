package dev.zacsweers.redacted.compiler.ir

import org.jetbrains.kotlin.backend.common.extensions.IrGenerationExtension
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.cli.common.messages.MessageCollector
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import org.jetbrains.kotlin.name.FqName

internal class RedactedIrGenerationExtension(
    private val messageCollector: MessageCollector,
    private val replacementString: String,
    private val redactedAnnotationName: FqName
) : IrGenerationExtension {

  override fun generate(moduleFragment: IrModuleFragment, pluginContext: IrPluginContext) {
    val redactedAnnotation = pluginContext.referenceClass(redactedAnnotationName)!!
    val redactedTransformer = RedactedIrVisitor(pluginContext, redactedAnnotation,
        replacementString, messageCollector)
    moduleFragment.transform(redactedTransformer, null)
  }
}