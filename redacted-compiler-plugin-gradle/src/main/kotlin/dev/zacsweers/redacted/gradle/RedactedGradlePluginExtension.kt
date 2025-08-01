// Copyright (C) 2021 Zac Sweers
// SPDX-License-Identifier: Apache-2.0
package dev.zacsweers.redacted.gradle

import javax.inject.Inject
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.Property
import org.gradle.api.provider.SetProperty

internal const val DEFAULT_ANNOTATION = "dev/zacsweers/redacted/annotations/Redacted"
internal val DEFAULT_ANNOTATION_SET = setOf(DEFAULT_ANNOTATION)
internal const val DEFAULT_UNREDACTED_ANNOTATION = "dev/zacsweers/redacted/annotations/Unredacted"
internal val DEFAULT_UNREDACTED_ANNOTATION_SET = setOf(DEFAULT_UNREDACTED_ANNOTATION)

public abstract class RedactedPluginExtension @Inject constructor(objects: ObjectFactory) {
  @Deprecated("Use redactedAnnotations instead", ReplaceWith("redactedAnnotations"))
  public val redactedAnnotation: Property<String> =
    objects.property(String::class.java).convention(DEFAULT_ANNOTATION)

  /**
   * Define custom redacted marker annotations. The -annotations artifact won't be automatically
   * added to dependencies if you define your own!
   *
   * Note that these must be in the format of a string where packages are delimited by '/' and
   * classes by '.', e.g. "kotlin/Map.Entry"
   */
  public val redactedAnnotations: SetProperty<String> =
    objects.setProperty(String::class.java).convention(setOf(DEFAULT_ANNOTATION))

  @Deprecated("Use unredactedAnnotations instead", ReplaceWith("unredactedAnnotations"))
  public val unredactedAnnotation: Property<String> =
    objects.property(String::class.java).convention(DEFAULT_UNREDACTED_ANNOTATION)

  /**
   * Define custom unredacted marker annotations. The -annotations artifact won't be automatically
   * added to dependencies if you define your own!
   *
   * Note that these must be in the format of a string where packages are delimited by '/' and
   * classes by '.', e.g. "kotlin/Map.Entry"
   */
  public val unredactedAnnotations: SetProperty<String> =
    objects.setProperty(String::class.java).convention(setOf(DEFAULT_UNREDACTED_ANNOTATION))

  /** Flag to enable/disable the plugin on this specific compilation. */
  public val enabled: Property<Boolean> =
    objects.property(Boolean::class.javaObjectType).convention(true)

  /** Defines a custom replacement string. The default is "██". */
  public val replacementString: Property<String> =
    objects.property(String::class.java).convention("██")
}
