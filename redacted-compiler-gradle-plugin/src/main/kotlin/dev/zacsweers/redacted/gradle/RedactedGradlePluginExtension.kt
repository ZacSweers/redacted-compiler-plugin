package dev.zacsweers.redacted.gradle

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
}
