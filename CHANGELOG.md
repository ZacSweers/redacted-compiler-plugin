Changelog
=========

**Unreleased**
--------------

- **Fix**: Fix CLI parsing for multiple custom annotations. Note that while the previous version was broken, it would have _also_ failed compilation so there shouldn't be a chance that broken redactions would have made it into a production build.
- Update to Kotlin `2.1.10`.
- Build against Gradle `8.12.1`.

1.12.0
------

_2024-12-20_

- Fix FIR diagnostics rendering in the IDE. Note this only works in the K2 Kotlin IDE plugin + setting the IntelliJ `kotlin.k2.only.bundled.compiler.plugins.enabled` registry key to `false`.
- When custom annotations are defined, report those names in FIR error messages.
- Support multiple custom annotations.
  - For Gradle configuration, the singular `*Annotation` properties are deprecated in favor of plural`*Annotations` `SetProperty` types.
  - For CLI consumers, the `redactedAnnotation` and `unredactedAnnotation` properties are now `redactedAnnotations` and `unredactedAnnotations`.
- Build against Gradle `8.12`.
- Only report errors in FIR now. Removes the `validateIr` plugin option.
- No longer support K1.
- Raise Gradle plugin Kotlin target to `1.9`.

1.11.0
------

_2024-11-29_

- Update to Kotlin `2.1.0`. This plugin now requires `2.1.0` or later.
- Build against Gradle `8.11.1`.

1.10.0
------

_2024-08-22_

- Update to Kotlin `2.0.20`.
- Build against Gradle `8.10`.

1.9.0
-----

_2024-05-22_

- Update to Kotlin `2.0.0` (aka K2). This plugin now assumes 2.0, but can be used with Kotlin 1.9.x as well.
- Fully implement validation checks in FIR, allowing the plugin to report errors earlier and also (eventually) have errors appear automatically in the IDE.
  - The IR plugin no longer validates by default.
  - At the time of writing, I'm not able to get errors to appear in the IDE even if non-bundled plugins are force-enabled. Follow [KTIJ-29248](https://youtrack.jetbrains.com/issue/KTIJ-29248) for more info.
- The `enabled` compiler option is no longer required and just defaults to true.
- Omit the stdlib from transitive dependencies on the compiler plugin and Gradle plugin artifacts. Both kotlinc and Gradle impose their own versions on the classpath.

1.8.1
-----

_2024-04-26_

- Allow `@Unredacted` to be applied to a class, only when a supertype is `@Redacted`
- Allow `@Redacted` supertypes to be inherited by objects, only when the child does not implement a custom `toString` method
- Fail compilation when `@Unredacted` and `@Redacted` are applied to the same class

Special thanks to [@DrewCarlson](https://github.com/DrewCarlson) for contributing to this release!

1.8.0
-----

_2024-04-23_

- **New**: Support for annotating interfaces and non-final classes as `@Redacted`. In this event, all `data`/`value` subclasses will be treated as `@Redacted`.
- **New**: Support for `@Unredacted` to explicitly opt out of redacting specific properties in otherwise-redacted classes.
  ```kotlin
  @Redacted
  data class User(
    @Unredacted val name: String,
    val phoneNumber: String
  )

  // This will redact `phoneNumber` but not `name`
  // User(name=Bob, phoneNumber=██)
  ```
- Update Kotlin to `1.9.23`.

Special thanks to [@DrewCarlson](https://github.com/DrewCarlson) for contributing to this release!

1.7.1
-----

_2023-11-26_

- Update Kotlin to `1.9.21`.

1.7.0
-----

_2023-10-31_

- Update to Kotlin `1.9.20`.  This plugin now requires `1.9.20`.
- Update wasm target to `wasmJs`.

1.6.1
-----

_2023-09-02_

- **Enhancement**: Simplify lookup of `KotlinCompilation`'s `implementation` configuration name in the Gradle plugin when using the default annotation.
- Update to Kotlin `1.9.10`.
- Build against Gradle `8.3`.

1.6.0
-----

_2023-07-08_

- **New**: Support `value class` types. Note that only annotating the class is supported, as annotating the property would be redundant.

1.5.0
-----

_2023-07-06_

- Update to Kotlin `1.9.0`. This plugin now requires `1.9.0`.

1.4.0
-----

_2023-04-03_

- Update to Kotlin `1.8.20`. This plugin now requires `1.8.20`.
- [annotations] Mark JS binaries as executable.
- [annotations] Remove deprecated `watchosX86()`.
- [annotations] Add `wasm` target. Note this is experimental and not stable.
- [annotations] Add `androidNativeArm32`, `androidNativeArm64`, `androidNativeX86`, `androidNativeX64`, and `watchosDeviceArm64` targets.

1.3.1
-----

_2023-03-16_

- **Fix**: Missing jvmTarget in the annotations artifact. Target is now properly set to Java 11.
- Update to Kotlin `1.8.10`.

1.3.0
-----

_2022-12-28_

- Update to Kotlin `1.8.0`. This release is only compatible with Kotlin 1.8 or later.
- Update JVM target to `11`.
- Kotlin JS artifact now only supports IR.
- Migrate the IR and FIR plugins to new `CompilerPluginRegistrar` entrypoint API.

1.2.1
-----

_2022-12-03_

This release is primarily under-the-hood changes + extra compile-time checks.

- [IR] Fail compilation if both class and any number of properties are annotated with `@Redacted`.
- [IR] Fail compilation if `@Redacted` is used in a non-class context.
  - Before it would only check if the class is non-data, but with Kotlin 1.8 introducing `data object` classes we need to also check that the target type is itself a class.
- [IR] Fail compilation if a custom `toString()` function is implemented in a data class using `@Redacted`.
- [IR] Harden `toString()` function declaration matching.
- [FIR] Promote `REDACTED_ON_CLASS_AND_PROPERTY_WARNING` to error.
- Update to Kotlin 1.7.22.

1.2.0
-----

_2022-10-03_

Experimental support for the new K2 compiler + FIR plugin.

Note this comes with several caveats:
- No IDE support yet
- Errors and warnings can't have custom messages yet: [KT-53510](https://youtrack.jetbrains.com/issue/KT-53510).
- Multiple errors and warnings result in only a single error being emitted: [KT-54287](https://youtrack.jetbrains.com/issue/KT-54287).
- K2 compiler itself is extremely experimental.

In short, this is only really to unblock anyone doing their own testing of K2 and don't want this
plugin to disable it. If you see any issues, please file a bug here and disable K2 in your project
in the meantime.

Details on K2 and instructions for enabling it can be found here: https://kotlinlang.org/docs/whatsnew17.html#new-kotlin-k2-compiler-for-the-jvm-in-alpha

Also: update to Kotlin 1.7.20.

1.1.0
-----

_2022-06-09_

Update to Kotlin 1.7.0.

Note that Kotlin 1.7.0 is now the _minimum_ for this version due to breaking API changes in Kotlin IR APIs.

1.0.1
-----

_2022-01-13_

* Use `implementation` instead of `api` adding the `redacted-compiler-plugin-annotations` dependency. [#76](https://github.com/ZacSweers/redacted-compiler-plugin/issues/76)

Thanks to [@gpeal](https://github.com/gpeal) for contributing to this release!

1.0.0
-----

_2021-12-24_

Stable release!

While Kotlin IR is not a stable API, the public API of redacted-compiler-plugin is. This project will use semver only
for its own API but intermediate versions may only work with specific versions of Kotlin (documented in the changelog).

**Changes since 0.10.0**
- Remove remaining obsolete descriptor API usages.
- Update to Kotlin `1.6.10`.

0.10.0
------

_2021-12-20_

This release introduces formal support for Kotlin multiplatform!

There are two parts to this:
- The compiler plugin itself supports all compilation types, not just JVM and Android.
- The first-party annotations artifact is now multiplatform.

The legacy backend support is now removed, IR is required going forward.

0.10.0-RC1
----------

_2021-11-21_

This is a release candidate with support for Kotlin multiplatform. Please test this out and report any issues.

There are two parts to this:
- The compiler plugin itself supports all compilation types, not just JVM and Android.
- The first-party annotations artifact is now multiplatform.

The legacy backend support is now removed, IR is required going forward.

0.9.0
------------

_2021-11-16_

* Formal support for Kotlin 1.6. This plugin requires Kotlin 1.6 now.

0.8.3
------------

_2021-10-15_

* Support for Kotlin 1.6 (built against `1.6.0-RC`). This release should only be used for testing with Kotlin 1.6 previews.

0.8.2
------------

_2021-10-12_

This release was accidentally broken and should not be used! Specifically, the gradle plugin accidentally targeted Java 17.

0.8.1
------------

This release was accidentally broken and should not be used! Specifically, the compiler plugin itself was missing a service file and would not run.

0.8.0
------------

_2021-02-05_

* **Change:** The gradle plugin id is now `dev.zacsweers.redacted`.
* Support (and requires) Kotlin 1.4.30.

_There may be 0.7.x versions on sonatype. Don't use them, they are broken._

0.6.1
------------

_2021-01-10_

* Small IR fixes courtesy of [@DrewHamilton](https://github.com/DrewHamilton)

0.6.0
------------

_2021-01-09_

* Gradle plugin extension now uses modern Gradle `Property` APIs. Minimum Gradle version is now 5.1.
* Android-specific APIs (i.e. `variantFilter`) are now removed. If you still want this, consider
configuring this manually in your own build however you see fit in tandem with the `enabled` property.
* **Fix:** IR supports redacted classes now too.

0.5.0
------------

_2021-01-08_

* **New!** Experimental support for Kotlin IR. Note that IR is still incubating and subject to change.
* Update Kotlin to `1.4.21`.
* Tested up to JDK 15.

0.4.0
------------

_2020-11-27_

**This version requires Kotlin 1.4.20 or higher!**

* Compatible with Kotlin 1.4.20
* Supports Kotlin's new `StringConcatFactory` automatically when `-Xstring-concat` is enabled.
  * See the [Kotlin 1.4.20 announcement post](https://blog.jetbrains.com/kotlin/2020/11/kotlin-1-4-20-released/) for details on `-Xstring-concat`.

0.3.0
------------

_2020-08-15_

This is identical in functionality to 0.2.0 but built against Kotlin 1.4.

0.2.0-1.4
------------

_2020-07-16_

This is identical in functionality to 0.2.0 but built against Kotlin 1.4. This version is not considered
stable and should only be used for testing compatibility with Kotlin 1.4 pre-releases.

0.2.0
-----

_2020-01-25_

**New:** There's now a batteries-included `redacted-compiler-plugin-annotations` artifact with a `@Redacted`
annotation you can use if you don't want to manage your own. The `redacted` Gradle extension now defaults
to this and will automatically add this annotations artifact to your project dependencies if a custom
one isn't set. This means that, if you use the default, then all you need to do to use this plugin
is simply apply the gradle plugin 🎉.

0.1.0
-----

_2020-01-24_

Initial release!
