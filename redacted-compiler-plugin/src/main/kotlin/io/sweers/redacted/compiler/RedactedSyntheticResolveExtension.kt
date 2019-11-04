package io.sweers.redacted.compiler

import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSeverity
import org.jetbrains.kotlin.cli.common.messages.MessageCollector
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.descriptors.annotations.Annotations
import org.jetbrains.kotlin.descriptors.impl.SimpleFunctionDescriptorImpl
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.descriptorUtil.builtIns
import org.jetbrains.kotlin.resolve.extensions.SyntheticResolveExtension

class RedactedSyntheticResolveExtension(
        val messageCollector: MessageCollector
) : SyntheticResolveExtension {

    override fun generateSyntheticMethods(thisDescriptor: ClassDescriptor, name: Name, bindingContext: BindingContext, fromSupertypes: List<SimpleFunctionDescriptor>, result: MutableCollection<SimpleFunctionDescriptor>) {
        super.generateSyntheticMethods(thisDescriptor, name, bindingContext, fromSupertypes, result)
        if (name.asString() == "toString") {
           result.clear()
            val methodDescriptor = SimpleFunctionDescriptorImpl.create(
                    thisDescriptor,
                    Annotations.EMPTY, name,
                    CallableMemberDescriptor.Kind.SYNTHESIZED, thisDescriptor.source
            )
                    .initialize(
                            null,
                            thisDescriptor.thisAsReceiverParameter,
                            emptyList(),
                            emptyList(),
                            thisDescriptor.builtIns.stringType,
                            Modality.FINAL,
                            Visibilities.PUBLIC
                    )

            result += methodDescriptor
        }
    }

    private fun println(s: Any) {
        messageCollector.report(CompilerMessageSeverity.STRONG_WARNING, s.toString())
    }

}