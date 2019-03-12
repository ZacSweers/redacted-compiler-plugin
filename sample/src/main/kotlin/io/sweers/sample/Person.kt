package io.sweers.sample

import io.sweers.redacted.annotation.Redacted
import io.sweers.redacted.annotation.RedactedBase

data class Person(
    val name: String,
    @Redacted val ssn: String
) : RedactedBase()

class NonDataPerson(
    val name: String,
    @Redacted val ssn: String
)

