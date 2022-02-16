package dev.zacsweers.redacted.gradle.variant

import com.android.build.gradle.api.BaseVariant
import org.gradle.api.Named

interface VariantFilter : Named {
  var ignore: Boolean
}

internal class CommonFilter(
  private val name: String,
) : VariantFilter {
  override fun getName(): String = name
  override var ignore: Boolean = false
}

class JvmVariantFilter internal constructor(
  commonFilter: CommonFilter
) : VariantFilter by commonFilter

class AndroidVariantFilter internal constructor(
  commonFilter: CommonFilter,
  val androidVariant: BaseVariant
) : VariantFilter by commonFilter
