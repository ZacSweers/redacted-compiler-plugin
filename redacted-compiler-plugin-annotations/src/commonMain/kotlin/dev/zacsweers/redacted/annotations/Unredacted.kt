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
