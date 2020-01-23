package dev.zacsweers.redacted.annotation

import kotlin.annotation.AnnotationRetention.BINARY
import kotlin.annotation.AnnotationTarget.PROPERTY

@Retention(BINARY)
@Target(PROPERTY)
annotation class Redacted
