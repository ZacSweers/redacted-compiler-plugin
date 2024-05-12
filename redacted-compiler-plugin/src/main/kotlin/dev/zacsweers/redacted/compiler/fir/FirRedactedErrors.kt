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
import org.jetbrains.kotlin.com.intellij.psi.PsiElement
import org.jetbrains.kotlin.diagnostics.KtDiagnosticFactoryToRendererMap
import org.jetbrains.kotlin.diagnostics.SourceElementPositioningStrategies.NAME_IDENTIFIER
import org.jetbrains.kotlin.diagnostics.error0
import org.jetbrains.kotlin.diagnostics.rendering.BaseDiagnosticRendererFactory
import org.jetbrains.kotlin.diagnostics.rendering.RootDiagnosticRendererFactory

internal object FirRedactedErrors {
  val REDACTED_ON_CLASS_AND_PROPERTY_ERROR by error0<PsiElement>(NAME_IDENTIFIER)
  val REDACTED_ON_NON_CLASS_ERROR by error0<PsiElement>(NAME_IDENTIFIER)
  val REDACTED_ON_ENUM_CLASS_ERROR by error0<PsiElement>(NAME_IDENTIFIER)
  val REDACTED_ON_NON_DATA_OR_VALUE_CLASS_ERROR by error0<PsiElement>(NAME_IDENTIFIER)
  val REDACTED_ON_VALUE_CLASS_PROPERTY_ERROR by error0<PsiElement>(NAME_IDENTIFIER)
  val CUSTOM_TO_STRING_IN_REDACTED_CLASS_ERROR by error0<PsiElement>(NAME_IDENTIFIER)
  val UNREDACTED_ON_OBJECT_ERROR by error0<PsiElement>(NAME_IDENTIFIER)
  val UNREDACTED_AND_REDACTED_ERROR by error0<PsiElement>(NAME_IDENTIFIER)
  val UNREDACTED_ON_NONREDACTED_SUBTYPE_ERROR by error0<PsiElement>(NAME_IDENTIFIER)
  val UNREDACTED_ON_NON_PROPERTY by error0<PsiElement>(NAME_IDENTIFIER)

  init {
    RootDiagnosticRendererFactory.registerFactory(FirRedactedErrorMessages)
  }
}

private object FirRedactedErrorMessages : BaseDiagnosticRendererFactory() {
  override val MAP: KtDiagnosticFactoryToRendererMap =
    KtDiagnosticFactoryToRendererMap("Redacted").apply {
      put(
        FirRedactedErrors.REDACTED_ON_CLASS_AND_PROPERTY_ERROR,
        ErrorMessages.REDACTED_ON_CLASS_AND_PROPERTY_ERROR,
      )
      put(FirRedactedErrors.REDACTED_ON_NON_CLASS_ERROR, ErrorMessages.REDACTED_ON_OBJECT_ERROR)
      put(
        FirRedactedErrors.REDACTED_ON_NON_DATA_OR_VALUE_CLASS_ERROR,
        ErrorMessages.REDACTED_ON_NON_DATA_OR_VALUE_CLASS_ERROR,
      )
      put(
        FirRedactedErrors.REDACTED_ON_VALUE_CLASS_PROPERTY_ERROR,
        ErrorMessages.REDACTED_ON_VALUE_CLASS_PROPERTY_ERROR,
      )
      put(
        FirRedactedErrors.CUSTOM_TO_STRING_IN_REDACTED_CLASS_ERROR,
        ErrorMessages.CUSTOM_TO_STRING_IN_REDACTED_CLASS_ERROR,
      )
      put(
        FirRedactedErrors.REDACTED_ON_ENUM_CLASS_ERROR,
        ErrorMessages.REDACTED_ON_ENUM_CLASS_ERROR,
      )
      put(FirRedactedErrors.UNREDACTED_ON_OBJECT_ERROR, ErrorMessages.UNREDACTED_ON_OBJECT_ERROR)
      put(
        FirRedactedErrors.UNREDACTED_AND_REDACTED_ERROR,
        ErrorMessages.UNREDACTED_AND_REDACTED_ERROR,
      )
      put(
        FirRedactedErrors.UNREDACTED_ON_NONREDACTED_SUBTYPE_ERROR,
        ErrorMessages.UNREDACTED_ON_NONREDACTED_SUBTYPE_ERROR,
      )
      put(FirRedactedErrors.UNREDACTED_ON_NON_PROPERTY, ErrorMessages.UNREDACTED_ON_NON_PROPERTY)
    }
}
