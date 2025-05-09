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
package dev.zacsweers.redacted.compiler

import com.google.auto.service.AutoService
import org.jetbrains.kotlin.compiler.plugin.AbstractCliOption
import org.jetbrains.kotlin.compiler.plugin.CliOption
import org.jetbrains.kotlin.compiler.plugin.CommandLineProcessor
import org.jetbrains.kotlin.compiler.plugin.ExperimentalCompilerApi
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.config.CompilerConfigurationKey

internal val KEY_ENABLED =
  CompilerConfigurationKey<Boolean>("Enable/disable Redacted's plugin on the given compilation")
internal val KEY_REPLACEMENT_STRING =
  CompilerConfigurationKey<String>("The replacement string to use in redactions")
internal val KEY_REDACTED_ANNOTATIONS =
  CompilerConfigurationKey<String>(
    "The redacted marker annotations (i.e. com/example/Redacted) to look for when redacting"
  )
internal val KEY_UNREDACTED_ANNOTATION =
  CompilerConfigurationKey<String>(
    "The unredacted marker annotations (i.e. com/example/Unredacted) to look for when redacting"
  )

@OptIn(ExperimentalCompilerApi::class)
@AutoService(CommandLineProcessor::class)
public class RedactedCommandLineProcessor : CommandLineProcessor {

  internal companion object {
    val OPTION_ENABLED =
      CliOption(
        optionName = "enabled",
        valueDescription = "<true | false>",
        description = KEY_ENABLED.toString(),
        required = false,
        allowMultipleOccurrences = false,
      )

    val OPTION_REPLACEMENT_STRING =
      CliOption(
        optionName = "replacementString",
        valueDescription = "String",
        description = KEY_REPLACEMENT_STRING.toString(),
        required = true,
        allowMultipleOccurrences = false,
      )

    val OPTION_REDACTED_ANNOTATIONS =
      CliOption(
        optionName = "redactedAnnotations",
        valueDescription = "String",
        description = KEY_REDACTED_ANNOTATIONS.toString(),
        required = true,
        allowMultipleOccurrences = false,
      )

    val OPTION_UNREDACTED_ANNOTATIONS =
      CliOption(
        optionName = "unredactedAnnotations",
        valueDescription = "String",
        description = KEY_UNREDACTED_ANNOTATION.toString(),
        required = true,
        allowMultipleOccurrences = false,
      )
  }

  override val pluginId: String = "dev.zacsweers.redacted.compiler"

  override val pluginOptions: Collection<AbstractCliOption> =
    listOf(
      OPTION_ENABLED,
      OPTION_REPLACEMENT_STRING,
      OPTION_REDACTED_ANNOTATIONS,
      OPTION_UNREDACTED_ANNOTATIONS,
    )

  override fun processOption(
    option: AbstractCliOption,
    value: String,
    configuration: CompilerConfiguration,
  ): Unit =
    when (option.optionName) {
      "enabled" -> configuration.put(KEY_ENABLED, value.toBoolean())
      "replacementString" -> configuration.put(KEY_REPLACEMENT_STRING, value)
      "redactedAnnotations" -> configuration.put(KEY_REDACTED_ANNOTATIONS, value)
      "unredactedAnnotations" -> configuration.put(KEY_UNREDACTED_ANNOTATION, value)
      else -> error("Unknown plugin option: ${option.optionName}")
    }
}
