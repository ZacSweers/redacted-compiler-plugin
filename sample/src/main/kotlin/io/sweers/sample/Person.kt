package io.sweers.sample

import io.sweers.redacted.annotation.Redacted

data class Person(
    val name: String,
    @Redacted val ssn: String
)

