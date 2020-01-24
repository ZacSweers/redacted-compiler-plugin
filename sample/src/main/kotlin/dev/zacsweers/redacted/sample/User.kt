package dev.zacsweers.redacted.sample

import dev.zacsweers.redacted.annotations.Redacted

data class User(
    val name: String,
    @Redacted val phoneNumber: String
)

