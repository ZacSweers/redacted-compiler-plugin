package dev.zacsweers.redacted.gradle

import com.android.build.gradle.api.BaseVariant
import com.android.build.gradle.api.TestVariant
import com.android.build.gradle.api.UnitTestVariant
import com.android.builder.model.BuildType
import com.android.builder.model.ProductFlavor
import com.google.auto.service.AutoService
import org.gradle.api.Project
import org.gradle.api.tasks.compile.AbstractCompile
import org.jetbrains.kotlin.gradle.dsl.KotlinCommonOptions
import org.jetbrains.kotlin.gradle.internal.KaptVariantData
import org.jetbrains.kotlin.gradle.plugin.KotlinCompilation
import org.jetbrains.kotlin.gradle.plugin.KotlinGradleSubplugin
import org.jetbrains.kotlin.gradle.plugin.SubpluginArtifact
import org.jetbrains.kotlin.gradle.plugin.SubpluginOption

@AutoService(KotlinGradleSubplugin::class)
class RedactedGradleSubplugin : KotlinGradleSubplugin<AbstractCompile> {

  override fun isApplicable(project: Project, task: AbstractCompile): Boolean =
      project.plugins.hasPlugin(RedactedGradlePlugin::class.java)

  override fun getCompilerPluginId(): String = "redacted-compiler-plugin"

  override fun getPluginArtifact(): SubpluginArtifact =
      SubpluginArtifact(
          groupId = "dev.zacsweers.redacted",
          artifactId = "redacted-compiler-plugin",
          version = VERSION
      )

  override fun apply(
      project: Project,
      kotlinCompile: AbstractCompile,
      javaCompile: AbstractCompile?,
      variantData: Any?,
      androidProjectHandler: Any?,
      kotlinCompilation: KotlinCompilation<KotlinCommonOptions>?
  ): List<SubpluginOption> {
    val extension = project.extensions.findByType(RedactedPluginExtension::class.java) ?: RedactedPluginExtension()
    val annotation = requireNotNull(extension.redactedAnnotation) {
      "Redacted annotation must be specified!"
    }

    val extensionFilter = extension.variantFilter
    var enabled = extension.enabled

    // If we're an android setup
    if (variantData != null && extensionFilter != null) {
      val variant = unwrapVariant(variantData)
      if (variant != null) {
        project.logger.debug("Resolving enabled status for android variant ${variant.name}")
        val filter = VariantFilterImpl(variant, enabled)
        extensionFilter.execute(filter)
        project.logger.debug("Variant '${variant.name}' redacted flag set to ${filter._enabled}")
        enabled = filter._enabled
      } else {
        project.logger.lifecycle("Unable to resolve variant type for $variantData. Falling back to default behavior of '$enabled'")
      }
    }

    return listOf(
        SubpluginOption(key = "enabled", value = enabled.toString()),
        SubpluginOption(key = "replacementString", value = extension.replacementString),
        SubpluginOption(key = "redactedAnnotation", value = annotation)
    )
  }
}

private fun unwrapVariant(variantData: Any?): BaseVariant? {
  return when (variantData) {
    is BaseVariant -> {
      when (variantData) {
        is TestVariant -> variantData.testedVariant
        is UnitTestVariant -> variantData.testedVariant as? BaseVariant
        else -> variantData
      }
    }
    is KaptVariantData<*> -> unwrapVariant(variantData.variantData)
    else -> null
  }
}

private class VariantFilterImpl(variant: BaseVariant, enableDefault: Boolean) : VariantFilter {
  var _enabled: Boolean = enableDefault

  override fun overrideEnabled(enabled: Boolean) {
    this._enabled = enabled
  }

  override val buildType: BuildType = variant.buildType
  override val flavors: List<ProductFlavor> = variant.productFlavors
  override val name: String = variant.name
}
