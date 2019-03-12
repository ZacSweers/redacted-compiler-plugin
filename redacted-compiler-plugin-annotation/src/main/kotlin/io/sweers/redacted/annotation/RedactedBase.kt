package io.sweers.redacted.annotation

/**
 * A base class to extend to enable redacted support in data classes. Just make your data class
 * extend this class.
 */
abstract class RedactedBase {
  final override fun toString(): String {
    return toRedactedString()
  }

  open fun toRedactedString(): String {
    return "<will be overridden>"
  }
}