Changelog
=========

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
