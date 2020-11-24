package dev.zacsweers.redacted.gradle

import com.android.builder.model.BuildType
import com.android.builder.model.ProductFlavor
import org.gradle.api.Action

internal const val DEFAULT_ANNOTATION = "dev.zacsweers.redacted.annotations.Redacted"

open class RedactedPluginExtension {
  var redactedAnnotation: String = DEFAULT_ANNOTATION
  var enabled: Boolean = true
  var replacementString: String = "██"
  var redactAllDataClasses : Boolean = false
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
   * Overrides whether or not to redact all data classes for this particular variant. Default is whatever is declared in
   * the extension.
   */
  fun overrideRedactAllDataClasses(redactAllDataClasses: Boolean)

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
