// Copyright (C) 2021 Zac Sweers
// SPDX-License-Identifier: Apache-2.0
package dev.zacsweers.redacted.sample

import dev.zacsweers.redacted.annotations.Redacted

data class User(val name: String, @Redacted val phoneNumber: String)

// This class has multiple issues that FIR will report
// - Duplicate redacted annotations
// - custom toString() impl
// @Redacted
// data class User(val name: String, @Redacted val phoneNumber: String) {
//  override fun toString(): String {
//    return "stuff"
//  }
// }
