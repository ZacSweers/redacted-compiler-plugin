package dev.zacsweers.redacted.compiler

import org.jetbrains.annotations.NotNull
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageLocation
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSeverity
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSeverity.LOGGING
import org.jetbrains.kotlin.cli.common.messages.MessageCollector
import org.jetbrains.kotlin.cli.common.messages.MessageUtil
import org.jetbrains.kotlin.codegen.AsmUtil.correctElementType
import org.jetbrains.kotlin.codegen.DescriptorAsmUtil.genInvokeAppendMethod
import org.jetbrains.kotlin.codegen.ClassBuilder
import org.jetbrains.kotlin.codegen.FunctionCodegen
import org.jetbrains.kotlin.codegen.ImplementationBodyCodegen
import org.jetbrains.kotlin.codegen.JvmKotlinType
import org.jetbrains.kotlin.codegen.OwnerKind.ERASED_INLINE_CLASS
import org.jetbrains.kotlin.codegen.StackValue
import org.jetbrains.kotlin.codegen.StringConcatGenerator
import org.jetbrains.kotlin.codegen.context.FieldOwnerContext
import org.jetbrains.kotlin.codegen.context.MethodContext
import org.jetbrains.kotlin.codegen.extensions.ExpressionCodegenExtension
import org.jetbrains.kotlin.codegen.serialization.JvmSerializationBindings
import org.jetbrains.kotlin.codegen.state.GenerationState
import org.jetbrains.kotlin.codegen.state.KotlinTypeMapper
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.descriptors.PropertyDescriptor
import org.jetbrains.kotlin.descriptors.SimpleFunctionDescriptor
import org.jetbrains.kotlin.descriptors.annotations.Annotated
import org.jetbrains.kotlin.descriptors.impl.referencedProperty
import org.jetbrains.kotlin.incremental.components.NoLookupLocation.WHEN_GET_ALL_DESCRIPTORS
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.KtClassOrObject
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.descriptorUtil.annotationClass
import org.jetbrains.kotlin.resolve.descriptorUtil.builtIns
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameSafe
import org.jetbrains.kotlin.resolve.jvm.AsmTypes.JAVA_STRING_TYPE
import org.jetbrains.kotlin.resolve.jvm.diagnostics.OtherOrigin
import org.jetbrains.kotlin.resolve.source.getPsi
import org.jetbrains.kotlin.resolve.substitutedUnderlyingType
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.org.objectweb.asm.AnnotationVisitor
import org.jetbrains.org.objectweb.asm.Opcodes
import org.jetbrains.org.objectweb.asm.Type
import org.jetbrains.org.objectweb.asm.commons.InstructionAdapter
import org.jetbrains.org.objectweb.asm.commons.Method

internal const val LOG_PREFIX = "*** REDACTED"

/**
 * A compiler codegen extension that generates custom toString() implementations that
 * respect `Redacted` annotations.
 */
internal class RedactedCodegenExtension(
    private val messageCollector: MessageCollector,
    private val replacementString: String,
    private val fqRedactedAnnotation: FqName
) : ExpressionCodegenExtension {

  private fun log(message: String) {
    messageCollector.report(
        LOGGING,
        "$LOG_PREFIX $message",
        CompilerMessageLocation.create(null))
  }

  override fun generateClassSyntheticParts(codegen: ImplementationBodyCodegen) {
    val targetClass = codegen.descriptor
    log("Reading ${targetClass.name}")

    val classIsRedacted = targetClass.isRedacted(fqRedactedAnnotation)

    val redactedParams: Boolean
    val properties: List<PropertyDescriptor>
    if (classIsRedacted) {
      redactedParams = false
      properties = emptyList()
    } else {
      val constructor = targetClass.constructors.firstOrNull { it.isPrimary } ?: return
      properties = constructor.valueParameters
          .mapNotNull { codegen.bindingContext.get(BindingContext.VALUE_PARAMETER_AS_PROPERTY, it) }

      redactedParams = properties
          .any { property ->
            log("Reading param ${property.name}")
            log("Property is ${property.referencedProperty}")
            log("Property annotations ${property.referencedProperty?.annotations?.joinToString { it.annotationClass?.fqNameSafe.toString() }}")
            property.isRedacted(fqRedactedAnnotation)
          }
    }
    if (!redactedParams && !classIsRedacted) {
      log("No redacted params")
      return
    } else if (!targetClass.isData) {
      log("Not a data class")
      val psi = codegen.descriptor.source.getPsi()
      val location = MessageUtil.psiElementToMessageLocation(psi)
      messageCollector.report(CompilerMessageSeverity.ERROR,
          "@Redacted is only supported on data classes!",
          location
      )
      return
    }

    ToStringGenerator(
        declaration = codegen.myClass as KtClassOrObject,
        classDescriptor = targetClass,
        classAsmType = codegen.typeMapper.mapType(targetClass),
        fieldOwnerContext = codegen.context,
        v = codegen.v,
        generationState = codegen.state,
        replacementString = replacementString,
        fqRedactedAnnotation = fqRedactedAnnotation
    ).generateToStringMethod(
        targetClass.findToStringFunction(),
        properties
    )
  }
}

private class ToStringGenerator(
    private val declaration: KtClassOrObject,
    private val classDescriptor: ClassDescriptor,
    private val classAsmType: Type,
    private val fieldOwnerContext: FieldOwnerContext<*>,
    private val v: ClassBuilder,
    private val generationState: GenerationState,
    private val replacementString: String,
    private val fqRedactedAnnotation: FqName
) {
  private val typeMapper: KotlinTypeMapper = generationState.typeMapper
  private val underlyingType = JvmKotlinType(
      typeMapper.mapType(classDescriptor),
      classDescriptor.defaultType.substitutedUnderlyingType())
  private val isInErasedInlineClass = fieldOwnerContext.contextKind == ERASED_INLINE_CLASS

  private val toStringDesc: String
    get() = "($firstParameterDesc)Ljava/lang/String;"

  private val firstParameterDesc: String
    get() {
      return if (isInErasedInlineClass) {
        underlyingType.type
            .descriptor
      } else {
        ""
      }
    }

  private val access: Int
    get() {
      var access = Opcodes.ACC_PUBLIC
      if (isInErasedInlineClass) {
        access = access or Opcodes.ACC_STATIC
      }

      return access
    }

  fun generateToStringMethod(function: FunctionDescriptor, properties: List<PropertyDescriptor>) {
    val context = fieldOwnerContext.intoFunction(function)
    val methodOrigin = OtherOrigin(function)
    val toStringMethodName = mapFunctionName(function)
    val mv = v.newMethod(methodOrigin, access, toStringMethodName, toStringDesc, null, null)

    if (!isInErasedInlineClass && classDescriptor.isInline) {
      FunctionCodegen.generateMethodInsideInlineClassWrapper(
          methodOrigin,
          function,
          classDescriptor,
          mv,
          typeMapper)
      return
    }

    if (!isInErasedInlineClass) {
      visitEndForAnnotationVisitor(
          mv.visitAnnotation(Type.getDescriptor(NotNull::class.java), false))
    }

    if (!generationState.classBuilderMode.generateBodies) {
      FunctionCodegen.endVisit(mv, toStringMethodName, declaration)
      return
    }

    val iv = InstructionAdapter(mv)

    val generator = StringConcatGenerator.create(generationState, iv)
    mv.visitCode()
    generator.genStringBuilderConstructorIfNeded()

    if (properties.isEmpty()) {
      // This is a redacted class, so just emit a single replacementString
      iv.aconst(classDescriptor.name.toString() + "(" + replacementString)
      generator.invokeAppend(JAVA_STRING_TYPE)
    } else {
      var first = true
      for (propertyDescriptor in properties) {
        val isRedacted = propertyDescriptor.isRedacted(fqRedactedAnnotation)
        val possibleValue = if (isRedacted) replacementString else ""
        if (first) {
          generator.addStringConstant(
              "${classDescriptor.name}(${propertyDescriptor.name.asString()}=$possibleValue")
          first = false
        } else {
          generator.addStringConstant(", ${propertyDescriptor.name.asString()}=$possibleValue")
        }

        if (!isRedacted) {
          val type = genOrLoadOnStack(iv, context, propertyDescriptor, 0)
          var asmType = type.type
          var kotlinType: KotlinType? = type.kotlinType

          if (asmType.sort == Type.ARRAY) {
            val elementType = correctElementType(asmType)
            if (elementType.sort == Type.OBJECT || elementType.sort == Type.ARRAY) {
              iv.invokestatic("java/util/Arrays", "toString",
                  "([Ljava/lang/Object;)Ljava/lang/String;", false)
              asmType = JAVA_STRING_TYPE
              kotlinType = function.builtIns.stringType
            } else if (elementType.sort != Type.CHAR) {
              iv.invokestatic("java/util/Arrays", "toString",
                  "(" + asmType.descriptor + ")Ljava/lang/String;", false)
              asmType = JAVA_STRING_TYPE
              kotlinType = function.builtIns.stringType
            }
          }
          genInvokeAppendMethod(generator, asmType, kotlinType, typeMapper, StackValue.onStack(asmType))
        }
      }
    }

    generator.addStringConstant(")")

    generator.genToString()
    iv.areturn(JAVA_STRING_TYPE)

    FunctionCodegen.endVisit(mv, toStringMethodName, declaration)

    recordMethodForFunctionIfRequired(function, toStringMethodName, toStringDesc)
  }

  private fun recordMethodForFunctionIfRequired(
      function: FunctionDescriptor,
      name: String,
      desc: String
  ) {
    if (isInErasedInlineClass) {
      v.serializationBindings.put(JvmSerializationBindings.METHOD_FOR_FUNCTION, function, Method(name,
          desc))
    }
  }

  private fun mapFunctionName(functionDescriptor: FunctionDescriptor): String {
    return typeMapper.mapFunctionName(functionDescriptor, fieldOwnerContext.contextKind)
  }

  private fun visitEndForAnnotationVisitor(annotation: AnnotationVisitor?) {
    annotation?.visitEnd()
  }

  @Suppress("SameParameterValue")
  private fun genOrLoadOnStack(iv: InstructionAdapter,
      context: MethodContext,
      propertyDescriptor: PropertyDescriptor,
      index: Int): JvmKotlinType {
    return if (fieldOwnerContext.contextKind == ERASED_INLINE_CLASS) {
      iv.load(index, underlyingType.type)
      underlyingType
    } else {
      ImplementationBodyCodegen.genPropertyOnStack(iv,
          context,
          propertyDescriptor,
          classAsmType,
          index,
          generationState)
    }
  }
}

private fun ClassDescriptor.findToStringFunction(): SimpleFunctionDescriptor {
  return unsubstitutedMemberScope
      .getContributedFunctions(Name.identifier("toString"), WHEN_GET_ALL_DESCRIPTORS)
      .first()
}

internal fun Annotated.isRedacted(redactedAnnotation: FqName): Boolean {
  return annotations.hasAnnotation(redactedAnnotation)
}
