// Copyright (C) 2021 Zac Sweers
// SPDX-License-Identifier: Apache-2.0
package dev.zacsweers.redacted.annotations

import kotlin.annotation.AnnotationRetention.BINARY
import kotlin.annotation.AnnotationTarget.CLASS
import kotlin.annotation.AnnotationTarget.PROPERTY

/**
 * An annotation to indicate that a particular property or class should be redacted in `toString()`
 * implementations. When applied to an interface, subtypes will behave as if the class is annotated
 * with `@Redacted`.
 *
 * For properties, each individual property will be redacted. Example: `User(name=Bob,
 * phoneNumber=██)`
 *
 * For classes, the entire class will be redacted. Example: `SensitiveData(██)`
 */
@Retention(BINARY) @Target(PROPERTY, CLASS) public annotation class Redacted
