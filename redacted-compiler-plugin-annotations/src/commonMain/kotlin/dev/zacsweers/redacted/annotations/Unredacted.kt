// Copyright (C) 2024 Zac Sweers
// SPDX-License-Identifier: Apache-2.0
package dev.zacsweers.redacted.annotations

import kotlin.annotation.AnnotationRetention.BINARY
import kotlin.annotation.AnnotationTarget.CLASS
import kotlin.annotation.AnnotationTarget.PROPERTY

/**
 * An annotation to indicate that a particular property should NOT be redacted in `toString()`
 * implementations.
 *
 * This annotation can be applied to a property in any class that applies `@Redacted` or implements
 * an interface that applies `@Redacted`.
 *
 * Example:
 * ```
 * @Redacted
 * data class User(@Unredacted val name: String, val phoneNumber: String)
 *
 * println(user) // User(name = "Bob", phoneNumber = "██")
 * ```
 */
@Retention(BINARY) @Target(PROPERTY, CLASS) public annotation class Unredacted
