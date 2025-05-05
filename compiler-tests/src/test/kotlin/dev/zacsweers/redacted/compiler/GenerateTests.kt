// Copyright (C) 2025 Zac Sweers
// SPDX-License-Identifier: Apache-2.0
package dev.zacsweers.redacted.compiler

import org.jetbrains.kotlin.generators.generateTestGroupSuiteWithJUnit5

fun main() {
  generateTestGroupSuiteWithJUnit5 {
    testGroup(
      testDataRoot = "compiler-tests/src/test/data",
      testsRoot = "compiler-tests/src/test/java",
    ) {
      testClass<AbstractBoxTest> { model("box") }
      testClass<AbstractDiagnosticTest> { model("diagnostic") }
    }
  }
}
