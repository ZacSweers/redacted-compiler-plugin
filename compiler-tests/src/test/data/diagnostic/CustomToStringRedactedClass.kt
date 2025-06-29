// RENDER_DIAGNOSTICS_FULL_TEXT

@Redacted
data class CustomToStringRedactedClass(val a: Int) {
  override fun <!REDACTED_ERROR!>toString<!>(): String = "foo"
}
