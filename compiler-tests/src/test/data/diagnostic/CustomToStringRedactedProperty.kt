// RENDER_DIAGNOSTICS_FULL_TEXT

data class CustomToStringRedactedProperty(@Redacted val a: Int) {
  override fun <!REDACTED_ERROR!>toString<!>(): String = "foo"
}
