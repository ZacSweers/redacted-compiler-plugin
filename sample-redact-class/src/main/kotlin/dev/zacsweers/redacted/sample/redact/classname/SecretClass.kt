package dev.zacsweers.redacted.sample.redact.classname

import dev.zacsweers.redacted.annotations.Redacted

@Redacted
data class SecretClass(val information: String)