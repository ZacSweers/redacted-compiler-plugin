package dev.zacsweers.redacted.sample

data class User(
    val name: String,
    @Redacted val phoneNumber: String
)

