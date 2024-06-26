/*
 * Copyright (C) 2021 Zac Sweers
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package dev.zacsweers.redacted.sample

import com.google.common.truth.Truth.assertThat
import dev.zacsweers.redacted.annotations.Redacted
import dev.zacsweers.redacted.annotations.Unredacted
import org.junit.Test

class SmokeTest {

  @Test
  fun abstractExample() {
    val secretChild = AbstractBase.SecretChild("private")
    assertThat(secretChild.toString()).isEqualTo("SecretChild(redact=██)")
  }

  @Test
  fun unredactedAbstractExample() {
    val notSoSecretChild = AbstractBase.NotSoSecretChild("public")
    assertThat(notSoSecretChild.toString()).isEqualTo("NotSoSecretChild(unredacted=public)")
  }

  @Test
  fun unredactedClassAbstractExample() {
    val notAtAllSecretChild = AbstractBase.NotAtAllSecretChild("public")
    assertThat(notAtAllSecretChild.toString()).isEqualTo("NotAtAllSecretChild(unredacted=public)")
  }

  @Test
  fun redactedObjectAbstractExample() {
    assertThat(AbstractBase.ProplessChild.toString()).isEqualTo("ProplessChild()")
  }

  @Redacted
  abstract class AbstractBase {

    data class SecretChild(val redact: String) : AbstractBase()

    data class NotSoSecretChild(@Unredacted val unredacted: String) : AbstractBase()

    @Unredacted data class NotAtAllSecretChild(val unredacted: String) : AbstractBase()

    data object ProplessChild : AbstractBase()
  }

  @Test
  fun sealedExample() {
    val secretChild = SecretParent.SecretChild("private")
    assertThat(secretChild.toString()).isEqualTo("SecretChild(redact=██)")
  }

  @Test
  fun unredactedSealedExample() {
    val notSoSecretChild = SecretParent.NotSoSecretChild("public")
    assertThat(notSoSecretChild.toString()).isEqualTo("NotSoSecretChild(unredacted=public)")
  }

  @Test
  fun unredactedClassSealedExample() {
    val notAtAllSecretChild = SecretParent.NotAtAllSecretChild("public")
    assertThat(notAtAllSecretChild.toString()).isEqualTo("NotAtAllSecretChild(unredacted=public)")
  }

  @Test
  fun redactedObjectSealedExample() {
    assertThat(SecretParent.ProplessChild.toString()).isEqualTo("ProplessChild()")
  }

  @Redacted
  sealed class SecretParent {

    data class SecretChild(val redact: String) : SecretParent()

    data class NotSoSecretChild(@Unredacted val unredacted: String) : SecretParent()

    @Unredacted data class NotAtAllSecretChild(val unredacted: String) : AbstractBase()

    data object ProplessChild : SecretParent()
  }

  @Test
  fun supertypeRedactedExample() {
    val data = SuperRedacted("Bob", "2815551234")
    assertThat(data.toString()).isEqualTo("SuperRedacted(name=Bob, phoneNumber=██)")
  }

  @Test
  fun unredactedPropertyOnRedactedClassExample() {
    val data = RedactedClass("Bob", "2815551234")
    assertThat(data.toString()).isEqualTo("RedactedClass(name=Bob, phoneNumber=██)")
  }

  @Redacted interface Base

  data class SuperRedacted(@Unredacted val name: String, val phoneNumber: String) : Base

  @Redacted data class RedactedClass(@Unredacted val name: String, val phoneNumber: String)

  @Test
  fun userExample() {
    val user = User("Bob", "2815551234")
    assertThat(user.toString()).isEqualTo("User(name=Bob, phoneNumber=██)")
  }

  @Test
  fun classExample() {
    val sensitiveData = SensitiveData("123-456-7890", "1/1/00")
    assertThat(sensitiveData.toString()).isEqualTo("SensitiveData(██)")
  }

  @Redacted data class SensitiveData(val ssn: String, val birthday: String)

  @Test
  fun valueExample() {
    val sensitiveData = ValueClass("123-456-7890")
    assertThat(sensitiveData.toString()).isEqualTo("ValueClass(██)")
  }

  @Redacted @JvmInline value class ValueClass(val ssn: String)

  @Test
  fun complex() {
    val complex =
      Complex(
        redactedReferenceType = "redactedReferenceType",
        redactedNullableReferenceType = null,
        referenceType = "referenceType",
        nullableReferenceType = null,
        redactedPrimitiveType = 1,
        redactedNullablePrimitiveType = null,
        primitiveType = 2,
        nullablePrimitiveType = null,
        redactedArrayReferenceType = arrayOf("redactedArrayReferenceType"),
        redactedNullableArrayReferenceType = null,
        arrayReferenceType = arrayOf("arrayReferenceType"),
        nullableArrayReferenceType = null,
        redactedArrayPrimitiveType = intArrayOf(3),
        redactedNullableArrayPrimitiveType = null,
        arrayPrimitiveType = intArrayOf(4),
        nullableArrayGenericType = null,
        redactedGenericCollectionType = listOf(5),
        redactedNullableGenericCollectionType = null,
        genericCollectionType = listOf(6),
        nullableGenericCollectionType = null,
        redactedGenericType = 7,
        redactedNullableGenericType = null,
        genericType = 8,
        nullableGenericType = null,
      )

    assertThat(complex.toString())
      .isEqualTo(
        "Complex(" +
          "redactedReferenceType=██, " +
          "redactedNullableReferenceType=██, " +
          "referenceType=referenceType, " +
          "nullableReferenceType=null, " +
          "redactedPrimitiveType=██, " +
          "redactedNullablePrimitiveType=██, " +
          "primitiveType=2, " +
          "nullablePrimitiveType=null, " +
          "redactedArrayReferenceType=██, " +
          "redactedNullableArrayReferenceType=██, " +
          "arrayReferenceType=[arrayReferenceType], " +
          "nullableArrayReferenceType=null, " +
          "redactedArrayPrimitiveType=██, " +
          "redactedNullableArrayPrimitiveType=██, " +
          "arrayPrimitiveType=[4], " +
          "nullableArrayGenericType=null, " +
          "redactedGenericCollectionType=██, " +
          "redactedNullableGenericCollectionType=██, " +
          "genericCollectionType=[6], " +
          "nullableGenericCollectionType=null, " +
          "redactedGenericType=██, " +
          "redactedNullableGenericType=██, " +
          "genericType=8, " +
          "nullableGenericType=null" +
          ")"
      )
  }

  data class Complex<T>(
    @Redacted val redactedReferenceType: String,
    @Redacted val redactedNullableReferenceType: String?,
    val referenceType: String,
    val nullableReferenceType: String?,
    @Redacted val redactedPrimitiveType: Int,
    @Redacted val redactedNullablePrimitiveType: Int?,
    val primitiveType: Int,
    val nullablePrimitiveType: Int?,
    @Redacted val redactedArrayReferenceType: Array<String>,
    @Redacted val redactedNullableArrayReferenceType: Array<String>?,
    val arrayReferenceType: Array<String>,
    val nullableArrayReferenceType: Array<String>?,
    @Redacted val redactedArrayPrimitiveType: IntArray,
    @Redacted val redactedNullableArrayPrimitiveType: IntArray?,
    val arrayPrimitiveType: IntArray,
    val nullableArrayGenericType: IntArray?,
    @Redacted val redactedGenericCollectionType: List<T>,
    @Redacted val redactedNullableGenericCollectionType: List<T>?,
    val genericCollectionType: List<T>,
    val nullableGenericCollectionType: List<T>?,
    @Redacted val redactedGenericType: T,
    @Redacted val redactedNullableGenericType: T?,
    val genericType: T,
    val nullableGenericType: T?,
  )
}
