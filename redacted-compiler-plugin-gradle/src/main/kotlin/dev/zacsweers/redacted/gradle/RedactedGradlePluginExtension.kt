/*
 * Copyright (C) 2021 Zac Sweers
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package dev.zacsweers.redacted.gradle

import javax.inject.Inject
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.Property

internal const val DEFAULT_ANNOTATION = "dev.zacsweers.redacted.annotations.Redacted"

abstract class RedactedPluginExtension @Inject constructor(objects: ObjectFactory) {
  val redactedAnnotation: Property<String> =
      objects.property(String::class.java).convention(DEFAULT_ANNOTATION)

  val enabled: Property<Boolean> = objects.property(Boolean::class.javaObjectType).convention(true)

  val replacementString: Property<String> = objects.property(String::class.java).convention("██")
}
