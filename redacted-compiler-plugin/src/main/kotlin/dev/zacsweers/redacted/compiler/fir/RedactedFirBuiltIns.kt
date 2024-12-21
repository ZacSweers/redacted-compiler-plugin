/*
 * Copyright (C) 2024 Zac Sweers
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
