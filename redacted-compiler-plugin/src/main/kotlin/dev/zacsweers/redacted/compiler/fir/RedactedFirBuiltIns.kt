// Copyright (C) 2024 Zac Sweers
// SPDX-License-Identifier: Apache-2.0
package dev.zacsweers.redacted.compiler.fir

import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.extensions.FirExtensionSessionComponent
import org.jetbrains.kotlin.name.ClassId

internal class RedactedFirBuiltIns(
  session: FirSession,
  val redactedAnnotations: Set<ClassId>,
  val unRedactedAnnotations: Set<ClassId>,
) : FirExtensionSessionComponent(session) {
  companion object {
    fun getFactory(redactedAnnotations: Set<ClassId>, unRedactedAnnotations: Set<ClassId>) =
      Factory { session ->
        RedactedFirBuiltIns(session, redactedAnnotations, unRedactedAnnotations)
      }
  }
}

internal val FirSession.redactedFirBuiltIns: RedactedFirBuiltIns by
  FirSession.sessionComponentAccessor()

internal val FirSession.redactedAnnotations: Set<ClassId>
  get() = redactedFirBuiltIns.redactedAnnotations

internal val FirSession.unRedactedAnnotations: Set<ClassId>
  get() = redactedFirBuiltIns.unRedactedAnnotations
