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

import com.google.common.truth.Truth.assertThat
import com.tschuchort.compiletesting.CompilationResult
import com.tschuchort.compiletesting.JvmCompilationResult
import com.tschuchort.compiletesting.KotlinCompilation
import com.tschuchort.compiletesting.KotlinCompilation.ExitCode.COMPILATION_ERROR
import com.tschuchort.compiletesting.PluginOption
import com.tschuchort.compiletesting.SourceFile
import com.tschuchort.compiletesting.SourceFile.Companion.kotlin
import dev.zacsweers.redacted.compiler.RedactedCommandLineProcessor.Companion.OPTION_ENABLED
import dev.zacsweers.redacted.compiler.RedactedCommandLineProcessor.Companion.OPTION_REDACTED_ANNOTATIONS
import dev.zacsweers.redacted.compiler.RedactedCommandLineProcessor.Companion.OPTION_REPLACEMENT_STRING
import dev.zacsweers.redacted.compiler.RedactedCommandLineProcessor.Companion.OPTION_UNREDACTED_ANNOTATIONS
import org.jetbrains.kotlin.compiler.plugin.CliOption
import org.jetbrains.kotlin.compiler.plugin.CommandLineProcessor
import org.jetbrains.kotlin.compiler.plugin.ExperimentalCompilerApi
import org.jetbrains.kotlin.config.JvmTarget
import org.jetbrains.kotlin.name.FqName
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@RunWith(Parameterized::class)
@OptIn(ExperimentalCompilerApi::class)
class RedactedPluginTest(redactedNames: Pair<FqName, FqName>) {

  companion object {
    @JvmStatic
    @Parameterized.Parameters(name = "ir={0}")
    fun data(): Collection<Array<Any>> {
      return listOf(
        arrayOf(
          FqName.fromSegments(listOf("test", "Redacted")) to
            FqName.fromSegments(listOf("test", "Unredacted"))
        ),
        arrayOf(
          FqName.fromSegments(listOf("test", "Redacted2")) to
            FqName.fromSegments(listOf("test", "Unredacted2"))
        ),
      )
    }
  }

  @Rule @JvmField var temporaryFolder: TemporaryFolder = TemporaryFolder()

  private val redactedAnnotation = redactedNames.first
  private val unredactedAnnotation = redactedNames.second

  private val redacted =
    kotlin(
      "Redacted.kt",
      """
      package test

      import kotlin.annotation.AnnotationRetention.BINARY
      import kotlin.annotation.AnnotationTarget.PROPERTY
      import kotlin.annotation.AnnotationTarget.CLASS

      @Retention(BINARY)
      @Target(PROPERTY, CLASS)
      annotation class ${redactedAnnotation.shortName().asString()}

      @Retention(BINARY)
      @Target(PROPERTY)
      annotation class ${unredactedAnnotation.shortName().asString()}
      """,
    )

  @Test
  fun classIsRequired() {
    val result =
      compile(
        kotlin(
          "NonClass.kt",
          """
          package dev.zacsweers.redacted.compiler.test

          import $redactedAnnotation

          enum class NonClass(@${redactedAnnotation.shortName().asString()} val a: Int)
          """,
        )
      )
    assertThat(result.exitCode).isEqualTo(COMPILATION_ERROR)

    // Full log is something like this:
    // e: /path/to/NonDataClass.kt:5:20 @${redactedAnnotation.shortName().asString() is only
    // supported on data classes!
    assertThat(result.messages).contains("NonClass.kt:5:")
    result.assertErrorMessage(
      "@${redactedAnnotation.shortName().asString()} does not support enum classes or entries!"
    )
  }

  @Test
  fun customToStringIsAnError() {
    val result =
      compile(
        kotlin(
          "CustomToString.kt",
          """
          package dev.zacsweers.redacted.compiler.test

          import $redactedAnnotation

          data class CustomToString(@${redactedAnnotation.shortName().asString()} val a: Int) {
            override fun toString(): String = "foo"
          }
          """,
        )
      )
    assertThat(result.exitCode).isEqualTo(COMPILATION_ERROR)

    // Full log is something like this:
    // e: /path/to/NonDataClass.kt:5:20 @${redactedAnnotation.shortName().asString() is only
    // supported on data classes!
    assertThat(result.messages).contains("CustomToString.kt:6:")
    result.assertErrorMessage(
      "@${redactedAnnotation.shortName().asString()} is only supported on data or value classes that do *not* have a custom toString() function"
    )
  }

  @Test
  fun valueClassWithAnnotatedProperty() {
    val result =
      compile(
        kotlin(
          "AnnotatedValueProp.kt",
          """
          package dev.zacsweers.redacted.compiler.test

          import $redactedAnnotation
          import kotlin.jvm.JvmInline

          @JvmInline
          value class AnnotatedValueProp(@${redactedAnnotation.shortName().asString()} val a: Int)
          """,
        )
      )
    assertThat(result.exitCode).isEqualTo(COMPILATION_ERROR)

    // Full log is something like this:
    // e: /path/to/AnnotatedValueProp.kt:5:1 @${redactedAnnotation.shortName().asString() is
    // redundant on value class properties
    // FIR reports this line number correctly, K1 reports it as 6
    assertThat(result.messages).contains("AnnotatedValueProp.kt:7:")
    result.assertErrorMessage(
      "@${redactedAnnotation.shortName().asString()} is redundant on value class properties"
    )
  }

  @Test
  fun dataObject() {
    val result =
      compile(
        kotlin(
          "DataObject.kt",
          """
          package dev.zacsweers.redacted.compiler.test

          import $redactedAnnotation

          @${redactedAnnotation.shortName().asString()}
          data object CustomToString
          """,
        )
      )
    assertThat(result.exitCode).isEqualTo(COMPILATION_ERROR)

    // Full log is something like this:
    // e: /path/to/NonDataClass.kt: (5, 1): @${redactedAnnotation.shortName().asString() is useless
    // on object classes
    assertThat(result.messages).contains("DataObject.kt:5:")
    result.assertErrorMessage(
      "@${redactedAnnotation.shortName().asString()} is useless on object classes"
    )
  }

  @Test
  fun annotatingBothClassAndPropertiesIsAnError() {
    val result =
      compile(
        kotlin(
          "DoubleAnnotation.kt",
          """
          package dev.zacsweers.redacted.compiler.test

          import $redactedAnnotation

          @${redactedAnnotation.shortName().asString()}
          data class DoubleAnnotation(@${redactedAnnotation.shortName().asString()} val a: Int)
          """,
        )
      )
    assertThat(result.exitCode).isEqualTo(COMPILATION_ERROR)

    result.assertErrorMessage(
      """
        @${redactedAnnotation.shortName().asString()} detected on multiple targets:
        class: 'DoubleAnnotation'
        properties: 'a'
      """
        .trimIndent()
    )
  }

  @Test
  fun `verbose should show extra logging`() {
    val compilation =
      prepareCompilation(
        kotlin(
          "source.kt",
          """
          package dev.zacsweers.redacted.compiler.test

          import $redactedAnnotation

          data class Test(@${redactedAnnotation.shortName().asString()} val a: Int)
          """,
        )
      )

    compilation.verbose = true
    val result = compilation.compile()
    assertThat(result.exitCode).isEqualTo(KotlinCompilation.ExitCode.OK)
    assertThat(result.messages).contains(LOG_PREFIX)
  }

  @Test
  fun `not verbose should show no extra logging`() {
    val result =
      compile(
        kotlin(
          "source.kt",
          """
          package dev.zacsweers.redacted.compiler.test

          import $redactedAnnotation

          data class Test(@${redactedAnnotation.shortName().asString()} val a: Int)
          """,
        )
      )
    assertThat(result.exitCode).isEqualTo(KotlinCompilation.ExitCode.OK)
    assertThat(result.messages).doesNotContain(LOG_PREFIX)
  }

  @Test
  fun customReplacement() {
    val result =
      compile(
        "<redacted>",
        kotlin(
          "source.kt",
          """
          package dev.zacsweers.redacted.compiler.test

          import $redactedAnnotation

          data class Test(@${redactedAnnotation.shortName().asString()} val a: Int)
          """,
        ),
      )
    assertThat(result.exitCode).isEqualTo(KotlinCompilation.ExitCode.OK)
    assertThat(result.messages).doesNotContain(LOG_PREFIX)
    check(result is JvmCompilationResult)
    val testClass = result.classLoader.loadClass("dev.zacsweers.redacted.compiler.test.Test")
    val instance = testClass.getConstructor(Int::class.javaPrimitiveType).newInstance(2)
    assertThat(instance.toString()).isEqualTo("Test(a=<redacted>)")
  }

  @Test
  fun classAnnotated() {
    val result =
      compile(
        kotlin(
          "source.kt",
          """
          package dev.zacsweers.redacted.compiler.test

          import $redactedAnnotation

          @${redactedAnnotation.shortName().asString()}
          data class SensitiveData(val ssn: String, val birthday: String)
          """,
        )
      )
    assertThat(result.exitCode).isEqualTo(KotlinCompilation.ExitCode.OK)
    check(result is JvmCompilationResult)
    val complex =
      result.classLoader
        .loadClass("dev.zacsweers.redacted.compiler.test.SensitiveData")
        .kotlin
        .constructors
        .first()
        .call("123-456-7890", "1/1/00")
    assertThat(complex.toString()).isEqualTo("SensitiveData(██)")
  }

  @Test
  fun valueClass() {
    val result =
      compile(
        kotlin(
          "source.kt",
          """
          package dev.zacsweers.redacted.compiler.test

          import kotlin.jvm.JvmInline
          import $redactedAnnotation

          @${redactedAnnotation.shortName().asString()}
          @JvmInline
          value class ValueClass(val ssn: String)
          """,
        )
      )
    assertThat(result.exitCode).isEqualTo(KotlinCompilation.ExitCode.OK)
    check(result is JvmCompilationResult)
    val complex =
      result.classLoader
        .loadClass("dev.zacsweers.redacted.compiler.test.ValueClass")
        .kotlin
        .constructors
        .first()
        .call("123-456-7890")
    assertThat(complex.toString()).isEqualTo("ValueClass(██)")
  }

  private fun prepareCompilation(vararg sourceFiles: SourceFile): KotlinCompilation {
    return prepareCompilation(null, *sourceFiles)
  }

  private fun prepareCompilation(
    replacementString: String? = null,
    vararg sourceFiles: SourceFile,
  ): KotlinCompilation {
    return KotlinCompilation().apply {
      workingDir = temporaryFolder.root
      compilerPluginRegistrars = listOf(RedactedComponentRegistrar())
      val processor = RedactedCommandLineProcessor()
      commandLineProcessors = listOf(processor)
      pluginOptions =
        listOf(
          processor.option(OPTION_ENABLED, "true"),
          processor.option(OPTION_REPLACEMENT_STRING, replacementString ?: "██"),
          processor.option(
            OPTION_REDACTED_ANNOTATIONS,
            redactedAnnotation.pathSegments().joinToString("/"),
          ),
          processor.option(
            OPTION_UNREDACTED_ANNOTATIONS,
            unredactedAnnotation.pathSegments().joinToString("/"),
          ),
        )
      inheritClassPath = true
      sources = sourceFiles.asList() + redacted
      verbose = false
      jvmTarget = JvmTarget.fromString(System.getProperty("rdt.jvmTarget", "1.8"))!!.description
    }
  }

  private fun CommandLineProcessor.option(key: CliOption, value: Any?): PluginOption {
    return PluginOption(pluginId, key.optionName, value.toString())
  }

  private fun compile(vararg sourceFiles: SourceFile): CompilationResult {
    return compile(null, *sourceFiles)
  }

  private fun compile(
    replacementString: String? = null,
    vararg sourceFiles: SourceFile,
  ): CompilationResult {
    return prepareCompilation(replacementString, *sourceFiles).compile()
  }

  private fun CompilationResult.assertErrorMessage(message: String) {
    assertThat(messages).contains(message)
  }
}
