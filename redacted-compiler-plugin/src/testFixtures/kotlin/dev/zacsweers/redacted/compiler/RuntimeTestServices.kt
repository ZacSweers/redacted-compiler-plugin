// Copyright (C) 2025 Zac Sweers
// SPDX-License-Identifier: Apache-2.0
package dev.zacsweers.redacted.compiler

import java.io.File
import org.jetbrains.kotlin.cli.jvm.config.addJvmClasspathRoot
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.test.model.TestModule
import org.jetbrains.kotlin.test.services.EnvironmentConfigurator
import org.jetbrains.kotlin.test.services.RuntimeClasspathProvider
import org.jetbrains.kotlin.test.services.TestServices

private val redactedRuntimeClasspath =
  System.getProperty("redactedRuntime.classpath")
    ?.split(File.pathSeparator)
    ?.singleOrNull()
    // Because Gradle's Test.systemProperties don't support Providers, we actually just get a
    // toString() of the provider here, which sucks
    // Looks like "fixed(class java.lang.String,
    // /Users/zacsweers/dev/kotlin/personal/redacted-compiler-plugin/redacted-compiler-plugin-annotations/build/libs/redacted-compiler-plugin-annotations-jvm-1.16.0-SNAPSHOT.jar)"
    // So we parse it out and never speak of this again
    ?.substringAfter(", ")
    ?.removeSuffix(")")
    ?.let(::File)
    ?: error("Unable to get a valid classpath from 'redactedRuntime.classpath' property")

class RedactedRuntimeEnvironmentConfigurator(testServices: TestServices) :
  EnvironmentConfigurator(testServices) {
  override fun configureCompilerConfiguration(
    configuration: CompilerConfiguration,
    module: TestModule,
  ) {
    configuration.addJvmClasspathRoot(redactedRuntimeClasspath)
  }
}

class RedactedRuntimeClassPathProvider(testServices: TestServices) :
  RuntimeClasspathProvider(testServices) {
  override fun runtimeClassPaths(module: TestModule): List<File> {
    return listOf(redactedRuntimeClasspath)
  }
}
