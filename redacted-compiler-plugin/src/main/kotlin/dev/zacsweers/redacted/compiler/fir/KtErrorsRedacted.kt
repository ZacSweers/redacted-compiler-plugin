package dev.zacsweers.redacted.compiler.fir

import org.jetbrains.kotlin.com.intellij.psi.PsiElement
import org.jetbrains.kotlin.diagnostics.SourceElementPositioningStrategies
import org.jetbrains.kotlin.diagnostics.error0
import org.jetbrains.kotlin.diagnostics.warning0

object KtErrorsRedacted {
    val REDACTED_ON_CLASS_AND_PROPERTY_WARNING by warning0<PsiElement>(SourceElementPositioningStrategies.NAME_IDENTIFIER)
    val REDACTED_ON_NON_CLASS_ERROR by error0<PsiElement>(SourceElementPositioningStrategies.NAME_IDENTIFIER)
    val REDACTED_ON_NON_DATA_CLASS_ERROR by error0<PsiElement>(SourceElementPositioningStrategies.NAME_IDENTIFIER)
    val CUSTOM_TO_STRING_IN_REDACTED_CLASS_ERROR by error0<PsiElement>(SourceElementPositioningStrategies.NAME_IDENTIFIER)
}