package dev.zacsweers.redacted.sample.redact.classname

import dev.zacsweers.redacted.annotations.Redacted

data class SecretClassWithRedactedParameters(@Redacted val secretParameter: String)