package dev.zacsweers.redacted.compiler

import org.jetbrains.kotlin.descriptors.CallableMemberDescriptor
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.DescriptorVisibilities
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.descriptors.PropertyDescriptor
import org.jetbrains.kotlin.descriptors.SimpleFunctionDescriptor
import org.jetbrains.kotlin.descriptors.annotations.Annotations
import org.jetbrains.kotlin.descriptors.impl.SimpleFunctionDescriptorImpl
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.descriptorUtil.builtIns
import org.jetbrains.kotlin.resolve.extensions.SyntheticResolveExtension

/**
 * A [SyntheticResolveExtension] that replaces the open toString descriptor
 * with a final descriptor for data classes.
 */
class RedactedSyntheticResolveExtension(
    private val fqRedactedAnnotation: FqName
) : SyntheticResolveExtension {

  override fun generateSyntheticMethods(
      thisDescriptor: ClassDescriptor,
      name: Name,
      bindingContext: BindingContext,
      fromSupertypes: List<SimpleFunctionDescriptor>,
      result: MutableCollection<SimpleFunctionDescriptor>
  ) {
    super.generateSyntheticMethods(thisDescriptor, name, bindingContext, fromSupertypes, result)

    val isRedacted = thisDescriptor.isRedacted(fqRedactedAnnotation) || run {
      val constructor = thisDescriptor.constructors.firstOrNull { it.isPrimary } ?: return
      val properties: List<PropertyDescriptor> = constructor.valueParameters
          .mapNotNull { bindingContext.get(BindingContext.VALUE_PARAMETER_AS_PROPERTY, it) }
      properties.any { it.isRedacted(fqRedactedAnnotation) }
    }

    if (name.asString() == "toString" && isRedacted) {
      // Remove the open toString descriptor
      result.clear()
      // Add a final toString descriptor
      result += SimpleFunctionDescriptorImpl.create(
          thisDescriptor,
          Annotations.EMPTY,
          name,
          CallableMemberDescriptor.Kind.SYNTHESIZED,
          thisDescriptor.source
      ).initialize(
          null,
          thisDescriptor.thisAsReceiverParameter,
          emptyList(),
          emptyList(),
          thisDescriptor.builtIns.stringType,
          Modality.FINAL,
          DescriptorVisibilities.PUBLIC
      )
    }
  }
}
