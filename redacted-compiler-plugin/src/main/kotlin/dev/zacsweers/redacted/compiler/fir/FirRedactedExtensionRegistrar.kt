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

import dev.zacsweers.redacted.compiler.firstNotNullResult
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
import org.jetbrains.kotlin.fir.declarations.toAnnotationClassIdSafe
import org.jetbrains.kotlin.fir.declarations.utils.isEnumClass
import org.jetbrains.kotlin.fir.declarations.utils.isExpect
import org.jetbrains.kotlin.fir.declarations.utils.isExtension
import org.jetbrains.kotlin.fir.declarations.utils.isExternal
import org.jetbrains.kotlin.fir.declarations.utils.isFinal
import org.jetbrains.kotlin.fir.declarations.utils.isInline
import org.jetbrains.kotlin.fir.declarations.utils.nameOrSpecialName
import org.jetbrains.kotlin.fir.expressions.FirAnnotation
import org.jetbrains.kotlin.fir.extensions.FirExtensionRegistrar
import org.jetbrains.kotlin.fir.resolve.fullyExpandedType
import org.jetbrains.kotlin.fir.resolve.toSymbol
import org.jetbrains.kotlin.fir.symbols.SymbolInternals
import org.jetbrains.kotlin.fir.symbols.resolvedAnnotationsWithClassIds
import org.jetbrains.kotlin.fir.types.ConeClassLikeType
import org.jetbrains.kotlin.fir.types.ConeErrorType
import org.jetbrains.kotlin.fir.types.FirTypeRef
import org.jetbrains.kotlin.fir.types.classId
import org.jetbrains.kotlin.fir.types.coneType
import org.jetbrains.kotlin.fir.types.coneTypeSafe
import org.jetbrains.kotlin.fir.types.isString
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.util.OperatorNameConventions

internal class FirRedactedExtensionRegistrar(
  private val redactedAnnotations: Set<ClassId>,
  private val unRedactedAnnotations: Set<ClassId>,
) : FirExtensionRegistrar() {
  override fun ExtensionRegistrarContext.configurePlugin() {
    +RedactedFirBuiltIns.getFactory(redactedAnnotations, unRedactedAnnotations)
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
  private class RedactedSupertype(
    val ref: FirTypeRef,
    val clazz: ConeClassLikeType,
    val redactedClassId: ClassId,
  )

  @OptIn(SymbolInternals::class)
  override fun check(declaration: FirClass, context: CheckerContext, reporter: DiagnosticReporter) {
    val classRedactedAnnotations =
      context.session.redactedAnnotations.mapNotNull {
        declaration.getAnnotationByClassId(it, context.session)
      }
    val classIsRedacted = classRedactedAnnotations.isNotEmpty()
    val classUnRedactedAnnotations =
      context.session.unRedactedAnnotations.mapNotNull {
        declaration.getAnnotationByClassId(it, context.session)
      }
    val classIsUnRedacted = classUnRedactedAnnotations.isNotEmpty()
    val redactedSupertype: RedactedSupertype? by unsafeLazy {
      for (ref in declaration.superTypeRefs) {
        val supertype = ref.coneTypeSafe<ConeClassLikeType>() ?: continue
        if (supertype is ConeErrorType) continue
        val redactedAnnotation =
          supertype.classId?.toSymbol(context.session)?.resolvedAnnotationClassIds?.firstOrNull {
            it in context.session.redactedAnnotations
          }
        if (redactedAnnotation != null) {
          return@unsafeLazy RedactedSupertype(ref, supertype, redactedAnnotation)
        }
      }
      null
    }

    val redactedProperties = mutableMapOf<FirProperty, Pair<FirAnnotation, ClassId>>()
    val unredactedProperties = mutableMapOf<FirProperty, Pair<FirAnnotation, ClassId>>()
    for (prop in declaration.declarations.filterIsInstance<FirProperty>()) {
      prop.redactedAnnotation(context.session)?.let { redactedProperties[prop] = it }
      prop.unredactedAnnotation(context.session)?.let { unredactedProperties[prop] = it }
    }
    val anyRedacted = redactedProperties.isNotEmpty()
    val anyUnredacted = unredactedProperties.isNotEmpty()

    val redactedName = { redactedProperties.values.first().second.shortClassName.asString() }

    val unRedactedName = { unredactedProperties.values.first().second.shortClassName.asString() }

    if (classIsRedacted || redactedSupertype != null || classIsUnRedacted || anyRedacted) {
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
        if (redactedSupertype == null) {
          val classAnnotation = classRedactedAnnotations.first()
          reporter.reportOn(
            classAnnotation.source,
            RedactedDiagnostics.REDACTED_ERROR,
            "@${classAnnotation.toAnnotationClassIdSafe(context.session)?.shortClassName?.asString()} is useless on object classes.",
            context,
          )
          return
        } else if (classIsUnRedacted) {
          reporter.reportOn(
            classUnRedactedAnnotations.firstOrNull()?.source,
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
      if (classIsUnRedacted && redactedSupertype == null) {
        reporter.reportOn(
          declaration.source,
          RedactedDiagnostics.REDACTED_ERROR,
          "@${unRedactedName()} cannot be applied to a class unless a supertype is marked @${redactedName()}.",
          context,
        )
        return
      }
      if (anyUnredacted && (!classIsRedacted && redactedSupertype == null)) {
        reporter.reportOn(
          declaration.source,
          RedactedDiagnostics.REDACTED_ERROR,
          "@${unRedactedName()} should only be applied to properties in a class or a supertype is marked @${redactedName()}.",
          context,
        )
        return
      }
      if (!(classIsRedacted xor anyRedacted xor (redactedSupertype != null))) {
        val redactedName =
          redactedProperties.values.firstOrNull()?.second
            ?: classRedactedAnnotations.firstOrNull()?.toAnnotationClassIdSafe(context.session)
            ?: redactedSupertype?.redactedClassId
            ?: error("Not possible!")

        val message = buildString {
          appendLine("@${redactedName.shortClassName.asString()} detected on multiple targets:")
          if (classIsRedacted) {
            appendLine("class: '${declaration.nameOrSpecialName.asString()}'")
          }
          if (anyRedacted) {
            appendLine(
              "properties: ${redactedProperties.keys.joinToString(", ") { "'${it.name.asString()}'" }}"
            )
          }
          redactedSupertype?.clazz?.let { appendLine("supertype: ${it.classId}") }
        }

        if (classIsRedacted) {
          reporter.reportOn(
            classRedactedAnnotations.first().source,
            RedactedDiagnostics.REDACTED_ERROR,
            message,
            context,
          )
        } else {
          // Supertype
          reporter.reportOn(
            redactedSupertype?.ref?.source,
            RedactedDiagnostics.REDACTED_ERROR,
            message,
            context,
          )
        }
        for ((_, annotationAndId) in redactedProperties) {
          reporter.reportOn(
            annotationAndId.first.source,
            RedactedDiagnostics.REDACTED_ERROR,
            message,
            context,
          )
        }
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

  @OptIn(SymbolInternals::class)
  private fun FirProperty.redactedAnnotation(session: FirSession): Pair<FirAnnotation, ClassId>? =
    resolvedAnnotationsWithClassIds(symbol).firstNotNullResult {
      val classId = it.toAnnotationClassIdSafe(session)
      if (classId != null && classId in session.redactedAnnotations) {
        it to classId
      } else {
        null
      }
    }

  @OptIn(SymbolInternals::class)
  private fun FirProperty.unredactedAnnotation(session: FirSession): Pair<FirAnnotation, ClassId>? =
    resolvedAnnotationsWithClassIds(symbol).firstNotNullResult {
      val classId = it.toAnnotationClassIdSafe(session)
      if (classId != null && classId in session.unRedactedAnnotations) {
        it to classId
      } else {
        null
      }
    }

  private val FirClass.isInstantiableEnum: Boolean
    get() = isEnumClass && !isExpect && !isExternal
}
