package dev.zacsweers.redacted.sample.redact.classname

import com.google.common.truth.Truth
import org.junit.Test

class RedactedClassNameTest {
    @Test
    fun secretClass() {
        val secret = SecretClass("Sensitive information")
        Truth.assertThat(secret.toString()).isEqualTo("██")
    }

    @Test
    fun secretClassWithRedactedParameters() {
        val secret = SecretClassWithRedactedParameters("email@address.com")
        Truth.assertThat(secret.toString()).isEqualTo("██(secretParameter=██)")
    }
}