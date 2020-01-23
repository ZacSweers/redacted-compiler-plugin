package io.sweers.redacted.gradle

import org.gradle.api.Plugin
import org.gradle.api.Project

class RedactedGradlePlugin : Plugin<Project> {
  override fun apply(target: Project) {
    target.extensions.create("redactedPlugin", RedactedPluginExtension::class.java)
  }
}

open class RedactedPluginExtension {
  var enabled: Boolean = true
  var replacementString: String = "██"
}
