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

import org.jetbrains.kotlin.com.intellij.psi.PsiElement
import org.jetbrains.kotlin.diagnostics.SourceElementPositioningStrategies
import org.jetbrains.kotlin.diagnostics.error0

// TODO expose custom error messages when K2 supports it:
//  https://youtrack.jetbrains.com/issue/KT-53510
internal object KtErrorsRedacted {
  val REDACTED_ON_CLASS_AND_PROPERTY_ERROR by
    error0<PsiElement>(SourceElementPositioningStrategies.NAME_IDENTIFIER)
  val REDACTED_ON_NON_CLASS_ERROR by
    error0<PsiElement>(SourceElementPositioningStrategies.NAME_IDENTIFIER)
  val REDACTED_ON_NON_DATA_OR_VALUE_CLASS_ERROR by
    error0<PsiElement>(SourceElementPositioningStrategies.NAME_IDENTIFIER)
  val REDACTED_ON_VALUE_CLASS_PROPERTY_ERROR by
    error0<PsiElement>(SourceElementPositioningStrategies.NAME_IDENTIFIER)
  val CUSTOM_TO_STRING_IN_REDACTED_CLASS_ERROR by
    error0<PsiElement>(SourceElementPositioningStrategies.NAME_IDENTIFIER)
}
