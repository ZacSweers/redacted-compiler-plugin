// Copyright (C) 2021 Zac Sweers
// SPDX-License-Identifier: Apache-2.0
package dev.zacsweers.redacted.sample

// This file is only really used for testing the K2 compiler right now
data class UserK2Testing(val name: String)

// This class has multiple issues that FIR will report
// - Duplicate redacted annotations
// - custom toString() impl
// @Redacted
// data class UserK2Testing(val name: String, @Redacted val phoneNumber: String) {
//  override fun toString(): String {
//    return "stuff"
//  }
// }
