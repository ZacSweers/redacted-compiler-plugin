package dev.zacsweers.redacted.sample

import dev.zacsweers.redacted.annotation.Redacted

data class Person(
    val name: String,
    @Redacted val ssn: String
)

