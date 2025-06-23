// REPLACEMENT_STRING: <redacted>
import kotlin.test.assertEquals

data class Test(@Redacted val a: Int)

fun box(): String {
  val test = Test(2)
  assertEquals("Test(a=<redacted>)", test.toString())
  return "OK"
}