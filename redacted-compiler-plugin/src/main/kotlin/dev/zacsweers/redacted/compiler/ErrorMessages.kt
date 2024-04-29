package dev.zacsweers.redacted.compiler

internal object ErrorMessages {
  // TODO make annotations configurable
  const val CUSTOM_TO_STRING_IN_REDACTED_CLASS_ERROR =
    "@Redacted is only supported on data or value classes that do *not* have a custom toString() function. Please remove the function or remove the @Redacted annotations."
  const val REDACTED_ON_ENUM_CLASS_ERROR = "@Redacted does not support enum classes or entries!"
  const val REDACTED_ON_NON_DATA_OR_VALUE_CLASS_ERROR =
    "@Redacted is only supported on data or value classes!"
  const val REDACTED_ON_VALUE_CLASS_PROPERTY_ERROR =
    "@Redacted is redundant on value class properties, just annotate the class instead."
  const val REDACTED_ON_OBJECT_ERROR = "@Redacted is useless on object classes."
  const val REDACTED_ON_CLASS_AND_PROPERTY_ERROR =
    "@Redacted should only be applied to the class or its properties, not both."
  // TODO port to FIR
  const val UNREDACTED_ON_OBJECT_ERROR = "@Unredacted is useless on object classes."
  const val UNREDACTED_AND_REDACTED_ERROR =
    "@Redacted and @Unredacted cannot be applied to a single class."
  const val UNREDACTED_ON_NONREDACTED_SUBTYPE_ERROR =
    "@Unredacted cannot be applied to a class unless a supertype is marked @Redacted."
  const val UNREDACTED_ON_NON_PROPERTY =
    "@Unredacted should only be applied to properties in a class or a supertype is marked @Redacted."
}
