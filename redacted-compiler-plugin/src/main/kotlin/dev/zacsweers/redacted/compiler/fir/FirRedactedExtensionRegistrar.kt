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

import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.diagnostics.KtDiagnosticFactory0
import org.jetbrains.kotlin.diagnostics.reportOn
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.checkers.declaration.DeclarationCheckers
import org.jetbrains.kotlin.fir.analysis.checkers.declaration.FirRegularClassChecker
import org.jetbrains.kotlin.fir.analysis.checkers.hasModifier
import org.jetbrains.kotlin.fir.analysis.extensions.FirAdditionalCheckersExtension
import org.jetbrains.kotlin.fir.declarations.FirDeclaration
import org.jetbrains.kotlin.fir.declarations.FirFunction
import org.jetbrains.kotlin.fir.declarations.FirProperty
import org.jetbrains.kotlin.fir.declarations.FirRegularClass
import org.jetbrains.kotlin.fir.declarations.utils.isOverride
import org.jetbrains.kotlin.fir.expressions.FirAnnotation
import org.jetbrains.kotlin.fir.extensions.FirExtensionRegistrar
import org.jetbrains.kotlin.fir.extensions.FirExtensionSessionComponent
import org.jetbrains.kotlin.fir.extensions.FirExtensionSessionComponent.Factory
import org.jetbrains.kotlin.fir.types.ConeClassLikeType
import org.jetbrains.kotlin.fir.types.classId
import org.jetbrains.kotlin.fir.types.coneType
import org.jetbrains.kotlin.fir.types.coneTypeSafe
import org.jetbrains.kotlin.fir.types.isString
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.Name

private val TO_STRING_NAME = Name.identifier("toString")

internal class FirRedactedExtensionRegistrar(private val redactedAnnotation: ClassId) :
  FirExtensionRegistrar() {
  override fun ExtensionRegistrarContext.configurePlugin() {
    +FirRedactedPredicateMatcher.getFactory(redactedAnnotation)
    +::FirRedactedCheckers
  }
}

internal class FirRedactedCheckers(session: FirSession) : FirAdditionalCheckersExtension(session) {
  override val declarationCheckers: DeclarationCheckers =
    object : DeclarationCheckers() {
      override val regularClassCheckers: Set<FirRegularClassChecker> =
        setOf(FirRedactedDeclarationChecker)
    }
}

internal object FirRedactedDeclarationChecker : FirRegularClassChecker() {
  override fun check(
    declaration: FirRegularClass,
    context: CheckerContext,
    reporter: DiagnosticReporter
  ) {
    val matcher = context.session.redactedPredicateMatcher
    val classRedactedAnnotation = declaration.redactedAnnotation(matcher)
    val redactedProperties = redactedProperties(declaration, matcher)
    val hasRedactedProperty = redactedProperties.isNotEmpty()
    val hasRedactions = classRedactedAnnotation != null || hasRedactedProperty
    if (!hasRedactions) return

    if (hasRedactedProperty && classRedactedAnnotation != null) {
      reporter.reportOn(
        classRedactedAnnotation.source,
        KtErrorsRedacted.REDACTED_ON_CLASS_AND_PROPERTY_ERROR,
        context
      )
      redactedProperties.forEach {
        reporter.reportOn(it.source, KtErrorsRedacted.REDACTED_ON_CLASS_AND_PROPERTY_ERROR, context)
      }
    }

    val allRedactions = redactedProperties.plus(classRedactedAnnotation).filterNotNull()
    fun report(diagnosticFactory: KtDiagnosticFactory0) {
      for (redaction in allRedactions) {
        reporter.reportOn(redaction.source, diagnosticFactory, context)
      }
    }

    if (declaration.classKind != ClassKind.CLASS) {
      report(KtErrorsRedacted.REDACTED_ON_NON_CLASS_ERROR)
      return
    }

    if (
      !declaration.hasModifier(KtTokens.DATA_KEYWORD) &&
        !declaration.hasModifier(KtTokens.VALUE_KEYWORD)
    ) {
      report(KtErrorsRedacted.REDACTED_ON_NON_DATA_OR_VALUE_CLASS_ERROR)
      return
    }

    if (declaration.hasModifier(KtTokens.VALUE_KEYWORD) && hasRedactedProperty) {
      report(KtErrorsRedacted.REDACTED_ON_VALUE_CLASS_PROPERTY_ERROR)
      return
    }

    val customToStringFunction =
      declaration.declarations.find {
        it is FirFunction &&
          it.isOverride &&
          it.symbol.callableId.callableName == TO_STRING_NAME &&
          it.dispatchReceiverType == null &&
          it.receiverParameter == null &&
          it.valueParameters.isEmpty() &&
          it.returnTypeRef.coneType.isString
      }
    if (customToStringFunction != null) {
      reporter.reportOn(
        customToStringFunction.source,
        KtErrorsRedacted.CUSTOM_TO_STRING_IN_REDACTED_CLASS_ERROR,
        context
      )
    }
  }

  private fun FirRegularClass.redactedAnnotation(matcher: FirRedactedPredicateMatcher) =
    matcher.redactedAnnotation(this)

  private fun redactedProperties(
    declaration: FirRegularClass,
    matcher: FirRedactedPredicateMatcher
  ) =
    declaration.declarations
      .asSequence()
      .filterIsInstance<FirProperty>()
      .mapNotNull { matcher.redactedAnnotation(it) }
      .toList()
}

internal class FirRedactedPredicateMatcher(
  session: FirSession,
  private val redactedAnnotation: ClassId
) : FirExtensionSessionComponent(session) {
  companion object {
    fun getFactory(redactedAnnotation: ClassId): Factory {
      return Factory { session -> FirRedactedPredicateMatcher(session, redactedAnnotation) }
    }
  }

  fun redactedAnnotation(declaration: FirDeclaration): FirAnnotation? {
    return declaration.annotations.firstOrNull { firAnnotation ->
      firAnnotation.annotationTypeRef.coneTypeSafe<ConeClassLikeType>()?.classId ==
        redactedAnnotation
    }
  }
}

internal val FirSession.redactedPredicateMatcher: FirRedactedPredicateMatcher by
  FirSession.sessionComponentAccessor()
