package dev.zacsweers.redacted.gradle

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
  var verbose: Boolean = false
}
