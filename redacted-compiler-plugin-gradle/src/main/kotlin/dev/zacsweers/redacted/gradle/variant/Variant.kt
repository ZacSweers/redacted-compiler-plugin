package dev.zacsweers.redacted.gradle.variant

import org.gradle.api.Project
import org.gradle.api.tasks.TaskProvider
import org.jetbrains.kotlin.gradle.plugin.KotlinCompilation
import org.jetbrains.kotlin.gradle.plugin.KotlinPlatformType
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinJvmAndroidCompilation
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

internal class Variant private constructor(
  val project: Project,
  val compileTaskProvider: TaskProvider<KotlinCompile>,
  val variantFilter: VariantFilter,
) {
  companion object {
    operator fun invoke(kotlinCompilation: KotlinCompilation<*>): Variant {
      // Sanity check.
      require(
        kotlinCompilation.platformType != KotlinPlatformType.androidJvm ||
            kotlinCompilation is KotlinJvmAndroidCompilation
      ) {
        "The KotlinCompilation is KotlinJvmAndroidCompilation, but the platform type is different."
      }

      val project = kotlinCompilation.target.project
      val androidVariant = (kotlinCompilation as? KotlinJvmAndroidCompilation)?.androidVariant

      val commonFilter = CommonFilter(kotlinCompilation.name)
      val variantFilter = if (androidVariant != null) {
        AndroidVariantFilter(commonFilter, androidVariant)
      } else {
        JvmVariantFilter(commonFilter)
      }

      @Suppress("UNCHECKED_CAST")
      return Variant(
        project = project,
        compileTaskProvider = kotlinCompilation.compileKotlinTaskProvider as TaskProvider<KotlinCompile>,
        variantFilter = variantFilter
      ).also {
        // Sanity check.
        check(it.compileTaskProvider.name.startsWith("compile"))
      }
    }
  }
}
