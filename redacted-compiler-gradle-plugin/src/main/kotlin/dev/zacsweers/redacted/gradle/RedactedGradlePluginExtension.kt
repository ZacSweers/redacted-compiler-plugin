package dev.zacsweers.redacted.gradle

import com.android.builder.model.BuildType
import com.android.builder.model.ProductFlavor
import org.gradle.api.Action
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.Property
import javax.inject.Inject

internal const val DEFAULT_ANNOTATION = "dev.zacsweers.redacted.annotations.Redacted"

abstract class RedactedPluginExtension @Inject constructor(
    objects: ObjectFactory
) {
  val redactedAnnotation: Property<String> = objects.property(String::class.java)
      .convention(DEFAULT_ANNOTATION)

  val enabled: Property<Boolean> = objects.property(Boolean::class.javaObjectType)
      .convention(true)

  val replacementString: Property<String> = objects.property(String::class.java)
      .convention("██")

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
