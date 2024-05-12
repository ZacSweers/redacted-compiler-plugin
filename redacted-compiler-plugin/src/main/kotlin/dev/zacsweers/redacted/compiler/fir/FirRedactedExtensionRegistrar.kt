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
import org.jetbrains.kotlin.fir.declarations.utils.isData
import org.jetbrains.kotlin.fir.declarations.utils.isEnumClass
import org.jetbrains.kotlin.fir.declarations.utils.isExpect
import org.jetbrains.kotlin.fir.declarations.utils.isExtension
import org.jetbrains.kotlin.fir.declarations.utils.isExternal
import org.jetbrains.kotlin.fir.declarations.utils.isFinal
import org.jetbrains.kotlin.fir.declarations.utils.isInline
import org.jetbrains.kotlin.fir.declarations.utils.nameOrSpecialName
import org.jetbrains.kotlin.fir.declarations.utils.superConeTypes
import org.jetbrains.kotlin.fir.extensions.FirExtensionRegistrar
import org.jetbrains.kotlin.fir.extensions.FirExtensionSessionComponent.Factory
import org.jetbrains.kotlin.fir.resolve.fullyExpandedType
import org.jetbrains.kotlin.fir.resolve.providers.toSymbol
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
    +FirRedactedCheckers.getFactory(redactedAnnotation, unRedactedAnnotation)
  }
}

internal class FirRedactedCheckers(
  session: FirSession,
  private val redactedAnnotation: ClassId,
  private val unRedactedAnnotation: ClassId,
) : FirAdditionalCheckersExtension(session) {
  companion object {
    fun getFactory(redactedAnnotation: ClassId, unRedactedAnnotation: ClassId) =
      Factory { session ->
        FirRedactedCheckers(session, redactedAnnotation, unRedactedAnnotation)
      }
  }

  override val declarationCheckers: DeclarationCheckers =
    object : DeclarationCheckers() {
      override val classCheckers: Set<FirClassChecker>
        get() =
          setOf(FirRedactedDeclarationChecker(session, redactedAnnotation, unRedactedAnnotation))
    }
}

internal class FirRedactedDeclarationChecker(
  private val session: FirSession,
  private val redactedAnnotation: ClassId,
  private val unRedactedAnnotation: ClassId,
) : FirClassChecker(MppCheckerKind.Common) {

  override fun check(declaration: FirClass, context: CheckerContext, reporter: DiagnosticReporter) {
    val classRedactedAnnotation = declaration.getAnnotationByClassId(redactedAnnotation, session)
    val classIsRedacted = classRedactedAnnotation != null
    val classUnRedactedAnnotation =
      declaration.getAnnotationByClassId(unRedactedAnnotation, session)
    val classIsUnRedacted = classUnRedactedAnnotation != null
    val supertypeIsRedacted by unsafeLazy {
      declaration.superConeTypes.any {
        if (it is ConeErrorType) return@any false
        it.classId?.toSymbol(session)?.hasAnnotation(unRedactedAnnotation, session) == true
      }
    }
    var anyRedacted = false
    var anyUnredacted = false

    for (prop in declaration.declarations.filterIsInstance<FirProperty>()) {
      val isRedacted = prop.isRedacted
      val isUnredacted = prop.isUnredacted
      if (isRedacted) {
        anyRedacted = true
      }
      if (isUnredacted) {
        anyUnredacted = true
      }
    }

    if (classIsRedacted || supertypeIsRedacted || classIsUnRedacted || anyRedacted) {
      val customToStringFunction =
        declaration.declarations.filterIsInstance<FirFunction>().find {
          it.isToStringFromAny() && it.origin == FirDeclarationOrigin.Source
        }
      if (customToStringFunction != null) {
        reporter.reportOn(
          customToStringFunction.source,
          FirRedactedErrors.CUSTOM_TO_STRING_IN_REDACTED_CLASS_ERROR,
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
          FirRedactedErrors.REDACTED_ON_ENUM_CLASS_ERROR,
          context,
        )
        return
      }
      if (declaration.isFinal && !declaration.status.isData && !declaration.isInline) {
        reporter.reportOn(
          declaration.source,
          FirRedactedErrors.REDACTED_ON_NON_DATA_OR_VALUE_CLASS_ERROR,
          context,
        )
        return
      }
      if (declaration.isInline && !classIsRedacted) {
        reporter.reportOn(
          declaration.source,
          FirRedactedErrors.REDACTED_ON_VALUE_CLASS_PROPERTY_ERROR,
          context,
        )
        return
      }
      if (declaration.classKind.isObject) {
        if (!supertypeIsRedacted) {
          reporter.reportOn(
            classRedactedAnnotation!!.source,
            FirRedactedErrors.REDACTED_ON_OBJECT_ERROR,
            context,
          )
          return
        } else if (classIsUnRedacted) {
          reporter.reportOn(
            classUnRedactedAnnotation.source,
            FirRedactedErrors.UNREDACTED_ON_OBJECT_ERROR,
            context,
          )
          return
        }
      }
      if (classIsRedacted && classIsUnRedacted) {
        reporter.reportOn(
          declaration.source,
          FirRedactedErrors.UNREDACTED_AND_REDACTED_ERROR,
          context,
        )
        return
      }
      if (classIsUnRedacted && !supertypeIsRedacted) {
        reporter.reportOn(
          declaration.source,
          FirRedactedErrors.UNREDACTED_ON_NONREDACTED_SUBTYPE_ERROR,
          context,
        )
        return
      }
      if (anyUnredacted && (!classIsRedacted && !supertypeIsRedacted)) {
        reporter.reportOn(declaration.source, FirRedactedErrors.UNREDACTED_ON_NON_PROPERTY, context)
        return
      }
      if (!(classIsRedacted xor anyRedacted xor supertypeIsRedacted)) {
        reporter.reportOn(
          declaration.source,
          FirRedactedErrors.REDACTED_ON_CLASS_AND_PROPERTY_ERROR,
          context,
        )
        return
      }
      // Rest filled in by the IR plugin
    }
  }

  private fun FirFunction.isToStringFromAny(): Boolean =
    nameOrSpecialName == OperatorNameConventions.TO_STRING &&
      dispatchReceiverType != null &&
      !isExtension &&
      valueParameters.isEmpty() &&
      returnTypeRef.coneType.fullyExpandedType(session).isString

  private val FirProperty.isRedacted: Boolean
    get() = hasAnnotation(redactedAnnotation, session)

  private val FirProperty.isUnredacted: Boolean
    get() = hasAnnotation(unRedactedAnnotation, session)

  private val FirClass.isInstantiableEnum: Boolean
    get() = isEnumClass && !isExpect && !isExternal
}
