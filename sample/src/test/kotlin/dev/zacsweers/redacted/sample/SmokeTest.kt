package dev.zacsweers.redacted.sample

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class SmokeTest {
  @Test
  fun person() {
    val person = User("Bob", "2815551234")
    assertThat(person.toString()).isEqualTo("User(name=Bob, ssn=██)")
  }
}