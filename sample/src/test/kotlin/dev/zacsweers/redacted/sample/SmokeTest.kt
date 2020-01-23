package dev.zacsweers.redacted.sample

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class SmokeTest {
  @Test
  fun person() {
    val person = Person("John Doe", "123-456-7890")
    assertThat(person.toString()).isEqualTo("Person(name=John Doe, ssn=██)")
  }
}