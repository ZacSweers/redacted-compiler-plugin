package dev.zacsweers.redacted.sample.redact.all

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class TotalRedactionTest {
    @Test
    fun creditCardExample() {
        val user = CardDetails("386421391737461246123", "238")
        assertThat(user.toString()).isEqualTo("CardDetails(██)")
    }
}