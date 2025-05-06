import kotlin.test.assertEquals
import kotlin.jvm.JvmInline

@Redacted
@JvmInline
value class ValueClass(val ssn: String)

fun box(): String {
  val valueClass = ValueClass("123-456-7890")
  assertEquals("ValueClass(██)", valueClass.toString())
  return "OK"
}