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

import kotlin.LazyThreadSafetyMode.NONE
import org.jetbrains.kotlin.backend.common.IrElementTransformerVoidWithContext
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.backend.common.lower.DeclarationIrBuilder
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageLocationWithRange
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSeverity
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSourceLocation
import org.jetbrains.kotlin.cli.common.messages.MessageCollector
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.backend.js.utils.isInstantiableEnum
import org.jetbrains.kotlin.ir.builders.IrBlockBodyBuilder
import org.jetbrains.kotlin.ir.builders.irBlockBody
import org.jetbrains.kotlin.ir.builders.irCall
import org.jetbrains.kotlin.ir.builders.irConcat
import org.jetbrains.kotlin.ir.builders.irGetField
import org.jetbrains.kotlin.ir.builders.irReturn
import org.jetbrains.kotlin.ir.builders.irString
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrDeclaration
import org.jetbrains.kotlin.ir.declarations.IrDeclarationOrigin
import org.jetbrains.kotlin.ir.declarations.IrFile
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.declarations.IrProperty
import org.jetbrains.kotlin.ir.declarations.IrValueParameter
import org.jetbrains.kotlin.ir.expressions.addArgument
import org.jetbrains.kotlin.ir.expressions.impl.IrGetValueImpl
import org.jetbrains.kotlin.ir.types.isArray
import org.jetbrains.kotlin.ir.types.isString
import org.jetbrains.kotlin.ir.util.*
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.util.OperatorNameConventions

internal const val LOG_PREFIX = "*** REDACTED (IR):"

internal class RedactedIrVisitor(
  private val pluginContext: IrPluginContext,
  private val redactedAnnotation: FqName,
  private val unredactedAnnotation: FqName,
  private val replacementString: String,
  private val messageCollector: MessageCollector,
) : IrElementTransformerVoidWithContext() {

  private class Property(
    val ir: IrProperty,
    val isRedacted: Boolean,
    val isUnredacted: Boolean,
    val parameter: IrValueParameter,
  )

  override fun visitFunctionNew(declaration: IrFunction): IrStatement {
    log("Reading <$declaration>")

    val declarationParent = declaration.parent
    if (declarationParent is IrClass && declaration.isToStringFromAny()) {
      val primaryConstructor =
        declarationParent.primaryConstructor ?: return super.visitFunctionNew(declaration)
      val constructorParameters =
        primaryConstructor.valueParameters.associateBy { it.name.asString() }

      val properties = mutableListOf<Property>()
      val classIsRedacted = declarationParent.hasAnnotation(redactedAnnotation)
      val supertypeIsRedacted by
        lazy(NONE) {
          declarationParent.getAllSuperclasses().any { it.hasAnnotation(redactedAnnotation) }
        }
      var anyRedacted = false
      var anyUnredacted = false
      for (prop in declarationParent.properties) {
        val parameter = constructorParameters[prop.name.asString()] ?: continue
        val isRedacted = prop.isRedacted
        val isUnredacted = prop.isUnredacted
        if (isRedacted) {
          anyRedacted = true
        }
        if (isUnredacted) {
          anyUnredacted = true
        }
        properties += Property(prop, isRedacted, isUnredacted, parameter)
      }

      if (classIsRedacted || supertypeIsRedacted || anyRedacted) {
        if (declaration.origin == IrDeclarationOrigin.DEFINED) {
          declaration.reportError(
            "@Redacted is only supported on data or value classes that do *not* have a custom toString() function. Please remove the function or remove the @Redacted annotations."
          )
          return super.visitFunctionNew(declaration)
        }
        if (
          declarationParent.isInstantiableEnum ||
            declarationParent.isEnumClass ||
            declarationParent.isEnumEntry
        ) {
          declarationParent.reportError("@Redacted does not support enum classes or entries!")
          return super.visitFunctionNew(declaration)
        }
        if (
          declarationParent.isFinalClass && !declarationParent.isData && !declarationParent.isValue
        ) {
          declarationParent.reportError("@Redacted is only supported on data or value classes!")
          return super.visitFunctionNew(declaration)
        }
        if (declarationParent.isValue && !classIsRedacted) {
          declarationParent.reportError(
            "@Redacted is redundant on value class properties, just annotate the class instead."
          )
          return super.visitFunctionNew(declaration)
        }
        if (declarationParent.isObject) {
          declarationParent.reportError("@Redacted is useless on object classes.")
          return super.visitFunctionNew(declaration)
        }
        if (anyUnredacted && (!classIsRedacted && !supertypeIsRedacted)) {
          declarationParent.reportError(
            "@Unredacted should only be applied to properties in a class or a supertype is marked @Redacted."
          )
          return super.visitFunctionNew(declaration)
        }
        if (!(classIsRedacted xor anyRedacted xor supertypeIsRedacted)) {
          declarationParent.reportError(
            "@Redacted should only be applied to the class or its properties, not both."
          )
          return super.visitFunctionNew(declaration)
        }
        declaration.convertToGeneratedToString(properties, classIsRedacted, supertypeIsRedacted)
      }
    }

    return super.visitFunctionNew(declaration)
  }

  private fun IrFunction.isToStringFromAny(): Boolean =
    name == OperatorNameConventions.TO_STRING &&
      dispatchReceiverParameter != null &&
      extensionReceiverParameter == null &&
      valueParameters.isEmpty() &&
      returnType.isString()

  private fun IrFunction.convertToGeneratedToString(
    properties: List<Property>,
    classIsRedacted: Boolean,
    supertypeIsRedacted: Boolean,
  ) {
    val parent = parent as IrClass

    origin = RedactedOrigin

    body =
      DeclarationIrBuilder(pluginContext, symbol).irBlockBody {
        generateToStringMethodBody(
          irClass = parent,
          irFunction = this@convertToGeneratedToString,
          irProperties = properties,
          classIsRedacted = classIsRedacted,
          supertypeIsRedacted = supertypeIsRedacted,
        )
      }

    reflectivelySetFakeOverride(false)
  }

  private fun IrFunction.reflectivelySetFakeOverride(isFakeOverride: Boolean) {
    with(javaClass.getDeclaredField("isFakeOverride")) {
      isAccessible = true
      setBoolean(this@reflectivelySetFakeOverride, isFakeOverride)
    }
  }

  private val IrProperty.isRedacted: Boolean
    get() = hasAnnotation(redactedAnnotation)

  private val IrProperty.isUnredacted: Boolean
    get() = hasAnnotation(unredactedAnnotation)

  /**
   * The actual body of the toString method. Copied from
   * [org.jetbrains.kotlin.ir.util.DataClassMembersGenerator.MemberFunctionBuilder.generateToStringMethodBody]
   * .
   */
  private fun IrBlockBodyBuilder.generateToStringMethodBody(
    irClass: IrClass,
    irFunction: IrFunction,
    irProperties: List<Property>,
    classIsRedacted: Boolean,
    supertypeIsRedacted: Boolean,
  ) {
    val irConcat = irConcat()
    irConcat.addArgument(irString(irClass.name.asString() + "("))
    val hasUnredactedProperties by lazy(NONE) { irProperties.any { it.isUnredacted } }
    if (classIsRedacted && !hasUnredactedProperties) {
      irConcat.addArgument(irString(replacementString))
    } else {
      var first = true
      for (property in irProperties) {
        if (!first) irConcat.addArgument(irString(", "))

        irConcat.addArgument(irString(property.ir.name.asString() + "="))
        val redactProperty =
          property.isRedacted ||
            (classIsRedacted && !property.isUnredacted) ||
            (supertypeIsRedacted && !property.isUnredacted)
        if (redactProperty) {
          irConcat.addArgument(irString(replacementString))
        } else {
          val irPropertyValue = irGetField(receiver(irFunction), property.ir.backingField!!)

          val param = property.parameter
          val irPropertyStringValue =
            if (param.type.isArray() || param.type.isPrimitiveArray()) {
              irCall(
                  context.irBuiltIns.dataClassArrayMemberToStringSymbol,
                  context.irBuiltIns.stringType,
                )
                .apply { putValueArgument(0, irPropertyValue) }
            } else {
              irPropertyValue
            }

          irConcat.addArgument(irPropertyStringValue)
        }
        first = false
      }
    }
    irConcat.addArgument(irString(")"))
    +irReturn(irConcat)
  }

  private fun IrBlockBodyBuilder.receiver(irFunction: IrFunction) =
    IrGetValueImpl(irFunction.dispatchReceiverParameter!!)

  private fun IrBlockBodyBuilder.IrGetValueImpl(irParameter: IrValueParameter) =
    IrGetValueImpl(startOffset, endOffset, irParameter.type, irParameter.symbol)

  private fun log(message: String) {
    messageCollector.report(CompilerMessageSeverity.LOGGING, "$LOG_PREFIX $message")
  }

  private fun IrDeclaration.reportError(message: String) {
    val location = file.locationOf(this)
    messageCollector.report(CompilerMessageSeverity.ERROR, "$LOG_PREFIX $message", location)
  }

  /** Finds the line and column of [irElement] within this file. */
  private fun IrFile.locationOf(irElement: IrElement?): CompilerMessageSourceLocation {
    val sourceRangeInfo =
      fileEntry.getSourceRangeInfo(
        beginOffset = irElement?.startOffset ?: SYNTHETIC_OFFSET,
        endOffset = irElement?.endOffset ?: SYNTHETIC_OFFSET,
      )
    return CompilerMessageLocationWithRange.create(
      path = sourceRangeInfo.filePath,
      lineStart = sourceRangeInfo.startLineNumber + 1,
      columnStart = sourceRangeInfo.startColumnNumber + 1,
      lineEnd = sourceRangeInfo.endLineNumber + 1,
      columnEnd = sourceRangeInfo.endColumnNumber + 1,
      lineContent = null,
    )!!
  }
}
