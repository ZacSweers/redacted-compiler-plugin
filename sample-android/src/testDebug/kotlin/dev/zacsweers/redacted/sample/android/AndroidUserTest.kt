package dev.zacsweers.redacted.sample.android

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class AndroidUserTest {
  @Test
  fun debugTestShouldHaveNoRedactions() {
    val user = AndroidUser("Bob", "123-456-7890")
    assertThat(user.toString()).isEqualTo("AndroidUser(name=Bob, ssn=123-456-7890)")
  }
}