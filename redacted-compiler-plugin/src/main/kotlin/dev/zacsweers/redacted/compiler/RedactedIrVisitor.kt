// Copyright (C) 2021 Zac Sweers
// SPDX-License-Identifier: Apache-2.0
@file:OptIn(UnsafeDuringIrConstructionAPI::class)

package dev.zacsweers.redacted.compiler

import org.jetbrains.kotlin.backend.common.IrElementTransformerVoidWithContext
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.backend.common.lower.DeclarationIrBuilder
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.builders.IrBlockBodyBuilder
import org.jetbrains.kotlin.ir.builders.irBlockBody
import org.jetbrains.kotlin.ir.builders.irCall
import org.jetbrains.kotlin.ir.builders.irConcat
import org.jetbrains.kotlin.ir.builders.irGet
import org.jetbrains.kotlin.ir.builders.irGetField
import org.jetbrains.kotlin.ir.builders.irReturn
import org.jetbrains.kotlin.ir.builders.irString
import org.jetbrains.kotlin.ir.declarations.IrAnnotationContainer
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.declarations.IrParameterKind
import org.jetbrains.kotlin.ir.declarations.IrProperty
import org.jetbrains.kotlin.ir.declarations.IrSimpleFunction
import org.jetbrains.kotlin.ir.declarations.IrValueParameter
import org.jetbrains.kotlin.ir.expressions.addArgument
import org.jetbrains.kotlin.ir.symbols.UnsafeDuringIrConstructionAPI
import org.jetbrains.kotlin.ir.types.isArray
import org.jetbrains.kotlin.ir.types.isString
import org.jetbrains.kotlin.ir.util.getAllSuperclasses
import org.jetbrains.kotlin.ir.util.hasAnnotation
import org.jetbrains.kotlin.ir.util.isPrimitiveArray
import org.jetbrains.kotlin.ir.util.parentClassOrNull
import org.jetbrains.kotlin.ir.util.primaryConstructor
import org.jetbrains.kotlin.ir.util.properties
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.util.OperatorNameConventions

internal class RedactedIrVisitor(
  private val pluginContext: IrPluginContext,
  private val redactedAnnotations: Set<ClassId>,
  private val unRedactedAnnotations: Set<ClassId>,
  private val replacementString: String,
) : IrElementTransformerVoidWithContext() {

  private class Property(
    val ir: IrProperty,
    val isRedacted: Boolean,
    val isUnredacted: Boolean,
    val parameter: IrValueParameter,
  )

  override fun visitFunctionNew(declaration: IrFunction): IrStatement {
    if (declaration !is IrSimpleFunction) return super.visitFunctionNew(declaration)
    if (!declaration.isToStringFromAny()) return super.visitFunctionNew(declaration)

    val declarationParent =
      declaration.parentClassOrNull ?: return super.visitFunctionNew(declaration)
    val primaryConstructor =
      declarationParent.primaryConstructor ?: return super.visitFunctionNew(declaration)
    val constructorParameters =
      primaryConstructor.parameters
        .filter { it.kind == IrParameterKind.Regular }
        .associateBy { it.name.asString() }

    val properties = mutableListOf<Property>()
    val classIsRedacted = redactedAnnotations.any(declarationParent::hasAnnotation)
    val classIsUnredacted = unRedactedAnnotations.any(declarationParent::hasAnnotation)
    val supertypeIsRedacted by unsafeLazy {
      declarationParent.getAllSuperclasses().any { redactedAnnotations.any(it::hasAnnotation) }
    }
    var anyRedacted = false
    var anyUnredacted = false
    for (prop in declarationParent.properties) {
      val parameter = constructorParameters[prop.name.asString()] ?: continue
      val isRedacted = prop.isRedacted || parameter.isRedacted
      val isUnredacted = prop.isUnredacted || parameter.isUnredacted
      if (isRedacted) {
        anyRedacted = true
      }
      if (isUnredacted) {
        anyUnredacted = true
      }
      properties += Property(prop, isRedacted, isUnredacted, parameter)
    }

    if (classIsRedacted || supertypeIsRedacted || classIsUnredacted || anyRedacted) {
      declaration.convertToGeneratedToString(
        properties,
        classIsRedacted,
        classIsUnredacted,
        supertypeIsRedacted,
        anyUnredacted,
      )
    }

    return super.visitFunctionNew(declaration)
  }

  private fun IrFunction.isToStringFromAny(): Boolean =
    name == OperatorNameConventions.TO_STRING &&
      parameters.singleOrNull()?.kind == IrParameterKind.DispatchReceiver &&
      returnType.isString()

  private fun IrSimpleFunction.convertToGeneratedToString(
    properties: List<Property>,
    classIsRedacted: Boolean,
    classIsUnredacted: Boolean,
    supertypeIsRedacted: Boolean,
    hasUnredactedProperties: Boolean,
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
          classIsUnredacted = classIsUnredacted,
          supertypeIsRedacted = supertypeIsRedacted,
          hasUnredactedProperties = hasUnredactedProperties,
        )
      }

    isFakeOverride = false
  }

  private val IrAnnotationContainer.isRedacted: Boolean
    get() = redactedAnnotations.any(::hasAnnotation)

  private val IrAnnotationContainer.isUnredacted: Boolean
    get() = unRedactedAnnotations.any(::hasAnnotation)

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
    classIsUnredacted: Boolean,
    supertypeIsRedacted: Boolean,
    hasUnredactedProperties: Boolean,
  ) {
    val irConcat = irConcat()
    irConcat.addArgument(irString(irClass.name.asString() + "("))
    if (classIsRedacted && !classIsUnredacted && !hasUnredactedProperties) {
      irConcat.addArgument(irString(replacementString))
    } else {
      var first = true
      for (property in irProperties) {
        if (!first) irConcat.addArgument(irString(", "))

        irConcat.addArgument(irString(property.ir.name.asString() + "="))
        val redactProperty =
          property.isRedacted ||
            (classIsRedacted && !property.isUnredacted) ||
            (supertypeIsRedacted && !classIsUnredacted && !property.isUnredacted)
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
                .apply { arguments[0] = irPropertyValue }
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
    irGet(irFunction.dispatchReceiverParameter!!)
}
