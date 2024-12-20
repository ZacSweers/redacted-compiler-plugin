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

import dev.zacsweers.redacted.compiler.unsafeLazy
import org.jetbrains.kotlin.descriptors.isEnumEntry
import org.jetbrains.kotlin.descriptors.isObject
import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.diagnostics.reportOn
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.analysis.checkers.MppCheckerKind
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.checkers.declaration.DeclarationCheckers
import org.jetbrains.kotlin.fir.analysis.checkers.declaration.FirClassChecker
import org.jetbrains.kotlin.fir.analysis.extensions.FirAdditionalCheckersExtension
import org.jetbrains.kotlin.fir.declarations.FirClass
import org.jetbrains.kotlin.fir.declarations.FirDeclarationOrigin
import org.jetbrains.kotlin.fir.declarations.FirFunction
import org.jetbrains.kotlin.fir.declarations.FirProperty
import org.jetbrains.kotlin.fir.declarations.getAnnotationByClassId
import org.jetbrains.kotlin.fir.declarations.hasAnnotation
import org.jetbrains.kotlin.fir.declarations.utils.isEnumClass
import org.jetbrains.kotlin.fir.declarations.utils.isExpect
import org.jetbrains.kotlin.fir.declarations.utils.isExtension
import org.jetbrains.kotlin.fir.declarations.utils.isExternal
import org.jetbrains.kotlin.fir.declarations.utils.isFinal
import org.jetbrains.kotlin.fir.declarations.utils.isInline
import org.jetbrains.kotlin.fir.declarations.utils.nameOrSpecialName
import org.jetbrains.kotlin.fir.declarations.utils.superConeTypes
import org.jetbrains.kotlin.fir.extensions.FirExtensionRegistrar
import org.jetbrains.kotlin.fir.resolve.fullyExpandedType
import org.jetbrains.kotlin.fir.resolve.toSymbol
import org.jetbrains.kotlin.fir.types.ConeErrorType
import org.jetbrains.kotlin.fir.types.classId
import org.jetbrains.kotlin.fir.types.coneType
import org.jetbrains.kotlin.fir.types.isString
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.util.OperatorNameConventions

internal class FirRedactedExtensionRegistrar(
  private val redactedAnnotation: ClassId,
  private val unRedactedAnnotation: ClassId,
) : FirExtensionRegistrar() {
  override fun ExtensionRegistrarContext.configurePlugin() {
    +RedactedFirBuiltIns.getFactory(redactedAnnotation, unRedactedAnnotation)
    +::FirRedactedCheckers
  }
}

internal class FirRedactedCheckers(session: FirSession) : FirAdditionalCheckersExtension(session) {
  override val declarationCheckers: DeclarationCheckers =
    object : DeclarationCheckers() {
      override val classCheckers: Set<FirClassChecker>
        get() = setOf(FirRedactedDeclarationChecker)
    }
}

internal object FirRedactedDeclarationChecker : FirClassChecker(MppCheckerKind.Common) {
  override fun check(declaration: FirClass, context: CheckerContext, reporter: DiagnosticReporter) {
    val classRedactedAnnotation =
      declaration.getAnnotationByClassId(context.session.redactedAnnotation, context.session)
    val classIsRedacted = classRedactedAnnotation != null
    val classUnRedactedAnnotation =
      declaration.getAnnotationByClassId(context.session.unRedactedAnnotation, context.session)
    val classIsUnRedacted = classUnRedactedAnnotation != null
    val supertypeIsRedacted by unsafeLazy {
      declaration.superConeTypes.any {
        if (it is ConeErrorType) return@any false
        it.classId
          ?.toSymbol(context.session)
          ?.hasAnnotation(context.session.redactedAnnotation, context.session) == true
      }
    }
    var anyRedacted = false
    var anyUnredacted = false

    for (prop in declaration.declarations.filterIsInstance<FirProperty>()) {
      val isRedacted = prop.isRedacted(context.session)
      val isUnredacted = prop.isUnredacted(context.session)
      if (isRedacted) {
        anyRedacted = true
      }
      if (isUnredacted) {
        anyUnredacted = true
      }
    }

    val redactedName = { context.session.redactedAnnotation.shortClassName.asString() }

    val unRedactedName = { context.session.redactedAnnotation.shortClassName.asString() }

    if (classIsRedacted || supertypeIsRedacted || classIsUnRedacted || anyRedacted) {
      val customToStringFunction =
        declaration.declarations.filterIsInstance<FirFunction>().find {
          it.isToStringFromAny(context.session) && it.origin == FirDeclarationOrigin.Source
        }
      if (customToStringFunction != null) {
        reporter.reportOn(
          customToStringFunction.source,
          RedactedDiagnostics.REDACTED_ERROR,
          "@${redactedName()} is only supported on data or value classes that do *not* have a custom toString() function. Please remove the function or remove the @${redactedName()} annotations.",
          context,
        )
        return
      }
      if (
        declaration.isInstantiableEnum ||
          declaration.isEnumClass ||
          declaration.classKind.isEnumEntry
      ) {
        reporter.reportOn(
          declaration.source,
          RedactedDiagnostics.REDACTED_ERROR,
          "@${redactedName()} does not support enum classes or entries!",
          context,
        )
        return
      }
      if (declaration.isFinal && !(declaration.status.isData || declaration.isInline)) {
        reporter.reportOn(
          declaration.source,
          RedactedDiagnostics.REDACTED_ERROR,
          "@${redactedName()} is only supported on data or value classes!",
          context,
        )
        return
      }
      if (declaration.isInline && !classIsRedacted) {
        reporter.reportOn(
          declaration.source,
          RedactedDiagnostics.REDACTED_ERROR,
          "@${redactedName()} is redundant on value class properties, just annotate the class instead.",
          context,
        )
        return
      }
      if (declaration.classKind.isObject) {
        if (!supertypeIsRedacted) {
          reporter.reportOn(
            classRedactedAnnotation!!.source,
            RedactedDiagnostics.REDACTED_ERROR,
            "@${redactedName()} is useless on object classes.",
            context,
          )
          return
        } else if (classIsUnRedacted) {
          reporter.reportOn(
            classUnRedactedAnnotation.source,
            RedactedDiagnostics.REDACTED_ERROR,
            "@${unRedactedName()} is useless on object classes.",
            context,
          )
          return
        }
      }
      if (classIsRedacted && classIsUnRedacted) {
        reporter.reportOn(
          declaration.source,
          RedactedDiagnostics.REDACTED_ERROR,
          "@${redactedName()} and @${unRedactedName()} cannot be applied to a single class.",
          context,
        )
        return
      }
      if (classIsUnRedacted && !supertypeIsRedacted) {
        reporter.reportOn(
          declaration.source,
          RedactedDiagnostics.REDACTED_ERROR,
          "@${unRedactedName()} cannot be applied to a class unless a supertype is marked @${redactedName()}.",
          context,
        )
        return
      }
      if (anyUnredacted && (!classIsRedacted && !supertypeIsRedacted)) {
        reporter.reportOn(
          declaration.source,
          RedactedDiagnostics.REDACTED_ERROR,
          "@${unRedactedName()} should only be applied to properties in a class or a supertype is marked @${redactedName()}.",
          context,
        )
        return
      }
      if (!(classIsRedacted xor anyRedacted xor supertypeIsRedacted)) {
        reporter.reportOn(
          declaration.source,
          RedactedDiagnostics.REDACTED_ERROR,
          "@${redactedName()} should only be applied to the class or its properties, not both.",
          context,
        )
        return
      }
      // Rest filled in by the IR plugin
    }
  }

  private fun FirFunction.isToStringFromAny(session: FirSession): Boolean =
    nameOrSpecialName == OperatorNameConventions.TO_STRING &&
      dispatchReceiverType != null &&
      !isExtension &&
      valueParameters.isEmpty() &&
      returnTypeRef.coneType.fullyExpandedType(session).isString

  private fun FirProperty.isRedacted(session: FirSession): Boolean =
    hasAnnotation(session.redactedAnnotation, session)

  private fun FirProperty.isUnredacted(session: FirSession): Boolean =
    hasAnnotation(session.unRedactedAnnotation, session)

  private val FirClass.isInstantiableEnum: Boolean
    get() = isEnumClass && !isExpect && !isExternal
}
