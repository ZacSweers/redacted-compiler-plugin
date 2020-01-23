package dev.zacsweers.redacted.sample

data class Person(
    val name: String,
    @Redacted val ssn: String
)

