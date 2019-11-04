package io.sweers.redacted.compiler

import io.sweers.redacted.annotation.Redacted
import org.jetbrains.annotations.NotNull
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageLocation
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSeverity
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSeverity.WARNING
import org.jetbrains.kotlin.cli.common.messages.MessageCollector
import org.jetbrains.kotlin.codegen.AsmUtil
import org.jetbrains.kotlin.codegen.ClassBuilder
import org.jetbrains.kotlin.codegen.FunctionCodegen
import org.jetbrains.kotlin.codegen.ImplementationBodyCodegen
import org.jetbrains.kotlin.codegen.JvmKotlinType
import org.jetbrains.kotlin.codegen.OwnerKind.ERASED_INLINE_CLASS
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
import org.jetbrains.kotlin.psi.KtClassOrObject
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.descriptorUtil.annotationClass
import org.jetbrains.kotlin.resolve.descriptorUtil.builtIns
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameSafe
import org.jetbrains.kotlin.resolve.jvm.AsmTypes
import org.jetbrains.kotlin.resolve.jvm.diagnostics.OtherOrigin
import org.jetbrains.kotlin.resolve.substitutedUnderlyingType
import org.jetbrains.org.objectweb.asm.AnnotationVisitor
import org.jetbrains.org.objectweb.asm.Opcodes
import org.jetbrains.org.objectweb.asm.Type
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
//    if (targetClass.isData) {
//      log("Not a data class")
//      messageCollector.report(CompilerMessageSeverity.ERROR,
//          "Redacted is not supported on data classes!",
//          // I don't know how to get a location from a descriptor :(
//          CompilerMessageLocation.create(null))
//      return
//    }
    val constructor = targetClass.constructors.first { it.isPrimary }
    val properties: List<PropertyDescriptor> = constructor.valueParameters
//        .filter { it.hasValOrVar() }
        .mapNotNull { codegen.bindingContext.get(BindingContext.VALUE_PARAMETER_AS_PROPERTY, it) }

    val redactedParams = properties
        .filter {
          log("Reading param ${it.name}")
          log("Property is ${it.referencedProperty}")
//          log("Annotations are ${it.annotations.getAllAnnotations().joinToString { it.annotation.fqName.toString() }}")
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

    ToStringGenerator(
        declaration = codegen.myClass as KtClassOrObject,
        classDescriptor = targetClass,
        classAsmType = codegen.typeMapper.mapType(targetClass),
        fieldOwnerContext = codegen.context,
        v = codegen.v,
        generationState = codegen.state
    ).generateToStringMethod(
        targetClass.findToStringFunction()!!,
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
    private val generationState: GenerationState
) {
  private val typeMapper: KotlinTypeMapper = generationState.typeMapper
  private val underlyingType: JvmKotlinType

  private val toStringDesc: String
    get() = "($firstParameterDesc)Ljava/lang/String;"

  private val firstParameterDesc: String
    get() {
      return if (fieldOwnerContext.contextKind == ERASED_INLINE_CLASS) {
        underlyingType.type
            .descriptor
      } else {
        ""
      }
    }

  private val access: Int
    get() {
      var access = Opcodes.ACC_PUBLIC
      if (fieldOwnerContext.contextKind == ERASED_INLINE_CLASS) {
        access = access or Opcodes.ACC_STATIC
      }

      return access
    }

  init {
    this.underlyingType = JvmKotlinType(
        typeMapper.mapType(classDescriptor),
        classDescriptor.defaultType.substitutedUnderlyingType())
  }

  fun generateToStringMethod(function: FunctionDescriptor,
      properties: List<PropertyDescriptor>) {
    val context = fieldOwnerContext.intoFunction(function)
    val methodOrigin = OtherOrigin(function)
    val toStringMethodName = mapFunctionName(function)
    val mv = v.newMethod(methodOrigin, access, toStringMethodName, toStringDesc, null, null)

    if (fieldOwnerContext.contextKind != ERASED_INLINE_CLASS && classDescriptor.isInline) {
      FunctionCodegen.generateMethodInsideInlineClassWrapper(
          methodOrigin,
          function,
          classDescriptor,
          mv,
          typeMapper)
      return
    }

    visitEndForAnnotationVisitor(
        mv.visitAnnotation(Type.getDescriptor(NotNull::class.java), false))

    if (!generationState.classBuilderMode.generateBodies) {
      FunctionCodegen.endVisit(mv, toStringMethodName, declaration)
      return
    }

    val iv = InstructionAdapter(mv)

    mv.visitCode()
    AsmUtil.genStringBuilderConstructor(iv)

    var first = true
    for (propertyDescriptor in properties) {
      val isRedacted = propertyDescriptor.isRedacted
      val possibleValue = if (isRedacted) "\"██\"" else ""
      if (first) {
        iv.aconst(classDescriptor.name.toString() + "(" + propertyDescriptor.name
            .asString() + "=$possibleValue")
        first = false
      } else {
        iv.aconst(", " + propertyDescriptor.name
            .asString() + "=$possibleValue")
      }
      AsmUtil.genInvokeAppendMethod(iv, AsmTypes.JAVA_STRING_TYPE, null)

      if (!isRedacted) {
        val type = genOrLoadOnStack(iv, context, propertyDescriptor, 0)
        var asmType = type.type
        var kotlinType = type.kotlinType

        if (asmType.sort == Type.ARRAY) {
          val elementType = AsmUtil.correctElementType(asmType)
          if (elementType.sort == Type.OBJECT || elementType.sort == Type.ARRAY) {
            iv.invokestatic("java/util/Arrays",
                "toString",
                "([Ljava/lang/Object;)Ljava/lang/String;",
                false)
            asmType = AsmTypes.JAVA_STRING_TYPE
            kotlinType = function.builtIns
                .stringType
          } else if (elementType.sort != Type.CHAR) {
            iv.invokestatic("java/util/Arrays",
                "toString",
                "(" + asmType.descriptor + ")Ljava/lang/String;",
                false)
            asmType = AsmTypes.JAVA_STRING_TYPE
            kotlinType = function.builtIns
                .stringType
          }
        }
        AsmUtil.genInvokeAppendMethod(iv, asmType, kotlinType, typeMapper)
      }
    }

    iv.aconst(")")
    AsmUtil.genInvokeAppendMethod(iv, AsmTypes.JAVA_STRING_TYPE, null)

    iv.invokevirtual("java/lang/StringBuilder", "toString", "()Ljava/lang/String;", false)
    iv.areturn(AsmTypes.JAVA_STRING_TYPE)

    FunctionCodegen.endVisit(mv, toStringMethodName, declaration)
  }

  private fun mapFunctionName(functionDescriptor: FunctionDescriptor): String {
    return typeMapper.mapFunctionName(functionDescriptor, fieldOwnerContext.contextKind)
  }

  private fun visitEndForAnnotationVisitor(annotation: AnnotationVisitor?) {
    annotation?.visitEnd()
  }

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
