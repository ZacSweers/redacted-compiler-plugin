// Copyright (C) 2024 Zac Sweers
// SPDX-License-Identifier: Apache-2.0
package dev.zacsweers.redacted.compiler

internal fun <T> unsafeLazy(initializer: () -> T) = lazy(LazyThreadSafetyMode.NONE, initializer)

internal inline fun <T, R : Any> Iterable<T>.firstNotNullResult(transform: (T) -> R?): R? {
  for (element in this) {
    val result = transform(element)
    if (result != null) return result
  }
  return null
}
