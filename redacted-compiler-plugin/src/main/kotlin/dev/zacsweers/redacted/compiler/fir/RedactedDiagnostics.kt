/*
 * Copyright (C) 2022 Zac Sweers
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
package dev.zacsweers.redacted.compiler.fir

import dev.zacsweers.redacted.compiler.ErrorMessages
import org.jetbrains.kotlin.diagnostics.AbstractSourceElementPositioningStrategy
import org.jetbrains.kotlin.diagnostics.DiagnosticFactory0DelegateProvider
import org.jetbrains.kotlin.diagnostics.KtDiagnosticFactoryToRendererMap
import org.jetbrains.kotlin.diagnostics.Severity
import org.jetbrains.kotlin.diagnostics.SourceElementPositioningStrategies
import org.jetbrains.kotlin.diagnostics.SourceElementPositioningStrategies.NAME_IDENTIFIER
import org.jetbrains.kotlin.diagnostics.rendering.BaseDiagnosticRendererFactory
import org.jetbrains.kotlin.diagnostics.rendering.RootDiagnosticRendererFactory

/**
 * The compiler and the IDE use a different version of this class, so use reflection to find the
 * available version.
 */
// Adapted from
// https://github.com/TadeasKriz/K2PluginBase/blob/main/kotlin-plugin/src/main/kotlin/com/tadeaskriz/example/ExamplePluginErrors.kt#L8
private val psiElementClass by lazy {
  try {
      Class.forName("org.jetbrains.kotlin.com.intellij.psi.PsiElement")
    } catch (_: ClassNotFoundException) {
      Class.forName("com.intellij.psi.PsiElement")
    }
    .kotlin
}

internal object RedactedDiagnostics : BaseDiagnosticRendererFactory() {
  val REDACTED_ON_CLASS_AND_PROPERTY_ERROR by error0(NAME_IDENTIFIER)
  val REDACTED_ON_OBJECT_ERROR by error0(NAME_IDENTIFIER)
  val REDACTED_ON_ENUM_CLASS_ERROR by error0(NAME_IDENTIFIER)
  val REDACTED_ON_NON_DATA_OR_VALUE_CLASS_ERROR by error0(NAME_IDENTIFIER)
  val REDACTED_ON_VALUE_CLASS_PROPERTY_ERROR by error0(NAME_IDENTIFIER)
  val CUSTOM_TO_STRING_IN_REDACTED_CLASS_ERROR by error0(NAME_IDENTIFIER)
  val UNREDACTED_ON_OBJECT_ERROR by error0(NAME_IDENTIFIER)
  val UNREDACTED_AND_REDACTED_ERROR by error0(NAME_IDENTIFIER)
  val UNREDACTED_ON_NONREDACTED_SUBTYPE_ERROR by error0(NAME_IDENTIFIER)
  val UNREDACTED_ON_NON_PROPERTY by error0(NAME_IDENTIFIER)

  override val MAP: KtDiagnosticFactoryToRendererMap =
    KtDiagnosticFactoryToRendererMap("Redacted").apply {
      put(REDACTED_ON_CLASS_AND_PROPERTY_ERROR, ErrorMessages.REDACTED_ON_CLASS_AND_PROPERTY_ERROR)
      put(REDACTED_ON_OBJECT_ERROR, ErrorMessages.REDACTED_ON_OBJECT_ERROR)
      put(
        REDACTED_ON_NON_DATA_OR_VALUE_CLASS_ERROR,
        ErrorMessages.REDACTED_ON_NON_DATA_OR_VALUE_CLASS_ERROR,
      )
      put(
        REDACTED_ON_VALUE_CLASS_PROPERTY_ERROR,
        ErrorMessages.REDACTED_ON_VALUE_CLASS_PROPERTY_ERROR,
      )
      put(
        CUSTOM_TO_STRING_IN_REDACTED_CLASS_ERROR,
        ErrorMessages.CUSTOM_TO_STRING_IN_REDACTED_CLASS_ERROR,
      )
      put(REDACTED_ON_ENUM_CLASS_ERROR, ErrorMessages.REDACTED_ON_ENUM_CLASS_ERROR)
      put(UNREDACTED_ON_OBJECT_ERROR, ErrorMessages.UNREDACTED_ON_OBJECT_ERROR)
      put(UNREDACTED_AND_REDACTED_ERROR, ErrorMessages.UNREDACTED_AND_REDACTED_ERROR)
      put(
        UNREDACTED_ON_NONREDACTED_SUBTYPE_ERROR,
        ErrorMessages.UNREDACTED_ON_NONREDACTED_SUBTYPE_ERROR,
      )
      put(UNREDACTED_ON_NON_PROPERTY, ErrorMessages.UNREDACTED_ON_NON_PROPERTY)
    }

  init {
    RootDiagnosticRendererFactory.registerFactory(this)
  }

  /** Copy of [org.jetbrains.kotlin.diagnostics.error0] with hack for correct `PsiElement` class. */
  private fun error0(
    positioningStrategy: AbstractSourceElementPositioningStrategy =
      SourceElementPositioningStrategies.DEFAULT
  ): DiagnosticFactory0DelegateProvider {
    return DiagnosticFactory0DelegateProvider(
      severity = Severity.ERROR,
      positioningStrategy = positioningStrategy,
      psiType = psiElementClass,
    )
  }
}
