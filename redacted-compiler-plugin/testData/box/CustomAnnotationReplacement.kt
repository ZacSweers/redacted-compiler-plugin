// REPLACEMENT_STRING: [HIDDEN]
// REDACTED_ANNOTATIONS: /CustomRedacted
// UNREDACTED_ANNOTATIONS: /CustomUnredacted

import kotlin.test.assertEquals

annotation class CustomRedacted
annotation class CustomUnredacted

data class Person(
  @Redacted val password: String,
  @CustomRedacted val creditCard: String,
)

@CustomRedacted
data class ClassRedacted(
  @Unredacted val name: String,
  @CustomUnredacted val email: String
)

fun box(): String {
  val person = Person("secret123", "1234-5678-9012-3456")
  
  // Verify that both @Redacted and @CustomRedacted properties are redacted with the custom replacement string
  assertEquals(
    "Person(password=[HIDDEN], creditCard=[HIDDEN])",
    person.toString()
  )

  val classRedacted = ClassRedacted("John Doe", "john@example.com")

  // Verify that both @Unredacted and @CustomUnredacted properties are not redacted
  assertEquals(
    "ClassRedacted(name=John Doe, email=john@example.com)",
    classRedacted.toString()
  )
  
  return "OK"
}