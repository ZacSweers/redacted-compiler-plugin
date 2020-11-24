redacted-compiler-plugin
========================

A Kotlin compiler plugin that generates redacted `toString()` implementations.

Inspired by the [`auto-value-redacted`](https://github.com/square/auto-value-redacted) extension for AutoValue.

## Usage

Include the gradle plugin in your project, define a `@Redacted` annotation, and apply it to any 
properties that you wish to redact.

```kotlin
@Retention(SOURCE)
@Target(PROPERTY)
annotation class Redacted

data class User(val name: String, @Redacted val phoneNumber: String)
```

When you call `toString()` any `@Redacted` properties are hidden:

```
User(name=Bob, phoneNumber=██)
```

If your annotation is applied to the class, then `toString()` will emit a single replacement string:

```kotlin
@Retention(SOURCE)
@Target(CLASS)
annotation class Redacted

@Redacted
data class SensitiveData(val ssn: String, val birthday: String)
```

```
SensitiveData(██)
```

## Installation

Apply the gradle plugin.

```gradle
buildscript {
  dependencies {
    classpath "dev.zacsweers.redacted:redacted-compiler-plugin-gradle:x.y.z"
  }  
}

apply plugin: 'dev.zacsweers.redacted.redacted-gradle-plugin'
```

And that's it! The default configuration will add the `-annotations` artifact (which has a
`@Redacted` annotation you can use) and wire it all automatically. Just annotate what you want to
redact.

You can configure custom behavior with properties on the `redacted` extension.

```
redacted {
  // Define a custom annotation. The -annotations artifact won't be automatically added to
  // dependencies if you define your own!
  redactedAnnotation = "dev.zacsweers.redacted.annotations.Redacted" // Default

  // Define whether or not this is enabled. Useful if you want to gate this behind a dynamic
  // build configuration.
  enabled = true // Default

  // Define a custom replacement string for redactions.
  replacementString = "██" // Default

  // Define whether you want to redact every data class regardless of the @Redact annotation. This 
  // will redact as if you annotated each class
  redactAllDataClasses = false // Default
}
```

### Android Per-Variant Configuration

If using Android, you can optionally configure the plugin to be applied on a per-variant basis via
`androidVariantFilter`. This is similar to the Android Gradle Plugin's native `variantFilter` API,
except with an `overrideEnabled` function to override the enabled status and there is no
`defaultConfig` property. If not overridden, the default is to match the `redacted` extension's value.
Other `VariantFilter` APIs should behave as expected (buildType, flavors, name, etc).

**Note:** Variants with different `enabled` values will have to be compiled separately. This is common
in most multi-variant projects anyway, but something to be aware of.

```groovy
redacted {
  enabled = true // Default
  redactAllDataClasses = false // Default
  androidVariantFilter {
    // Don't enable on debug
    if (buildType.name == "debug") {
      overrideEnabled(false)
    }
  }
}
```

Snapshots of the development version are available in [Sonatype's `snapshots` repository][snapshots].

## Caveats

- Kotlin compiler plugins are not a stable API! Compiled outputs from this plugin _should_ be stable,
but usage in newer versions of kotlinc are not guaranteed to be stable.
  - Doesn't support Kotlin IR yet. PRs welcome if anyone is ambitious [#22](https://github.com/ZacSweers/redacted-compiler-plugin/issues/22)
- IDE support is not currently possible. See [#8](https://github.com/ZacSweers/redacted-compiler-plugin/issues/8).

License
-------

    Copyright (C) 2018 Zac Sweers

    Licensed under the Apache License, Version 2.0 (the "License");
    you may not use this file except in compliance with the License.
    You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License.

 [snapshots]: https://oss.sonatype.org/content/repositories/snapshots/
