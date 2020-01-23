package dev.zacsweers.redacted.compiler

import com.google.auto.service.AutoService
import org.jetbrains.kotlin.compiler.plugin.AbstractCliOption
import org.jetbrains.kotlin.compiler.plugin.CliOption
import org.jetbrains.kotlin.compiler.plugin.CommandLineProcessor
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.config.CompilerConfigurationKey

val KEY_ENABLED = CompilerConfigurationKey<Boolean>("enabled")
val KEY_REPLACEMENT_STRING = CompilerConfigurationKey<String>("replacementString")

@AutoService(CommandLineProcessor::class)
class RedactedCommandLineProcessor : CommandLineProcessor {

  override val pluginId: String = "redacted-compiler-plugin"

  override val pluginOptions: Collection<AbstractCliOption> =
      listOf(
          CliOption("enabled", "<true | false>", "", required = true),
          CliOption("replacementString", "String", "", required = true)
      )

  override fun processOption(
      option: AbstractCliOption,
      value: String,
      configuration: CompilerConfiguration
  ) = when (option.optionName) {
    "enabled" -> configuration.put(KEY_ENABLED, value.toBoolean())
    "replacementString" -> configuration.put(KEY_REPLACEMENT_STRING, value)
    else -> error("Unknown plugin option: ${option.optionName}")
  }
}
