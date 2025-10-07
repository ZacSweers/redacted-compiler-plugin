import kotlin.test.assertEquals

@Redacted
data class SensitiveData(val ssn: String, val birthday: String)

fun box(): String {
  val sensitiveData = SensitiveData("123-456-7890", "1/1/00")
  assertEquals("SensitiveData(██)", sensitiveData.toString())
  return "OK"
}
