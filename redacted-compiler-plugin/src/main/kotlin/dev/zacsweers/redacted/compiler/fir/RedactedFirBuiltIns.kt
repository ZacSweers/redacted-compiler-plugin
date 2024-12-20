package dev.zacsweers.redacted.compiler.fir

import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.extensions.FirExtensionSessionComponent
import org.jetbrains.kotlin.name.ClassId

internal class RedactedFirBuiltIns(
  session: FirSession,
  val redactedAnnotation: ClassId,
  val unRedactedAnnotation: ClassId,
) : FirExtensionSessionComponent(session) {
  companion object {
    fun getFactory(redactedAnnotation: ClassId, unRedactedAnnotation: ClassId) =
      Factory { session ->
        RedactedFirBuiltIns(session, redactedAnnotation, unRedactedAnnotation)
      }
  }
}

internal val FirSession.redactedFirBuiltIns: RedactedFirBuiltIns by
  FirSession.sessionComponentAccessor()

internal val FirSession.redactedAnnotation: ClassId
  get() = redactedFirBuiltIns.redactedAnnotation

internal val FirSession.unRedactedAnnotation: ClassId
  get() = redactedFirBuiltIns.unRedactedAnnotation
