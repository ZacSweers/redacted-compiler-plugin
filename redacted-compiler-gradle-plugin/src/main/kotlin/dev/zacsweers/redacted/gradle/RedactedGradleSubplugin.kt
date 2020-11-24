package dev.zacsweers.redacted.gradle

import com.android.build.gradle.api.BaseVariant
import com.android.build.gradle.api.TestVariant
import com.android.build.gradle.api.UnitTestVariant
import com.android.builder.model.BuildType
import com.android.builder.model.ProductFlavor
import org.gradle.api.Project
import org.gradle.api.provider.Provider
import org.jetbrains.kotlin.gradle.plugin.KotlinCompilation
import org.jetbrains.kotlin.gradle.plugin.KotlinCompilerPluginSupportPlugin
import org.jetbrains.kotlin.gradle.plugin.KotlinPlatformType
import org.jetbrains.kotlin.gradle.plugin.SubpluginArtifact
import org.jetbrains.kotlin.gradle.plugin.SubpluginOption
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinJvmAndroidCompilation

class RedactedGradleSubplugin : KotlinCompilerPluginSupportPlugin {

  override fun apply(target: Project) {
    target.extensions.create("redacted", RedactedPluginExtension::class.java)
  }

  override fun getCompilerPluginId(): String = "redacted-compiler-plugin"

  override fun getPluginArtifact(): SubpluginArtifact =
      SubpluginArtifact(
          groupId = "dev.zacsweers.redacted",
          artifactId = "redacted-compiler-plugin",
          version = VERSION
      )

  override fun isApplicable(kotlinCompilation: KotlinCompilation<*>): Boolean {
    return (kotlinCompilation.platformType == KotlinPlatformType.jvm || kotlinCompilation.platformType == KotlinPlatformType.androidJvm)
  }

  override fun applyToCompilation(
      kotlinCompilation: KotlinCompilation<*>
  ): Provider<List<SubpluginOption>> {
    val project = kotlinCompilation.target.project
    val extension = project.extensions.findByType(RedactedPluginExtension::class.java)
        ?: RedactedPluginExtension()
    val annotation = extension.redactedAnnotation

    // Default annotation is used, so add it as a dependency
    if (annotation == DEFAULT_ANNOTATION) {
      project.dependencies.add("implementation",
          "dev.zacsweers.redacted:redacted-compiler-plugin-annotations:$VERSION")
    }

    val extensionFilter = extension.variantFilter
    var enabled = extension.enabled
    var redactAllDataClasses = extension.redactAllDataClasses

    // If we're an android setup
    if (extensionFilter != null && kotlinCompilation is KotlinJvmAndroidCompilation) {
      val variantData = kotlinCompilation.androidVariant
      val variant = unwrapVariant(variantData)
      if (variant != null) {
        project.logger.debug("Resolving enabled status for android variant ${variant.name}")
        val filter = VariantFilterImpl(variant, enabled,redactAllDataClasses)
        extensionFilter.execute(filter)
        project.logger.debug("Variant '${variant.name}' redacted flag set to ${filter._enabled} redactAllDataClasses set to ${filter._redactAllDataClasses}")
        enabled = filter._enabled
        redactAllDataClasses = filter._redactAllDataClasses
      } else {
        project.logger.lifecycle(
            "Unable to resolve variant type for $variantData. Falling back to default behavior of '$enabled' with redactAllDataClasses as ${redactAllDataClasses}")
      }
    }

    return project.provider {
      listOf(
          SubpluginOption(key = "enabled", value = enabled.toString()),
          SubpluginOption(key = "replacementString", value = extension.replacementString),
          SubpluginOption(key = "redactedAnnotation", value = annotation),
          SubpluginOption(key = "redactAllDataClasses", value = redactAllDataClasses.toString())
      )
    }
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
    else -> null
  }
}

private class VariantFilterImpl(variant: BaseVariant, enableDefault: Boolean,redactAllDataClassesDefault: Boolean) : VariantFilter {
  var _enabled: Boolean = enableDefault
  var _redactAllDataClasses: Boolean = redactAllDataClassesDefault

  override fun overrideEnabled(enabled: Boolean) {
    this._enabled = enabled
  }

  override fun overrideRedactAllDataClasses(redactAllDataClasses: Boolean) {
    this._redactAllDataClasses = redactAllDataClasses
  }

  override val buildType: BuildType = variant.buildType
  override val flavors: List<ProductFlavor> = variant.productFlavors
  override val name: String = variant.name
}
