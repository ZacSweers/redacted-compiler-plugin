package dev.zacsweers.redacted.sample.android

import dev.zacsweers.redacted.sample.Redacted

data class AndroidUser(val name: String, @Redacted val ssn: String)