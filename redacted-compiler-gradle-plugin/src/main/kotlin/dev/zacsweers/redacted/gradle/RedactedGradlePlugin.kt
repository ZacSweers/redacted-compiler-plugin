package dev.zacsweers.redacted.gradle

import com.android.builder.model.BuildType
import com.android.builder.model.ProductFlavor
import org.gradle.api.Action
import org.gradle.api.Plugin
import org.gradle.api.Project

class RedactedGradlePlugin : Plugin<Project> {
  override fun apply(target: Project) {
    target.extensions.create("redacted", RedactedPluginExtension::class.java)
  }
}

open class RedactedPluginExtension {
  var redactedAnnotation: String? = null
  var enabled: Boolean = true
  var replacementString: String = "██"
  internal var variantFilter: Action<VariantFilter>? = null

  /**
   * Applies a variant filter for Android.
   *
   * @param action the configure action for the [VariantFilter]
   */
  fun androidVariantFilter(action: Action<VariantFilter>) {
    this.variantFilter = action
  }
}

interface VariantFilter {
  /**
   * Overrides whether or not to enable this particular variant. Default is whatever is declared in
   * the extension.
   */
  fun overrideEnabled(enabled: Boolean)

  /**
   * Returns the Build Type.
   */
  val buildType: BuildType

  /**
   * Returns the list of flavors, or an empty list.
   */
  val flavors: List<ProductFlavor>

  /**
   * Returns the unique variant name.
   */
  val name: String?
}
