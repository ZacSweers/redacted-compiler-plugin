package dev.zacsweers.redacted.compiler

import com.google.common.truth.Truth.assertThat
import com.tschuchort.compiletesting.KotlinCompilation
import com.tschuchort.compiletesting.PluginOption
import com.tschuchort.compiletesting.SourceFile
import com.tschuchort.compiletesting.SourceFile.Companion.kotlin
import org.jetbrains.kotlin.compiler.plugin.CommandLineProcessor
import org.jetbrains.kotlin.config.JvmTarget
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class RedactedPluginTest {

  @Rule
  @JvmField
  var temporaryFolder: TemporaryFolder = TemporaryFolder()

  private val redacted = kotlin("Redacted.kt",
      """
      package dev.zacsweers.redacted.compiler.test
      
      import kotlin.annotation.AnnotationRetention.BINARY
      import kotlin.annotation.AnnotationTarget.PROPERTY
      
      @Retention(BINARY)
      @Target(PROPERTY)
      annotation class Redacted
      """)

  @Test
  fun dataIsRequired() {
    val result = compile(kotlin("NonDataClass.kt",
        """
          package dev.zacsweers.redacted.compiler.test

          import dev.zacsweers.redacted.compiler.test.Redacted

          class NonDataClass(@Redacted val a: Int)
          """
    ))
    assertThat(result.exitCode).isEqualTo(KotlinCompilation.ExitCode.COMPILATION_ERROR)

    // Full log is something like this:
    // e: /path/to/NonDataClass.kt: (5, 1): @Redacted is only supported on data classes!
    assertThat(result.messages).contains(
        "NonDataClass.kt: (5, 1): @Redacted is only supported on data classes!")
  }

  @Test
  fun `verbose should show extra logging`() {
    val compilation = prepareCompilation(kotlin("source.kt",
        """
          package dev.zacsweers.redacted.compiler.test

          import dev.zacsweers.redacted.compiler.test.Redacted

          data class Test(@Redacted val a: Int)
          """
    ))

    compilation.verbose = true
    val result = compilation.compile()
    assertThat(result.exitCode).isEqualTo(KotlinCompilation.ExitCode.OK)
    assertThat(result.messages).contains(LOG_PREFIX)
  }

  @Test
  fun `not verbose should show no extra logging`() {
    val result = compile(kotlin("source.kt",
        """
          package dev.zacsweers.redacted.compiler.test

          import dev.zacsweers.redacted.compiler.test.Redacted

          data class Test(@Redacted val a: Int)
          """
    ))
    assertThat(result.exitCode).isEqualTo(KotlinCompilation.ExitCode.OK)
    assertThat(result.messages).doesNotContain(LOG_PREFIX)
  }

  private fun prepareCompilation(vararg sourceFiles: SourceFile): KotlinCompilation {
    return KotlinCompilation()
        .apply {
          workingDir = temporaryFolder.root
          compilerPlugins = listOf(RedactedComponentRegistrar())
          val processor = RedactedCommandLineProcessor()
          commandLineProcessors = listOf(processor)
          pluginOptions = listOf(
              processor.option(KEY_ENABLED, "true"),
              processor.option(KEY_REPLACEMENT_STRING, "██"),
              processor.option(KEY_REDACTED_ANNOTATION, "dev.zacsweers.redacted.compiler.test.Redacted"),
              processor.option(KEY_REDACT_ALL_DATA_CLASSES, "false")
          )
          inheritClassPath = true
          sources = sourceFiles.asList() + redacted
          verbose = false
          jvmTarget = JvmTarget.fromString(
              System.getenv()["ci_java_version"] ?: "1.8")!!.description
        }
  }

  private fun CommandLineProcessor.option(key: Any, value: Any?): PluginOption {
    return PluginOption(pluginId, key.toString(), value.toString())
  }

  private fun compile(vararg sourceFiles: SourceFile): KotlinCompilation.Result {
    return prepareCompilation(*sourceFiles).compile()
  }

}