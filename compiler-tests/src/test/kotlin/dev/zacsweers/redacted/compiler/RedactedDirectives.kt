// Copyright (C) 2025 Zac Sweers
// SPDX-License-Identifier: Apache-2.0
package dev.zacsweers.redacted.compiler

import org.jetbrains.kotlin.test.directives.model.SimpleDirectivesContainer

object RedactedDirectives : SimpleDirectivesContainer() {
  val REPLACEMENT_STRING by stringDirective("Replacement string to use")
  val REDACTED_ANNOTATIONS by stringDirective("TODO")
  val UNREDACTED_ANNOTATIONS by stringDirective("TODO")
  val ENABLED by
    directive("TODO")
}
