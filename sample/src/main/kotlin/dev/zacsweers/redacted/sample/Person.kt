package dev.zacsweers.redacted.sample

import io.sweers.redacted.annotation.Redacted

data class Person(
    val name: String,
    @Redacted val ssn: String
)

