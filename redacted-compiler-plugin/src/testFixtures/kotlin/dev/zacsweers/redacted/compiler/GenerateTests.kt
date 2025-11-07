// Copyright (C) 2025 Zac Sweers
// SPDX-License-Identifier: Apache-2.0
package dev.zacsweers.redacted.compiler

import org.jetbrains.kotlin.generators.dsl.junit5.generateTestGroupSuiteWithJUnit5

fun main() {
  generateTestGroupSuiteWithJUnit5 {
    testGroup(
      testDataRoot = "redacted-compiler-plugin/testData",
      testsRoot = "redacted-compiler-plugin/test-gen/java",
    ) {
      testClass<AbstractBoxTest> { model("box") }
      testClass<AbstractDiagnosticTest> { model("diagnostic") }
    }
  }
}
