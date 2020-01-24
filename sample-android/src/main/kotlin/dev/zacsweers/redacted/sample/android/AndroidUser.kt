package dev.zacsweers.redacted.sample.android

import dev.zacsweers.redacted.annotations.Redacted

data class AndroidUser(val name: String, @Redacted val ssn: String)