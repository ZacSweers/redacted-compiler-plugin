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

import dev.zacsweers.redacted.annotations.Redacted
import dev.zacsweers.redacted.annotations.Unredacted
import io.ktor.util.Platform
import io.ktor.util.PlatformUtils
import kotlin.test.Test
import kotlin.test.assertEquals

class SmokeTest {

  @Test
  fun abstractExample() {
    val secretChild = AbstractBase.SecretChild("private")
    assertEquals("SecretChild(redact=██)", secretChild.toString())
  }

  @Test
  fun unredactedAbstractExample() {
    val notSoSecretChild = AbstractBase.NotSoSecretChild("public")
    assertEquals("NotSoSecretChild(unredacted=public)", notSoSecretChild.toString())
  }

  @Test
  fun unredactedClassAbstractExample() {
    val notAtAllSecretChild = AbstractBase.NotAtAllSecretChild("public")
    assertEquals("NotAtAllSecretChild(unredacted=public)", notAtAllSecretChild.toString())
  }

  @Test
  fun redactedObjectAbstractExample() {
    assertEquals("ProplessChild()", AbstractBase.ProplessChild.toString())
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
    assertEquals("SecretChild(redact=██)", secretChild.toString())
  }

  @Test
  fun unredactedSealedExample() {
    val notSoSecretChild = SecretParent.NotSoSecretChild("public")
    assertEquals("NotSoSecretChild(unredacted=public)", notSoSecretChild.toString())
  }

  @Test
  fun unredactedClassSealedExample() {
    val notAtAllSecretChild = SecretParent.NotAtAllSecretChild("public")
    assertEquals("NotAtAllSecretChild(unredacted=public)", notAtAllSecretChild.toString())
  }

  @Test
  fun redactedObjectSealedExample() {
    assertEquals("ProplessChild()", SecretParent.ProplessChild.toString())
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
    assertEquals("SuperRedacted(name=Bob, phoneNumber=██)", data.toString())
  }

  @Test
  fun unredactedPropertyOnRedactedClassExample() {
    val data = RedactedClass("Bob", "2815551234")
    assertEquals("RedactedClass(name=Bob, phoneNumber=██)", data.toString())
  }

  @Redacted interface Base

  data class SuperRedacted(@Unredacted val name: String, val phoneNumber: String) : Base

  @Redacted data class RedactedClass(@Unredacted val name: String, val phoneNumber: String)

  @Test
  fun userExample() {
    val user = User("Bob", "2815551234")
    assertEquals("User(name=Bob, phoneNumber=██)", user.toString())
  }

  @Test
  fun classExample() {
    val sensitiveData = SensitiveData("123-456-7890", "1/1/00")
    assertEquals("SensitiveData(██)", sensitiveData.toString())
  }

  @Redacted data class SensitiveData(val ssn: String, val birthday: String)

  // TODO KMP-ify this test?
  //  @Test
  //  fun valueExample() {
  //    val sensitiveData = ValueClass("123-456-7890")
  //    assertEquals("ValueClass(██)", sensitiveData.toString())
  //  }
  //
  //  @Redacted value class ValueClass(val ssn: String)

  /*
  Complex(redactedReferenceType=██, redactedNullableReferenceType=██, referenceType=referenceType, nullableReferenceType=null, redactedPrimitiveType=██, redactedNullablePrimitiveType=██, primitiveType=2, nullablePrimitiveType=null, redactedArrayReferenceType=██, redactedNullableArrayReferenceType=██, arrayReferenceType=[...], nullableArrayReferenceType=null, redactedArrayPrimitiveType=██, redactedNullableArrayPrimitiveType=██, arrayPrimitiveType=[...], nullableArrayGenericType=null, redactedGenericCollectionType=██, redactedNullableGenericCollectionType=██, genericCollectionType=[...], nullableGenericCollectionType=null, redactedGenericType=██, redactedNullableGenericType=██, genericType=8, nullableGenericType=null)
  Complex(redactedReferenceType=██, redactedNullableReferenceType=██, referenceType=referenceType, nullableReferenceType=null, redactedPrimitiveType=██, redactedNullablePrimitiveType=██, primitiveType=2, nullablePrimitiveType=null, redactedArrayReferenceType=██, redactedNullableArrayReferenceType=██, arrayReferenceType=[...], nullableArrayReferenceType=null, redactedArrayPrimitiveType=██, redactedNullableArrayPrimitiveType=██, arrayPrimitiveType=[...], nullableArrayGenericType=null, redactedGenericCollectionType=██, redactedNullableGenericCollectionType=██, genericCollectionType=[6], nullableGenericCollectionType=null, redactedGenericType=██, redactedNullableGenericType=██, genericType=8, nullableGenericType=null)>.
	at protoOf.assertTrue_rpw5fg(/var/folders/_s/ft8kp2k12ps1jlfbt2r38z_r0000gn/T/_karma_webpack_177656/commons.js:22610)

   */

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

    assertEquals(
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
        "arrayReferenceType=[${arrayContent("arrayReferenceType")}], " +
        "nullableArrayReferenceType=null, " +
        "redactedArrayPrimitiveType=██, " +
        "redactedNullableArrayPrimitiveType=██, " +
        "arrayPrimitiveType=[${arrayContent(4)}], " +
        "nullableArrayGenericType=null, " +
        "redactedGenericCollectionType=██, " +
        "redactedNullableGenericCollectionType=██, " +
        "genericCollectionType=[6], " +
        "nullableGenericCollectionType=null, " +
        "redactedGenericType=██, " +
        "redactedNullableGenericType=██, " +
        "genericType=8, " +
        "nullableGenericType=null" +
        ")",
      complex.toString(),
    )
  }

  private fun arrayContent(content: Any): String {
    return if (
      PlatformUtils.IS_JS) {
      "..."
    } else {
      content.toString()
    }
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
