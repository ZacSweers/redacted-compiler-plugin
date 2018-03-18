package io.sweers.redacted.compiler

import io.sweers.redacted.annotation.Redacted
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageLocation
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSeverity.ERROR
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSeverity.WARNING
import org.jetbrains.kotlin.cli.common.messages.MessageCollector
import org.jetbrains.kotlin.codegen.AsmUtil.correctElementType
import org.jetbrains.kotlin.codegen.AsmUtil.genInvokeAppendMethod
import org.jetbrains.kotlin.codegen.AsmUtil.genStringBuilderConstructor
import org.jetbrains.kotlin.codegen.ExpressionCodegen
import org.jetbrains.kotlin.codegen.FunctionGenerationStrategy.CodegenBased
import org.jetbrains.kotlin.codegen.ImplementationBodyCodegen
import org.jetbrains.kotlin.codegen.JvmCodegenUtil.couldUseDirectAccessToProperty
import org.jetbrains.kotlin.codegen.context.FieldOwnerContext
import org.jetbrains.kotlin.codegen.context.MethodContext
import org.jetbrains.kotlin.codegen.extensions.ExpressionCodegenExtension
import org.jetbrains.kotlin.codegen.state.GenerationState
import org.jetbrains.kotlin.codegen.state.KotlinTypeMapper
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.descriptors.PropertyDescriptor
import org.jetbrains.kotlin.descriptors.SimpleFunctionDescriptor
import org.jetbrains.kotlin.descriptors.impl.referencedProperty
import org.jetbrains.kotlin.incremental.components.NoLookupLocation.WHEN_GET_ALL_DESCRIPTORS
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.descriptorUtil.annotationClass
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameSafe
import org.jetbrains.kotlin.resolve.jvm.AsmTypes.JAVA_STRING_TYPE
import org.jetbrains.kotlin.resolve.jvm.diagnostics.JvmDeclarationOrigin
import org.jetbrains.kotlin.resolve.jvm.diagnostics.JvmDeclarationOriginKind
import org.jetbrains.kotlin.resolve.jvm.jvmSignature.JvmMethodSignature
import org.jetbrains.org.objectweb.asm.Type
import org.jetbrains.org.objectweb.asm.Type.getObjectType
import org.jetbrains.org.objectweb.asm.commons.InstructionAdapter

/**
 * A compiler codegen extension that generates custom toString() implementations that
 * respect [Redacted] annotations.
 */
class RedactedCodegenExtension(
    private val messageCollector: MessageCollector) : ExpressionCodegenExtension {

  private fun log(message: String) {
    messageCollector.report(
        WARNING,
        "*** REDACTED: $message",
        CompilerMessageLocation.create(null))
  }

  override fun generateClassSyntheticParts(codegen: ImplementationBodyCodegen) {
    val targetClass = codegen.descriptor
    log("Reading ${targetClass.name}")
    if (targetClass.isData) {
      log("Not a data class")
      messageCollector.report(ERROR,
          "Redacted is not supported on data classes!",
          // I don't know how to get a location from a descriptor :(
          CompilerMessageLocation.create(null))
      return
    }
    val constructor = targetClass.constructors.first { it.isPrimary }
    val properties: List<PropertyDescriptor> = constructor.valueParameters
//        .filter { it.hasValOrVar() }
        .mapNotNull { codegen.bindingContext.get(BindingContext.VALUE_PARAMETER_AS_PROPERTY, it) }

    val redactedParams = properties
        .filter {
          log("Reading param ${it.name}")
          log("Property is ${it.referencedProperty}")
          log("Annotations are ${it.annotations.getAllAnnotations().joinToString { it.annotation.fqName.toString() }}")
          log("Property annotations ${it.referencedProperty?.annotations?.joinToString { it.annotationClass?.fqNameSafe.toString() }}")
          it.isRedacted
        }
    if (redactedParams.isEmpty()) {
      log("No redacted params")
      return
    }

    log("Reading params")
    val finalProperties = properties
        .map { it to it.isRedacted }
    log("Found params: ${finalProperties.joinToString { it.first.name.asString() }}")

    targetClass.writeToStringFunction(codegen, finalProperties)
  }

  private fun ClassDescriptor.writeToStringFunction(
      codegen: ImplementationBodyCodegen,
      propertiesToSerialize: List<Pair<PropertyDescriptor, Boolean>>
  ): Unit? {
    log("Writing toString()")

    return findToStringFunction()?.write(codegen) {
      genStringBuilderConstructor(v)
      var first = true
      for ((propertyDescriptor, isRedacted) in propertiesToSerialize) {
        val possibleValue = if (isRedacted) "\"██\"" else ""
        if (isRedacted) {
          log("Writing redacted property ${propertyDescriptor.name}")
        }
        if (first) {
          v.aconst("$name(${propertyDescriptor.name.asString()}=$possibleValue")
          first = false
        } else {
          v.aconst(", ${propertyDescriptor.name.asString()}=$possibleValue")
        }
        genInvokeAppendMethod(v, JAVA_STRING_TYPE)

        if (!isRedacted) {
          log("Writing non-redacted property ${propertyDescriptor.name}")
          var type = genPropertyOnStack(state,
              state.typeMapper,
              v,
              context,
              propertyDescriptor,
              /*copied from descriptor.classAsmType*/
              getObjectType(state.typeMapper.classInternalName(this@writeToStringFunction)),
              0)
          if (type.sort == Type.ARRAY) {
            val elementType = correctElementType(type)
            if (elementType.sort == Type.OBJECT || elementType.sort == Type.ARRAY) {
              v.invokestatic(
                  "java/util/Arrays",
                  "toString",
                  "([Ljava/lang/Object;)Ljava/lang/String;",
                  false
              )
              type = JAVA_STRING_TYPE
            } else {
              if (elementType.sort != Type.CHAR) {
                v.invokestatic(
                    "java/util/Arrays",
                    "toString",
                    "(${type.descriptor})Ljava/lang/String;",
                    false
                )
                type = JAVA_STRING_TYPE
              }
            }
          }
          genInvokeAppendMethod(v, type)
        }
      }

      v.aconst(")")
      genInvokeAppendMethod(v, JAVA_STRING_TYPE)

      v.invokevirtual("java/lang/StringBuilder", "toString", "()Ljava/lang/String;", false)
      v.areturn(JAVA_STRING_TYPE)
    }
  }

  private fun genPropertyOnStack(
      state: GenerationState,
      typeMapper: KotlinTypeMapper,
      iv: InstructionAdapter,
      context: MethodContext,
      propertyDescriptor: PropertyDescriptor,
      classAsmType: Type,
      index: Int
  ): Type {
    iv.load(index, classAsmType)
    return if (couldUseDirectAccessToProperty(
            propertyDescriptor,
            true,
            false,
            context,
            state.shouldInlineConstVals
        )) {
      val type = typeMapper.mapType(propertyDescriptor.type)
      val fieldName = (context.parentContext as FieldOwnerContext).getFieldName(
          propertyDescriptor, false)
      iv.getfield(classAsmType.internalName, fieldName, type.descriptor)
      type
    } else {
      val method = typeMapper.mapAsmMethod(propertyDescriptor.getter!!)
      iv.invokevirtual(classAsmType.internalName, method.name, method.descriptor, false)
      method.returnType
    }
  }
}

private fun FunctionDescriptor.write(
    codegen: ImplementationBodyCodegen,
    code: ExpressionCodegen.() -> Unit) {
  val declarationOrigin = JvmDeclarationOrigin(JvmDeclarationOriginKind.OTHER, null, this)
  // Red in the IDE but they do exist! MemberCodegen has state and functionCodegen
  codegen.functionCodegen.generateMethod(declarationOrigin, this,
      object : CodegenBased(codegen.state) {
        override fun doGenerateBody(e: ExpressionCodegen, signature: JvmMethodSignature) =
            e.code()
      })
}

private fun ClassDescriptor.findToStringFunction(): SimpleFunctionDescriptor? {
  return unsubstitutedMemberScope
      .getContributedFunctions(Name.identifier("toString"), WHEN_GET_ALL_DESCRIPTORS)
      .first()
}

// Can't import the annotation here because it's for some reason not visible when the plugin runs
private val REDACTED_CLASS_FQNAME = FqName("io.sweers.redacted.annotation.Redacted")

private val PropertyDescriptor.isRedacted: Boolean
  get() {
    return this.annotations.hasAnnotation(REDACTED_CLASS_FQNAME)
  }
